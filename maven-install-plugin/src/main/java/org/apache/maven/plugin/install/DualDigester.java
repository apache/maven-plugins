package org.apache.maven.plugin.install;

import org.apache.commons.codec.binary.Hex;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

/**
 * Calculates md5 and sha1 digest.
 * <p/>
 * Todo: Consider using a thread to calculate one of the digests when the files are large; it's fairly slow !
 *
 * @author Kristian Rosenvold
 */
public class DualDigester
{
    private final MessageDigest md5 = getDigester( "MD5" );

    private final MessageDigest sh1 = getDigester( "SHA-1" );

    private static final int bufsize = 65536 * 2;

    private final byte[] buffer = new byte[bufsize];

    static MessageDigest getDigester( String algorithm )
    {
        try
        {
            return MessageDigest.getInstance( algorithm );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RuntimeException( "Unable to initialize digest " + algorithm + " : " + e.getMessage() );
        }
    }

    public void calculate( File file )
        throws MojoExecutionException
    {
        FileInputStream fis = null;
        BufferedInputStream bis = null;

        try
        {
            fis = new FileInputStream( file );
            calculate( fis );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to calculate digest checksum for " + file, e );
        }
        finally
        {
            IOUtil.close( bis );
            IOUtil.close( fis );
        }
    }

    void calculate( InputStream stream )
        throws IOException
    {
        md5.reset();
        sh1.reset();
        update( stream );
    }

    public String getMd5()
    {
        return Hex.encodeHexString( md5.digest() );
    }

    public String getSha1()
    {
        return Hex.encodeHexString( sh1.digest() );
    }

    private void update( InputStream is )
        throws IOException
    {
        int size = is.read( buffer, 0, bufsize );
        while ( size >= 0 )
        {
            md5.update( buffer, 0, size );
            sh1.update( buffer, 0, size );
            size = is.read( buffer, 0, bufsize );
        }
    }
}
