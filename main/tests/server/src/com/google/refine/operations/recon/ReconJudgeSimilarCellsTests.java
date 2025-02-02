/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.operations.recon;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconJudgeSimilarCellsOperation;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ReconJudgeSimilarCellsTests extends RefineTest {

    static final EngineConfig ENGINE_CONFIG = EngineConfig.reconstruct("{\"mode\":\"row-based\"}}");

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation(getCoreModule(), "recon-judge-similar-cells", ReconJudgeSimilarCellsOperation.class);
    }

    @Test
    public void serializeReconJudgeSimilarCellsOperation() throws IOException {
        String json = "{\"op\":\"core/recon-judge-similar-cells\","
                + "\"description\":\"Mark to create one single new item for all cells containing \\\"foo\\\" in column A\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"A\","
                + "\"similarValue\":\"foo\","
                + "\"judgment\":\"new\","
                + "\"shareNewTopics\":true}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconJudgeSimilarCellsOperation.class), json);
    }

    @Test
    public void serializeReconJudgeSimilarCellsOperationMatch() throws IOException {
        String json = "{\"op\":\"core/recon-judge-similar-cells\","
                + "\"description\":\"Match item Douglas Adams (Q42) for cells containing \\\"foo\\\" in column A\","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"A\","
                + "\"similarValue\":\"foo\","
                + "\"judgment\":\"matched\","
                + "\"match\":{\"id\":\"Q42\",\"name\":\"Douglas Adams\",\"types\":[\"Q5\"],\"score\":85},"
                + "\"shareNewTopics\":false"
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconJudgeSimilarCellsOperation.class), json);
    }

    @Test
    public void testMarkNewTopics() throws Exception {
        Project project = createCSVProject(
                "A,B\n"
                        + "foo,bar\n"
                        + "alpha,beta\n");

        Column column = project.columnModel.columns.get(0);
        ReconConfig config = new StandardReconConfig(
                "http://my.database/recon_service",
                "http://my.database/entity/",
                "http://my.database/schema/",
                null,
                null,
                true, 10, Collections.emptyList());
        column.setReconConfig(config);

        AbstractOperation op = new ReconJudgeSimilarCellsOperation(
                ENGINE_CONFIG,
                "A",
                "foo",
                Recon.Judgment.New,
                null, true);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        Cell cell = project.rows.get(0).cells.get(0);
        assertEquals(Recon.Judgment.New, cell.recon.judgment);
        assertEquals("http://my.database/entity/", cell.recon.identifierSpace);
        assertNull(project.rows.get(1).cells.get(0).recon);
    }
}
