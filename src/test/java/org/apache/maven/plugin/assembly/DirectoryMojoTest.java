package org.apache.maven.plugin.assembly;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.stubs.ArchiverManagerStub;
import org.apache.maven.plugin.assembly.stubs.CountingArchiver;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.easymock.MockControl;

/**
 * @author Allan Q. Ramirez
 */
public class DirectoryMojoTest
    extends AbstractMojoTestCase
{
    private MockManager mockManager = new MockManager();
    
    private MockControl archiverManagerControl;
    
    private ArchiverManager archiverManager;
    
    public void setUp() throws Exception
    {
        super.setUp();
        
        archiverManagerControl = MockControl.createControl( ArchiverManager.class );
        mockManager.add( archiverManagerControl );
        
        archiverManager = (ArchiverManager) archiverManagerControl.getMock();
    }
    
    public void tearDown() throws Exception
    {
        super.tearDown();
        
        TestUtils.cleanUp();
    }

    public void testEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/plugin-configs/directory/min-plugin-config.xml" );

        DirectoryMojo mojo = ( DirectoryMojo ) lookupMojo( "directory", testPom );

        assertNotNull( mojo );
    }

    public void testAssemblyDirectory()
        throws Exception
    {
        // I'm assuming this test is meant to simply verify the bare-bones
        // behavior of the directory mojo...
        
//        File testPom = new File( getBasedir(),
//                                 "src/test/plugin-configs/directory/min-plugin-config.xml" );

        DirectoryMojo mojo = new DirectoryMojo();
        
        File descriptor = TestUtils.findFileForClasspathResource( "assemblies/fileSet.xml" );
        
        mojo.setDescriptor( descriptor );
        
        File basedir = TestUtils.createTempBasedir();
        
        mojo.setBasedir( basedir );
        mojo.setFinalName( "directory-min" );
        mojo.setAppendAssemblyId( true );
        
        Model model = new Model();
        
        model.setGroupId( "directory-tests" );
        model.setArtifactId( "min" );
        model.setVersion( "1.0" );
        
        File outputDir = new File( basedir, "target/test-harness/directory/min/target" );
        
        mojo.setOutputDirectory( outputDir );
        
        MavenProject project = new MavenProject( model );
        
        mojo.setProject( project );
        mojo.setExecutedProject( project );
        
        CountingArchiver archiver = new CountingArchiver();
        
        archiverManager.getArchiver( "dir" );
        archiverManagerControl.setReturnValue( archiver );
        
        mojo.setArchiverManager( archiverManager );

        AssemblyMojoTest.generateTestFileSets(basedir.getAbsolutePath(), "\n");
        
        mockManager.replayAll();
        
        mojo.execute();

        assertEquals( 4, archiver.getFileCount() );
        
        mockManager.verifyAll();
    }

    public void testAssemblyDirectoryWithAppendAssemblyIdAsFalse()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/plugin-configs/directory/appendAssemblyId-false-plugin-config.xml" );

        DirectoryMojo mojo = ( DirectoryMojo ) lookupMojo( "directory", testPom );

        assertNotNull( mojo );

        String classifier = ( String ) getVariableValueFromObject( mojo, "classifier" );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        File outputDir = ( File ) getVariableValueFromObject( mojo, "outputDirectory" );

        MavenProject project = ( MavenProject ) getVariableValueFromObject( mojo, "executedProject" );

        Set artifacts = project.getArtifacts();

        mojo.execute();

        File dir = new File( outputDir, finalName + "-" + classifier );

        assertTrue( dir.exists() );

        Map filesArchived = ArchiverManagerStub.archiverStub.getFiles();

        Set files = filesArchived.keySet();

        for( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = ( Artifact ) iter.next();

            assertTrue( files.contains( artifact.getFile() ) );
            assertTrue( artifact.getFile().getName().endsWith( ".jar" ) );
        }

        assertTrue( "Test project is in archive", files.contains( project.getArtifact().getFile() ) );
    }

    public void testAssemblyDirectoryWithDependencySet()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/plugin-configs/directory/dependency-set-plugin-config.xml" );

        DirectoryMojo mojo = ( DirectoryMojo ) lookupMojo( "directory", testPom );

        assertNotNull( mojo );

        MavenProject project = ( MavenProject ) getVariableValueFromObject( mojo, "executedProject" );

        Set artifacts = project.getArtifacts();

        mojo.execute();

        Map filesArchived = ArchiverManagerStub.archiverStub.getFiles();

        Set files = filesArchived.keySet();

        for( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = ( Artifact ) iter.next();

            assertTrue( files.contains( artifact.getFile() ) );
            assertTrue( artifact.getFile().getName().endsWith( ".jar" ) );
        }

        assertTrue( "Test project is in archive", files.contains( project.getArtifact().getFile() ) );
    }

    public void testAssemblyDirectoryToThrowNoSuchArchiverException()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/plugin-configs/directory/min-plugin-config-with-exceptions.xml" );

        DirectoryMojo mojo = ( DirectoryMojo ) lookupMojo( "directory", testPom );

        assertNotNull( mojo );

        try
        {
            mojo.execute();

            fail( "Failure Expected" );
        }
        catch( Exception e )
        {
            //expected
        }
    }
    
}
