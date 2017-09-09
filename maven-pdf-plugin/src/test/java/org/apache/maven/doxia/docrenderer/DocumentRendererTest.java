package org.apache.maven.doxia.docrenderer;

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
import java.util.List;

import org.apache.maven.doxia.docrenderer.pdf.PdfRenderer;
import org.apache.maven.doxia.document.DocumentModel;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: DocumentRendererTest.java 1185508 2011-10-18 06:58:50Z ltheussl $
 * @since 1.1.1
 */
public class DocumentRendererTest
    extends PlexusTestCase
{
    private PdfRenderer docRenderer;

    private File siteDirectoryFile;

    /** @throws java.lang.Exception */
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        siteDirectoryFile = getTestFile( "src/test/resources/site" );
    }

    /** @throws java.lang.Exception */
    @Override
    protected void tearDown()
        throws Exception
    {
        release( docRenderer );
        super.tearDown();
    }

    /** @throws java.lang.Exception */
    public void testFo()
        throws Exception
    {
        renderImpl( "fo" );
    }

    /** @throws java.lang.Exception */
    public void testFoAggregate()
        throws Exception
    {
        renderAggregatedImpl( "fo" );
    }

    /** @throws java.lang.Exception */
    public void testIText()
        throws Exception
    {
        renderImpl( "itext" );
    }

    /** @throws java.lang.Exception */
    public void testITextAggregate()
        throws Exception
    {
        renderAggregatedImpl( "itext" );
    }

    @SuppressWarnings ( "unchecked" )
    private void renderImpl( String implementation )
        throws Exception
    {
        File outputDirectory = getTestFile( "target/output/" + implementation );
        if ( outputDirectory.exists() )
        {
            FileUtils.deleteDirectory( outputDirectory );
        }
        outputDirectory.mkdirs();

        docRenderer = (PdfRenderer) lookup( PdfRenderer.ROLE, implementation );
        assertNotNull( docRenderer );

        docRenderer.render( siteDirectoryFile, outputDirectory, null );

        @SuppressWarnings ( "unchecked" )
        List<String> files =
            FileUtils.getFileNames( new File( siteDirectoryFile, "apt" ), "**/*.apt",
                                    FileUtils.getDefaultExcludesAsString(), false );
        files.addAll( FileUtils.getFileNames( new File( siteDirectoryFile, "fml" ), "**/*.fml",
                                              FileUtils.getDefaultExcludesAsString(), false ) );
        files.addAll( FileUtils.getFileNames( new File( siteDirectoryFile, "xdoc" ), "**/*.xml",
                                              FileUtils.getDefaultExcludesAsString(), false ) );

        for ( String relativeFile : files )
        {
            String relativePdf = StringUtils.replace( relativeFile, FileUtils.getExtension( relativeFile ), "pdf" );
            File pdf = new File( outputDirectory, relativePdf );

            assertTrue( pdf.exists() );
            assertTrue( pdf.length() > 0 );
        }
    }

    private void renderAggregatedImpl( String implementation )
        throws Exception
    {
        File outputDirectory = getTestFile( "target/output/" + implementation + "-aggregated" );
        if ( outputDirectory.exists() )
        {
            FileUtils.deleteDirectory( outputDirectory );
        }
        outputDirectory.mkdirs();

        docRenderer = (PdfRenderer) lookup( PdfRenderer.ROLE, implementation );
        assertNotNull( docRenderer );

        DocumentModel descriptor = docRenderer.readDocumentModel( new File( siteDirectoryFile, "pdf.xml" ) );
        assertNotNull( descriptor );

        docRenderer.render( siteDirectoryFile, outputDirectory, descriptor );

        File pdf = new File( outputDirectory, descriptor.getOutputName() + ".pdf" );

        assertTrue( pdf.exists() );
        assertTrue( pdf.length() > 0 );
    }
}
