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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.assembly.stubs.ArchiverManagerStub;
import org.apache.maven.plugin.assembly.stubs.ArchiverStub;
import org.apache.maven.plugin.assembly.stubs.ReactorMavenProjectStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Edwin Punzalan
 */
public class AssemblyMojoTest
    extends AbstractMojoTestCase
{
    private String basedir = PlexusTestCase.getBasedir();

    public void testMinConfiguration()
        throws Exception
    {
        executeMojo( "min-plugin-config.xml" );
    }

    public void testProjectWithClassifier()
        throws Exception
    {
        executeMojo( "classifier-plugin-config.xml" );

        File assemblyJar = ArchiverManagerStub.archiverStub.getDestFile();

        assertTrue( "Test if archive ends with the classifier", assemblyJar.getName().endsWith( "test-harness.zip" ) );
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

        for ( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact expected = (Artifact) artifacts.next();

            assertTrue( "Test expected dependency artifacts in archive", archivedFiles.contains( expected.getFile() ) );
            assertTrue( "Test expected dependency is not unpacked", expected.getFile().getName().endsWith( ".jar" ) );

            ArchiverStub.ArchiverFile archiveFile =
                (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
            String archivePath = archiveFile.getOutputName();
            assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        ArchiverStub.ArchiverFile archiveFile =
            (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
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

        for ( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact expected = (Artifact) artifacts.next();

            assertTrue( "Test expected dependency artifacts in archive", archivedFiles.contains( expected.getFile() ) );
            assertTrue( "Test expected dependency is not unpacked", expected.getFile().getName().endsWith( ".jar" ) );

            ArchiverStub.ArchiverFile archiveFile = (ArchiverStub.ArchiverFile) archiveMap.get( expected.getFile() );
            String archivePath = archiveFile.getOutputName();
            String expectedName =
                "libs/" + expected.getVersion() + "-" + expected.getArtifactId() + "-" + expected.getGroupId();
            assertTrue( "Test filename mapping was used", archivePath.equals( expectedName ) );
            assertFalse( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        ArchiverStub.ArchiverFile archiveFile =
            (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
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

        for ( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact expected = (Artifact) artifacts.next();

            if ( !expected.getScope().equals( Artifact.SCOPE_TEST ) )
            {
                assertTrue( "Test expected dependency artifacts in archive",
                            archivedFiles.contains( expected.getFile() ) );
                assertTrue( "Test expected dependency is not unpacked",
                            expected.getFile().getName().endsWith( ".jar" ) );

                ArchiverStub.ArchiverFile archiveFile =
                    (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
                String archivePath = archiveFile.getOutputName();
                assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
            }
            else
            {
                assertFalse( "Test unexpected dependency artifacts in archive",
                             archivedFiles.contains( expected.getFile() ) );
            }
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        ArchiverStub.ArchiverFile archiveFile =
            (ArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
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

        for ( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact dependency = (Artifact) artifacts.next();

            String name = dependency.getArtifactId() + "-" + dependency.getVersion() + "." + dependency.getType();
            File workPath = new File( workDir, name );

            assertTrue( "Test if expected dependency is unpacked", workPath.exists() && workPath.isDirectory() );
            assertTrue( "Test if expected dependency is in the archive", archivedFileSet.contains( workPath ) );
        }

        String name = project.getArtifact().getArtifactId() + "-" + project.getArtifact().getVersion() + "." +
            project.getArtifact().getType();
        File workPath = new File( workDir, name );

        assertTrue( "Test if project is unpacked", workPath.exists() && workPath.isDirectory() );
        assertTrue( "Test if project is in the archive", archivedFileSet.contains( workPath ) );
    }

    public void testFileSet()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        AssemblyMojo mojo = executeMojo( "fileSet-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive adds", archiverFiles.size(), 1 );

        File key = new File( basedir, "target/test-classes/fileSet" );
        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( key );

        assertNotNull( "Test expected FileSet", file );
        assertNull( "Test includes", file.getIncludes() );
        assertTrue( "Test excludes",
                    assertEquals( FileUtils.getDefaultExcludesAsList().toArray( new String[0] ), file.getExcludes() ) );
    }

    public void testFileSetWithArchiveBaseDir()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        AssemblyMojo mojo = executeMojo( "fileSet-archiveBaseDir-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive adds", archiverFiles.size(), 1 );

        File key = new File( basedir, "target/test-classes/fileSet" );
        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( key );

        assertNotNull( "Test expected FileSet", file );
        assertNull( "Test includes", file.getIncludes() );
        assertTrue( "Test excludes",
                    assertEquals( FileUtils.getDefaultExcludesAsList().toArray( new String[0] ), file.getExcludes() ) );
    }

    public void testFileSetIncludesExcludes()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        AssemblyMojo mojo = executeMojo( "fileSet-includes-excludes-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive adds", archiverFiles.size(), 1 );

        File key = new File( basedir, "target/test-classes/fileSet" );
        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( key );

        assertNotNull( "Test expected FileSet", file );
        assertTrue( "Test includes", assertEquals( new String[]{"**/*.txt"}, file.getIncludes() ) );

        List excludesList = new ArrayList();
        excludesList.add( "**/*.xml" );
        excludesList.addAll( FileUtils.getDefaultExcludesAsList() );

        assertTrue( "Test excludes", assertEquals( excludesList.toArray( new String[0] ), file.getExcludes() ) );
    }

    public void testFileSetUnixLineEndings()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        AssemblyMojo mojo = executeMojo( "fileSet-unix-lineEndings-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive adds", archiverFiles.size(), 1 );

        File key = new File( basedir, "target/test-classes/fileSet" );
        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( key );
        assertNull( "Test original FileSet is not in archive", file );

        File tempRoot = (File) getVariableValueFromObject( mojo, "tempRoot" );
        File tempDir = (File) archiverFiles.keySet().iterator().next();
        assertTrue( "Test if dir is in tempRoot", tempDir.getAbsolutePath().startsWith( tempRoot.getAbsolutePath() ) );

        String newFile = tempDir.getAbsolutePath() + "/hibernate.hbm.xml";
        String unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) < 0 );

        newFile = tempDir.getAbsolutePath() + "/LICENSE.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) < 0 );

        newFile = tempDir.getAbsolutePath() + "/README.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) < 0 );

        newFile = tempDir.getAbsolutePath() + "/configs/config.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) < 0 );
    }

    public void testFileSetLFLineEndings()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        AssemblyMojo mojo = executeMojo( "fileSet-lf-lineEndings-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive adds", archiverFiles.size(), 1 );

        File key = new File( basedir, "target/test-classes/fileSet" );
        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( key );
        assertNull( "Test original FileSet is not in archive", file );

        File tempRoot = (File) getVariableValueFromObject( mojo, "tempRoot" );
        File tempDir = (File) archiverFiles.keySet().iterator().next();
        assertTrue( "Test if dir is in tempRoot", tempDir.getAbsolutePath().startsWith( tempRoot.getAbsolutePath() ) );

        String newFile = tempDir.getAbsolutePath() + "/hibernate.hbm.xml";
        String unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) < 0 );

        newFile = tempDir.getAbsolutePath() + "/LICENSE.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) < 0 );

        newFile = tempDir.getAbsolutePath() + "/README.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) < 0 );

        newFile = tempDir.getAbsolutePath() + "/configs/config.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) < 0 );
    }

    public void testFileSetDOSLineEndings()
        throws Exception
    {
        generateTestFileSets( "\n" );

        AssemblyMojo mojo = executeMojo( "fileSet-dos-lineEndings-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive adds", archiverFiles.size(), 1 );

        File key = new File( basedir, "target/test-classes/fileSet" );
        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( key );
        assertNull( "Test original FileSet is not in archive", file );

        File tempRoot = (File) getVariableValueFromObject( mojo, "tempRoot" );
        File tempDir = (File) archiverFiles.keySet().iterator().next();
        assertTrue( "Test if dir is in tempRoot", tempDir.getAbsolutePath().startsWith( tempRoot.getAbsolutePath() ) );

        String newFile = tempDir.getAbsolutePath() + "/hibernate.hbm.xml";
        String unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) >= 0 );

        newFile = tempDir.getAbsolutePath() + "/LICENSE.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) >= 0 );

        newFile = tempDir.getAbsolutePath() + "/README.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) >= 0 );

        newFile = tempDir.getAbsolutePath() + "/configs/config.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) >= 0 );
    }

    public void testFileSetCRLFLineEndings()
        throws Exception
    {
        generateTestFileSets( "\n" );

        AssemblyMojo mojo = executeMojo( "fileSet-crlf-lineEndings-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive adds", archiverFiles.size(), 1 );

        File key = new File( basedir, "target/test-classes/fileSet" );
        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( key );
        assertNull( "Test original FileSet is not in archive", file );

        File tempRoot = (File) getVariableValueFromObject( mojo, "tempRoot" );
        File tempDir = (File) archiverFiles.keySet().iterator().next();
        assertTrue( "Test if dir is in tempRoot", tempDir.getAbsolutePath().startsWith( tempRoot.getAbsolutePath() ) );

        String newFile = tempDir.getAbsolutePath() + "/hibernate.hbm.xml";
        String unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) >= 0 );

        newFile = tempDir.getAbsolutePath() + "/LICENSE.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) >= 0 );

        newFile = tempDir.getAbsolutePath() + "/README.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) >= 0 );

        newFile = tempDir.getAbsolutePath() + "/configs/config.txt";
        unixContents = FileUtils.fileRead( newFile );
        assertTrue( "Test if modified file exists in tempRoot", FileUtils.fileExists( newFile ) );
        assertTrue( "Test if all \\r\\n are replaced", unixContents.indexOf( "\r\n" ) >= 0 );
    }

    public void testFileSetInvalidLineEndings()
        throws Exception
    {
        try
        {
            executeMojo( "fileSet-lineEndings-exception-plugin-config.xml" );

            fail( "Expected exception not thrown" );
        }
        catch ( MojoFailureException e )
        {
            // expected
        }
    }

    public void testFileSetDirectoryDoesntExist()
        throws Exception
    {
        executeMojo( "fileSet-doesnt-exist-plugin-config.xml" );
        assertTrue( true );
    }

    public void testFileItem()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        AssemblyMojo mojo = executeMojo( "fileItem-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive files", 2, archiverFiles.size() );

        Iterator files = archiverFiles.keySet().iterator();

        File archivedFile = (File) files.next();

        assertTrue( "Test if archived file exists", archivedFile.exists() );

        if ( "README.txt".equals( archivedFile.getName() ) )
        {
            String contents = FileUtils.fileRead( archivedFile.getAbsolutePath() );

            assertTrue( "Test if file filtering is disabled", contents.indexOf( "${project.artifactId}" ) >= 0 );
        }

        archivedFile = (File) files.next();

        assertTrue( "Test if archived file exists", archivedFile.exists() );

        if ( "README.txt".equals( archivedFile.getName() ) )
        {
            String contents = FileUtils.fileRead( archivedFile.getAbsolutePath() );

            assertTrue( "Test if file filtering is disabled", contents.indexOf( "${project.artifactId}" ) >= 0 );
        }
    }

    public void testFileItemWithOutputName()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        AssemblyMojo mojo = executeMojo( "fileItem-output-name-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive files", 1, archiverFiles.size() );

        File archivedFile = (File) archiverFiles.keySet().iterator().next();

        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( archivedFile );

        assertEquals( "Test archive file path", "assembly/output/READTHIS.txt", file.getOutputName() );

        assertTrue( "Test if archived file exists", archivedFile.exists() );

        String contents = FileUtils.fileRead( archivedFile.getAbsolutePath() );

        assertTrue( "Test if file filtering is disabled", contents.indexOf( "${project.artifactId}" ) >= 0 );
    }

    public void testFileItemWithFiltering()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        executeMojo( "fileItem-filtered-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive files", 1, archiverFiles.size() );

        File archivedFile = (File) archiverFiles.keySet().iterator().next();

        assertTrue( "Test if archived file exists", archivedFile.exists() );

        String contents = FileUtils.fileRead( archivedFile.getAbsolutePath() );

        assertTrue( "Test if file filtering is enabled", contents.indexOf( "${project.artifactId}" ) < 0 );
    }

    public void testFileItemLineEndings()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        executeMojo( "fileItem-lineEndings-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive files", 1, archiverFiles.size() );

        File archivedFile = (File) archiverFiles.keySet().iterator().next();

        assertTrue( "Test if archived file exists", archivedFile.exists() );

        String contents = FileUtils.fileRead( archivedFile.getAbsolutePath() );

        assertTrue( "Test file line endings", contents.indexOf( "\r\n" ) < 0 );
    }

    public void testFileItemFileMode()
        throws Exception
    {
        generateTestFileSets( "\r\n" );

        executeMojo( "fileItem-fileMode-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive files", 1, archiverFiles.size() );

        File archivedFile = (File) archiverFiles.keySet().iterator().next();

        assertTrue( "Test if archived file exists", archivedFile.exists() );

        ArchiverStub.ArchiverFile file = (ArchiverStub.ArchiverFile) archiverFiles.get( archivedFile );

        assertEquals( "Test file mode", 777, file.getFileMode() );
    }

    public void testModuleSet()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "moduleSet-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );

        List reactorProjectsList = (List) getVariableValueFromObject( mojo, "reactorProjects" );

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            reactorProject.setParent( project );
        }

        mojo.execute();

        assertTrue( "Test an archive was created", ArchiverManagerStub.archiverStub.getDestFile().exists() );

        File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            File unpacked = new File( workDir, reactorProject.getBuild().getFinalName() );
            assertTrue( "Test if reactor project was unpacked in work directory", unpacked.exists() );
            assertTrue( "Test if unpacked directory is in the archive", archiverFiles.containsKey( unpacked ) );
            archiverFiles.remove( unpacked );

            File srcDir = reactorProject.getBasedir();
            assertTrue( "Test if reactor sources is in the archive", archiverFiles.containsKey( srcDir ) );
            archiverFiles.remove( srcDir );
        }

        assertEquals( "Test that there are no other archive files added", 0, archiverFiles.size() );
    }

    public void testModuleSetIncludes()
        throws Exception
    {
        ReactorMavenProjectStub.reactorProjects.clear();

        AssemblyMojo mojo = getMojo( "moduleSet-includes-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );

        List reactorProjectsList = (List) getVariableValueFromObject( mojo, "reactorProjects" );

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            reactorProject.setParent( project );
        }

        mojo.execute();

        assertTrue( "Test an archive was created", ArchiverManagerStub.archiverStub.getDestFile().exists() );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );

        File unpacked = new File( workDir, "reactor-project-1-1.0.jar" );

        assertTrue( "Test if reactor project was unpacked in work directory", unpacked.exists() );
        assertTrue( "Test if unpacked directory is in the archive", archiverFiles.containsKey( unpacked ) );
        archiverFiles.remove( unpacked );

        File srcDir = new File( workDir.getParentFile(), "reactor-project-1" );
        assertTrue( "Test if reactor project sources is in the archive", archiverFiles.containsKey( srcDir ) );
        archiverFiles.remove( srcDir );

        assertEquals( "Test that there are no other archive files added", 0, archiverFiles.size() );
    }

    public void testModuleSetExcludes()
        throws Exception
    {
        ReactorMavenProjectStub.reactorProjects.clear();

        AssemblyMojo mojo = getMojo( "moduleSet-excludes-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );

        List reactorProjectsList = (List) getVariableValueFromObject( mojo, "reactorProjects" );

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            reactorProject.setParent( project );
        }

        mojo.execute();

        assertTrue( "Test an archive was created", ArchiverManagerStub.archiverStub.getDestFile().exists() );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );

        File unpacked = new File( workDir, "reactor-project-2-1.0.jar" );

        assertTrue( "Test if reactor project was unpacked in work directory", unpacked.exists() );
        assertTrue( "Test if unpacked directory is in the archive", archiverFiles.containsKey( unpacked ) );
        archiverFiles.remove( unpacked );

        File srcDir = new File( workDir.getParentFile(), "reactor-project-2" );
        assertTrue( "Test if reactor project sources is in the archive", archiverFiles.containsKey( srcDir ) );
        archiverFiles.remove( srcDir );

        assertEquals( "Test that there are no other archive files added", 0, archiverFiles.size() );
    }

    public void testModuleSetIncludeDependencies()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "moduleSet-include-dependencies-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );

        List reactorProjectsList = (List) getVariableValueFromObject( mojo, "reactorProjects" );

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            reactorProject.setParent( project );
        }

        mojo.execute();

        assertTrue( "Test an archive was created", ArchiverManagerStub.archiverStub.getDestFile().exists() );

        File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            File unpacked = new File( workDir, reactorProject.getBuild().getFinalName() );
            assertTrue( "Test if reactor project is unpacked in work directory", unpacked.exists() );
            File dependency = new File( unpacked, "reactor-dependency-1.0.jar.extracted" );
            assertTrue( "Test if reactor dependency is also unpacked", dependency.exists() );
            assertTrue( "Test if unpacked directory is in the archive", archiverFiles.containsKey( unpacked ) );
            archiverFiles.remove( unpacked );

            File srcDir = reactorProject.getBasedir();
            assertTrue( "Test if reactor sources is in the archive", archiverFiles.containsKey( srcDir ) );
            archiverFiles.remove( srcDir );
        }

        assertEquals( "Test that there are no other archive files added", 0, archiverFiles.size() );
    }

    public void testModuleSetPacked()
        throws Exception
    {
        ReactorMavenProjectStub.reactorProjects.clear();

        AssemblyMojo mojo = getMojo( "moduleSet-packed-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );

        List reactorProjectsList = (List) getVariableValueFromObject( mojo, "reactorProjects" );

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            reactorProject.setParent( project );
        }

        mojo.execute();

        assertTrue( "Test an archive was created", ArchiverManagerStub.archiverStub.getDestFile().exists() );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            assertFalse( "Test if work directory is used", workDir.exists() );
            File reactorProjectFile = reactorProject.getArtifact().getFile();
            assertNotNull( "Test if reactor project is in the archive", archiverFiles.remove( reactorProjectFile ) );
            Artifact artifact = (Artifact) reactorProject.getArtifacts().iterator().next();
            File depFile = artifact.getFile();
            assertNull( "Test if reactor dependency is not in the archive", archiverFiles.remove( depFile ) );
        }

        assertTrue( "Test that there are no other archive files added", archiverFiles.isEmpty() );
    }

    public void testModuleSetPackedIncludingDependencies()
        throws Exception
    {
        ReactorMavenProjectStub.reactorProjects.clear();

        AssemblyMojo mojo = getMojo( "moduleSet-packed-including-dependencies-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );

        List reactorProjectsList = (List) getVariableValueFromObject( mojo, "reactorProjects" );

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            reactorProject.setParent( project );
        }

        mojo.execute();

        assertTrue( "Test an archive was created", ArchiverManagerStub.archiverStub.getDestFile().exists() );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );

        for ( Iterator reactorProjects = reactorProjectsList.iterator(); reactorProjects.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) reactorProjects.next();

            assertFalse( "Test if work directory is used", workDir.exists() );
            File reactorProjectFile = reactorProject.getArtifact().getFile();
            assertNotNull( "Test if reactor project is in the archive", archiverFiles.remove( reactorProjectFile ) );
            Artifact artifact = (Artifact) reactorProject.getArtifacts().iterator().next();
            File depFile = artifact.getFile();
            assertNotNull( "Test if reactor dependency is also in the archive", archiverFiles.remove( depFile ) );
        }

        assertTrue( "Test that there are no other archive files added", archiverFiles.isEmpty() );
    }

    public void testRepository()
        throws Exception
    {
        AssemblyMojo mojo = executeMojo( "repository-plugin-config.xml" );

        File tempRoot = (File) getVariableValueFromObject( mojo, "tempRoot" );

        File tmpRepositoryDir = new File( tempRoot, "repository" );
        assertTrue( "Test if repository output directory is used", tmpRepositoryDir.exists() );

        String repoPath = "assembly/dependency-artifact/1.0/dependency-artifact-1.0.";

        File tmpArtifactJar = new File( tmpRepositoryDir, repoPath + "jar" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactJar.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "jar.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "jar.sha1" ).exists() );

        File tmpArtifactPom = new File( tmpRepositoryDir, repoPath + "pom" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactPom.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "pom.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "pom.sha1" ).exists() );
    }

    public void testRepositoryWithMetadata()
        throws Exception
    {
        AssemblyMojo mojo = executeMojo( "repository-with-metadata-plugin-config.xml" );

        File tempRoot = (File) getVariableValueFromObject( mojo, "tempRoot" );

        File tmpRepositoryDir = new File( tempRoot, "repository" );
        assertTrue( "Test if repository output directory is used", tmpRepositoryDir.exists() );

        String repoPath = "assembly/dependency-artifact/1.0/dependency-artifact-1.0.";

        File tmpArtifactJar = new File( tmpRepositoryDir, repoPath + "jar" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactJar.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "jar.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "jar.sha1" ).exists() );

        File tmpArtifactPom = new File( tmpRepositoryDir, repoPath + "pom" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactPom.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "pom.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "pom.sha1" ).exists() );

        File tmpArtifactMetadatadir = new File( tmpRepositoryDir, "assembly/dependency-artifact" );
        File metadataXml = new File( tmpArtifactMetadatadir, "maven-metadata.xml" );
        assertTrue( "Test if metadata was created", metadataXml.exists() );
        assertTrue( "Test if metadata md5 was generated", new File( tmpArtifactMetadatadir, "maven-metadata.xml.md5" ).exists() );
        assertTrue( "Test if metadata md5 was generated", new File( tmpArtifactMetadatadir, "maven-metadata.xml.sha1" ).exists() );

        metadataXml = new File( tmpArtifactMetadatadir, "maven-metadata-central.xml" );
        assertTrue( "Test if metadata was created", metadataXml.exists() );
        assertTrue( "Test if metadata md5 was generated", new File( tmpArtifactMetadatadir, "maven-metadata-central.xml.md5" ).exists() );
        assertTrue( "Test if metadata md5 was generated", new File( tmpArtifactMetadatadir, "maven-metadata-central.xml.sha1" ).exists() );
    }

    public void testRepositoryGroupVersionAlignment()
        throws Exception
    {
        AssemblyMojo mojo = executeMojo( "repository-groupVersionAlignment-plugin-config.xml" );

        File tempRoot = (File) getVariableValueFromObject( mojo, "tempRoot" );

        File tmpRepositoryDir = new File( tempRoot, "repository" );
        assertTrue( "Test if repository output directory is used", tmpRepositoryDir.exists() );

        String repoPath = "assembly/dependency-artifact/1.1/dependency-artifact-1.1.";

        File tmpArtifactJar = new File( tmpRepositoryDir, repoPath + "jar" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactJar.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "jar.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "jar.sha1" ).exists() );

        File tmpArtifactPom = new File( tmpRepositoryDir, repoPath + "pom" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactPom.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "pom.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "pom.sha1" ).exists() );
    }

    public void testRepositoryGroupVersionAlignmentExcludes()
        throws Exception
    {
        AssemblyMojo mojo = executeMojo( "repository-groupVersionAlignment-excludes-plugin-config.xml" );

        File tempRoot = (File) getVariableValueFromObject( mojo, "tempRoot" );

        File tmpRepositoryDir = new File( tempRoot, "repository" );
        assertTrue( "Test if repository output directory is used", tmpRepositoryDir.exists() );

        String repoPath = "assembly/dependency-artifact/1.0/dependency-artifact-1.0.";

        File tmpArtifactJar = new File( tmpRepositoryDir, repoPath + "jar" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactJar.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "jar.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "jar.sha1" ).exists() );

        File tmpArtifactPom = new File( tmpRepositoryDir, repoPath + "pom" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactPom.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "pom.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "pom.sha1" ).exists() );
    }

    public void testMASSEMBLY98()
        throws Exception
    {
        AssemblyMojo mojo = executeMojo( "MASSEMBLY-98-plugin-config.xml" );

        File tempRoot = (File) getVariableValueFromObject( mojo, "tempRoot" );

        File tmpRepositoryDir = new File( tempRoot, "repository" );
        assertTrue( "Test if repository output directory is used", tmpRepositoryDir.exists() );

        String repoPath = "assembly/dependency-artifact/1.1/dependency-artifact-1.1.";

        File tmpArtifactJar = new File( tmpRepositoryDir, repoPath + "jar" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactJar.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "jar.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "jar.sha1" ).exists() );

        File tmpArtifactPom = new File( tmpRepositoryDir, repoPath + "pom" );
        assertTrue( "Test if dependency artifact is in repository", tmpArtifactPom.exists() );
        assertTrue( "Test if md5 was generated", new File( tmpRepositoryDir, repoPath + "pom.md5" ).exists() );
        assertTrue( "Test if sha1 was generated", new File( tmpRepositoryDir, repoPath + "pom.sha1" ).exists() );
    }

    private AssemblyMojo getMojo( String pluginXml )
        throws Exception
    {
        return (AssemblyMojo) lookupMojo( "assembly", basedir + "/src/test/plugin-configs/assembly/" + pluginXml );
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
                if ( !obj1[ctr].equals( obj2[ctr] ) )
                {
                    break;
                }
            }

            equal = true;
        }

        return equal;
    }

    private void generateTestFileSets( String lineEnding )
        throws Exception
    {
        String fileSetDir = basedir + "/target/test-classes/fileSet";

        FileUtils.mkdir( fileSetDir + "/configs" );

        String fileContents = "<hibernate>" + lineEnding + "  <hibernate-mapping>" + lineEnding + "    <class/>" +
            lineEnding + "  </hibernate-mapping>" + lineEnding + "</hibernate>";
        FileUtils.fileWrite( fileSetDir + "/hibernate.hbm.xml", fileContents );

        fileContents = "Copyright 2001-2006 The Apache Software Foundation." + lineEnding + lineEnding +
            "Licensed under the Apache License, Version 2.0 (the \"License\");" + lineEnding +
            "you may not use this file except in compliance with the License." + lineEnding +
            "You may obtain a copy of the License at" + lineEnding + lineEnding +
            "     http://www.apache.org/licenses/LICENSE-2.0" + lineEnding + lineEnding +
            "Unless required by applicable law or agreed to in writing, software" + lineEnding +
            "distributed under the License is distributed on an \"AS IS\" BASIS," + lineEnding +
            "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied." + lineEnding +
            "See the License for the specific language governing permissions and" + lineEnding +
            "limitations under the License.";
        FileUtils.fileWrite( fileSetDir + "/LICENSE.txt", fileContents );

        fileContents = "Readme file with only one line for ${project.artifactId}.";
        FileUtils.fileWrite( fileSetDir + "/README.txt", fileContents );

        fileContents = "sample configuration file line 1" + lineEnding + "sample configuration file line 2" +
            lineEnding + "sample configuration file line 3" + lineEnding + "sample configuration file line 4";
        FileUtils.fileWrite( fileSetDir + "/configs/config.txt", fileContents );
    }
}
