package org.apache.maven.plugin.assembly.testutils;

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

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

public class TestFileManager
{

    public static final String TEMP_DIR_PATH = System.getProperty( "java.io.tmpdir" );

    private List filesToDelete = new ArrayList();

    private final String baseFilename;

    private final String fileSuffix;

    private StackTraceElement callerInfo;

    private Thread cleanupWarning;

    private boolean warnAboutCleanup = false;

    public TestFileManager( String baseFilename, String fileSuffix )
    {
        this.baseFilename = baseFilename;
        this.fileSuffix = fileSuffix;

        initializeCleanupMonitoring();
    }

    private void initializeCleanupMonitoring()
    {
        callerInfo = new NullPointerException().getStackTrace()[2];

        Runnable warning = new Runnable()
        {

            public void run()
            {
                maybeWarnAboutCleanUp();
            }

        };

        cleanupWarning = new Thread( warning );

        Runtime.getRuntime().addShutdownHook( cleanupWarning );
    }

    private void maybeWarnAboutCleanUp()
    {
        if ( warnAboutCleanup )
        {
            System.out.println( "[WARNING] TestFileManager from: " + callerInfo.getClassName() + " not cleaned up!" );
        }
    }

    public void markForDeletion( File toDelete )
    {
        filesToDelete.add( toDelete );
        warnAboutCleanup = true;
    }

    public synchronized File createTempDir()
    {
        try
        {
            Thread.sleep( 20 );
        }
        catch ( InterruptedException e )
        {
        }

        File dir = new File( TEMP_DIR_PATH, baseFilename + System.currentTimeMillis() );

        dir.mkdirs();
        markForDeletion( dir );

        return dir;
    }

    public synchronized File createTempFile()
        throws IOException
    {
        File tempFile = File.createTempFile( baseFilename, fileSuffix );
        tempFile.deleteOnExit();
        markForDeletion( tempFile );

        return tempFile;
    }

    public void cleanUp()
    {
        for ( Iterator it = filesToDelete.iterator(); it.hasNext(); )
        {
            File file = ( File ) it.next();

            if ( file.exists() )
            {
                try
                {
                    FileUtils.forceDelete( file );
                }
                catch ( Exception e )
                {
                    System.err.println( "Error while deleting test file/dir: " + file + "; ignoring." );
                }
            }

            it.remove();
        }

        warnAboutCleanup = false;
    }

    public void assertFileExistence( File dir, String filename, boolean shouldExist )
    {
        File file = new File( dir, filename );

        if ( shouldExist )
        {
            Assert.assertTrue( file.exists() );
        }
        else
        {
            Assert.assertFalse( file.exists() );
        }
    }

    public void assertFileContents( File dir, String filename, String contentsTest )
        throws IOException
    {
        assertFileExistence( dir, filename, true );

        File file = new File( dir, filename );

        Assert.assertEquals( contentsTest, getFileContents( file ) );
    }

    /**
     * NOTE: the file content is written using platform encoding.
     */
    public File createFile( File dir, String filename, String contents )
        throws IOException
    {
        File file = new File( dir, filename );

        file.getParentFile().mkdirs();

        FileWriter writer = null;

        try
        {
            writer = new FileWriter( file ); // platform encoding

            writer.write( contents );
        }
        finally
        {
            IOUtil.close( writer );
        }

        markForDeletion( file );

        return file;
    }

    /**
     * NOTE: the file content is read using platform encoding.
     */
    public String getFileContents( File file )
        throws IOException
    {
        String result = null;

        FileReader reader = null;
        try
        {
            reader = new FileReader( file ); // platform encoding

            result = IOUtil.toString( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return result;
    }

    protected void finalize()
        throws Throwable
    {
        maybeWarnAboutCleanUp();

        super.finalize();
    }

    public File createFile( String filename, String content )
        throws IOException
    {
        File dir = createTempDir();
        return createFile( dir, filename, content );
    }

}
