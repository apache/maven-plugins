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
package org.apache.maven.plugin.eclipse.osgiplugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Common functionality for both exploded and packaged plugins.
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public abstract class AbstractEclipseOsgiPlugin
    implements EclipseOsgiPlugin
{

    private File file;

    private Properties pluginProperties;

    public AbstractEclipseOsgiPlugin( File file )
    {
        this.setFile( file );
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public File getFile()
    {
        return file;
    }

    public String toString()
    {
        return getFile().getAbsolutePath();
    }

    public Properties getPluginProperties()
        throws IOException
    {
        if ( pluginProperties == null )
        {
            JarFile file = getJar();
            InputStream pluginPropertiesStream = null;
            try
            {
                pluginProperties = new Properties();
                ZipEntry jarEntry = file.getEntry( "plugin.properties" );
                if ( jarEntry != null )
                {
                    pluginPropertiesStream = file.getInputStream( jarEntry );
                    pluginProperties.load( pluginPropertiesStream );
                }
            }
            finally
            {
                if ( pluginPropertiesStream != null )
                {
                    try
                    {
                        pluginPropertiesStream.close();
                    }
                    catch ( IOException e )
                    {
                        // ignore
                    }
                }
            }
        }
        return pluginProperties;
    }

    public Properties getPomProperties()
    {
        return new Properties();
    }

    public String getManifestAttribute( String key )
        throws IOException
    {
        String value = getManifest().getMainAttributes().getValue( key );

        if ( value == null )
        {
            return null;
        }

        /* check the plugin properties for translations */
        if ( value.startsWith( "%" ) )
        {
            String valueFromProperties = getPluginProperties().getProperty( value.substring( 1 ) );
            if ( valueFromProperties != null )
            {
                value = valueFromProperties;
            }
        }

        return value;
    }
}
