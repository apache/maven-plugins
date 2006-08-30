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
public class PropertiesReleaseDescriptorStore
    extends AbstractLogEnabled
    implements ReleaseDescriptorStore
{
    public ReleaseDescriptor read( ReleaseDescriptor mergeDescriptor )
        throws ReleaseDescriptorStoreException
    {
        return read( mergeDescriptor, getDefaultReleasePropertiesFile( mergeDescriptor ) );
    }

    public ReleaseDescriptor read( File file )
        throws ReleaseDescriptorStoreException
    {
        return read( null, file );
    }

    public ReleaseDescriptor read( ReleaseDescriptor mergeDescriptor, File file )
        throws ReleaseDescriptorStoreException
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
            throw new ReleaseDescriptorStoreException(
                "Error reading properties file '" + file.getName() + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( inStream );
        }

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setCompletedPhase( properties.getProperty( "completedPhase" ) );
        releaseDescriptor.setScmSourceUrl( properties.getProperty( "scm.url" ) );
        releaseDescriptor.setScmUsername( properties.getProperty( "scm.username" ) );
        releaseDescriptor.setScmPassword( properties.getProperty( "scm.password" ) );
        releaseDescriptor.setScmPrivateKey( properties.getProperty( "scm.privateKey" ) );
        releaseDescriptor.setScmPrivateKeyPassPhrase( properties.getProperty( "scm.passphrase" ) );
        releaseDescriptor.setScmTagBase( properties.getProperty( "scm.tagBase" ) );
        releaseDescriptor.setScmReleaseLabel( properties.getProperty( "scm.tag" ) );
        releaseDescriptor.setAdditionalArguments( properties.getProperty( "exec.additionalArguments" ) );
        releaseDescriptor.setPomFileName( properties.getProperty( "exec.pomFileName" ) );
        releaseDescriptor.setPreparationGoals( properties.getProperty( "preparationGoals" ) );

        // boolean properties are not written to the properties file because the value from the caller is always used

        for ( Iterator i = properties.keySet().iterator(); i.hasNext(); )
        {
            String property = (String) i.next();
            if ( property.startsWith( "project.rel." ) )
            {
                releaseDescriptor.mapReleaseVersion( property.substring( "project.rel.".length() ),
                                                     properties.getProperty( property ) );
            }
            else if ( property.startsWith( "project.dev." ) )
            {
                releaseDescriptor.mapDevelopmentVersion( property.substring( "project.dev.".length() ),
                                                         properties.getProperty( property ) );
            }
            else if ( property.startsWith( "project.scm." ) )
            {
                int index = property.lastIndexOf( '.' );
                if ( index > "project.scm.".length() )
                {
                    String key = property.substring( "project.scm.".length(), index );

                    if ( !releaseDescriptor.getOriginalScmInfo().containsKey( key ) )
                    {
                        if ( properties.getProperty( "project.scm." + key + ".empty" ) != null )
                        {
                            releaseDescriptor.mapOriginalScmInfo( key, null );
                        }
                        else
                        {
                            Scm scm = new Scm();
                            scm.setConnection( properties.getProperty( "project.scm." + key + ".connection" ) );
                            scm.setDeveloperConnection(
                                properties.getProperty( "project.scm." + key + ".developerConnection" ) );
                            scm.setUrl( properties.getProperty( "project.scm." + key + ".url" ) );
                            scm.setTag( properties.getProperty( "project.scm." + key + ".tag" ) );

                            releaseDescriptor.mapOriginalScmInfo( key, scm );
                        }
                    }
                }
            }
        }

        if ( mergeDescriptor != null )
        {
            releaseDescriptor = ReleaseUtils.merge( releaseDescriptor, mergeDescriptor );
        }

        return releaseDescriptor;
    }

    public void write( ReleaseDescriptor config )
        throws ReleaseDescriptorStoreException
    {
        write( config, getDefaultReleasePropertiesFile( config ) );
    }

    public void delete( ReleaseDescriptor config )
    {
        File file = getDefaultReleasePropertiesFile( config );
        if ( file.exists() )
        {
            file.delete();
        }
    }

    public void write( ReleaseDescriptor config, File file )
        throws ReleaseDescriptorStoreException
    {
        Properties properties = new Properties();
        properties.setProperty( "completedPhase", config.getCompletedPhase() );
        properties.setProperty( "scm.url", config.getScmSourceUrl() );
        if ( config.getScmUsername() != null )
        {
            properties.setProperty( "scm.username", config.getScmUsername() );
        }
        if ( config.getScmPassword() != null )
        {
            properties.setProperty( "scm.password", config.getScmPassword() );
        }
        if ( config.getScmPrivateKey() != null )
        {
            properties.setProperty( "scm.privateKey", config.getScmPrivateKey() );
        }
        if ( config.getScmPrivateKeyPassPhrase() != null )
        {
            properties.setProperty( "scm.passphrase", config.getScmPrivateKeyPassPhrase() );
        }
        if ( config.getScmTagBase() != null )
        {
            properties.setProperty( "scm.tagBase", config.getScmTagBase() );
        }
        if ( config.getScmReleaseLabel() != null )
        {
            properties.setProperty( "scm.tag", config.getScmReleaseLabel() );
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
            String prefix = "project.scm." + entry.getKey();
            if ( scm != null )
            {
                if ( scm.getConnection() != null )
                {
                    properties.setProperty( prefix + ".connection", scm.getConnection() );
                }
                if ( scm.getDeveloperConnection() != null )
                {
                    properties.setProperty( prefix + ".developerConnection", scm.getDeveloperConnection() );
                }
                if ( scm.getUrl() != null )
                {
                    properties.setProperty( prefix + ".url", scm.getUrl() );
                }
                if ( scm.getTag() != null )
                {
                    properties.setProperty( prefix + ".tag", scm.getTag() );
                }
            }
            else
            {
                properties.setProperty( prefix + ".empty", "true" );
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
            throw new ReleaseDescriptorStoreException(
                "Error writing properties file '" + file.getName() + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( outStream );
        }

    }

    private static File getDefaultReleasePropertiesFile( ReleaseDescriptor mergeDescriptor )
    {
        return new File( mergeDescriptor.getWorkingDirectory(), "release.properties" );
    }
}
