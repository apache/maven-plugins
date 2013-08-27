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
import java.io.IOException;
import java.io.Reader;

import org.apache.maven.doxia.document.DocumentAuthor;
import org.apache.maven.doxia.document.DocumentCover;
import org.apache.maven.doxia.document.DocumentMeta;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOC;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.plugins.pdf.stubs.ModelBuilderMavenProjectStub;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author ltheussl
 * @version $Id$
 */
public class DocumentModelBuilderTest
    extends PlexusTestCase
{
    /**
     * Test of getDocumentModel method, of class DocumentModelBuilder.
     */
    public void testEmptyDocumentModel()
    {
        DocumentModel model = new DocumentModelBuilder( null ).getDocumentModel();

        assertNotNull( model );
        assertNull( model.getModelEncoding() );
        assertEquals( "unnamed", model.getOutputName() );
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
        assertEquals( "Test Organization", cover.getCompanyName() );
        assertEquals( 2, cover.getAuthors().size() );
        assertFirstDocumentAuthor( cover.getAuthors().get( 0 ) );

        DocumentMeta meta = model.getMeta();
        assertEquals( "Test Description", meta.getDescription() );
        assertEquals( 2, meta.getAuthors().size() );
        assertFirstDocumentAuthor( meta.getAuthors().get( 0 ) );
        assertEquals( "Test Name", meta.getSubject() );
        assertEquals( "Test Name", meta.getTitle() );

        DocumentTOC toc = model.getToc();
        assertEquals( 0, toc.getItems().size() );
    }

    /**
     * Test of getDocumentModel method, of class DocumentModelBuilder.
     *
     * @throws Exception if something happens.
     */
    public void testGetDocumentModelWithSiteDescriptor()
        throws Exception
    {
        File descriptorFile = new File( testBaseDir() + "src/site/", "model_builder_site.xml" );
        DecorationModel dModel = getDecorationModelFromFile( descriptorFile );
        DocumentModel model = new DocumentModelBuilder( new ModelBuilderMavenProjectStub(), dModel ).getDocumentModel();

        DocumentTOC toc = model.getToc();
        assertEquals( 1, toc.getItems().size() );
        assertEquals( "Intro", toc.getItems().get( 0 ).getName() );
        assertEquals( "index.html", toc.getItems().get( 0 ).getRef() );
    }

    private void assertFirstDocumentAuthor( DocumentAuthor author )
    {
        assertEquals( "dev name", author.getName() );
        assertEquals( "dev@email", author.getEmail() );
        assertEquals( "dev broetchengeber", author.getCompanyName() );
        assertEquals( "dev main role, dev second role", author.getPosition() );
    }

    private DecorationModel getDecorationModelFromFile( File descriptorFile )
        throws IOException, XmlPullParserException
    {
        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( descriptorFile );

            return new DecorationXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private String testBaseDir()
    {
        return getBasedir() + "/src/test/resources/unit/pdf/";
    }
}
