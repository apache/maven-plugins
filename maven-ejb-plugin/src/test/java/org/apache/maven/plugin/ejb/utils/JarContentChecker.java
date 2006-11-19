package org.apache.maven.plugin.ejb.utils;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Jar Content Checker
 */
public class JarContentChecker
{
    private final static String FOUND = "1";

    private final static String NOT_FOUND = "0";

    private HashMap fileMap;

    private HashMap directoryMap;

    private HashMap dataMap;


    public JarContentChecker()
    {
        fileMap = new HashMap();
        directoryMap = new HashMap();
        dataMap = new HashMap();
    }

    public void addDirectory( File dir )
    {
        directoryMap.put( dir, NOT_FOUND );
    }

    public void addFile( File file )
    {
        fileMap.put( file, NOT_FOUND );
    }

    public void addFile( String file, String data )
    {
        fileMap.put( file, NOT_FOUND );
        dataMap.put( file, data );
    }


    /**
     * checks whether the jar file contains the files for this checker,
     * files with the same file name but with different data will not
     * be considered.
     *
     * @param File
     * @return boolean
     */
    public boolean isOK( JarFile jarFile )
    {
        boolean bRetVal = false;
        Enumeration zipentries = jarFile.entries();
        ZipEntry entry;
        File entryFile;

        resetList();

        while ( zipentries.hasMoreElements() )
        {
            entry = (ZipEntry) zipentries.nextElement();
            entryFile = new File( entry.getName() );

            if ( entry.isDirectory() )
            {
                // cross out all files found in the jar file     
                // found files with incorrect content will not
                // be counted
                if ( directoryMap.containsKey( entryFile ) )
                {
                    directoryMap.put( entryFile, FOUND );
                }
            }
            else if ( fileMap.containsKey( entryFile ) )
            {
                try
                {
                    if ( checkContent( entryFile, jarFile.getInputStream( entry ) ) )
                    {
                        fileMap.put( entryFile, FOUND );
                    }
                }
                catch ( IOException ex )
                {
                    // TODO: handle exception
                }
            }
        }

        bRetVal = checkFinalResult();

        return bRetVal;
    }


    private boolean checkContent( File file, InputStream istream )
    {
        boolean bRetVal = true;

        if ( dataMap.containsKey( file ) )
        {
            // TODO: do content checking here
        }

        return bRetVal;
    }

    private boolean checkFinalResult()
    {
        boolean bRetVal = true;

        Iterator keys = fileMap.keySet().iterator();

        while ( keys.hasNext() && bRetVal )
        {
            if ( fileMap.get( keys.next() ).equals( NOT_FOUND ) )
            {
                bRetVal = false;
            }
        }

        keys = directoryMap.keySet().iterator();

        while ( keys.hasNext() && bRetVal )
        {
            if ( directoryMap.get( keys.next() ).equals( NOT_FOUND ) )
            {
                bRetVal = false;
            }
        }

        return bRetVal;
    }

    private void resetList()
    {
        Iterator keys = fileMap.keySet().iterator();

        while ( keys.hasNext() )
        {
            fileMap.put( keys.next(), NOT_FOUND );
        }

        keys = directoryMap.keySet().iterator();

        while ( keys.hasNext() )
        {
            directoryMap.put( keys.next(), NOT_FOUND );
        }
    }
}
