package org.apache.maven.plugin.assembly.format;

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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class FileSetFormatterTest
    extends PlexusTestCase
{

    private MockManager mockManager;

    private TestFileManager fileManager;

    private Logger logger;

    private AssemblerConfigurationSource configSource;

    private MockControl configSourceControl;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        mockManager = new MockManager();

        fileManager = new TestFileManager( "fileSetFormatter-test.", "" );

        logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        configSourceControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceControl );

        configSource = (AssemblerConfigurationSource) configSourceControl.getMock();
    }

    @Override
    public void tearDown() throws IOException
    {
        fileManager.cleanUp();
    }

    public void testShouldReturnOriginalUnalteredDirectoryWhenLineEndingIsNull()
        throws AssemblyFormattingException, IOException
    {
        final FileSet fs = new FileSet();

        final FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        final File dir = fileManager.createTempDir();

        final File result = formatter.formatFileSetForAssembly( dir, fs );

        assertEquals( dir, result );
    }

    public void testShouldReturnOriginalUnalteredDirectoryWhenLineEndingIsKeep()
        throws AssemblyFormattingException, IOException
    {
        final FileSet fs = new FileSet();
        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_KEEP );

        final FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        final File dir = fileManager.createTempDir();

        final File result = formatter.formatFileSetForAssembly( dir, fs );

        assertEquals( dir, result );
    }

    public void testShouldReturnOriginalUnalteredDirectoryWhenIncludedFileSetIsEmpty()
        throws AssemblyFormattingException, IOException
    {
        final File dir = fileManager.createTempDir();

        final FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_LF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addExclude( "**/*" );

        final FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        final File result = formatter.formatFileSetForAssembly( dir, fs );

        assertEquals( dir, result );
    }

    public void testShouldConvertLineEndingsOnTwoFiles() throws AssemblyFormattingException, IOException
    {
        final File dir = fileManager.createTempDir();

        final String filename1 = "one.txt";
        final String filename2 = "two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        final FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addInclude( "**/*.txt" );

        final FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( dir );

        configSource.getEncoding();
        configSourceControl.setReturnValue( "UTF-8", MockControl.ONE_OR_MORE );

        mockManager.replayAll();
        
        final File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        try
        {
            fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test." );
            fileManager.assertFileContents( result, filename2, "Hello\r\nThis is also a test." );
        }
        finally
        {
            FileUtils.deleteDirectory( result );
        }
    }

    public void testShouldConvertLineEndingsOnOneFileWithAnotherExplicitlyExcluded()
        throws AssemblyFormattingException, IOException
    {
        final File dir = fileManager.createTempDir();

        final String filename1 = "one.txt";
        final String filename2 = "two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        final FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addExclude( "**/two.txt" );

        final FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( dir );

        configSource.getEncoding();
        configSourceControl.setReturnValue( "UTF-8", MockControl.ONE_OR_MORE );

        mockManager.replayAll();
        
        final File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        try
        {
            fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test." );
            fileManager.assertFileExistence( result, filename2, false );
        }
        finally
        {
            FileUtils.deleteDirectory( result );
        }
    }

    public void testShouldConvertLineEndingsOnOneExplicitlyIncludedFile()
        throws AssemblyFormattingException, IOException
    {
        final File dir = fileManager.createTempDir();

        final String filename1 = "one.txt";
        final String filename2 = "two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        final FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addInclude( "**/one.txt" );

        final FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( dir );

        configSource.getEncoding();
        configSourceControl.setReturnValue( "UTF-8", MockControl.ONE_OR_MORE );

        mockManager.replayAll();
        
        final File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );
        try
        {
            fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test." );
            fileManager.assertFileExistence( result, filename2, false );
        }
        finally
        {
            FileUtils.deleteDirectory( result );
        }
    }

    public void testShouldConvertLineEndingsOnOneFileAndIgnoreFileWithinDefaultExcludedDir()
        throws AssemblyFormattingException, IOException
    {
        final File dir = fileManager.createTempDir();

        final String filename1 = "one.txt";
        final String filename2 = "CVS/two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        final FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );

        final FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( dir );

        configSource.getEncoding();
        configSourceControl.setReturnValue( "UTF-8", MockControl.ONE_OR_MORE );

        mockManager.replayAll();
        
        final File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        try
        {
            fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test." );
            fileManager.assertFileExistence( result, filename2, false );
        }
        finally
        {
            FileUtils.deleteDirectory( result );
        }
    }

    public void testShouldFilterSeveralFiles() throws Exception
    {
        final File basedir = fileManager.createTempDir();

        final String filename1 = "one.txt";
        final String filename2 = "two.txt";

        // this file will be filtered with a project expression
        fileManager.createFile( basedir, filename1, "This is the filtered artifactId: ${project.artifactId}." );
        // this one fill be filtered with a filter file
        fileManager.createFile( basedir, filename2, "This is the filtered 'foo' property: ${foo}." );
        final File filterProps = fileManager.createFile( basedir, "filter.properties", "foo=bar" );

        final FileSet fs = new FileSet();
        fs.setFiltered( true );
        fs.setDirectory( basedir.getCanonicalPath() );
        fs.addInclude( "**/*.txt" );

        enableBasicFilteringConfiguration( basedir, Collections.singletonList( filterProps.getCanonicalPath() ) );

        mockManager.replayAll();

        final FileSetFormatter formatter = new FileSetFormatter( configSource, logger );
        final File result = formatter.formatFileSetForAssembly( basedir, fs );

        assertFalse( result.equals( basedir ) );

        try
        {
            fileManager.assertFileContents( result, filename1, "This is the filtered artifactId: artifact." );
            fileManager.assertFileContents( result, filename2, "This is the filtered 'foo' property: bar." );

            mockManager.verifyAll();
        }
        finally
        {
            FileUtils.deleteDirectory( result );
        }
    }

    private void enableBasicFilteringConfiguration( final File basedir, final List<String> filterFilenames )
        throws Exception
    {
        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        final Model model = new Model();
        model.setArtifactId( "artifact" );
        model.setGroupId( "group" );
        model.setVersion( "version" );

        final MavenProject project = new MavenProject( model );
        project.getBuild()
               .setFilters( filterFilenames );

        configSource.getProject();
        configSourceControl.setReturnValue( project, MockControl.ONE_OR_MORE );

        configSource.getMavenFileFilter();
        configSourceControl.setReturnValue( lookup( "org.apache.maven.shared.filtering.MavenFileFilter" ),
                                            MockControl.ONE_OR_MORE );

        configSource.getMavenSession();
        configSourceControl.setReturnValue( null, MockControl.ONE_OR_MORE );

        configSource.getFilters();
        configSourceControl.setReturnValue( Collections.EMPTY_LIST, MockControl.ONE_OR_MORE );

        configSource.getEncoding();
        configSourceControl.setReturnValue( "UTF-8", MockControl.ONE_OR_MORE );

        configSource.getEscapeString();
        configSourceControl.setReturnValue( null, MockControl.ONE_OR_MORE );
    }

}
