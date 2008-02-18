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
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForArtifact;
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

    private MockManager mockManager = new MockManager();

    public void testAddDependencySet_ShouldInterpolateDefaultOutputFileNameMapping()
        throws AssemblyFormattingException, ArchiveCreationException,
        InvalidAssemblerConfigurationException, IOException
    {
        String outDir = "tmp/";
        String mainAid = "main";
        String mainGid = "org.maingrp";
        String mainVer = "9";
        String depAid = "dep";
        String depGid = "org.depgrp";
        String depVer = "1";
        String depExt = "war";

        DependencySet ds = new DependencySet();
        ds.setOutputDirectory( outDir );
        ds.setDirectoryMode( Integer.toString( 10, 8 ) );
        ds.setFileMode( Integer.toString( 10, 8 ) );

        Model mainModel = new Model();
        mainModel.setArtifactId( mainAid );
        mainModel.setGroupId( mainGid );
        mainModel.setVersion( mainVer );

        MavenProject mainProject = new MavenProject( mainModel );

        ArtifactMock mainArtifactMock = new ArtifactMock( mockManager, mainGid, mainAid, mainVer,
                                                          "jar", false );

        mainProject.setArtifact( mainArtifactMock.getArtifact() );

        Model depModel = new Model();
        depModel.setArtifactId( depAid );
        depModel.setGroupId( depGid );
        depModel.setVersion( depVer );
        depModel.setPackaging( depExt );

        MavenProject depProject = new MavenProject( depModel );

        ArtifactMock depArtifactMock = new ArtifactMock( mockManager, depGid, depAid, depVer,
                                                         depExt, false );

        File newFile = depArtifactMock.setNewFile();

        depProject.setArtifact( depArtifactMock.getArtifact() );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask(
                                                                                                     mockManager,
                                                                                                     mainProject );

        macTask.expectBuildFromRepository( depProject );
        macTask.expectCSGetFinalName( mainAid + "-" + mainVer );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( depArtifactMock.getArtifact() ) );

        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( newFile, outDir + depAid + "-" + depVer + "." + depExt, 10 );

        mockManager.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        AddDependencySetsTask task = new AddDependencySetsTask( Collections.singletonList( ds ),
                                                                depProject, macTask.projectBuilder,
                                                                macTask.dependencyResolver, logger );

        task.addDependencySet( ds, macTask.archiver, macTask.configSource );

        mockManager.verifyAll();
    }

    public void testAddDependencySet_ShouldNotAddDependenciesWhenProjectHasNone()
        throws AssemblyFormattingException, ArchiveCreationException,
        InvalidAssemblerConfigurationException
    {
        MavenProject project = new MavenProject( new Model() );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask(
                                                                                                     mockManager );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.EMPTY_SET );

        DependencySet ds = new DependencySet();
        ds.setOutputDirectory( "/out" );

        mockManager.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        AddDependencySetsTask task = new AddDependencySetsTask( Collections.singletonList( ds ),
                                                                project, macTask.projectBuilder,
                                                                macTask.dependencyResolver, logger );

        task.addDependencySet( ds, null, macTask.configSource );

        mockManager.verifyAll();
    }

    // TODO: Find a better way of testing the project-stubbing behavior when a ProjectBuildingException takes place.
    public void testAddDependencySet_ShouldNotAddDependenciesWhenProjectIsStubbed()
        throws AssemblyFormattingException, ArchiveCreationException,
        InvalidAssemblerConfigurationException, IOException
    {
        MavenProject project = new MavenProject( new Model() );

        ProjectBuildingException pbe = new ProjectBuildingException( "test", "Test error." );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask(
                                                                                                     mockManager );

        String gid = "org.test";
        String aid = "test-dep";
        String version = "2.0-SNAPSHOT";
        String type = "jar";

        File file = new File( "dep-artifact.jar" );

        ArtifactMock depMock = new ArtifactMock( mockManager, gid, aid, version, type, true );
        depMock.setBaseVersion( version );
        depMock.setFile( file );

        File destFile = new File( "assembly-dep-set.zip" );

        macTask.expectGetDestFile( destFile );
        macTask.expectBuildFromRepository( pbe );
        macTask.expectCSGetRepositories( null, null );
        macTask.expectCSGetFinalName( "final-name" );
        macTask.expectResolveDependencies( Collections.singleton( depMock.getArtifact() ) );
        macTask.expectAddFile( file, "out/" + aid + "-" + version + "." + type );

        DependencySet ds = new DependencySet();
        ds.setOutputDirectory( "/out" );

        mockManager.replayAll();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        AddDependencySetsTask task = new AddDependencySetsTask( Collections.singletonList( ds ),
                                                                project, macTask.projectBuilder,
                                                                macTask.dependencyResolver, logger );

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

    private void verifyOneDependencyAdded( String outputLocation,
                                           boolean unpack )
        throws AssemblyFormattingException, ArchiveCreationException, IOException,
        InvalidAssemblerConfigurationException
    {
        MavenProject project = new MavenProject( new Model() );

        DependencySet ds = new DependencySet();
        ds.setOutputDirectory( outputLocation );
        ds.setOutputFileNameMapping( "artifact" );
        ds.setUnpack( unpack );
        ds.setScope( Artifact.SCOPE_COMPILE );

        ds.setDirectoryMode( Integer.toString( 10, 8 ) );
        ds.setFileMode( Integer.toString( 10, 8 ) );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask(
                                                                                                     mockManager );

        ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "artifact", "version",
                                                      "jar", false );
        File artifactFile = artifactMock.setNewFile();

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( artifactMock.getArtifact() ) );

        if ( unpack )
        {
            macTask.expectAddArchivedFileSet( artifactFile,
                                              outputLocation + "/",
                                              AddArtifactTask.DEFAULT_INCLUDES_ARRAY,
                                              null );
            macTask.expectModeChange( -1, -1, 10, 10, 2 );
        }
        else
        {
            macTask.expectAddFile( artifactFile, outputLocation + "/artifact", 10 );
        }

        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectCSGetFinalName( "final-name" );

        MavenProject depProject = new MavenProject( new Model() );

        macTask.expectBuildFromRepository( depProject );

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        AddDependencySetsTask task = new AddDependencySetsTask( Collections.singletonList( ds ),
                                                                project, macTask.projectBuilder,
                                                                macTask.dependencyResolver, logger );

        mockManager.replayAll();

        task.addDependencySet( ds, macTask.archiver, macTask.configSource );

        mockManager.verifyAll();
    }

    public void testGetDependencyArtifacts_ShouldGetOneDependencyArtifact()
        throws ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        MavenProject project = new MavenProject( new Model() );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask(
                                                                                                     mockManager );

        ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "artifact", "version",
                                                      "jar", false );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( artifactMock.getArtifact() ) );

        project.setArtifacts( Collections.singleton( artifactMock.getArtifact() ) );

        DependencySet dependencySet = new DependencySet();

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mockManager.replayAll();

        AddDependencySetsTask task = new AddDependencySetsTask(
                                                                Collections.singletonList( dependencySet ),
                                                                project, macTask.projectBuilder,
                                                                macTask.dependencyResolver, logger );

        Set result = task.resolveDependencyArtifacts( dependencySet, macTask.configSource );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( artifactMock.getArtifact(), result.iterator().next() );

        mockManager.verifyAll();
    }

    public void testGetDependencyArtifacts_ShouldFilterOneDependencyArtifactViaInclude()
        throws ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        MavenProject project = new MavenProject( new Model() );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask(
                                                                                                     mockManager );

        Set artifacts = new HashSet();

        MockAndControlForArtifact mac = new MockAndControlForArtifact( mockManager );

        mac.expectGetGroupId( "group" );
        mac.expectGetArtifactId( "artifact" );
        mac.expectGetDependencyConflictId( "group:artifact:jar" );
        mac.expectGetId( "group:artifact:1.0" );

        artifacts.add( mac.artifact );

        MockAndControlForArtifact mac2 = new MockAndControlForArtifact( mockManager );

        mac2.expectGetGroupId( "group2" );
        mac2.expectGetArtifactId( "artifact2" );
        mac2.expectGetDependencyConflictId( "group2:artifact2:jar" );
        mac2.expectGetDependencyTrail( Collections.EMPTY_LIST );
        mac2.expectGetId( "group2:artifact2:1.0" );

        artifacts.add( mac2.artifact );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( artifacts );

        DependencySet dependencySet = new DependencySet();

        dependencySet.addInclude( "group:artifact" );
        dependencySet.setUseTransitiveFiltering( true );

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mockManager.replayAll();

        AddDependencySetsTask task = new AddDependencySetsTask(
                                                                Collections.singletonList( dependencySet ),
                                                                project, null,
                                                                macTask.dependencyResolver, logger );

        Set result = task.resolveDependencyArtifacts( dependencySet, macTask.configSource );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( mac.artifact, result.iterator().next() );

        mockManager.verifyAll();
    }

    public void testGetDependencyArtifacts_ShouldIgnoreTransitivePathFilteringWhenIncludeNotTransitive()
        throws ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        MavenProject project = new MavenProject( new Model() );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask(
                                                                                                     mockManager );

        Set artifacts = new HashSet();

        MockAndControlForArtifact mac = new MockAndControlForArtifact( mockManager );

        mac.expectGetGroupId( "group" );
        mac.expectGetArtifactId( "artifact" );
        mac.expectGetDependencyConflictId( "group:artifact:jar" );
        mac.expectGetId( "group:artifact:1.0" );

        artifacts.add( mac.artifact );

        MockAndControlForArtifact mac2 = new MockAndControlForArtifact( mockManager );

        mac2.expectGetGroupId( "group2" );
        mac2.expectGetArtifactId( "artifact2" );
        mac2.expectGetDependencyConflictId( "group2:artifact2:jar" );
        mac2.expectGetId( "group2:artifact2:1.0" );

        artifacts.add( mac2.artifact );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( artifacts );

        DependencySet dependencySet = new DependencySet();

        dependencySet.addInclude( "group:artifact" );
        dependencySet.setUseTransitiveFiltering( false );

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        mockManager.replayAll();

        AddDependencySetsTask task = new AddDependencySetsTask(
                                                                Collections.singletonList( dependencySet ),
                                                                project, null,
                                                                macTask.dependencyResolver, logger );

        Set result = task.resolveDependencyArtifacts( dependencySet, macTask.configSource );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( mac.artifact, result.iterator().next() );

        mockManager.verifyAll();
    }

}
