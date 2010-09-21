package org.apache.maven.plugin.assembly.archive.phase;

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
import org.apache.maven.plugin.assembly.DefaultAssemblyContext;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.ArtifactMock;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddDependencySetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;

public class DependencySetAssemblyPhaseTest
    extends TestCase
{

    private final MockManager mockManager = new MockManager();

    public void testExecute_ShouldAddOneDependencyFromProject()
        throws AssemblyFormattingException, ArchiveCreationException, IOException,
        InvalidAssemblerConfigurationException
    {
        final String outputLocation = "/out";

        final MavenProject project = newMavenProject( "group", "project", "0" );

        final ArtifactMock projectArtifactMock = new ArtifactMock( mockManager, "group", "project", "0", "jar", false );

        project.setArtifact( projectArtifactMock.getArtifact() );

        final DependencySet ds = new DependencySet();
        ds.setUseProjectArtifact( false );
        ds.setOutputDirectory( outputLocation );
        ds.setOutputFileNameMapping( "${artifact.artifactId}" );
        ds.setUnpack( false );
        ds.setScope( Artifact.SCOPE_COMPILE );
        ds.setFileMode( Integer.toString( 10, 8 ) );

        final Assembly assembly = new Assembly();

        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );
        assembly.addDependencySet( ds );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mockManager, project );

        final ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "dep", "1", "jar", false );
        final File artifactFile = artifactMock.setNewFile();

        System.out.println( "On test setup, hashcode for dependency artifact: " + artifactMock.getArtifact()
                                                                                              .hashCode() );

        macTask.expectCSGetRepositories( null, null );

        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( artifactFile, "out/dep", 10 );

        macTask.expectGetSession( null );

        project.setArtifacts( Collections.singleton( artifactMock.getArtifact() ) );

        macTask.expectCSGetFinalName( "final-name" );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final MavenProject depProject = newMavenProject( "group", "dep", "1" );

        macTask.expectBuildFromRepository( depProject );

        mockManager.replayAll();

        final DefaultAssemblyContext context = new DefaultAssemblyContext();
        context.setResolvedArtifacts( Collections.singleton( artifactMock.getArtifact() ) );

        createPhase( macTask, logger ).execute( assembly, macTask.archiver, macTask.configSource, context );

        mockManager.verifyAll();
    }

    private MavenProject newMavenProject( final String groupId, final String artifactId, final String version )
    {
        final Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );

        return new MavenProject( model );
    }

    public void testExecute_ShouldNotAddDependenciesWhenProjectHasNone()
        throws AssemblyFormattingException, ArchiveCreationException, IOException,
        InvalidAssemblerConfigurationException
    {
        final Assembly assembly = new Assembly();

        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mockManager, null );

        mockManager.replayAll();

        createPhase( macTask, logger ).execute( assembly, null, macTask.configSource, new DefaultAssemblyContext() );

        mockManager.verifyAll();
    }

    private DependencySetAssemblyPhase createPhase( final MockAndControlForAddDependencySetsTask macTask,
                                                    final Logger logger )
    {
        MavenProjectBuilder projectBuilder = null;

        if ( macTask != null )
        {
            projectBuilder = macTask.projectBuilder;
        }

        final DependencySetAssemblyPhase phase = new DependencySetAssemblyPhase( projectBuilder, logger );

        phase.enableLogging( logger );

        return phase;
    }

}
