package org.apache.maven.plugins.release.config;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.model.Scm;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Read and write release configuration and state from a properties file.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PropertiesReleaseConfigurationStore
    extends AbstractLogEnabled
    implements ReleaseConfigurationStore
{
    public ReleaseConfiguration read( ReleaseConfiguration mergeConfiguration )
        throws ReleaseConfigurationStoreException
    {
        return read( mergeConfiguration, getDefaultReleasePropertiesFile( mergeConfiguration ) );
    }

    public ReleaseConfiguration read( File file )
        throws ReleaseConfigurationStoreException
    {
        return read( null, file );
    }

    public ReleaseConfiguration read( ReleaseConfiguration mergeConfiguration, File file )
        throws ReleaseConfigurationStoreException
    {
        Properties properties = new Properties();

        InputStream inStream = null;
        try
        {
            inStream = new FileInputStream( file );

            properties.load( inStream );
        }
        catch ( FileNotFoundException e )
        {
            getLogger().debug( file.getName() + " not found - using empty properties" );
        }
        catch ( IOException e )
        {
            throw new ReleaseConfigurationStoreException(
                "Error reading properties file '" + file.getName() + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( inStream );
        }

        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setCompletedPhase( properties.getProperty( "completedPhase" ) );
        releaseConfiguration.setUrl( properties.getProperty( "scm.url" ) );
        releaseConfiguration.setUsername( properties.getProperty( "scm.username" ) );
        releaseConfiguration.setPassword( properties.getProperty( "scm.password" ) );
        releaseConfiguration.setPrivateKey( properties.getProperty( "scm.privateKey" ) );
        releaseConfiguration.setPassphrase( properties.getProperty( "scm.passphrase" ) );
        releaseConfiguration.setTagBase( properties.getProperty( "scm.tagBase" ) );
        releaseConfiguration.setReleaseLabel( properties.getProperty( "scm.tag" ) );
        releaseConfiguration.setAdditionalArguments( properties.getProperty( "exec.additionalArguments" ) );
        releaseConfiguration.setPomFileName( properties.getProperty( "exec.pomFileName" ) );
        releaseConfiguration.setPreparationGoals( properties.getProperty( "preparationGoals" ) );

        // boolean properties are not written to the properties file because the value from the caller is always used

        for ( Iterator i = properties.keySet().iterator(); i.hasNext(); )
        {
            String property = (String) i.next();
            if ( property.startsWith( "project.rel." ) )
            {
                releaseConfiguration.mapReleaseVersion( property.substring( "project.rel.".length() ),
                                                        properties.getProperty( property ) );
            }
            else if ( property.startsWith( "project.dev." ) )
            {
                releaseConfiguration.mapDevelopmentVersion( property.substring( "project.dev.".length() ),
                                                            properties.getProperty( property ) );
            }
            else if ( property.startsWith( "project.scm." ) )
            {
                int index = property.lastIndexOf( '.' );
                if ( index > "project.scm.".length() )
                {
                    String key = property.substring( "project.scm.".length(), index );

                    if ( !releaseConfiguration.getOriginalScmInfo().containsKey( key ) )
                    {
                        Scm scm = new Scm();
                        scm.setConnection( properties.getProperty( "project.scm." + key + ".connection" ) );
                        scm.setDeveloperConnection(
                            properties.getProperty( "project.scm." + key + ".developerConnection" ) );
                        scm.setUrl( properties.getProperty( "project.scm." + key + ".url" ) );
                        scm.setTag( properties.getProperty( "project.scm." + key + ".tag" ) );

                        releaseConfiguration.mapOriginalScmInfo( key, scm );
                    }
                }
            }
        }

        if ( mergeConfiguration != null )
        {
            releaseConfiguration.merge( mergeConfiguration );
        }

        return releaseConfiguration;
    }

    public void write( ReleaseConfiguration config )
        throws ReleaseConfigurationStoreException
    {
        write( config, getDefaultReleasePropertiesFile( config ) );
    }

    public void write( ReleaseConfiguration config, File file )
        throws ReleaseConfigurationStoreException
    {
        Properties properties = new Properties();
        properties.setProperty( "completedPhase", config.getCompletedPhase() );
        properties.setProperty( "scm.url", config.getUrl() );
        if ( config.getUsername() != null )
        {
            properties.setProperty( "scm.username", config.getUsername() );
        }
        if ( config.getPassword() != null )
        {
            properties.setProperty( "scm.password", config.getPassword() );
        }
        if ( config.getPrivateKey() != null )
        {
            properties.setProperty( "scm.privateKey", config.getPrivateKey() );
        }
        if ( config.getPassphrase() != null )
        {
            properties.setProperty( "scm.passphrase", config.getPassphrase() );
        }
        if ( config.getTagBase() != null )
        {
            properties.setProperty( "scm.tagBase", config.getTagBase() );
        }
        if ( config.getReleaseLabel() != null )
        {
            properties.setProperty( "scm.tag", config.getReleaseLabel() );
        }
        if ( config.getAdditionalArguments() != null )
        {
            properties.setProperty( "exec.additionalArguments", config.getAdditionalArguments() );
        }
        if ( config.getPomFileName() != null )
        {
            properties.setProperty( "exec.pomFileName", config.getPomFileName() );
        }
        if ( config.getPreparationGoals() != null )
        {
            properties.setProperty( "preparationGoals", config.getPreparationGoals() );
        }

        // boolean properties are not written to the properties file because the value from the caller is always used

        for ( Iterator i = config.getReleaseVersions().entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            properties.setProperty( "project.rel." + entry.getKey(), (String) entry.getValue() );
        }

        for ( Iterator i = config.getDevelopmentVersions().entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            properties.setProperty( "project.dev." + entry.getKey(), (String) entry.getValue() );
        }

        for ( Iterator i = config.getOriginalScmInfo().entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            Scm scm = (Scm) entry.getValue();
            properties.setProperty( "project.scm." + entry.getKey() + ".connection", scm.getConnection() );
            if ( scm.getDeveloperConnection() != null )
            {
                properties.setProperty( "project.scm." + entry.getKey() + ".developerConnection",
                                        scm.getDeveloperConnection() );
            }
            if ( scm.getUrl() != null )
            {
                properties.setProperty( "project.scm." + entry.getKey() + ".url", scm.getUrl() );
            }
            if ( scm.getTag() != null )
            {
                properties.setProperty( "project.scm." + entry.getKey() + ".tag", scm.getTag() );
            }
        }

        OutputStream outStream = null;
        //noinspection OverlyBroadCatchBlock
        try
        {
            outStream = new FileOutputStream( file );

            properties.store( outStream, "release configuration" );
        }
        catch ( IOException e )
        {
            throw new ReleaseConfigurationStoreException(
                "Error writing properties file '" + file.getName() + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( outStream );
        }

    }

    private static File getDefaultReleasePropertiesFile( ReleaseConfiguration mergeConfiguration )
    {
        return new File( mergeConfiguration.getWorkingDirectory(), "release.properties" );
    }
}
