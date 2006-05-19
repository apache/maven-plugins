package org.apache.maven.plugin.assembly;

import org.apache.maven.plugin.assembly.stubs.ArchiverManagerStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

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
    public void testAssemblyDirectory()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/plugin-configs/directory-inline/min-plugin-config.xml" );

        DirectoryInlineMojo mojo = ( DirectoryInlineMojo ) lookupMojo( "directory-inline", testPom );

        assertNotNull( mojo );

        mojo.execute();

        Map filesArchived = ArchiverManagerStub.archiverStub.getFiles();

        Set files = filesArchived.keySet();

        assertEquals( 1, files.size() );
    }

    public void testDependencySet()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/plugin-configs/directory-inline/dependency-set-plugin-config.xml" );

        DirectoryInlineMojo mojo = ( DirectoryInlineMojo ) lookupMojo( "directory-inline", testPom );

        assertNotNull( mojo );

        MavenProject project = ( MavenProject ) getVariableValueFromObject( mojo, "project" );

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
}
