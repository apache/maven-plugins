package org.apache.maven.plugin.install;

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

import org.apache.commons.codec.binary.Hex;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kristian Rosenvold
 */
public class SimpleDigester {

    private final MessageDigest messageDigest;
    private static final int bufsize = 65536;

    public SimpleDigester(String algorithm) {
        try
        {
            messageDigest = MessageDigest.getInstance( algorithm );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RuntimeException( "Unable to initialize digest " + algorithm + " : "
                    + e.getMessage() );
        }
    }

    public static SimpleDigester md5(){
        return new SimpleDigester("MD5");
    }

    public static SimpleDigester sha1(){
        return new SimpleDigester("SHA-1");
    }

    public String getAlgorithm() {
        return messageDigest.getAlgorithm();
    }

    public String calculate( File file ) throws MojoExecutionException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;

        try
        {
            fis = new FileInputStream( file );
            int bufsiz = (int) Math.min(file.length(), bufsize);
            bis = new BufferedInputStream(fis, bufsiz);
            messageDigest.reset();
            update(bis);
            return Hex.encodeHexString(messageDigest.digest());
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to calculate " + messageDigest.getAlgorithm() + " checksum for "
                    + file, e );
        } finally
        {
            IOUtil.close(bis);
            IOUtil.close( fis );
        }
    }

    private void update( InputStream is )
            throws IOException {
            byte[] buffer = new byte[bufsize];
            int size = is.read( buffer, 0, bufsize );
            while ( size >= 0 )
            {
                messageDigest.update( buffer, 0, size );
                size = is.read( buffer, 0, bufsize );
            }
    }

    public void verify( File file, String checksum )
        throws MojoExecutionException
    {
        String trimmed = cleanChecksum( checksum, messageDigest.getAlgorithm(), file.getName() );
        String sum = calculate( file );
        if ( !StringUtils.equalsIgnoreCase( trimmed, sum ) )
        {
            throw new RuntimeException( "Checksum failed (expected=" + trimmed + ", actual=" + sum + ")" );
        }
    }

    private static String cleanChecksum( String checksum, String algorithm, String path )
    {
        String trimmed = checksum.replace( '\n', ' ' ).trim();

        // Free-BSD / openssl
        String regex = algorithm.replaceAll( "-", "" ) + "\\s*\\((.*?)\\)\\s*=\\s*([a-fA-F0-9]+)";
        Matcher m = Pattern.compile( regex ).matcher( trimmed );
        if ( m.matches() )
        {
            String filename = m.group( 1 );
            if ( !isValidChecksumPattern( filename, path ) )
            {
                throw new RuntimeException( "Supplied checksum does not match checksum pattern" );
            }
            trimmed = m.group( 2 );
        }
        else
        {
            // GNU tools
            m = Pattern.compile( "([a-fA-F0-9]+)\\s+\\*?(.+)" ).matcher( trimmed );
            if ( m.matches() )
            {
                String filename = m.group( 2 );
                if ( !isValidChecksumPattern( filename, path ) )
                {
                    throw new RuntimeException( "Supplied checksum does not match checksum pattern" );
                }
                trimmed = m.group( 1 );
            }
        }
        return trimmed;
    }

    private static boolean isValidChecksumPattern( String filename, String path )
    {
        return filename.endsWith( path ) || filename.equals("-");
    }

}
