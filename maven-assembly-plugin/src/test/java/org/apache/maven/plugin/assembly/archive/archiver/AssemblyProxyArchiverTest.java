package org.apache.maven.plugin.assembly.archive.archiver;

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

import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class AssemblyProxyArchiverTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "massembly-proxyArchiver", "" );

    public void tearDown()
        throws Exception
    {
        fileManager.cleanUp();
    }

    public void testAddFile_NoPerms_CallAcceptFilesOnlyOnce()
        throws IOException, ArchiverException
    {
        MockControl delegateControl = MockControl.createControl( Archiver.class );
        Archiver delegate = (Archiver) delegateControl.getMock();

        delegate.addFile( null, null );
        delegateControl.setMatcher( MockControl.ALWAYS_MATCHER );
        delegateControl.setVoidCallable();

        CounterSelector counter = new CounterSelector( true );
        List selectors = Collections.singletonList( counter );

        delegateControl.replay();

        AssemblyProxyArchiver archiver = new AssemblyProxyArchiver( "", delegate, Collections.EMPTY_LIST, selectors,
                                                                      Collections.EMPTY_LIST, new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        File inputFile = fileManager.createTempFile();

        archiver.addFile( inputFile, "file.txt" );

        assertEquals( 1, counter.getCount() );

        delegateControl.verify();
    }

    public void testAddDirectory_NoPerms_CallAcceptFilesOnlyOnce()
        throws IOException, ArchiverException
    {
        Archiver delegate = new JarArchiver();

        File output = fileManager.createTempFile();
        delegate.setDestFile( output );

        CounterSelector counter = new CounterSelector( true );
        List selectors = Collections.singletonList( counter );

        AssemblyProxyArchiver archiver = new AssemblyProxyArchiver( "", delegate, Collections.EMPTY_LIST, selectors,
                                                                      Collections.EMPTY_LIST, new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        File dir = fileManager.createTempDir();
        fileManager.createFile( dir, "file.txt", "This is a test." );

        archiver.addDirectory( dir );
        
        archiver.createArchive();

        assertEquals( 1, counter.getCount() );
    }

    private static final class CounterSelector
        implements FileSelector
    {

        private int count = 0;

        private boolean answer = false;

        public CounterSelector( boolean answer )
        {
            this.answer = answer;
        }

        public int getCount()
        {
            return count;
        }

        public boolean isSelected( FileInfo fileInfo )
            throws IOException
        {
            if ( fileInfo.isFile() )
            {
                count++;
            }

            return answer;
        }

    }

}
