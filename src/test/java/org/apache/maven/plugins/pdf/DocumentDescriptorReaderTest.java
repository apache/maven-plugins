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


import java.io.File;

import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.plugins.pdf.stubs.FilteringMavenProjectStub;

import org.codehaus.plexus.PlexusTestCase;

/**
 *
 * @author ltheussl
 * @version $Id$
 */
public class DocumentDescriptorReaderTest
        extends PlexusTestCase
{
/**
     * Test of readAndFilterDocumentDescriptor method, of class DocumentDescriptorReader.
     * @throws Exception if something happens.
     */
    public void testReaderNoProject()
            throws Exception
    {
        DocumentDescriptorReader reader = new DocumentDescriptorReader();
        File descriptorFile = new File( testBaseDir() + "src/site/", "model_builder_site.xml" );
        DocumentModel model = reader.readAndFilterDocumentDescriptor( descriptorFile );
        assertNotNull( model );
        assertNull( model.getCover() );
        assertNull( model.getMeta() );
        assertNull( model.getToc() );
    }

    /**
     * Test of readAndFilterDocumentDescriptor method, of class DocumentDescriptorReader.
     * @throws Exception if something happens.
     */
    public void testFiltering()
            throws Exception
    {
        DocumentDescriptorReader reader = new DocumentDescriptorReader( new FilteringMavenProjectStub() );
        File descriptorFile = new File( testBaseDir() + "src/site/", "pdf_filtering.xml" );
        DocumentModel model = reader.readAndFilterDocumentDescriptor( descriptorFile );
        assertNotNull( model );
        assertNull( model.getCover() );
        assertNotNull( model.getToc() );
        assertEquals( "Table of Contents", model.getToc().getName() );
        assertEquals( 5, model.getToc().getItems().size() );
        assertNotNull( model.getMeta() );
        assertTrue( model.getMeta().getTitle().indexOf(
                "User guide in en of Test filtering version 1.0-SNAPSHOT" ) == 0 );
        assertEquals( "vsiveton@apache.org ltheussl@apache.org", model.getMeta().getAuthor() );
    }

    private String testBaseDir()
    {
        return getBasedir() + "/src/test/resources/unit/pdf/";
    }
}
