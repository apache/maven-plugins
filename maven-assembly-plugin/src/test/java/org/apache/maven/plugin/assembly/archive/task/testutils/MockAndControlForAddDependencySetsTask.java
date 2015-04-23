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

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.easymock.EasyMock;
import org.easymock.classextension.EasyMockSupport;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

public class MockAndControlForAddDependencySetsTask
{

    public final Archiver archiver;

    public final AssemblerConfigurationSource configSource;

    public final MavenProjectBuilder projectBuilder;

    public final ArchiverManager archiverManager;

    private final MavenProject project;

    public final DependencyResolver dependencyResolver;


    public MockAndControlForAddDependencySetsTask( final EasyMockSupport mockManager )
    {
        this( mockManager, null );
    }

    public MockAndControlForAddDependencySetsTask( final EasyMockSupport mockManager, final MavenProject project )
    {
        this.project = project;

        archiver = mockManager.createMock(Archiver.class);
        configSource = mockManager.createMock (AssemblerConfigurationSource.class);


        projectBuilder = mockManager.createMock(MavenProjectBuilder.class);

        archiverManager = mockManager.createMock(ArchiverManager.class);

        dependencyResolver = mockManager.createMock( DependencyResolver.class);


        enableDefaultExpectations();
    }

    private void enableDefaultExpectations()
    {
        expect(configSource.getProject()).andReturn( project ).anyTimes();
    }

    public void expectAddArchivedFileSet()
    {
        try
        {
            archiver.addArchivedFileSet( (File) anyObject(), (String) anyObject(), (String[]) anyObject(),
                                             (String[]) anyObject() );
            EasyMock.expectLastCall().anyTimes();
            archiver.addArchivedFileSet( (ArchivedFileSet)anyObject(), (Charset) anyObject()  );
            EasyMock.expectLastCall().anyTimes();

        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectModeChange( final int originalDirMode, final int originalFileMode, final int dirMode,
                                  final int fileMode, final int numberOfChanges )
    {
        expectGetMode( originalDirMode, originalFileMode );
        // one of the changes will occur below, when we restore the original mode.
        if ( numberOfChanges > 1 )
        {
            for ( int i = 1; i < numberOfChanges; i++ )
            {
                archiver.setDirectoryMode( dirMode );
                archiver.setFileMode( fileMode );
            }
        }

        archiver.setDirectoryMode( originalDirMode );
        archiver.setFileMode( originalFileMode );
    }

    public void expectGetMode( final int originalDirMode, final int originalFileMode )
    {
        archiver.setFileMode( anyInt() );
        EasyMock.expectLastCall().anyTimes();
        expect( archiver.getOverrideDirectoryMode()).andReturn( originalDirMode );
        expect( archiver.getOverrideFileMode() ).andReturn( originalFileMode );
        archiver.setDirectoryMode( anyInt() );
        EasyMock.expectLastCall().anyTimes();

    }

    public void expectAddFile( final File file, final String outputLocation )
    {
        try
        {
            archiver.addFile( file, outputLocation );
        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectAddFile( final File file, final String outputLocation, final int fileMode )
    {
        try
        {
            archiver.addFile( file, outputLocation, fileMode );
        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectAddAnyFile(  )
    {
        try
        {
            archiver.addFile( (File) anyObject(), (String) anyObject(), anyInt());
        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectGetReactorProjects( final List<MavenProject> projects )
    {
        expect(configSource.getReactorProjects()).andReturn( projects ).anyTimes();
    }

    public void expectCSGetFinalName( final String finalName )
    {
        expect(configSource.getFinalName()).andReturn( finalName ).anyTimes();
    }

    public void expectGetDestFile( final File destFile )
    {
        expect(archiver.getDestFile()).andReturn( destFile ).anyTimes();
    }

    public void expectCSGetRepositories( final ArtifactRepository localRepo, final List<ArtifactRepository> remoteRepos )
    {
        expect(configSource.getLocalRepository()).andReturn( localRepo ).anyTimes();
        expect(configSource.getRemoteRepositories()).andReturn( remoteRepos ).anyTimes();
    }

    public void expectBuildFromRepository( final ProjectBuildingException error )
    {
        try
        {
            expect(projectBuilder.buildFromRepository( (Artifact) anyObject(), (List) anyObject(),
                                                (ArtifactRepository) anyObject() )).andThrow( error );
//            projectBuilderCtl.setThrowable( error, MockControl.ONE_OR_MORE );
        }
        catch ( final ProjectBuildingException e )
        {
            Assert.fail( "should never happen" );
        }
    }

    public void expectBuildFromRepository( final MavenProject project )
    {
        try
        {
            expect(projectBuilder.buildFromRepository( ( Artifact) anyObject(), (List)anyObject(), (ArtifactRepository)anyObject() )).andReturn( project ).anyTimes();
        }
        catch ( final ProjectBuildingException e )
        {
            Assert.fail( "should never happen" );
        }
    }

    public void expectGetSession( final MavenSession session )
    {
        expect(configSource.getMavenSession()).andReturn( session ).anyTimes();
    }

    public void expectResolveDependencySets()
        throws DependencyResolutionException
    {
        expect( dependencyResolver.resolveDependencySets( (Assembly) anyObject(),
                                                          (AssemblerConfigurationSource) anyObject(), (List<DependencySet>) anyObject() ))
            .andReturn( new LinkedHashMap<DependencySet, Set<Artifact>>(  ) ).anyTimes();

    }


}
