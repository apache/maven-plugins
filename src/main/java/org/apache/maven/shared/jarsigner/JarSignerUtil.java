package org.apache.maven.shared.jarsigner;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Usuful methods.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @since 1.0
 */
public class JarSignerUtil
{

    private JarSignerUtil()
    {
        // static class
    }

    /**
     * Checks whether the specified file is a JAR file. For our purposes, a ZIP file is a ZIP stream with at least one
     * entry.
     *
     * @param file The file to check, must not be <code>null</code>.
     * @return <code>true</code> if the file looks like a ZIP file, <code>false</code> otherwise.
     */
    public static boolean isZipFile( final File file )
    {
        try
        {
            ZipInputStream zis = new ZipInputStream( new FileInputStream( file ) );
            try
            {
                return zis.getNextEntry() != null;
            }
            finally
            {
                zis.close();
            }
        }
        catch ( Exception e )
        {
            // ignore, will fail below
        }

        return false;
    }

    /**
     * Removes any existing signatures from the specified JAR file. We will stream from the input JAR directly to the
     * output JAR to retain as much metadata from the original JAR as possible.
     *
     * @param jarFile The JAR file to unsign, must not be <code>null</code>.
     * @throws java.io.IOException
     */
    public static void unsignArchive( File jarFile )
        throws IOException
    {

        File unsignedFile = new File( jarFile.getAbsolutePath() + ".unsigned" );

        ZipInputStream zis = null;
        ZipOutputStream zos = null;
        try
        {
            zis = new ZipInputStream( new BufferedInputStream( new FileInputStream( jarFile ) ) );
            zos = new ZipOutputStream( new BufferedOutputStream( new FileOutputStream( unsignedFile ) ) );

            for ( ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry() )
            {
                if ( isSignatureFile( ze.getName() ) )
                {

                    continue;
                }

                zos.putNextEntry( ze );

                IOUtil.copy( zis, zos );
            }

        }
        finally
        {
            IOUtil.close( zis );
            IOUtil.close( zos );
        }

        FileUtils.rename( unsignedFile, jarFile );

    }

    /**
     * Checks whether the specified JAR file entry denotes a signature-related file, i.e. matches
     * <code>META-INF/*.SF</code>, <code>META-INF/*.DSA</code> or <code>META-INF/*.RSA</code>.
     *
     * @param entryName The name of the JAR file entry to check, must not be <code>null</code>.
     * @return <code>true</code> if the entry is related to a signature, <code>false</code> otherwise.
     */
    private static boolean isSignatureFile( String entryName )
    {
        if ( entryName.regionMatches( true, 0, "META-INF", 0, 8 ) )
        {
            entryName = entryName.replace( '\\', '/' );

            if ( entryName.indexOf( '/' ) == 8 && entryName.lastIndexOf( '/' ) == 8 )
            {
                if ( entryName.regionMatches( true, entryName.length() - 3, ".SF", 0, 3 ) )
                {
                    return true;
                }
                if ( entryName.regionMatches( true, entryName.length() - 4, ".DSA", 0, 4 ) )
                {
                    return true;
                }
                if ( entryName.regionMatches( true, entryName.length() - 4, ".RSA", 0, 4 ) )
                {
                    return true;
                }
            }
        }
        return false;
    }
}
