package org.apache.maven.plugin.checkstyle.resource;

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

import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.resource.DefaultResourceManager;
import org.codehaus.plexus.resource.PlexusResource;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.ResourceLoader;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.resource.loader.ThreadContextClasspathResourceLoader;

@Component( role = ResourceManager.class, hint = "license" )
public class LicenseResourceManager
    extends DefaultResourceManager
{

    @Requirement( role = ResourceLoader.class )
    private Map<String, ResourceLoader> resourceLoaders;

    @Override
    public void addSearchPath( String id, String path )
    {
        ResourceLoader loader = (ResourceLoader) resourceLoaders.get( id );

        if ( loader == null )
        {
            throw new IllegalArgumentException( "unknown resource loader: " + id );
        }

        loader.addSearchPath( path );
    }

    @Override
    public PlexusResource getResource( String name )
        throws ResourceNotFoundException
    {
        for ( ResourceLoader resourceLoader : resourceLoaders.values() )
        {
            if ( resourceLoader instanceof ThreadContextClasspathResourceLoader
                && !"config/maven-header.txt".equals( name ) )
            {
                // MCHECKSTYLE-219: Don't load the license from the plugin
                // classloader, only allow config/maven-header.txt
                continue;
            }

            try
            {
                PlexusResource resource = resourceLoader.getResource( name );

                getLogger().debug( "The resource " + "'" + name + "'" + " was found as " + resource.getName() + "." );

                return resource;
            }
            catch ( ResourceNotFoundException e )
            {
                getLogger().debug( "The resource " + "'" + name + "'" + " was not found with resourceLoader "
                                       + resourceLoader.getClass().getName() + "." );
            }
        }

        throw new ResourceNotFoundException( name );
    }
}
