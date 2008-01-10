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

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.easymock.MockControl;

import java.io.File;
import java.util.List;

import junit.framework.Assert;

public class MockAndControlForAddArtifactTask
{

    public Archiver archiver;

    public MockControl archiverCtl;

    public AssemblerConfigurationSource configSource;

    public MockControl configSourceCtl;

    private MavenProject project = null;

    public MockAndControlForAddArtifactTask( MockManager mockManager )
    {
        this( mockManager, null );
    }

    public MockAndControlForAddArtifactTask( MockManager mockManager, MavenProject project )
    {
        this.project = project;

        archiverCtl = MockControl.createControl( Archiver.class );
        mockManager.add( archiverCtl );

        archiver = (Archiver) archiverCtl.getMock();

        configSourceCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceCtl );

        configSource = (AssemblerConfigurationSource) configSourceCtl.getMock();

        enableDefaultExpectations();
    }

    private void enableDefaultExpectations()
    {
        configSource.getProject();
        configSourceCtl.setReturnValue( project, MockControl.ZERO_OR_MORE );
    }

    public void expectGetFinalName( String finalName )
    {
        configSource.getFinalName();
        configSourceCtl.setReturnValue( finalName, MockControl.ONE_OR_MORE );
    }

    public void expectAddArchivedFileSet( File artifactFile, String outputLocation, String[] includes, String[] excludes )
    {
        try
        {
            archiver.addArchivedFileSet( artifactFile, outputLocation, includes, excludes );

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

    public void expectAddFile( File file, String outputLocation, int fileMode )
    {
        try
        {
            archiver.addFile( file, outputLocation, fileMode );
            archiverCtl.setVoidCallable( MockControl.ONE_OR_MORE );
        }
        catch ( ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectAddFile( File file, String outputLocation )
    {
        try
        {
            archiver.addFile( file, outputLocation );
            archiverCtl.setVoidCallable( MockControl.ONE_OR_MORE );
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
}
