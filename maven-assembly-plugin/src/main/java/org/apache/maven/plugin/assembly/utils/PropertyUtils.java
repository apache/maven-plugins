package org.apache.maven.plugin.assembly.utils;

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

import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;


/**
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @version $Id$
 */
public final class PropertyUtils
{
    private PropertyUtils()
    {
        // prevent instantiation
    }

    /**
     * Reads a property file, resolving all internal variables.
     *
     * @param propfile       The property file to load
     * @param fail           wheter to throw an exception when the file cannot be loaded or to return null
     * @param useSystemProps wheter to incorporate System.getProperties settings into the returned Properties object.
     * @return the loaded and fully resolved Properties object
     */
    public static Properties getInterpolatedPropertiesFromFile( File propfile, boolean fail, boolean useSystemProps )
        throws IOException, AssemblyFormattingException
    {
        Properties props;

        if ( useSystemProps )
        {
            props = new Properties( System.getProperties() );
        }
        else
        {
            props = new Properties();
        }

        if ( propfile.exists() )
        {
            FileInputStream inStream = new FileInputStream( propfile );
            try
            {
                props.load( inStream );
            }
            finally
            {
                IOUtil.close( inStream );
            }
        }
        else if ( fail )
        {
            throw new FileNotFoundException( propfile.toString() );
        }

        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource( new PropertiesBasedValueSource( props ) );

        for ( Enumeration n = props.propertyNames(); n.hasMoreElements(); )
        {
            String key = (String) n.nextElement();
            String value = props.getProperty( key );
            try
            {
                value = interpolator.interpolate( value );
            }
            catch ( InterpolationException e )
            {
                throw new AssemblyFormattingException( "Failed to interpolate property value: '" + value + "' for key: '" + key + "'. Reason: " + e.getMessage(), e );
            }

            props.setProperty( key, value );
        }

        return props;
    }

}
