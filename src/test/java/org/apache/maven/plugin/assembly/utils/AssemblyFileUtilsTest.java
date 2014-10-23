package org.apache.maven.plugin.assembly.utils;

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

import junit.framework.TestCase;
import org.apache.maven.plugin.assembly.archive.ArchiveExpansionException;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.easymock.classextension.EasyMockSupport;

import java.io.File;
import java.io.IOException;

import static org.easymock.EasyMock.expect;

public class AssemblyFileUtilsTest
    extends TestCase
{

    private final TestFileManager fileManager = new TestFileManager( "file-utils.test.", "" );

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testUnpack_ShouldSetSourceAndDestinationAndCallExtract()
        throws IOException, ArchiveExpansionException, NoSuchArchiverException
    {
        EasyMockSupport mockManager = new EasyMockSupport();

        File source = fileManager.createTempFile();
        File destDir = fileManager.createTempDir();

        UnArchiver unarchiver = mockManager.createMock( UnArchiver.class );

        ArchiverManager archiverManager = mockManager.createMock( ArchiverManager.class );

        try
        {
            expect(archiverManager.getUnArchiver( source )).andReturn( unarchiver );
        }
        catch ( NoSuchArchiverException e )
        {
            fail( "Should never happen." );
        }

        unarchiver.setSourceFile( source );
        unarchiver.setDestDirectory( destDir );

        try
        {
            unarchiver.extract();
        }
        catch ( ArchiverException e )
        {
            fail( "Should never happen." );
        }

        mockManager.replayAll();

        AssemblyFileUtils.unpack( source, destDir, archiverManager );

        mockManager.verifyAll();
    }

}
