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
import java.util.List;

import junit.framework.Assert;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.easymock.EasyMock;
import org.easymock.classextension.EasyMockSupport;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

public class MockAndControlForAddArtifactTask
{

    public final Archiver archiver;

    public AssemblerConfigurationSource configSource;

    private MavenProject project = null;

    public MockAndControlForAddArtifactTask( final EasyMockSupport mockManager )
    {
        this( mockManager, null );
    }

    public MockAndControlForAddArtifactTask( final EasyMockSupport mockManager, final MavenProject project )
    {
        this.project = project;

        archiver = mockManager.createMock(Archiver.class);
        configSource = mockManager.createMock(AssemblerConfigurationSource.class);

        enableDefaultExpectations();
    }

    private void enableDefaultExpectations()
    {
        expect( configSource.getProject()).andReturn( project ).anyTimes();
        expect( configSource.getMavenSession()).andReturn( null ).anyTimes();
    }

    public void expectGetFinalName( final String finalName )
    {
        expect(configSource.getFinalName()).andReturn( finalName ).atLeastOnce();
    }

    public void expectGetDestFile( final File destFile )
    {
        expect(archiver.getDestFile()).andReturn( destFile ).atLeastOnce();
    }

    public void expectAddArchivedFileSet()
    {
        try
        {
            archiver.addArchivedFileSet( (File)anyObject(), (String)anyObject(), (String[])anyObject(), (String[])anyObject() );
            EasyMock.expectLastCall().anyTimes();
            archiver.addArchivedFileSet((ArchivedFileSet) anyObject());
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
        expect( archiver.getOverrideDirectoryMode()).andReturn( originalDirMode );
        expect(archiver.getOverrideFileMode()).andReturn( originalFileMode );

        // one of the changes will occur below, when we restore the original mode.
        if ( numberOfChanges > 1 )
        {
            for ( int i = 1; i < numberOfChanges; i++ )
            {
                if ( dirMode > -1 )
                {
                    archiver.setDirectoryMode( dirMode );
                }

                if ( fileMode > -1 )
                {
                    archiver.setFileMode( fileMode );
                }
            }
        }

        if ( dirMode > -1 )
        {
            archiver.setDirectoryMode( originalDirMode );
        }

        if ( fileMode > -1 )
        {
            archiver.setFileMode( originalFileMode );
        }
    }

    public void expectAddFile( final File file, final String outputLocation, final int fileMode )
    {
        try
        {
            archiver.addFile( (File)anyObject(), (String) anyObject(), anyInt() );
            EasyMock.expectLastCall().anyTimes();
        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectAddFile( final File file, final String outputLocation )
    {
        try
        {
            archiver.addFile( file, outputLocation );
            EasyMock.expectLastCall().atLeastOnce();
        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectGetReactorProjects( final List<MavenProject> projects )
    {
       expect(configSource.getReactorProjects()).andReturn( projects ).atLeastOnce();
    }

}
