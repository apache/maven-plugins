package org.apache.maven.plugin.assembly.utils;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
        throws IOException
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

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
        interpolator.addValueSource( new PropertiesInterpolationValueSource( props ) );
        
        for ( Enumeration n = props.propertyNames(); n.hasMoreElements(); )
        {
            String key = (String) n.nextElement();
            String value = interpolator.interpolate( props.getProperty( key ), "__properties" );
            
            props.setProperty( key, value );
        }

        return props;
    }

}
