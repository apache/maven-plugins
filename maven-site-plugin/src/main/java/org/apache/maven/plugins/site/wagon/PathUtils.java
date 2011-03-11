package org.apache.maven.plugins.site.wagon;

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

import org.apache.maven.wagon.WagonConstants;

import java.io.File;
import java.util.StringTokenizer;

/**
 * Various path (URL) manipulation routines.
 *
 * <strong>Note: </strong> This is a copy of a file from Wagon. It was copied here to be able to work around WAGON-307.
 * This class can be removed when the prerequisite Maven version uses wagon-provider-api:1.0-beta-7.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public final class PathUtils
{
    private PathUtils()
    {
    }
    
    /**
     * Returns the directory path portion of a file specification string.
     * Matches the equally named unix command.
     *
     * @return The directory portion excluding the ending file separator.
     */
    public static String dirname( final String path )
    {
        final int i = path.lastIndexOf( "/" );

        return ( ( i >= 0 ) ? path.substring( 0, i ) : "" );
    }

    /**
     * Returns the filename portion of a file specification string.
     *
     * @return The filename string with extension.
     */
    public static String filename( final String path )
    {
        final int i = path.lastIndexOf( "/" );
        return ( ( i >= 0 ) ? path.substring( i + 1 ) : path );
    }

    public static String[] dirnames( final String path )
    {
        final String dirname = PathUtils.dirname( path );
        return split( dirname, "/", -1 );

    }

    private static String[] split( final String str, final String separator, final int max )
    {
        final StringTokenizer tok;

        if ( separator == null )
        {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer( str );
        }
        else
        {
            tok = new StringTokenizer( str, separator );
        }

        int listSize = tok.countTokens();

        if ( max > 0 && listSize > max )
        {
            listSize = max;
        }

        final String[] list = new String[listSize];

        int i = 0;

        int lastTokenBegin;
        int lastTokenEnd = 0;

        while ( tok.hasMoreTokens() )
        {
            if ( max > 0 && i == listSize - 1 )
            {
                // In the situation where we hit the max yet have
                // tokens left over in our input, the last list
                // element gets all remaining text.
                final String endToken = tok.nextToken();

                lastTokenBegin = str.indexOf( endToken, lastTokenEnd );

                list[i] = str.substring( lastTokenBegin );

                break;

            }
            else
            {
                list[i] = tok.nextToken();

                lastTokenBegin = str.indexOf( list[i], lastTokenEnd );

                lastTokenEnd = lastTokenBegin + list[i].length();
            }

            i++;
        }
        return list;
    }

    /**
     * Return the host name (Removes protocol and path from the URL) E.g: for input
     * <code>http://www.codehause.org</code> this method will return <code>www.apache.org</code>
     *
     * @param url the url
     * @return the host name
     */
    public static String host( final String url )
    {
        String authorization = authorization( url );
        int index = authorization.indexOf( '@' );
        if ( index >= 0 )
        {
            return authorization.substring( index + 1 );
        }
        else
        {
            return authorization;
        }
    }

    /**
     * This was changed from private to package local so that it can be unit tested.
     */
    static String authorization( final String url )
    {
        if ( url == null )
        {
            return "localhost";
        }

        final String protocol = PathUtils.protocol( url );

        if ( protocol == null || protocol.equalsIgnoreCase( "file" ) )
        {
            return "localhost";
        }

        String host = url;
        if ( protocol.equalsIgnoreCase( "scm" ) )
        {
            // skip over type
            host = host.substring( host.indexOf( ":", 4 ) + 1 ).trim();
        }

        // skip over protocol
        host = host.substring( host.indexOf( ":" ) + 1 ).trim();
        if ( host.startsWith( "//" ) )
        {
            host = host.substring( 2 );
        }

        int pos = host.indexOf( "/" );

        if ( pos > 0 )
        {
            host = host.substring( 0, pos );
        }

        pos = host.indexOf( '@' );

        if ( pos > 0 )
        {
            pos = host.indexOf( ':', pos );
        }
        else
        {
            pos = host.indexOf( ":" );
        }

        if ( pos > 0 )
        {
            host = host.substring( 0, pos );
        }
        return host;
    }

    /**
     * /**
     * Return the protocol name.
     * <br/>
     * E.g: for input
     * <code>http://www.codehause.org</code> this method will return <code>http</code>
     *
     * @param url the url
     * @return the host name
     */
    public static String protocol( final String url )
    {
        final int pos = url.indexOf( ":" );

        if ( pos == -1 )
        {
            return "";
        }
        return url.substring( 0, pos ).trim();
    }

    /**
     * @param url
     * @return the port or {@link WagonConstants#UNKNOWN_PORT} if not existent
     */
    public static int port( String url )
    {

        final String protocol = PathUtils.protocol( url );

        if ( protocol == null || protocol.equalsIgnoreCase( "file" ) )
        {
            return WagonConstants.UNKNOWN_PORT;
        }

        final String authorization = PathUtils.authorization( url );

        if ( authorization == null )
        {
            return WagonConstants.UNKNOWN_PORT;
        }

        if ( protocol.equalsIgnoreCase( "scm" ) )
        {
            // skip over type
            url = url.substring( url.indexOf( ":", 4 ) + 1 ).trim();
        }

        if ( url.regionMatches( true, 0, "file:", 0, 5 ) || url.regionMatches( true, 0, "local:", 0, 6 ) )
        {
            return WagonConstants.UNKNOWN_PORT;
        }

        // skip over protocol
        url = url.substring( url.indexOf( ":" ) + 1 ).trim();
        if ( url.startsWith( "//" ) )
        {
            url = url.substring( 2 );
        }

        int start = authorization.length();

        if ( url.length() > start && url.charAt( start ) == ':' )
        {
            int end = url.indexOf( '/', start );

            if ( end == start + 1 )
            {
                // it is :/
                return WagonConstants.UNKNOWN_PORT;
            }

            if ( end == -1 )
            {
                end = url.length();
            }

            return Integer.parseInt( url.substring( start + 1, end ) );
        }
        else
        {
            return WagonConstants.UNKNOWN_PORT;
        }

    }

    /**
     * Derive the path portion of the given URL.
     * 
     * @param url the repository URL
     * @return the basedir of the repository
     * @todo need to URL decode for spaces?
     */
    public static String basedir( String url )
    {
        String protocol = PathUtils.protocol( url );

        String retValue = null;

        if ( protocol.equalsIgnoreCase( "scm" ) )
        {
            // skip over SCM bits
            if ( url.regionMatches( true, 0, "scm:svn:", 0, 8 ) )
            {
                url = url.substring( url.indexOf( ":", 4 ) + 1 );
                protocol = PathUtils.protocol( url );
            }
        }

        if ( protocol.equalsIgnoreCase( "file" ) )
        {
            retValue = url.substring( protocol.length() + 1 );
            retValue = decode( retValue );
            // special case: if omitted // on protocol, keep path as is
            if ( retValue.startsWith( "//" ) )
            {
                retValue = retValue.substring( 2 );

                if ( retValue.length() >= 2 && ( retValue.charAt( 1 ) == '|' || retValue.charAt( 1 ) == ':' ) )
                {
                    // special case: if there is a windows drive letter, then keep the original return value
                    retValue = retValue.charAt( 0 ) + ":" + retValue.substring( 2 );
                }
                else
                {
                    // Now we expect the host
                    int index = retValue.indexOf( "/" );
                    if ( index >= 0 )
                    {
                        retValue = retValue.substring( index + 1 );
                    }

                    // special case: if there is a windows drive letter, then keep the original return value
                    if ( retValue.length() >= 2 && ( retValue.charAt( 1 ) == '|' || retValue.charAt( 1 ) == ':' ) )
                    {
                        retValue = retValue.charAt( 0 ) + ":" + retValue.substring( 2 );
                    }
                    else if ( index >= 0 )
                    {
                        // leading / was previously stripped
                        retValue = "/" + retValue;
                    }
                }
            }

            // special case: if there is a windows drive letter using |, switch to :
            if ( retValue.length() >= 2 && retValue.charAt( 1 ) == '|' )
            {
                retValue = retValue.charAt( 0 ) + ":" + retValue.substring( 2 );
            }
        }
        else
        {
            final String authorization = PathUtils.authorization( url );

            final int port = PathUtils.port( url );

            int pos = 0;

            if ( protocol.equalsIgnoreCase( "scm" ) )
            {
                pos = url.indexOf( ":", 4 ) + 1;
                pos = url.indexOf( ":", pos ) + 1;
            }
            else
            {
                int index = url.indexOf( "://" );
                if ( index != -1 )
                {
                    pos = index + 3;
                }
            }

            pos += authorization.length();

            if ( port != WagonConstants.UNKNOWN_PORT )
            {
                pos = pos + Integer.toString( port ).length() + 1;
            }

            if ( url.length() > pos )
            {
                retValue = url.substring( pos );
                if ( retValue.startsWith( ":" ) )
                {
                    // this is for :/ after the host
                    retValue = retValue.substring( 1 );
                }

                // one module may be allowed in the path in CVS
                retValue = retValue.replace( ':', '/' );
            }
        }

        if ( retValue == null )
        {
            retValue = "/";
        }
        return retValue.trim();
    }

    /**
     * Decodes the specified (portion of a) URL. <strong>Note:</strong> This decoder assumes that ISO-8859-1 is used to
     * convert URL-encoded octets to characters.
     * 
     * @param url The URL to decode, may be <code>null</code>.
     * @return The decoded URL or <code>null</code> if the input was <code>null</code>.
     */
    private static String decode( String url )
    {
        String decoded = url;
        if ( url != null )
        {
            int pos = -1;
            while ( ( pos = decoded.indexOf( '%', pos + 1 ) ) >= 0 )
            {
                if ( pos + 2 < decoded.length() )
                {
                    String hexStr = decoded.substring( pos + 1, pos + 3 );
                    char ch = (char) Integer.parseInt( hexStr, 16 );
                    decoded = decoded.substring( 0, pos ) + ch + decoded.substring( pos + 3 );
                }
            }
        }
        return decoded;
    }

    public static String user( String url )
    {
        String host = authorization( url );
        int index = host.indexOf( '@' );
        if ( index > 0 )
        {
            String userInfo = host.substring( 0, index );
            index = userInfo.indexOf( ':' );
            if ( index > 0 )
            {
                return userInfo.substring( 0, index );
            }
            else if ( index < 0 )
            {
                return userInfo;
            }
        }
        return null;
    }

    public static String password( String url )
    {
        String host = authorization( url );
        int index = host.indexOf( '@' );
        if ( index > 0 )
        {
            String userInfo = host.substring( 0, index );
            index = userInfo.indexOf( ':' );
            if ( index >= 0 )
            {
                return userInfo.substring( index + 1 );
            }
        }
        return null;
    }

    // TODO: move to plexus-utils or use something appropriate from there
    public static String toRelative( File basedir, String absolutePath )
    {
        String relative;

        absolutePath = absolutePath.replace( '\\', '/' );
        String basedirPath = basedir.getAbsolutePath().replace( '\\', '/' );

        if ( absolutePath.startsWith( basedirPath ) )
        {
            relative = absolutePath.substring( basedirPath.length() );
            if ( relative.startsWith( "/" ) )
            {
                relative = relative.substring( 1 );
            }
            if ( relative.length() <= 0 )
            {
                relative = ".";
            }
        }
        else
        {
            relative = absolutePath;
        }

        return relative;
    }
}
