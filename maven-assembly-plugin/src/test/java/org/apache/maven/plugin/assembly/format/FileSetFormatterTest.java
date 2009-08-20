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

import junit.framework.TestCase;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.PlexusTestCase;
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

    public void setUp()
        throws Exception
    {
        super.setUp();

        mockManager = new MockManager();

        fileManager = new TestFileManager( "fileSetFormatter-test.", "" );

        logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        configSourceControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceControl );

        configSource = (AssemblerConfigurationSource) configSourceControl.getMock();
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testShouldReturnOriginalUnalteredDirectoryWhenLineEndingIsNull()
        throws AssemblyFormattingException, IOException
    {
        FileSet fs = new FileSet();

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File dir = fileManager.createTempDir();

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertEquals( dir, result );
    }

    public void testShouldReturnOriginalUnalteredDirectoryWhenLineEndingIsKeep()
        throws AssemblyFormattingException, IOException
    {
        FileSet fs = new FileSet();
        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_KEEP );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File dir = fileManager.createTempDir();

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertEquals( dir, result );
    }

    public void testShouldReturnOriginalUnalteredDirectoryWhenIncludedFileSetIsEmpty()
        throws AssemblyFormattingException, IOException
    {
        File dir = fileManager.createTempDir();

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_LF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addExclude( "**/*" );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertEquals( dir, result );
    }

    public void testShouldConvertLineEndingsOnTwoFiles()
        throws AssemblyFormattingException, IOException
    {
        File dir = fileManager.createTempDir();

        String filename1 = "one.txt";
        String filename2 = "two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addInclude( "**/*.txt" );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        try
        {
            fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test.\r\n" );
            fileManager.assertFileContents( result, filename2, "Hello\r\nThis is also a test.\r\n" );
        }
        finally
        {
            FileUtils.deleteDirectory( result );
        }
    }

    public void testShouldConvertLineEndingsOnOneFileWithAnotherExplicitlyExcluded()
        throws AssemblyFormattingException, IOException
    {
        File dir = fileManager.createTempDir();

        String filename1 = "one.txt";
        String filename2 = "two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addExclude( "**/two.txt" );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        try
        {
            fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test.\r\n" );
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
        File dir = fileManager.createTempDir();

        String filename1 = "one.txt";
        String filename2 = "two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addInclude( "**/one.txt" );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );
        try
        {
            fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test.\r\n" );
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
        File dir = fileManager.createTempDir();

        String filename1 = "one.txt";
        String filename2 = "CVS/two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        try
        {
            fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test.\r\n" );
            fileManager.assertFileExistence( result, filename2, false );
        }
        finally
        {
            FileUtils.deleteDirectory( result );
        }
    }

    public void testShouldFilterSeveralFiles()
        throws Exception
    {
        File basedir = fileManager.createTempDir();

        String filename1 = "one.txt";
        String filename2 = "two.txt";

        // this file will be filtered with a project expression
        fileManager.createFile( basedir, filename1, "This is the filtered artifactId: ${project.artifactId}." );
        // this one fill be filtered with a filter file
        fileManager.createFile( basedir, filename2, "This is the filtered 'foo' property: ${foo}." );
        File filterProps = fileManager.createFile( basedir, "filter.properties", "foo=bar" );

        FileSet fs = new FileSet();
        fs.setFiltered( true );
        fs.setDirectory( basedir.getCanonicalPath() );
        fs.addInclude( "**/*.txt" );

        enableBasicFilteringConfiguration( basedir, Collections.singletonList( filterProps.getCanonicalPath() ) );

        mockManager.replayAll();

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );
        File result = formatter.formatFileSetForAssembly( basedir, fs );

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

    private void enableBasicFilteringConfiguration( File basedir, List filterFilenames )
        throws Exception
    {
        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        Model model = new Model();
        model.setArtifactId( "artifact" );
        model.setGroupId( "group" );
        model.setVersion( "version" );

        MavenProject project = new MavenProject( model );
        project.getBuild().setFilters( filterFilenames );

        configSource.getProject();
        configSourceControl.setReturnValue( project, MockControl.ONE_OR_MORE );

        configSource.getMavenFileFilter();
        configSourceControl.setReturnValue( lookup( "org.apache.maven.shared.filtering.MavenFileFilter" ), MockControl.ONE_OR_MORE );

        configSource.getMavenSession();
        configSourceControl.setReturnValue( null, MockControl.ONE_OR_MORE );
    }

}
