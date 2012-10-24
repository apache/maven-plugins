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

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileFormatterTest
    extends PlexusTestCase
{

    private final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

    private final TestFileManager fileManager = new TestFileManager( "fileFormatterTest.", "" );

    private final MockManager mockManager = new MockManager();

    private AssemblerConfigurationSource configSource;

    private MockControl configSourceControl;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        configSourceControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceControl );

        configSource = (AssemblerConfigurationSource) configSourceControl.getMock();
    }

    @Override
    public void tearDown() throws IOException
    {
        fileManager.cleanUp();
    }

    public void testTemporaryRootDirectoryNotExist() throws IOException, AssemblyFormattingException
    {
        final File basedir = fileManager.createTempDir();
        final File tempRoot = new File( basedir, "tempdir" );
        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( tempRoot );

        final File file = fileManager.createFile( basedir, "one.txt", "This is a\ntest." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, false, "dos", "UTF-8" );

        assertTrue( !file.equals( result ) );

        mockManager.verifyAll();
    }

    public void testShouldNotTransformOneFile() throws IOException, AssemblyFormattingException
    {
        final File basedir = fileManager.createTempDir();

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        // Model model = new Model();
        // model.setArtifactId( "artifact" );
        // model.setGroupId( "group" );
        // model.setVersion( "version" );
        //
        // MavenProject project = new MavenProject( model );
        //
        // configSource.getProject();
        // configSourceControl.setReturnValue( project );

        final File file = fileManager.createFile( basedir, "one.txt", "This is a test." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, false, null, "UTF-8" );

        assertEquals( file, result );

        mockManager.verifyAll();
    }

    // TODO: Should not be appending line-ending at the end if there is none in the source.
    public void testShouldConvertCRLFLineEndingsInFile() throws IOException, AssemblyFormattingException
    {
        final File basedir = fileManager.createTempDir();

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        final File file = fileManager.createFile( basedir, "one.txt", "This is a\ntest." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, false, "dos", "UTF-8" );

        assertEquals( "This is a\r\ntest.\r\n", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    // TODO: Should not be appending line-ending at the end if there is none in the source.
    public void testShouldConvertLFLineEndingsInFile() throws IOException, AssemblyFormattingException
    {
        final File basedir = fileManager.createTempDir();

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        final File file = fileManager.createFile( basedir, "one.txt", "This is a\r\ntest." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, false, "unix", "UTF-8" );

        assertEquals( "This is a\ntest.\n", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterProjectExpressionInFile() throws Exception
    {
        final File basedir = fileManager.createTempDir();

        enableBasicFilteringConfiguration( basedir, null );

        final File file =
            fileManager.createFile( basedir, "one.txt", "This is a test for project: ${artifactId} @artifactId@." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        assertEquals( "This is a test for project: artifact artifact.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionInPropertiesFileWithWindowsEscapes() throws Exception
    {

        final File sourceDir = fileManager.createTempDir();
        final MavenProject project = createBasicMavenProject();
        final Build build = new Build();

        // project.build.outputDirectory = C:\out\deeper
        build.setOutputDirectory( "C:\\out\\deeper" );
        project.setBuild( build );

        enableBasicFilteringConfiguration( project, sourceDir );

        final File file = fileManager.createFile( sourceDir, "one.properties", "out=${project.build.outputDirectory}" );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        // expect: C:\\out\\deeper
        assertEquals( "out=C:\\\\out\\\\deeper", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionInPropertiesFileWithoutWindowsEscapes() throws Exception
    {

        final File sourceDir = fileManager.createTempDir();
        final MavenProject project = createBasicMavenProject();
        final Build build = new Build();
        build.setOutputDirectory( "C:\\out\\deeper" );
        project.setBuild( build );

        enableBasicFilteringConfiguration( project, sourceDir );

        final File file =
            fileManager.createFile( sourceDir, "one.txt", "project.basedirA=${project.build.outputDirectory}" );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        assertEquals( "project.basedirA=C:\\out\\deeper", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionFromFiltersFileInFile() throws Exception
    {
        final File basedir = fileManager.createTempDir();

        final File filterProps = fileManager.createFile( basedir, "filter.properties", "property=Test" );

        enableBasicFilteringConfiguration( basedir, Collections.singletonList( filterProps.getCanonicalPath() ) );

        final File file =
            fileManager.createFile( basedir, "one.txt", "This is a test for project: ${property} @property@." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        assertEquals( "This is a test for project: Test Test.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionFromFiltersFileInPropertiesFileWithoutWindowsEscapes() throws Exception
    {
        final File basedir = fileManager.createTempDir();

        final File filterProps = fileManager.createFile( basedir, "filter.properties", "property=C:\\\\Test" );

        enableBasicFilteringConfiguration( basedir, Collections.singletonList( filterProps.getCanonicalPath() ) );

        final File file = fileManager.createFile( basedir, "one.properties", "project: ${property} @property@." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        assertEquals( "project: C:\\\\Test C:\\\\Test.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionFromFiltersFileInNonPropertiesFileWithoutWindowsEscapes() throws Exception
    {
        final File basedir = fileManager.createTempDir();

        final File filterProps = fileManager.createFile( basedir, "filter.properties", "property=C:\\\\Test" );

        enableBasicFilteringConfiguration( basedir, Collections.singletonList( filterProps.getCanonicalPath() ) );

        final File file =
            fileManager.createFile( basedir, "one.txt", "This is a test for project: ${property} @property@." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        assertEquals( "This is a test for project: C:\\Test C:\\Test.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldNotIgnoreFirstWordInDotNotationExpressions() throws Exception
    {
        final File basedir = fileManager.createTempDir();

        enableBasicFilteringConfiguration( basedir, null );

        final File file =
            fileManager.createFile( basedir, "one.txt", "testing ${bean.id} which used to resolve to project.id" );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        assertEquals( "testing ${bean.id} which used to resolve to project.id", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionsFromTwoFiltersFilesInFile() throws Exception
    {
        final File basedir = fileManager.createTempDir();

        final File filterProps = fileManager.createFile( basedir, "filter.properties", "property=Test" );
        final File filterProps2 = fileManager.createFile( basedir, "filter2.properties", "otherProperty=OtherValue" );

        final List<String> filters = new ArrayList<String>();
        filters.add( filterProps.getCanonicalPath() );
        filters.add( filterProps2.getCanonicalPath() );

        enableBasicFilteringConfiguration( basedir, filters );

        final File file =
            fileManager.createFile( basedir, "one.txt",
                                    "property: ${property} @property@ otherProperty: ${otherProperty} @otherProperty@." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        assertEquals( "property: Test Test otherProperty: OtherValue OtherValue.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldOverrideOneFilterValueWithAnotherAndFilterFile() throws Exception
    {
        final File basedir = fileManager.createTempDir();

        final File filterProps = fileManager.createFile( basedir, "filter.properties", "property=Test" );
        final File filterProps2 = fileManager.createFile( basedir, "filter2.properties", "property=OtherValue" );

        final List<String> filters = new ArrayList<String>();
        filters.add( filterProps.getCanonicalPath() );
        filters.add( filterProps2.getCanonicalPath() );

        enableBasicFilteringConfiguration( basedir, filters );

        final File file = fileManager.createFile( basedir, "one.txt", "property: ${property} @property@." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        assertEquals( "property: OtherValue OtherValue.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldOverrideProjectValueWithFilterValueAndFilterFile() throws Exception
    {
        final File basedir = fileManager.createTempDir();

        final File filterProps = fileManager.createFile( basedir, "filter.properties", "artifactId=Test" );

        final List<String> filters = new ArrayList<String>();
        filters.add( filterProps.getCanonicalPath() );

        enableBasicFilteringConfiguration( basedir, filters );

        final File file =
            fileManager.createFile( basedir, "one.txt", "project artifact-id: ${artifactId} @artifactId@." );

        mockManager.replayAll();

        final File result = new FileFormatter( configSource, logger ).format( file, true, null, "UTF-8" );

        assertEquals( "project artifact-id: Test Test.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    private MavenProject createBasicMavenProject()
    {
        final Model model = new Model();
        model.setArtifactId( "artifact" );
        model.setGroupId( "group" );
        model.setVersion( "version" );

        return new MavenProject( model );
    }

    private void enableBasicFilteringConfiguration( final MavenProject project, final File basedir ) throws Exception
    {
        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        configSource.getEscapeString();
        configSourceControl.setReturnValue( null, MockControl.ONE_OR_MORE );

        configSource.getProject();
        configSourceControl.setReturnValue( project, MockControl.ONE_OR_MORE );

        configSource.getMavenFileFilter();
        configSourceControl.setReturnValue( lookup( "org.apache.maven.shared.filtering.MavenFileFilter" ) );

        configSource.getMavenSession();
        configSourceControl.setReturnValue( null );

        configSource.getFilters();
        configSourceControl.setReturnValue( Collections.EMPTY_LIST );
    }

    private void enableBasicFilteringConfiguration( final File basedir, final List<String> filterFilenames )
        throws Exception
    {
        final MavenProject project = createBasicMavenProject();
        if ( filterFilenames != null )
        {
            project.getBuild()
                   .setFilters( filterFilenames );
        }

        enableBasicFilteringConfiguration( project, basedir );
    }

}
