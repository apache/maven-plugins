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

import junit.framework.Assert;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.easymock.EasyMock;
import org.easymock.classextension.EasyMockSupport;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

public class MockAndControlForAddFileSetsTask
{

    public final AssemblerConfigurationSource configSource;


    public Archiver archiver;

    public File archiveBaseDir;

    public MockAndControlForAddFileSetsTask( EasyMockSupport mockManager, TestFileManager fileManager )
    {
        configSource = mockManager.createMock (AssemblerConfigurationSource.class);
        archiver = mockManager.createMock (Archiver.class);
        archiveBaseDir = fileManager.createTempDir();

        expect(configSource.getMavenSession()).andReturn( null).anyTimes();
    }

    public void expectGetArchiveBaseDirectory()
    {
        expect(configSource.getArchiveBaseDirectory()).andReturn( archiveBaseDir ).anyTimes();
    }

    public void expectGetBasedir( File basedir )
    {
        expect( configSource.getBasedir()).andReturn( basedir ).anyTimes();
    }

    void expectModeChanges( int[] modes, int modeChangeCount )
    {
        expect(archiver.getOverrideDirectoryMode()).andReturn( modes[0] );
        expect(archiver.getOverrideFileMode()).andReturn( modes[1] );


        if ( modeChangeCount > 1 )
        {
            for ( int i = 1; i < modeChangeCount; i++ )
            {
                if ( modes[2] > -1 )
                {
                    archiver.setDirectoryMode( modes[2] );
                }
                
                if ( modes[3] > -1 )
                {
                    archiver.setFileMode( modes[3] );
                }
            }
        }

        if ( modes[2] > -1 )
        {
            archiver.setDirectoryMode( modes[0] );
        }
        
        if ( modes[3] > -1 )
        {
            archiver.setFileMode( modes[1] );
        }
    }

    public void expectAdditionOfSingleFileSet( MavenProject project, File basedir, String finalName,
                                               boolean shouldAddDir, int[] modes, int modeChangeCount,
                                               boolean isDebugEnabled )
    {
        expectAdditionOfSingleFileSet( project, finalName, shouldAddDir, modes, modeChangeCount,
                                       isDebugEnabled, true );

    }

    public void expectAdditionOfSingleFileSet( MavenProject project, String finalName, boolean shouldAddDir, int[] modes, int modeChangeCount,
                                               boolean isDebugEnabled, boolean isProjectUsed )
    {
        // the logger sends a debug message with this info inside the addFileSet(..) method..
        if ( isDebugEnabled )
        {
            expect(archiver.getOverrideDirectoryMode()).andReturn( modes[0] );
            expect(archiver.getOverrideFileMode()).andReturn( modes[1] );
        }

        if ( isProjectUsed )
        {
            expect(configSource.getProject()).andReturn( project ).atLeastOnce();
        }

        expect( configSource.getFinalName()).andReturn( finalName ).atLeastOnce();

        if ( shouldAddDir )
        {
            expectModeChanges( modes, modeChangeCount );

            try
            {
                archiver.addFileSet( (FileSet) anyObject() );
                EasyMock.expectLastCall().atLeastOnce();
            }
            catch ( ArchiverException e )
            {
                Assert.fail( "Should never happen." );
            }
        }

    }

    public void expectGetProject( MavenProject project )
    {
        expect(configSource.getProject()).andReturn( project ).atLeastOnce();
    }

    public void expectGetSession( MavenSession session )
    {
        expect(configSource.getMavenSession()).andReturn( session ).atLeastOnce();
    }

    public void expectGetFinalName( String finalName )
    {
        expect( configSource.getFinalName()).andReturn( finalName ).atLeastOnce();
    }

}
