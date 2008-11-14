package org.apache.maven.plugin.assembly.archive.task;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.ArtifactMock;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

public class AddArtifactTaskTest
    extends TestCase
{

    private MockManager mockManager;

    private MockAndControlForAddArtifactTask mac;

    private MavenProject mainProject;

    public void setUp()
        throws IOException
    {
        mockManager = new MockManager();

        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "main" );
        model.setVersion( "1000" );

        mainProject = new MavenProject( model );

        mac = new MockAndControlForAddArtifactTask( mockManager, mainProject );
        mac.expectGetFinalName( "final-name" );
    }

    public void testShouldAddArchiveFileWithoutUnpacking()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        String outputLocation = "artifact";

        ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "artifact", "version", "jar", false );
        File artifactFile = artifactMock.setNewFile();

        mac.expectGetDestFile( new File( "junk" ) );
        mac.expectAddFile( artifactFile, outputLocation );
        
        mockManager.replayAll();

        AddArtifactTask task = createTask( artifactMock.getArtifact() );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithDefaultOutputLocation()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        String artifactId = "myArtifact";
        String version = "1";
        String ext = "jar";
        String outputDir = "tmp/";

        ArtifactMock mock = new ArtifactMock( mockManager, "group", artifactId, version, ext, false );

        File file = mock.setNewFile();
        mock.setExtension( ext );

        mac.expectGetDestFile( new File( "junk" ) );
        mac.expectAddFile( file, outputDir + artifactId + "-" + version + "." + ext );
        
        mockManager.replayAll();

        AddArtifactTask task = new AddArtifactTask( mock.getArtifact(), new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        task.setOutputDirectory( outputDir );
        task.setFileNameMapping( new DependencySet().getOutputFileNameMapping() );

        Model model = new Model();
        model.setArtifactId( artifactId );
        model.setVersion( version );

        MavenProject project = new MavenProject( model );
        task.setProject( project );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

    private AddArtifactTask createTask( Artifact artifact )
    {
        AddArtifactTask task = new AddArtifactTask( artifact, new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        task.setFileNameMapping( "artifact" );

        return task;
    }

    public void testShouldAddArchiveFileWithUnpack()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        mac.expectModeChange( -1, -1, -1, -1, 1 );

        ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "artifact", "version", "jar", false );
        File artifactFile = artifactMock.setNewFile();

        String outputLocation = "";

        mac.expectGetDestFile( new File( "junk" ) );
        try
        {
            mac.archiver.addArchivedFileSet( artifactFile, outputLocation, AddArtifactTask.DEFAULT_INCLUDES_ARRAY, null );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = createTask( artifactMock.getArtifact() );

        task.setUnpack( true );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpackAndModes()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        int directoryMode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        int fileMode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        mac.expectModeChange( -1, -1, directoryMode, fileMode, 2 );
//        mac.expectIsSnapshot( false );

        String outputLocation = "";

        ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "artifact", "version", "jar", false );
        File artifactFile = artifactMock.setNewFile();

        mac.expectGetDestFile( new File( "junk" ) );
        try
        {
            mac.archiver.addArchivedFileSet( artifactFile, outputLocation, AddArtifactTask.DEFAULT_INCLUDES_ARRAY, null );
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AddArtifactTask task = createTask( artifactMock.getArtifact() );

        task.setUnpack( true );
        
        task.setDirectoryMode( directoryMode );
        task.setFileMode( fileMode );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

    public void testShouldAddArchiveFileWithUnpackIncludesAndExcludes()
        throws ArchiveCreationException, AssemblyFormattingException, IOException
    {
        mac.expectModeChange( -1, -1, -1, -1, 1 );

        String outputLocation = "";

        String[] includes = { "**/*.txt" };
        String[] excludes = { "**/README.txt" };

        ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "artifact", "version", "jar", false );
        File artifactFile = artifactMock.setNewFile();

        mac.expectGetDestFile( new File( "junk" ) );
        mac.expectAddArchivedFileSet( artifactFile, outputLocation, includes, excludes );

        mockManager.replayAll();

        AddArtifactTask task = createTask( artifactMock.getArtifact() );

        task.setUnpack( true );
        task.setIncludes( Arrays.asList( includes ) );
        task.setExcludes( Arrays.asList( excludes ) );

        task.execute( mac.archiver, mac.configSource );

        mockManager.verifyAll();
    }

}
