package org.apache.maven.coreit.testdep;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

public class QuotableSolutions
{
    private final static String RESOURCE = "/org/apache/maven/coreit/testdep/quotes.properties";

    public QuotableSolutions()
    {
        /* do nothing */
    }

    public String getQuoteViaThread( String who )
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return getQuote( cl, who );
    }

    public String getQuoteViaThis( String who )
    {
        ClassLoader cl = this.getClass().getClassLoader();
        return getQuote( cl, who );
    }

    public String getQuote( ClassLoader cl, String who )
    {

        URL url = cl.getResource( RESOURCE );
        if ( url == null )
        {
            System.err.println( "Unable to find Resource " + RESOURCE );
            return null;
        }

        try
        {
            InputStream is = url.openStream();
            if ( is == null )
            {
                System.err.println( "Unabel to open stream for " + RESOURCE );
                return null;
            }
            Properties props = new Properties();
            props.load( is );

            return props.getProperty( "quote." + who );
        }
        catch ( IOException e )
        {
            System.err.println( "Unable to open resource " + RESOURCE + ": " + e.getMessage() );
        }

        return null;
    }
}
