package org.apache.maven.plugins.pdf;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;

import org.apache.maven.doxia.document.DocumentCover;
import org.apache.maven.doxia.document.DocumentMeta;
import org.apache.maven.doxia.document.DocumentModel;

import org.apache.maven.doxia.document.DocumentTOC;
import org.apache.maven.plugins.pdf.stubs.ModelBuilderMavenProjectStub;

/**
 *
 * @author ltheussl
 * @version $Id$
 */
public class DocumentModelBuilderTest
        extends TestCase
{
    /**
     * Test of getDocumentModel method, of class DocumentModelBuilder.
     */
    public void testEmptyDocumentModel()
    {
        DocumentModel model = new DocumentModelBuilder( null ).getDocumentModel();

        assertNotNull( model );
        assertNull( model.getModelEncoding() );
        assertNull( model.getOutputName() );
        assertNotNull( model.getCover() );
        assertNotNull( model.getMeta() );
        assertNotNull( model.getToc() );
    }

    /**
     * Test of getDocumentModel method, of class DocumentModelBuilder.
     */
    public void testGetDocumentModel()
    {
        DocumentModel model = new DocumentModelBuilder( new ModelBuilderMavenProjectStub() ).getDocumentModel();

        assertEquals( "ISO-8859-1", model.getModelEncoding() );
        assertEquals( "Test ArtifactId", model.getOutputName() );

        DocumentCover cover = model.getCover();
        assertEquals( "Test Version", cover.getCoverVersion() );
        assertEquals( "Test Name", cover.getProjectName() );
        assertEquals( "Test Name", cover.getCoverTitle() );
        assertEquals( "v. Test Version", cover.getCoverSubTitle() );
        assertEquals( 0, cover.getAuthors().size() );

        DocumentMeta meta = model.getMeta();
        assertEquals( "Test Description", meta.getDescription() );
        assertEquals( 0, meta.getAuthors().size() );
        assertEquals( "Test Name", meta.getSubject() );
        assertEquals( "Test Name", meta.getTitle() );

        DocumentTOC toc = model.getToc();
        assertEquals( 0, toc.getItems().size() );
    }
}
