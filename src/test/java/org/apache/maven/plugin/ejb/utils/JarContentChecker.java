package org.apache.maven.plugin.ejb.utils;

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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Jar Content Checker
 */
public class JarContentChecker
{
    private final static Boolean FOUND = Boolean.TRUE;

    private final static Boolean NOT_FOUND = Boolean.FALSE;

    private Map<File, Boolean> fileMap;

    private Map<File, Boolean> directoryMap;


    public JarContentChecker()
    {
        fileMap = new HashMap<File, Boolean>();
        directoryMap = new HashMap<File, Boolean>();
    }

    public void addDirectory( File dir )
    {
        directoryMap.put( dir, NOT_FOUND );
    }

    public void addFile( File file )
    {
        fileMap.put( file, NOT_FOUND );
    }

    /**
     * checks whether the jar file contains the files for this checker,
     * files with the same file name but with different data will not
     * be considered.
     *
     * @param jarFile
     * @return boolean
     */
    public boolean isOK( JarFile jarFile )
    {
        boolean bRetVal;
        Enumeration<JarEntry> zipentries = jarFile.entries();
        JarEntry entry;
        File entryFile;

        resetList();

        while ( zipentries.hasMoreElements() )
        {
            entry = zipentries.nextElement();
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
                fileMap.put( entryFile, FOUND );
            }
        }

        bRetVal = checkFinalResult();

        return bRetVal;
    }

    private boolean checkFinalResult()
    {
        boolean bRetVal = true;

        Iterator<File> keys = fileMap.keySet().iterator();

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
        Iterator<File> keys = fileMap.keySet().iterator();

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
