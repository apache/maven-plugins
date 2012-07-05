package org.apache.maven.plugin.resources;

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

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

/**
 * @deprecated use classes in the component maven-filtering
 * TODO remove the class ?
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @author William Ferguson
 *
 */
public final class PropertyUtils
{
    private PropertyUtils()
    {
        // prevent instantiation
    }

    /**
     * Reads a property file, resolving all internal variables, using the supplied base properties.
     * <p>
     * The properties are resolved iteratively, so if the value of property A refers to property B, then after
     * resolution the value of property B will contain the value of property B.
     * </p>
     *
     * @param propFile The property file to load.
     * @param baseProps Properties containing the initial values to subsitute into the properties file.
     * @return Properties object containing the properties in the file with their values fully resolved.
     * @throws IOException if profile does not exist, or cannot be read.
     */
    public static Properties loadPropertyFile( File propFile, Properties baseProps )
        throws IOException
    {
        if ( !propFile.exists() )
        {
            throw new FileNotFoundException( propFile.toString() );
        }

        final Properties fileProps = new Properties();
        final FileInputStream inStream = new FileInputStream( propFile );
        try
        {
            fileProps.load( inStream );
        }
        finally
        {
            IOUtil.close( inStream );
        }

        final Properties combinedProps = new Properties();
        combinedProps.putAll( baseProps );
        combinedProps.putAll( fileProps );

        // The algorithm iterates only over the fileProps which is all that is required to resolve
        // the properties defined within the file. This is slighlty different to current, however
        // I suspect that this was the actual original intent.
        //
        // The difference is that #loadPropertyFile(File, boolean, boolean) also resolves System properties
        // whose values contain expressions. I believe this is unexpected and is not validated by the test cases,
        // as can be verified by replacing the implementation of #loadPropertyFile(File, boolean, boolean)
        // with the commented variant I have provided that reuses this method.

        for ( Iterator iter = fileProps.keySet().iterator(); iter.hasNext(); )
        {
            final String k = (String) iter.next();
            final String propValue = getPropertyValue( k, combinedProps );
            fileProps.setProperty( k, propValue );
        }

        return fileProps;
    }

    /**
     * Reads a property file, resolving all internal variables.
     *
     * @param propfile The property file to load
     * @param fail wheter to throw an exception when the file cannot be loaded or to return null
     * @param useSystemProps wheter to incorporate System.getProperties settings into the returned Properties object.
     * @return the loaded and fully resolved Properties object
     */
    public static Properties loadPropertyFile( File propfile, boolean fail, boolean useSystemProps )
        throws IOException
    {

        final Properties baseProps = new Properties();

        if ( useSystemProps )
        {
            baseProps.putAll( System.getProperties() );
        }

        final Properties resolvedProps = new Properties();
        try
        {
            resolvedProps.putAll( loadPropertyFile( propfile, baseProps ) );
        }
        catch ( FileNotFoundException e )
        {
            if ( fail )
            {
                throw new FileNotFoundException( propfile.toString() );
            }
        }

        if ( useSystemProps )
        {
            resolvedProps.putAll( baseProps );
        }

        return resolvedProps;
    }

    /**
     * Retrieves a property value, replacing values like ${token}
     * using the Properties to look them up.
     *
     * It will leave unresolved properties alone, trying for System
     * properties, and implements reparsing (in the case that
     * the value of a property contains a key), and will
     * not loop endlessly on a pair like
     * test = ${test}.
     */
    private static String getPropertyValue( String k, Properties p )
    {
        // This can also be done using InterpolationFilterReader,
        // but it requires reparsing the file over and over until
        // it doesn't change.

        String v = p.getProperty( k );
        String ret = "";
        int idx, idx2;

        while ( ( idx = v.indexOf( "${" ) ) >= 0 )
        {
            // append prefix to result
            ret += v.substring( 0, idx );

            // strip prefix from original
            v = v.substring( idx + 2 );

            // if no matching } then bail
            idx2 = v.indexOf( '}' );
            if ( idx2 < 0 )
            {
                break;
            }

            // strip out the key and resolve it
            // resolve the key/value for the ${statement}
            String nk = v.substring( 0, idx2 );
            v = v.substring( idx2 + 1 );
            String nv = p.getProperty( nk );

            // try global environment..
            if ( nv == null )
            {
                nv = System.getProperty( nk );
            }

            // if the key cannot be resolved,
            // leave it alone ( and don't parse again )
            // else prefix the original string with the
            // resolved property ( so it can be parsed further )
            // taking recursion into account.
            if ( nv == null || nv.equals( k ) )
            {
                ret += "${" + nk + "}";
            }
            else
            {
                v = nv + v;
            }
        }
        return ret + v;
    }
}
