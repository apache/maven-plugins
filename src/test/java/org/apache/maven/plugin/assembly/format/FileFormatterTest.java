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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.PlexusTestCase;
import org.easymock.MockControl;

public class FileFormatterTest
    extends PlexusTestCase
{

    private Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

    private TestFileManager fileManager = new TestFileManager( "fileFormatterTest.", "" );

    private MockManager mockManager = new MockManager();

    private AssemblerConfigurationSource configSource;

    private MockControl configSourceControl;

    public void setUp()
        throws Exception
    {
        super.setUp();

        configSourceControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceControl );

        configSource = (AssemblerConfigurationSource) configSourceControl.getMock();
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testTemporaryRootDirectoryNotExist()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();
        File tempRoot = new File(basedir, "tempdir");
        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( tempRoot );

        File file = fileManager.createFile( basedir, "one.txt", "This is a\ntest." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, false, "dos" );

        assertTrue( !file.equals(result) );


        mockManager.verifyAll();
    }

    public void testShouldNotTransformOneFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        //        Model model = new Model();
        //        model.setArtifactId( "artifact" );
        //        model.setGroupId( "group" );
        //        model.setVersion( "version" );
        //
        //        MavenProject project = new MavenProject( model );
        //
        //        configSource.getProject();
        //        configSourceControl.setReturnValue( project );

        File file = fileManager.createFile( basedir, "one.txt", "This is a test." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, false, null );

        assertEquals( file, result );

        mockManager.verifyAll();
    }

    // TODO: Should not be appending line-ending at the end if there is none in the source.
    public void testShouldConvertCRLFLineEndingsInFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        File file = fileManager.createFile( basedir, "one.txt", "This is a\ntest." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, false, "dos" );

        assertEquals( "This is a\r\ntest.\r\n", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    // TODO: Should not be appending line-ending at the end if there is none in the source.
    public void testShouldConvertLFLineEndingsInFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        File file = fileManager.createFile( basedir, "one.txt", "This is a\r\ntest." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, false, "unix" );

        assertEquals( "This is a\ntest.\n", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterProjectExpressionInFile()
        throws Exception
    {
        File basedir = fileManager.createTempDir();

        enableBasicFilteringConfiguration( basedir, Collections.EMPTY_LIST );

        File file = fileManager.createFile( basedir, "one.txt", "This is a test for project: ${artifactId} @artifactId@." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "This is a test for project: artifact artifact.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionInPropertiesFileWithWindowsEscapes()
        throws Exception
    {

       File sourceDir = fileManager.createTempDir();
       MavenProject project = createBasicMavenProject();
       Build build = new Build();
       
       // project.build.outputDirectory = C:\out\deeper
       build.setOutputDirectory( "C:\\out\\deeper" );
       project.setBuild(build);
       
       enableBasicFilteringConfiguration( project, sourceDir );

       File file = fileManager.createFile(sourceDir, "one.properties", "out=${project.build.outputDirectory}");

       mockManager.replayAll();

       File result = new FileFormatter(configSource, logger).format(file, true, null);

       // expect: C\:\\out\\deeper
       assertEquals("out=C\\:\\\\out\\\\deeper",fileManager.getFileContents(result));

       mockManager.verifyAll();
   }

    public void testShouldFilterExpressionInPropertiesFileWithoutWindowsEscapes()
        throws Exception
    {

       File sourceDir = fileManager.createTempDir();
       MavenProject project = createBasicMavenProject();
       Build build = new Build();
       build.setOutputDirectory( "C:\\out\\deeper" );
       project.setBuild(build);
     
       enableBasicFilteringConfiguration( project, sourceDir );

       File file = fileManager.createFile(sourceDir, "one.txt", "project.basedirA=${project.build.outputDirectory}");

       mockManager.replayAll();

       File result = new FileFormatter(configSource, logger).format(file, true, null);

       assertEquals("project.basedirA=C:\\out\\deeper",fileManager.getFileContents(result));

       mockManager.verifyAll();
    }

    
    public void testShouldFilterExpressionFromFiltersFileInFile()
        throws Exception
    {
        File basedir = fileManager.createTempDir();

        File filterProps = fileManager.createFile( basedir, "filter.properties", "property=Test" );

        enableBasicFilteringConfiguration( basedir, Collections.singletonList( filterProps.getCanonicalPath() ) );

        File file = fileManager.createFile( basedir, "one.txt", "This is a test for project: ${property} @property@." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "This is a test for project: Test Test.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }
    
    public void testShouldFilterExpressionFromFiltersFileInPropertiesFileWithoutWindowsEscapes()
        throws Exception
    {
       File basedir = fileManager.createTempDir();

       File filterProps = fileManager.createFile( basedir, "filter.properties", "property=C:\\\\Test" );

       enableBasicFilteringConfiguration( basedir, Collections.singletonList( filterProps.getCanonicalPath() ) );

       File file = fileManager.createFile( basedir, "one.properties", "project: ${property} @property@." );

       mockManager.replayAll();

       File result = new FileFormatter( configSource, logger ).format( file, true, null );

       assertEquals( "project: C\\:\\\\Test C\\:\\\\Test.", fileManager.getFileContents( result ) );

       mockManager.verifyAll();
    }

    public void testShouldFilterExpressionFromFiltersFileInNonPropertiesFileWithoutWindowsEscapes()
        throws Exception
    {
       File basedir = fileManager.createTempDir();

       File filterProps = fileManager.createFile( basedir, "filter.properties", "property=C:\\\\Test" );

       enableBasicFilteringConfiguration( basedir, Collections.singletonList( filterProps.getCanonicalPath() ) );

       File file = fileManager.createFile( basedir, "one.txt", "This is a test for project: ${property} @property@." );

       mockManager.replayAll();

       File result = new FileFormatter( configSource, logger ).format( file, true, null );

       assertEquals( "This is a test for project: C:\\Test C:\\Test.", fileManager.getFileContents( result ) );

       mockManager.verifyAll();
    }

    public void testShouldNotIgnoreFirstWordInDotNotationExpressions()
        throws Exception
    {
        File basedir = fileManager.createTempDir();

        enableBasicFilteringConfiguration( basedir, Collections.EMPTY_LIST );

        File file = fileManager.createFile( basedir, "one.txt", "testing ${bean.id} which used to resolve to project.id" );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "testing ${bean.id} which used to resolve to project.id", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionsFromTwoFiltersFilesInFile()
        throws Exception
    {
        File basedir = fileManager.createTempDir();

        File filterProps = fileManager.createFile( basedir, "filter.properties", "property=Test" );
        File filterProps2 = fileManager.createFile( basedir, "filter2.properties", "otherProperty=OtherValue" );

        List filters = new ArrayList();
        filters.add( filterProps.getCanonicalPath() );
        filters.add( filterProps2.getCanonicalPath() );

        enableBasicFilteringConfiguration( basedir, filters );

        File file = fileManager.createFile( basedir, "one.txt",
                                            "property: ${property} @property@ otherProperty: ${otherProperty} @otherProperty@." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "property: Test Test otherProperty: OtherValue OtherValue.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldOverrideOneFilterValueWithAnotherAndFilterFile()
        throws Exception
    {
        File basedir = fileManager.createTempDir();

        File filterProps = fileManager.createFile( basedir, "filter.properties", "property=Test" );
        File filterProps2 = fileManager.createFile( basedir, "filter2.properties", "property=OtherValue" );

        List filters = new ArrayList();
        filters.add( filterProps.getCanonicalPath() );
        filters.add( filterProps2.getCanonicalPath() );

        enableBasicFilteringConfiguration( basedir, filters );

        File file = fileManager.createFile( basedir, "one.txt", "property: ${property} @property@." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "property: OtherValue OtherValue.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldOverrideProjectValueWithFilterValueAndFilterFile()
        throws Exception
    {
        File basedir = fileManager.createTempDir();

        File filterProps = fileManager.createFile( basedir, "filter.properties", "artifactId=Test" );

        List filters = new ArrayList();
        filters.add( filterProps.getCanonicalPath() );

        enableBasicFilteringConfiguration( basedir, filters );

        File file = fileManager.createFile( basedir, "one.txt", "project artifact-id: ${artifactId} @artifactId@." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "project artifact-id: Test Test.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    private MavenProject createBasicMavenProject() {
        Model model = new Model();
        model.setArtifactId( "artifact" );
        model.setGroupId( "group" );
        model.setVersion( "version" );

        return new MavenProject( model );
    }
    
    private void enableBasicFilteringConfiguration( MavenProject project, File basedir )
        throws Exception
    {
      configSource.getTemporaryRootDirectory();
      configSourceControl.setReturnValue( basedir );

      configSource.getProject();
      configSourceControl.setReturnValue( project, MockControl.ONE_OR_MORE );

      configSource.getMavenFileFilter();
      configSourceControl.setReturnValue( lookup( "org.apache.maven.shared.filtering.MavenFileFilter" ) );

      configSource.getMavenSession();
      configSourceControl.setReturnValue( null );
    }
    
    private void enableBasicFilteringConfiguration( File basedir, List filterFilenames )
        throws Exception
    {
        MavenProject project = createBasicMavenProject();
        project.getBuild().setFilters( filterFilenames );

        enableBasicFilteringConfiguration( project, basedir );
    }

}
