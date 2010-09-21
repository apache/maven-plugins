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
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.ArtifactMock;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddDependencySetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class AddDependencySetsTaskTest
    extends TestCase
{

    private final MockManager mockManager = new MockManager();

    public void testAddDependencySet_ShouldInterpolateDefaultOutputFileNameMapping()
        throws AssemblyFormattingException, ArchiveCreationException, InvalidAssemblerConfigurationException,
        IOException
    {
        final String outDir = "tmp/";
        final String mainAid = "main";
        final String mainGid = "org.maingrp";
        final String mainVer = "9";
        final String depAid = "dep";
        final String depGid = "org.depgrp";
        final String depVer = "1";
        final String depExt = "war";

        final DependencySet ds = new DependencySet();
        ds.setOutputDirectory( outDir );
        ds.setDirectoryMode( Integer.toString( 10, 8 ) );
        ds.setFileMode( Integer.toString( 10, 8 ) );

        final Model mainModel = new Model();
        mainModel.setArtifactId( mainAid );
        mainModel.setGroupId( mainGid );
        mainModel.setVersion( mainVer );

        final MavenProject mainProject = new MavenProject( mainModel );

        final ArtifactMock mainArtifactMock = new ArtifactMock( mockManager, mainGid, mainAid, mainVer, "jar", false );

        mainProject.setArtifact( mainArtifactMock.getArtifact() );

        final Model depModel = new Model();
        depModel.setArtifactId( depAid );
        depModel.setGroupId( depGid );
        depModel.setVersion( depVer );
        depModel.setPackaging( depExt );

        final MavenProject depProject = new MavenProject( depModel );

        final ArtifactMock depArtifactMock = new ArtifactMock( mockManager, depGid, depAid, depVer, depExt, false );

        final File newFile = depArtifactMock.setNewFile();

        depProject.setArtifact( depArtifactMock.getArtifact() );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mockManager, mainProject );

        macTask.expectBuildFromRepository( depProject );
        macTask.expectCSGetFinalName( mainAid + "-" + mainVer );

        macTask.expectCSGetRepositories( null, null );

        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( newFile, outDir + depAid + "-" + depVer + "." + depExt, 10 );

        macTask.expectGetSession( null );

        mockManager.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ),
                                       Collections.singleton( depArtifactMock.getArtifact() ), depProject,
                                       macTask.projectBuilder, logger );

        task.addDependencySet( ds, macTask.archiver, macTask.configSource );

        mockManager.verifyAll();
    }

    public void testAddDependencySet_ShouldNotAddDependenciesWhenProjectHasNone()
        throws AssemblyFormattingException, ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        final MavenProject project = new MavenProject( new Model() );

        final MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager );

        final DependencySet ds = new DependencySet();
        ds.setOutputDirectory( "/out" );

        mockManager.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ), null, project, macTask.projectBuilder, logger );

        task.addDependencySet( ds, null, macTask.configSource );

        mockManager.verifyAll();
    }

    // TODO: Find a better way of testing the project-stubbing behavior when a ProjectBuildingException takes place.
    public void testAddDependencySet_ShouldNotAddDependenciesWhenProjectIsStubbed()
        throws AssemblyFormattingException, ArchiveCreationException, InvalidAssemblerConfigurationException,
        IOException
    {
        final MavenProject project = new MavenProject( new Model() );

        final ProjectBuildingException pbe = new ProjectBuildingException( "test", "Test error." );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mockManager, new MavenProject( new Model() ) );

        final String gid = "org.test";
        final String aid = "test-dep";
        final String version = "2.0-SNAPSHOT";
        final String type = "jar";

        final File file = new File( "dep-artifact.jar" );

        final ArtifactMock depMock = new ArtifactMock( mockManager, gid, aid, version, type, true );
        depMock.setBaseVersion( version );
        depMock.setFile( file );

        final File destFile = new File( "assembly-dep-set.zip" );

        macTask.expectGetDestFile( destFile );
        macTask.expectBuildFromRepository( pbe );
        macTask.expectCSGetRepositories( null, null );
        macTask.expectCSGetFinalName( "final-name" );
        macTask.expectAddFile( file, "out/" + aid + "-" + version + "." + type );

        macTask.expectGetSession( null );

        final DependencySet ds = new DependencySet();
        ds.setOutputDirectory( "/out" );

        mockManager.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ), Collections.singleton( depMock.getArtifact() ),
                                       project, macTask.projectBuilder, logger );

        task.addDependencySet( ds, macTask.archiver, macTask.configSource );

        mockManager.verifyAll();
    }

    public void testAddDependencySet_ShouldAddOneDependencyFromProjectWithoutUnpacking()
        throws AssemblyFormattingException, ArchiveCreationException, IOException,
        InvalidAssemblerConfigurationException
    {
        verifyOneDependencyAdded( "out", false );
    }

    public void testAddDependencySet_ShouldAddOneDependencyFromProjectUnpacked()
        throws AssemblyFormattingException, ArchiveCreationException, IOException,
        InvalidAssemblerConfigurationException
    {
        verifyOneDependencyAdded( "out", true );
    }

    private void verifyOneDependencyAdded( final String outputLocation, final boolean unpack )
        throws AssemblyFormattingException, ArchiveCreationException, IOException,
        InvalidAssemblerConfigurationException
    {
        final MavenProject project = new MavenProject( new Model() );

        final DependencySet ds = new DependencySet();
        ds.setOutputDirectory( outputLocation );
        ds.setOutputFileNameMapping( "artifact" );
        ds.setUnpack( unpack );
        ds.setScope( Artifact.SCOPE_COMPILE );

        ds.setDirectoryMode( Integer.toString( 10, 8 ) );
        ds.setFileMode( Integer.toString( 10, 8 ) );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mockManager, new MavenProject( new Model() ) );

        final ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "artifact", "version", "jar", false );
        final File artifactFile = artifactMock.setNewFile();

        if ( unpack )
        {
            macTask.expectAddArchivedFileSet( artifactFile, outputLocation + "/",
                                              AddArtifactTask.DEFAULT_INCLUDES_ARRAY, null );
            macTask.expectModeChange( -1, -1, 10, 10, 2 );
        }
        else
        {
            macTask.expectAddFile( artifactFile, outputLocation + "/artifact", 10 );
        }

        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectCSGetFinalName( "final-name" );
        macTask.expectCSGetRepositories( null, null );

        macTask.expectGetSession( null );

        final MavenProject depProject = new MavenProject( new Model() );

        macTask.expectBuildFromRepository( depProject );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ),
                                       Collections.singleton( artifactMock.getArtifact() ), project,
                                       macTask.projectBuilder, logger );

        mockManager.replayAll();

        task.addDependencySet( ds, macTask.archiver, macTask.configSource );

        mockManager.verifyAll();
    }

    public void testGetDependencyArtifacts_ShouldGetOneDependencyArtifact()
        throws ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        final MavenProject project = new MavenProject( new Model() );

        final MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager );

        final ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "artifact", "version", "jar", false );

        project.setArtifacts( Collections.singleton( artifactMock.getArtifact() ) );

        final DependencySet dependencySet = new DependencySet();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mockManager.replayAll();

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( dependencySet ),
                                       Collections.singleton( artifactMock.getArtifact() ), project,
                                       macTask.projectBuilder, logger );

        final Set<Artifact> result = task.resolveDependencyArtifacts( dependencySet );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( artifactMock.getArtifact(), result.iterator()
                                                      .next() );

        mockManager.verifyAll();
    }

    public void testGetDependencyArtifacts_ShouldFilterOneDependencyArtifactViaInclude()
        throws ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        final MavenProject project = new MavenProject( new Model() );

        final Set<Artifact> artifacts = new HashSet<Artifact>();

        final ArtifactMock am = new ArtifactMock( mockManager, "group", "artifact", "1.0", "jar", false );
        am.setDependencyTrail( Collections.singletonList( project.getId() ) );
        artifacts.add( am.getArtifact() );

        final ArtifactMock am2 = new ArtifactMock( mockManager, "group2", "artifact2", "1.0", "jar", false );
        am2.setDependencyTrail( Collections.singletonList( project.getId() ) );
        artifacts.add( am2.getArtifact() );

        final DependencySet dependencySet = new DependencySet();

        dependencySet.addInclude( "group:artifact" );
        dependencySet.setUseTransitiveFiltering( true );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mockManager.replayAll();

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( dependencySet ), artifacts, project, null, logger );

        final Set<Artifact> result = task.resolveDependencyArtifacts( dependencySet );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( am.getArtifact(), result.iterator()
                                            .next() );

        mockManager.verifyAll();
    }

    public void testGetDependencyArtifacts_ShouldIgnoreTransitivePathFilteringWhenIncludeNotTransitive()
        throws ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        final MavenProject project = new MavenProject( new Model() );

        final Set<Artifact> artifacts = new HashSet<Artifact>();

        final ArtifactMock am = new ArtifactMock( mockManager, "group", "artifact", "1.0", "jar", false );
        artifacts.add( am.getArtifact() );

        final ArtifactMock am2 = new ArtifactMock( mockManager, "group2", "artifact2", "1.0", "jar", false );
        artifacts.add( am2.getArtifact() );

        final DependencySet dependencySet = new DependencySet();

        dependencySet.addInclude( "group:artifact" );
        dependencySet.setUseTransitiveFiltering( false );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mockManager.replayAll();

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( dependencySet ), artifacts, project, null, logger );

        final Set<Artifact> result = task.resolveDependencyArtifacts( dependencySet );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( am.getArtifact(), result.iterator()
                                            .next() );

        mockManager.verifyAll();
    }

}
