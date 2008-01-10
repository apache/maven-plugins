package org.apache.maven.plugin.assembly.archive.task.testutils;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.easymock.MockControl;

import java.io.File;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

public class MockAndControlForAddDependencySetsTask
{

    public Archiver archiver;

    public MockControl archiverCtl;

    public AssemblerConfigurationSource configSource;

    public MockControl configSourceCtl;

    public DependencyResolver dependencyResolver;

    public MockControl dependencyResolverCtl;

    public MavenProjectBuilder projectBuilder;

    public MockControl projectBuilderCtl;

    private MavenProject project;

    public MockAndControlForAddDependencySetsTask( MockManager mockManager )
    {
        this( mockManager, null );
    }

    public MockAndControlForAddDependencySetsTask( MockManager mockManager, MavenProject project )
    {
        this.project = project;

        archiverCtl = MockControl.createControl( Archiver.class );
        mockManager.add( archiverCtl );

        archiver = (Archiver) archiverCtl.getMock();

        configSourceCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceCtl );

        configSource = (AssemblerConfigurationSource) configSourceCtl.getMock();

        dependencyResolverCtl = MockControl.createControl( DependencyResolver.class );
        mockManager.add( dependencyResolverCtl );

        dependencyResolver = ( DependencyResolver ) dependencyResolverCtl.getMock();

        projectBuilderCtl = MockControl.createControl( MavenProjectBuilder.class );
        mockManager.add( projectBuilderCtl );

        projectBuilder = ( MavenProjectBuilder ) projectBuilderCtl.getMock();

        enableDefaultExpectations();
    }

    private void enableDefaultExpectations()
    {
        configSource.getProject();
        configSourceCtl.setReturnValue( project, MockControl.ZERO_OR_MORE );
    }

    public void expectAddArchivedFileSet( File file, String outputLocation, String[] includes, String[] excludes )
    {
        try
        {
            archiver.addArchivedFileSet( file, outputLocation, includes, excludes );

            if ( ( includes != null ) || ( excludes != null ) )
            {
                archiverCtl.setMatcher( MockControl.ARRAY_MATCHER );
            }

            archiverCtl.setVoidCallable( MockControl.ONE_OR_MORE );
        }
        catch ( ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectModeChange( int originalDirMode, int originalFileMode, int dirMode, int fileMode,
                                            int numberOfChanges )
    {
        archiver.getDefaultDirectoryMode();
        archiverCtl.setReturnValue( originalDirMode );

        archiver.getDefaultFileMode();
        archiverCtl.setReturnValue( originalFileMode );

        // one of the changes will occur below, when we restore the original mode.
        if ( numberOfChanges > 1 )
        {
            for( int i = 1; i< numberOfChanges; i++ )
            {
                archiver.setDefaultDirectoryMode( dirMode );
                archiver.setDefaultFileMode( fileMode );
            }
        }

        archiver.setDefaultDirectoryMode( originalDirMode );
        archiver.setDefaultFileMode( originalFileMode );
    }

    public void expectAddFile( File file, String outputLocation )
    {
        try
        {
            archiver.addFile( file, outputLocation );
        }
        catch ( ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectAddFile( File file, String outputLocation, int fileMode )
    {
        try
        {
            archiver.addFile( file, outputLocation, fileMode );
        }
        catch ( ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectGetReactorProjects( List projects )
    {
        configSource.getReactorProjects();
        configSourceCtl.setReturnValue( projects, MockControl.ONE_OR_MORE );
    }

    public void expectCSGetFinalName( String finalName )
    {
        configSource.getFinalName();
        configSourceCtl.setReturnValue( finalName, MockControl.ONE_OR_MORE );
    }

    public void expectCSGetRepositories( ArtifactRepository localRepo, List remoteRepos )
    {
        configSource.getLocalRepository();
        configSourceCtl.setReturnValue( localRepo, MockControl.ONE_OR_MORE );

        configSource.getRemoteRepositories();
        configSourceCtl.setReturnValue( remoteRepos, MockControl.ONE_OR_MORE );
    }

    public void expectBuildFromRepository( MavenProject project )
    {
        try
        {
            projectBuilder.buildFromRepository( null, null, null, true );
            projectBuilderCtl.setMatcher( MockControl.ALWAYS_MATCHER );
            projectBuilderCtl.setReturnValue( project, MockControl.ONE_OR_MORE );
        }
        catch ( ProjectBuildingException e )
        {
            Assert.fail( "should never happen" );
        }
    }

    public void expectResolveDependencies( Set resolvedArtifacts )
    {
        try
        {
            dependencyResolver.resolveDependencies( null, null, null, null, true );
        }
        catch ( ArtifactResolutionException e )
        {
            Assert.fail( "should never happen!" );
        }
        catch ( ArtifactNotFoundException e )
        {
            Assert.fail( "should never happen!" );
        }
        catch ( InvalidDependencyVersionException e )
        {
            Assert.fail( "should never happen!" );
        }

        dependencyResolverCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        dependencyResolverCtl.setReturnValue( resolvedArtifacts, MockControl.ONE_OR_MORE );
    }

}
