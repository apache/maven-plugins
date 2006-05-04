package org.apache.maven.plugin.assembly;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.assembly.stubs.ArchiverManagerStub;
import org.apache.maven.plugin.assembly.stubs.UnArchiverWithException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class UnpackMojoTest
    extends AbstractMojoTestCase
{
    public void testMinConfiguration()
        throws Exception
    {
        executeMojo( "min-plugin-config.xml" );
    }

    public void testMinConfigurationAgainWithoutClean()
        throws Exception
    {
        executeMojo( "min-plugin-config.xml" );
    }

    public void testMinConfigurationWithReactorProjects()
        throws Exception
    {
        executeMojo( "with-reactor-projects-plugin-config.xml" );
    }

    public void testArchiverManagerException()
        throws Exception
    {
        try
        {
            UnpackMojo mojo = getMojo( "archiver-manager-exception-plugin-config.xml" );

            mojo.execute();

            MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
            String filename = project.getArtifact().getFile().getName();
            String dir = filename.substring( 0, filename.lastIndexOf( '.' ) );
            File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );
            File unpackDir = new File( workDir, dir );
            File unpacked = new File( unpackDir, filename + ".extracted" );

            assertFalse( "Test extracted project artifact", unpacked.exists() );
        }
        catch ( NoSuchArchiverException e )
        {
            fail( "Expected exception in ArchiverManager should not fail the build" );
        }
    }

    public void testUnpackException()
        throws Exception
    {
        try
        {
            UnpackMojo mojo = getMojo( "unpack-exception-plugin-config.xml" );

            ArchiverManagerStub archiverManager = (ArchiverManagerStub) getVariableValueFromObject( mojo, "archiverManager" );
            archiverManager.setUnArchiver( new UnArchiverWithException() );

            mojo.execute();

            fail( "Expected exception not thrown" );
        }
        catch ( MojoExecutionException e )
        {
            //expected
        }
    }

    public UnpackMojo getMojo( String pluginXml )
        throws Exception
    {
        return (UnpackMojo) lookupMojo( "unpack", PlexusTestCase.getBasedir() +
                                                   "/src/test/plugin-configs/unpack/" + pluginXml );
    }

    public void executeMojo( String pluginXml )
        throws Exception
    {
        UnpackMojo mojo = getMojo( pluginXml );

        mojo.execute();

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        String filename = project.getArtifact().getFile().getName();
        String dir = filename.substring( 0, filename.lastIndexOf( '.' ) );
        File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );
        File unpackDir = new File( workDir, dir );
        File unpacked = new File( unpackDir, filename + ".extracted" );

        assertTrue( "Test extracted project artifact", unpacked.exists() );
    }
}
