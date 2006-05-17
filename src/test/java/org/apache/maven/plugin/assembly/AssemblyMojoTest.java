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
import org.apache.maven.plugin.assembly.stubs.JarArchiverStub;
import org.apache.maven.plugin.assembly.stubs.ReactorMavenProjectStub;
import org.apache.maven.plugin.assembly.stubs.WarArchiverStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.PrintWriter;
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

    public void testDescriptorSourceDirectory()
        throws Exception
    {
        executeMojo( "descriptorSourceDirectory-plugin-config.xml" );
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

            JarArchiverStub.ArchiverFile archiveFile =
                (JarArchiverStub.ArchiverFile) archiveMap.get( expected.getFile() );
            String archivePath = archiveFile.getOutputName();
            assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        JarArchiverStub.ArchiverFile archiveFile =
            (JarArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
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

            JarArchiverStub.ArchiverFile archiveFile = (JarArchiverStub.ArchiverFile) archiveMap.get( expected.getFile() );
            String archivePath = archiveFile.getOutputName();
            String expectedName =
                "libs/" + expected.getVersion() + "-" + expected.getArtifactId() + "-" + expected.getGroupId();
            assertTrue( "Test filename mapping was used", archivePath.equals( expectedName ) );
            assertFalse( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        JarArchiverStub.ArchiverFile archiveFile =
            (JarArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
        String archivePath = archiveFile.getOutputName();
        assertFalse( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
    }

    public void testPackedDependencySetWithFilenameMappingAndClassifier()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "depSet-filename-mapping-and-classifier-plugin-config.xml" );

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

            JarArchiverStub.ArchiverFile archiveFile = (JarArchiverStub.ArchiverFile) archiveMap.get( expected.getFile() );
            String archivePath = archiveFile.getOutputName();
            String expectedName;
            if ( StringUtils.isEmpty( expected.getClassifier() ) )
            {
                expectedName =
                    "libs/" + expected.getVersion() + "." + expected.getArtifactId() + "." +
                    expected.getGroupId();
            }
            else
            {
                expectedName =
                    "libs/" + expected.getVersion() + "." + expected.getArtifactId() +
                    "-classifier" + "." + expected.getGroupId();

            }
            assertEquals( "Test filename mapping was used", archivePath, expectedName );
            assertFalse( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        JarArchiverStub.ArchiverFile archiveFile =
            (JarArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
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

                JarArchiverStub.ArchiverFile archiveFile =
                    (JarArchiverStub.ArchiverFile) archiveMap.get( expected.getFile() );
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

        JarArchiverStub.ArchiverFile archiveFile =
            (JarArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
        String archivePath = archiveFile.getOutputName();
        assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
    }

    public void testPackedDependencySetIncludes()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "depSet-includes-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );
        Set artifactSet = project.getArtifacts();

        mojo.execute();

        Map archiveMap = ArchiverManagerStub.archiverStub.getFiles();
        Collection archivedFiles = archiveMap.keySet();

        assertEquals( "Test number of files in archive", 1, archivedFiles.size() );

        for ( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact depArtifact = (Artifact) artifacts.next();

            if ( depArtifact.getArtifactId().equals( "dependency-artifact1" ) )
            {
                assertTrue( "Test expected dependency artifacts in archive", archivedFiles.contains( depArtifact.getFile() ) );
                assertTrue( "Test expected dependency is not unpacked", depArtifact.getFile().getName().endsWith( ".jar" ) );

                JarArchiverStub.ArchiverFile archiveFile =
                    (JarArchiverStub.ArchiverFile) archiveMap.get( depArtifact.getFile() );
                String archivePath = archiveFile.getOutputName();
                assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
            }
            else
            {
                assertFalse( "Test dependency artifacts is NOT in archive", archivedFiles.contains( depArtifact.getFile() ) );
            }
        }

        assertFalse( "Test project is NOT in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
    }

    public void testPackedDependencySetExcludes()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "depSet-excludes-plugin-config.xml" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );
        Set artifactSet = project.getArtifacts();

        mojo.execute();

        Map archiveMap = ArchiverManagerStub.archiverStub.getFiles();
        Collection archivedFiles = archiveMap.keySet();

        assertEquals( "Test number of files in archive", artifactSet.size(), archivedFiles.size() );

        for ( Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); )
        {
            Artifact depArtifact = (Artifact) artifacts.next();

            if ( !depArtifact.getArtifactId().equals( "dependency-artifact1" ) )
            {
                assertTrue( "Test expected dependency artifacts in archive", archivedFiles.contains( depArtifact.getFile() ) );
                assertTrue( "Test expected dependency is not unpacked", depArtifact.getFile().getName().endsWith( ".jar" ) );

                JarArchiverStub.ArchiverFile archiveFile =
                    (JarArchiverStub.ArchiverFile) archiveMap.get( depArtifact.getFile() );
                String archivePath = archiveFile.getOutputName();
                assertTrue( "Test includeBaseDirectory", archivePath.startsWith( "assembly/" ) );
            }
            else
            {
                assertFalse( "Test dependency artifacts is NOT in archive", archivedFiles.contains( depArtifact.getFile() ) );
            }
        }

        assertTrue( "Test project is in archive", archivedFiles.contains( project.getArtifact().getFile() ) );
        assertTrue( "Test project is not unpacked", project.getArtifact().getFile().getName().endsWith( ".jar" ) );

        JarArchiverStub.ArchiverFile archiveFile =
            (JarArchiverStub.ArchiverFile) archiveMap.get( project.getArtifact().getFile() );
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
        JarArchiverStub.ArchiverFile file = (JarArchiverStub.ArchiverFile) archiverFiles.get( key );

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
        JarArchiverStub.ArchiverFile file = (JarArchiverStub.ArchiverFile) archiverFiles.get( key );

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
        JarArchiverStub.ArchiverFile file = (JarArchiverStub.ArchiverFile) archiverFiles.get( key );

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
        JarArchiverStub.ArchiverFile file = (JarArchiverStub.ArchiverFile) archiverFiles.get( key );
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
        JarArchiverStub.ArchiverFile file = (JarArchiverStub.ArchiverFile) archiverFiles.get( key );
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
        JarArchiverStub.ArchiverFile file = (JarArchiverStub.ArchiverFile) archiverFiles.get( key );
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
        JarArchiverStub.ArchiverFile file = (JarArchiverStub.ArchiverFile) archiverFiles.get( key );
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

    public void testFileSetWithNoDirectoryInAssemblyXml()
        throws Exception
    {
        AssemblyMojo mojo = executeMojo( "fileSet-no-directory-plugin-config.xml" );

        File basedir = (File) getVariableValueFromObject( mojo, "basedir" );

        Map archivedFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertNotNull( "Test if basedir is used when <fileSet><directory> is not given", archivedFiles.remove( basedir ) );

        assertTrue( "Test that there are no other files added", archivedFiles.isEmpty() );
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

        executeMojo( "fileItem-output-name-plugin-config.xml" );

        Map archiverFiles = ArchiverManagerStub.archiverStub.getFiles();

        assertEquals( "Test archive files", 1, archiverFiles.size() );

        File archivedFile = (File) archiverFiles.keySet().iterator().next();

        JarArchiverStub.ArchiverFile file = (JarArchiverStub.ArchiverFile) archiverFiles.get( archivedFile );

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

        JarArchiverStub.ArchiverFile file = (JarArchiverStub.ArchiverFile) archiverFiles.get( archivedFile );

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

            File metaInf = new File( unpacked, "META-INF" );
            if ( metaInf.exists() && metaInf.listFiles().length > 0 )
            {
                testSignatureFiles( metaInf );
            }

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

        File metaInf = new File( unpacked, "META-INF" );
        if ( metaInf.exists() && metaInf.listFiles().length > 0 )
        {
            testSignatureFiles( metaInf );
        }

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

        File metaInf = new File( unpacked, "META-INF" );
        if ( metaInf.exists() && metaInf.listFiles().length > 0 )
        {
            testSignatureFiles( metaInf );
        }

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

            File metaInf = new File( unpacked, "META-INF" );
            if ( metaInf.exists() && metaInf.listFiles().length > 0 )
            {
                testSignatureFiles( metaInf );
            }

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

    public void testComponents()
        throws Exception
    {
        AssemblyMojo mojo = executeMojo( "component-plugin-config.xml" );

        Map archivedFiles = ArchiverManagerStub.archiverStub.getFiles();

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        assertNotNull( "Test if project jar is in archive", archivedFiles.remove( project.getArtifact().getFile() ) );

        File fileSetDir = new File( PlexusTestCase.getBasedir(), "target/test-classes/fileSet" );
        assertNotNull( "Test if FileSet is in the archive", archivedFiles.remove( fileSetDir ) );
        
        File readme = new File( PlexusTestCase.getBasedir(), "target/test-classes/fileSet/README.txt" );
        assertNotNull( "Test if FileItem README.txt is in the archive", archivedFiles.remove( readme ) );

        File license = new File( PlexusTestCase.getBasedir(), "target/test-classes/fileSet/LICENSE.txt" );
        assertNotNull( "Test if FileItem LICENSE.txt is in the archive", archivedFiles.remove( license ) );

        assertTrue( "Test there are no more files in the archive", archivedFiles.isEmpty() );
    }

    public void testPlexusComponents()
        throws Exception
    {
        File plexusDir = new File( basedir + "/target/test-classes/fileSet/META-INF/plexus" );
        plexusDir.mkdirs();
        String fileContents = "<component-set>\n  <components>\n    <component>\n      <role>class.Role</role>" +
            "\n      <implementation>class.Implementation</implementation>\n    </component>\n  </components>" +
            "\n</component-set>";
        FileUtils.fileWrite( new File( plexusDir, "components.xml" ).getAbsolutePath(), fileContents );

        AssemblyMojo mojo = executeMojo( "plexus-components-plugin-config.xml" );

        Map files = ArchiverManagerStub.archiverStub.getFiles();

        File fileSetDir = new File( PlexusTestCase.getBasedir() + "/target/test-classes/fileSet" );
        assertNotNull( "Test if FileSet is in the archive", files.remove( fileSetDir ) );

        File componentXml = (File) files.keySet().iterator().next();
        files.remove( componentXml );
        assertTrue( "Test if componentXml tmp file was created", componentXml.exists() );
        FileReader fileReader = new FileReader( componentXml );
        assertNotNull( "Test if componentXml is correctly created", Xpp3DomBuilder.build( fileReader ) );

        assertTrue( "Test there are no more files in the archive", files.isEmpty() );
    }

    public void testIncludeSiteDirectory()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "includeSite-plugin-config.xml" );

        File siteDir = (File) getVariableValueFromObject( mojo, "siteDirectory" );

        siteDir.mkdirs();

        mojo.execute();

        assertTrue( "Test an archive was created", ArchiverManagerStub.archiverStub.getDestFile().exists() );

        Map files = ArchiverManagerStub.archiverStub.getFiles();

        assertTrue( "Test if site directory was added", files.containsKey( siteDir ) );
    }

    public void testTarGzipArchive()
        throws Exception
    {
        executeMojo( "tar-gz-plugin-config.xml" );

        TarArchiver.TarCompressionMethod method = (TarArchiver.TarCompressionMethod) getVariableValueFromObject( ArchiverManagerStub.archiverStub, "tarCompressionMethod" );

        assertEquals( "Test Tar compression method", "gzip", method.getValue() );

        TarLongFileMode longFileMode = (TarLongFileMode) getVariableValueFromObject( ArchiverManagerStub.archiverStub, "longFileMode" );

        assertTrue( "Test tar long file mode default", longFileMode.isWarnMode() );
    }

    public void testTarBzip2Archive()
        throws Exception
    {
        executeMojo( "tar-bz2-plugin-config.xml" );

        TarArchiver.TarCompressionMethod method = (TarArchiver.TarCompressionMethod) getVariableValueFromObject( ArchiverManagerStub.archiverStub, "tarCompressionMethod" );

        assertEquals( "Test Tar compression method", "bzip2", method.getValue() );

        TarLongFileMode longFileMode = (TarLongFileMode) getVariableValueFromObject( ArchiverManagerStub.archiverStub, "longFileMode" );

        assertTrue( "Test tar long file mode default", longFileMode.isFailMode() );
    }

    public void testWarArchive()
        throws Exception
    {
        executeMojo( "war-plugin-config.xml" );

        WarArchiverStub archiver = (WarArchiverStub) ArchiverManagerStub.archiverStub;

        assertFalse( "Test that web.xml is not ignored", archiver.getIgnoreWebxml() );
    }

    public void testManifestFile()
        throws Exception
    {
        String contents = "Manifest-Version: 1.0\n" + "Archiver-Version: Plexus Archiver\n" +
            "Created-By: Apache Maven\n" + "Built-By: User\n" + "Build-Jdk: 1.4.2_10\n" +
            "Extension-Name: maven-assembly-plugin\n" + "Specification-Title: Maven Plugins\n" +
            "Specification-Vendor: Apache Software Foundation\n" +
            "Implementation-Vendor: Apache Software Foundation\n" + "Implementation-Title: maven-assembly-plugin\n" +
            "Implementation-Version: 2.2-SNAPSHOT";

        File manifestFile = new File( PlexusTestCase.getBasedir() + "/target/test-harness/assembly/manifestFile/test-manifest.file" );

        manifestFile.getParentFile().mkdirs();

        FileUtils.fileWrite( manifestFile.getAbsolutePath(), contents );

        executeMojo( "manifestFile-plugin-config.xml" );

        JarArchiverStub archiver = (JarArchiverStub) ArchiverManagerStub.archiverStub;

        assertEquals( "Test if provided manifest is used", new Manifest( new FileReader( manifestFile ) ), archiver.getManifest() );
    }

    public void testManifest()
        throws Exception
    {
        executeMojo( "manifest-plugin-config.xml" );

        JarArchiverStub archiver = (JarArchiverStub) ArchiverManagerStub.archiverStub;

        Manifest manifest = archiver.getManifest();

        manifest.write( new PrintWriter( System.out ) );
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

    private void testSignatureFiles( File metaInf )
    {
        File[] metaInfFiles = metaInf.listFiles(
            new FilenameFilter()
            {
                String[] signatureExt = { ".rsa", ".dsa", ".sf" };

                public boolean accept( File dir, String name )
                {
                    for ( int idx = 0; idx < signatureExt.length; idx++ )
                    {
                        if ( name.toLowerCase().endsWith( signatureExt[ idx ] ) )
                        {
                            return true;
                        }
                    }

                    return false;
                }
            }
        );

        assertEquals( "Test for signature files", 0, metaInfFiles.length );
    }
}
