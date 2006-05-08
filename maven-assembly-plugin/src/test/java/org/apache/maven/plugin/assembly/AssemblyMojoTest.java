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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.stubs.ArchiverManagerStub;
import org.apache.maven.plugin.assembly.stubs.ArchiverStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Edwin Punzalan
 */
public class AssemblyMojoTest
    extends AbstractMojoTestCase
{
    public void testMinConfiguration()
        throws Exception
    {
        executeMojo( "min-plugin-config.xml" );
    }

    public void testPackedDependencySet()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "depSet-default-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );
        Set artifactSet = project.getArtifacts();

        mojo.execute();

        Map archiveMap = ArchiverManagerStub.archiverStub.getFiles();
        Collection archivedFiles = archiveMap.keySet();

        assertEquals( "Test number of files in archive", artifactSet.size() + 1, archivedFiles.size() );

        for( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact expected = (Artifact) artifacts.next();

            assertTrue( "Test expected dependency artifacts in archive", archivedFiles.contains( expected.getFile() ) );
            assertTrue( "Test expected dependency is not unpacked", expected.getFile().getName().endsWith( ".jar" ) );

            ArchiverStub.ArchiverFile archiveFile = (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
            String archivePath = archiveFile.getOutputName();
            assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        ArchiverStub.ArchiverFile archiveFile = (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
        String archivePath = archiveFile.getOutputName();
        assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
    }

    public void testPackedDependencySetWithFilenameMapping()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "depSet-filename-mapping-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );
        Set artifactSet = project.getArtifacts();

        mojo.execute();

        Map archiveMap = ArchiverManagerStub.archiverStub.getFiles();
        Collection archivedFiles = archiveMap.keySet();

        assertEquals( "Test number of files in archive", artifactSet.size() + 1, archivedFiles.size() );

        for( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact expected = (Artifact) artifacts.next();

            assertTrue( "Test expected dependency artifacts in archive", archivedFiles.contains( expected.getFile() ) );
            assertTrue( "Test expected dependency is not unpacked", expected.getFile().getName().endsWith( ".jar" ) );

            ArchiverStub.ArchiverFile archiveFile = (ArchiverStub.ArchiverFile) archiveMap.get( expected.getFile() );
            String archivePath = archiveFile.getOutputName();
            String expectedName = "libs/" + expected.getVersion() + "-" +
                                  expected.getArtifactId() + "-" + expected.getGroupId();
            assertTrue( "Test filename mapping was used", archivePath.equals( expectedName ) );
            assertFalse( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        ArchiverStub.ArchiverFile archiveFile = (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
        String archivePath = archiveFile.getOutputName();
        assertFalse( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
    }

    public void testPackedDependencySetWithScope()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "depSet-scoped-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );
        Set artifactSet = project.getArtifacts();

        mojo.execute();

        Map archiveMap = ArchiverManagerStub.archiverStub.getFiles();
        Collection archivedFiles = archiveMap.keySet();

        for( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact expected = (Artifact) artifacts.next();

            if ( !expected.getScope().equals( Artifact.SCOPE_TEST ) )
            {
                assertTrue( "Test expected dependency artifacts in archive", archivedFiles.contains( expected.getFile() ) );
                assertTrue( "Test expected dependency is not unpacked", expected.getFile().getName().endsWith( ".jar" ) );

                ArchiverStub.ArchiverFile archiveFile = (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
                String archivePath = archiveFile.getOutputName();
                assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
            }
            else
            {
                assertFalse( "Test unexpected dependency artifacts in archive", archivedFiles.contains( expected.getFile() ) );
            }
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        ArchiverStub.ArchiverFile archiveFile = (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
        String archivePath = archiveFile.getOutputName();
        assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
    }

    public void testUnpackedDependencySet()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "depSet-unpack-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );
        Set artifactSet = project.getArtifacts();

        File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );

        mojo.execute();

        Collection archivedFileSet = ArchiverManagerStub.archiverStub.getFiles().keySet();

        assertEquals( "Test number of files in archive", artifactSet.size() + 1, archivedFileSet.size() );

        for( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact dependency = (Artifact) artifacts.next();

            String name = dependency.getArtifactId() + "-" + dependency.getVersion() + "." + dependency.getType();
            File workPath = new File( workDir, name );

            assertTrue( "Test if expected dependency is unpacked", workPath.exists() && workPath.isDirectory() );
            assertTrue( "Test if expected dependency is in the archive", archivedFileSet.contains( workPath ) );
        }

        String name = project.getArtifact().getArtifactId() + "-" + project.getArtifact().getVersion() +
                      "." + project.getArtifact().getType();
        File workPath = new File( workDir, name );

        assertTrue( "Test if project is unpacked", workPath.exists() && workPath.isDirectory() );
        assertTrue( "Test if project is in the archive", archivedFileSet.contains( workPath ) );
    }

    public void testFileSet()
        throws Exception
    {
        AssemblyMojo mojo = executeMojo( "fileSet-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive adds", archiverFiles.size(), 1 );

        File key = new File( PlexusTestCase.getBasedir(), "target/test-classes/fileSet" );
        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( key );

        assertNotNull( "Test expected FileSet", file );
        assertNull( "Test includes", file.getIncludes() );

        String[] excludes = file.getExcludes();
        assertTrue( "Test excludes", assertEquals( FileUtils.getDefaultExcludesAsList().toArray( new String[0] ),
                                                   file.getExcludes() ) );
    }

    private AssemblyMojo getMojo( String pluginXml )
        throws Exception
    {
        return (AssemblyMojo) lookupMojo( "assembly", PlexusTestCase.getBasedir() +
                                        "/src/test/plugin-configs/assembly/" + pluginXml );
    }

    private AssemblyMojo executeMojo( String pluginXml )
        throws Exception
    {
        AssemblyMojo mojo = getMojo( pluginXml );

        mojo.execute();

        assertTrue( "Test an archive was created", ArchiverManagerStub.archiverStub.getDestFile().exists() );

        return mojo;
    }

    private boolean assertEquals( Object[] obj1, Object[] obj2 )
    {
        boolean equal = false;

        if ( obj1.length == obj2.length )
        {
            for ( int ctr = 0; ctr < obj1.length; ctr++ )
            {
                if ( !obj1[ ctr ].equals( obj2[ ctr ]) )
                {
                    break;
                }
            }

            equal = true;
        }

        return equal;
    }
}
