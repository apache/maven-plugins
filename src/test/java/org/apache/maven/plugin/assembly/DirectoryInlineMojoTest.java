package org.apache.maven.plugin.assembly;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.stubs.ArchiverManagerStub;
import org.apache.maven.plugin.assembly.stubs.CountingArchiver;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.MockControl;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class DirectoryInlineMojoTest
    extends AbstractMojoTestCase
{
    private MockManager mockManager = new MockManager();

    private MockControl archiverManagerControl;

    private ArchiverManager archiverManager;

    public void setUp()
        throws Exception
    {
        super.setUp();

        archiverManagerControl = MockControl.createControl( ArchiverManager.class );
        mockManager.add( archiverManagerControl );

        archiverManager = (ArchiverManager) archiverManagerControl.getMock();
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();

        TestUtils.cleanUp();
    }

    public void testAssemblyDirectory()
        throws Exception
    {
        File basedir = TestUtils.createTempBasedir();

        // prepare the dir structure...
        File fileSource = new File( basedir, "target/test-harness/assembly/min/target" );
        fileSource.mkdirs();
        
        File sourceJar = TestUtils.findFileForClasspathResource( "test-fodder/assembly.jar" );
        FileUtils.copyFile( sourceJar, new File( fileSource, "assembly.jar" ) );
        
        DirectoryMojo mojo = new DirectoryMojo();

        File descriptor = TestUtils.findFileForClasspathResource( "assemblies/simple.xml" );

        mojo.setDescriptor( descriptor );

        mojo.setBasedir( basedir );
        mojo.setFinalName( "directory-inline-min" );
        mojo.setAppendAssemblyId( true );

        Model model = new Model();

        model.setGroupId( "directory-inline-tests" );
        model.setArtifactId( "min" );
        model.setVersion( "1.0" );

        File outputDir = new File( basedir, "target/test-harness/directory-inline/min/target" );

        mojo.setOutputDirectory( outputDir );

        MavenProject project = new MavenProject( model );

        mojo.setProject( project );

        CountingArchiver archiver = new CountingArchiver();

        archiverManager.getArchiver( "dir" );
        archiverManagerControl.setReturnValue( archiver );

        mojo.setArchiverManager( archiverManager );

        mockManager.replayAll();

        mojo.execute();

        assertEquals( 1, archiver.getFileCount() );

        mockManager.verifyAll();
    }

    public void testDependencySet()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/plugin-configs/directory-inline/dependency-set-plugin-config.xml" );

        DirectoryInlineMojo mojo = (DirectoryInlineMojo) lookupMojo( "directory-inline", testPom );

        assertNotNull( mojo );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );

        Set artifacts = project.getArtifacts();

        mojo.execute();

        Map filesArchived = ArchiverManagerStub.archiverStub.getFiles();

        Set files = filesArchived.keySet();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();

            assertTrue( files.contains( artifact.getFile() ) );
            assertTrue( artifact.getFile().getName().endsWith( ".jar" ) );
        }

        assertTrue( "Test project is in archive", files.contains( project.getArtifact().getFile() ) );
    }
}
