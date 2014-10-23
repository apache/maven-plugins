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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddFileSetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.resolved.ResolvedAssembly;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.easymock.EasyMock;
import org.easymock.classextension.EasyMockSupport;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

public class FileSetAssemblyPhaseTest
    extends TestCase
{

    final EasyMockSupport mm = new EasyMockSupport();

    private final TestFileManager fileManager = new TestFileManager( "file-set-assembly.test.", "" );

    @Override
    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testShouldNotFailWhenNoFileSetsSpecified()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final Assembly assembly = new Assembly();

        assembly.setId( "test" );

        final MockAndControlForLogger macLogger = new MockAndControlForLogger();
        final MockAndControlForAddFileSetsTask macTask =
            new MockAndControlForAddFileSetsTask( mm, fileManager );

        mm.replayAll();

        createPhase( macLogger ).execute( ResolvedAssembly.create( assembly ), macTask.archiver, macTask.configSource );

        mm.verifyAll();
    }

    public void testShouldAddOneFileSet()
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final Assembly assembly = new Assembly();

        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );

        final FileSet fs = new FileSet();
        fs.setOutputDirectory( "/out" );
        fs.setDirectory( "/input" );
        fs.setFileMode( "777" );
        fs.setDirectoryMode( "777" );

        assembly.addFileSet( fs );

        final MockAndControlForLogger macLogger = new MockAndControlForLogger();
        final MockAndControlForAddFileSetsTask macTask =
            new MockAndControlForAddFileSetsTask( mm, fileManager );

        macTask.expectGetArchiveBaseDirectory();

        final File basedir = fileManager.createTempDir();

        final MavenProject project = new MavenProject( new Model() );

        macLogger.expectDebug( true, true );

        final int dirMode = Integer.parseInt( "777", 8 );
        final int fileMode = Integer.parseInt( "777", 8 );

        final int[] modes = { -1, -1, dirMode, fileMode };

        macTask.expectAdditionOfSingleFileSet( project, "final-name", false, modes, 1, true );

        mm.replayAll();

        createPhase( macLogger ).execute( ResolvedAssembly.create( assembly), macTask.archiver, macTask.configSource );

        mm.verifyAll();
    }

    private FileSetAssemblyPhase createPhase( final MockAndControlForLogger macLogger )
    {
        final FileSetAssemblyPhase phase = new FileSetAssemblyPhase();

        phase.enableLogging( macLogger.logger );

        return phase;
    }

    private final class MockAndControlForLogger
    {
        final Logger logger;

        MockAndControlForLogger()
        {
            logger = mm.createMock(Logger.class);
        }

        public void expectDebug( final boolean debugCheck, final boolean debugEnabled )
        {
            if ( debugCheck )
            {
                expect( logger.isDebugEnabled()).andReturn( debugEnabled ).anyTimes();
            }

            logger.debug( (String) anyObject() );
            EasyMock.expectLastCall().anyTimes();
        }
    }

}
