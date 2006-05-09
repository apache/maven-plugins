/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.codehaus.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact codehaus@codehaus.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.codehaus.org/>.
 */

package org.apache.maven.plugin.resources.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class InterpolationFilterReader
    extends FilterReader
{
    /** replacement text from a token */
    private String replaceData = null;

    /** Index into replacement data */
    private int replaceIndex = -1;

    /** Hashtable to hold the replacee-replacer pairs (String to String). */
    private Map variables = new HashMap();

    /** Character marking the beginning of a token. */
    private String beginToken;

    /** Character marking the end of a token. */
    private String endToken;

    /** Length of begin token. */
    private int beginTokenLength;

    /** Length of end token. */
    private int endTokenLength;

    /** Default begin token. */
    private static String DEFAULT_BEGIN_TOKEN = "${";

    /** Default end token. */
    private static String DEFAULT_END_TOKEN = "}";

    public InterpolationFilterReader( Reader in, Map variables, String beginToken, String endToken )
    {
        super( in );

        this.variables = variables;
        this.beginToken = beginToken;
        this.endToken = endToken;

        beginTokenLength = beginToken.length();
        endTokenLength = endToken.length();
    }

    public InterpolationFilterReader( Reader in, Map variables )
    {
        this( in, variables, DEFAULT_BEGIN_TOKEN, DEFAULT_END_TOKEN );
    }

    /**
     * Skips characters.  This method will block until some characters are
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * @param  n  The number of characters to skip
     *
     * @return    the number of characters actually skipped
     *
     * @exception  IllegalArgumentException  If <code>n</code> is negative.
     * @exception  IOException  If an I/O error occurs
     */
    public long skip( long n ) throws IOException
    {
        if ( n < 0L )
        {
            throw new IllegalArgumentException( "skip value is negative" );
        }

        for ( long i = 0; i < n; i++ )
        {
            if ( read() == -1 )
            {
                return i;
            }
        }
        return n;
    }

    /**
     * Reads characters into a portion of an array.  This method will block
     * until some input is available, an I/O error occurs, or the end of the
     * stream is reached.
     *
     * @param      cbuf  Destination buffer to write characters to.
     *                   Must not be <code>null</code>.
     * @param      off   Offset at which to start storing characters.
     * @param      len   Maximum number of characters to read.
     *
     * @return     the number of characters read, or -1 if the end of the
     *             stream has been reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read( char cbuf[],
                     int off,
                     int len )
        throws IOException
    {
        for ( int i = 0; i < len; i++ )
        {
            int ch = read();
            if ( ch == -1 )
            {
                if ( i == 0 )
                {
                    return -1;
                }
                else
                {
                    return i;
                }
            }
            cbuf[off + i] = (char) ch;
        }
        return len;
    }

    /**
     * Returns the next character in the filtered stream, replacing tokens
     * from the original stream.
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading
     */
    public int read() throws IOException
    {
        if ( replaceIndex != -1 )
        {
            int ch = replaceData.charAt( replaceIndex++ );
            if ( replaceIndex >= replaceData.length() )
            {
                replaceIndex = -1;
            }
            return ch;
        }

        int ch = in.read();

        if ( ch == beginToken.charAt( 0 ) )
        {
            StringBuffer key = new StringBuffer();

            int beginTokenMatchPos = 1;

            do
            {
                ch = in.read();
                if ( ch != -1 )
                {
                    key.append( (char) ch );

                    if ( ( beginTokenMatchPos < beginTokenLength ) &&
                            ( ch != beginToken.charAt( beginTokenMatchPos++ ) ) )
                    {
                        ch = -1; // not really EOF but to trigger code below
                        break;
                    }
                }
                else
                {
                    break;
                }
            }
            while ( ch != endToken.charAt( 0 ) );

            // now test endToken
            if ( ch != -1 && endTokenLength > 1 )
            {
                int endTokenMatchPos = 1;

                do
                {
                    ch = in.read();
                    
                    if ( ch != -1 )
                    {
                        key.append( (char) ch );

                        if ( ch != endToken.charAt( endTokenMatchPos++ ) )
                        {
                            ch = -1; // not really EOF but to trigger code below
                            break;
                        }

                    }
                    else
                    {
                        break;
                    }
                }
                while ( endTokenMatchPos < endTokenLength );
            }

            // There is nothing left to read so we have the situation where the begin/end token
            // are in fact the same and as there is nothing left to read we have got ourselves
            // end of a token boundary so let it pass through.
            if ( ch == -1 )
            {
                replaceData = key.toString();
                replaceIndex = 0;
                return beginToken.charAt( 0 );
            }

            String variableKey = key.substring( beginTokenLength - 1, key.length() - endTokenLength );

            Object o = variables.get(variableKey);
            if ( o != null )
            {
                String value = o.toString();
                if ( value.length() != 0 )
                {
                    replaceData = value;
                    replaceIndex = 0;
                }
                return read();
            }
            else
            {
                replaceData = key.toString();
                replaceIndex = 0;
                return beginToken.charAt(0);
            }
        }

        return ch;
    }
}
