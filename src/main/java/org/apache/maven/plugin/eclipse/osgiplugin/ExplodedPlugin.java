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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.plugin.eclipse.InstallPluginsMojo;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Represents an Eclipse plugin that it's exploded in a directory
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class ExplodedPlugin
    extends AbstractEclipseOsgiPlugin
{

    private File tempJarFile;

    public ExplodedPlugin( File folder )
    {
        super( folder );
    }

    private File getManifestFile()
    {
        return new File( getFile(), "META-INF/MANIFEST.MF" );
    }

    public Manifest getManifest()
        throws IOException
    {
        if ( !getManifestFile().exists() )
        {
            return null;
        }

        FileInputStream is = new FileInputStream( getManifestFile() );
        try
        {
            return new Manifest( is );
        }
        finally
        {
            is.close();
        }
    }

    public boolean hasManifest()
    {
        return getManifestFile().exists();
    }

    public File getJarFile()
        throws IOException
    {
        if ( tempJarFile == null )
        {
            try
            {
                tempJarFile = File.createTempFile( "mvn-eclipse", null );
                tempJarFile.deleteOnExit();

                JarArchiver jarArchiver = new JarArchiver();

                jarArchiver.setDestFile( tempJarFile );
                jarArchiver.addDirectory( getFile() );
                jarArchiver.setManifest( getManifestFile() );
                jarArchiver.createArchive();

                return tempJarFile;
            }
            catch ( ArchiverException e )
            {
                // TODO only accepts the cause as parameter in JDK 1.6+
                throw new IOException( e.getMessage() );
            }
        }
        return tempJarFile;
    }

    public JarFile getJar()
        throws IOException
    {
        return new JarFile( getJarFile(), false );
    }

    /**
     * set the pom property to install unpacked if it was unpacked
     */
    public Properties getPomProperties()
    {
        Properties properties = new Properties();
        properties.setProperty( InstallPluginsMojo.PROP_UNPACK_PLUGIN, Boolean.TRUE.toString() );
        return properties;
    }

}
