package org.apache.maven.plugin.ear.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.ear.EarModuleFactory;
import org.apache.maven.plugin.ear.EarPluginException;
import org.apache.maven.plugin.ear.UnknownArtifactTypeException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Allows to map custom artifact type to standard type.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class ArtifactTypeMappingService
{
    static final String ARTIFACT_TYPE_MAPPING_ELEMENT = "artifactTypeMapping";

    static final String TYPE_ATTRIBUTE = "type";

    static final String MAPPING_ATTRIBUTE = "mapping";

    // A standard type to a list of customType
    private final Map typeMappings;

    // The user-defined mapping for direct access
    private final Map customMappings;

    public ArtifactTypeMappingService()
    {
        this.typeMappings = new HashMap();
        this.customMappings = new HashMap();
        init();
    }

    public void configure( final PlexusConfiguration plexusConfiguration )
        throws EarPluginException, PlexusConfigurationException
    {

        // No user defined configuration
        if ( plexusConfiguration == null )
        {
            return;
        }

        // Inject users configuration
        final PlexusConfiguration[] artifactTypeMappings =
            plexusConfiguration.getChildren( ARTIFACT_TYPE_MAPPING_ELEMENT );
        for ( int i = 0; i < artifactTypeMappings.length; i++ )
        {
            PlexusConfiguration artifactTypeMapping = artifactTypeMappings[i];
            final String customType = artifactTypeMapping.getAttribute( TYPE_ATTRIBUTE );
            final String mapping = artifactTypeMapping.getAttribute( MAPPING_ATTRIBUTE );

            if ( customType == null )
            {
                throw new EarPluginException( "Invalid artifact type mapping, type attribute should be set." );
            }
            else if ( mapping == null )
            {
                throw new EarPluginException( "Invalid artifact type mapping, mapping attribute should be set." );
            }
            else if ( !EarModuleFactory.isStandardArtifactType( mapping ) )
            {
                throw new EarPluginException(
                    "Invalid artifact type mapping, mapping[" + mapping + "] must be a standard Ear artifact type[" +
                        EarModuleFactory.getStandardArtifactTypes() + "]" );
            }
            else if ( customMappings.containsKey( customType ) )
            {
                throw new EarPluginException(
                    "Invalid artifact type mapping, type[" + customType + "] is already registered." );
            }
            else
            {
                // Add the custom mapping
                customMappings.put( customType, mapping );

                // Register the custom mapping to its standard type
                List typeMapping = (List) typeMappings.get( mapping );
                typeMapping.add( customType );
            }
        }
    }

    /**
     * Specify whether the <tt>customType</tt> could be mapped to the
     * <tt>standardType</tt>.
     *
     * @param standardType the standard type (ejb, jar, war, ...)
     * @param customType   a user-defined type
     * @return true if the customType could be mapped to the standard type
     */
    public boolean isMappedToType( final String standardType, final String customType )
    {
        if ( !EarModuleFactory.isStandardArtifactType( standardType ) )
        {
            throw new IllegalStateException(
                "Artifact type[" + standardType + "] is not a standard Ear artifact type[" +
                    EarModuleFactory.getStandardArtifactTypes() + "]" );
        }
        final List typeMappings = (List) this.typeMappings.get( standardType );
        return typeMappings.contains( customType );

    }

    /**
     * Returns the standard type for the specified <tt>type</tt>. If
     * the specified type is already a standard type, the orignal type
     * is returned.
     *
     * @param type a type
     * @return the standard type (ejb, jar, war, ...) for this type
     */
    public String getStandardType( final String type )
        throws UnknownArtifactTypeException
    {
        if ( type == null )
        {
            throw new IllegalStateException( "custom type could not be null." );
        }
        else if ( EarModuleFactory.getStandardArtifactTypes().contains( type ) )
        {
            return type;
        }
        else if ( !customMappings.containsKey( type ) )
        {
            throw new UnknownArtifactTypeException( "Unknown artifact type[" + type + "]" );
        }
        else
        {
            return (String) customMappings.get( type );
        }
    }

    private void init()
    {
        // Initialize the typeMappings
        typeMappings.clear();

        // Clear the customMappings
        customMappings.clear();

        // Initialize the mapping with the standard artifact types
        final Iterator it = EarModuleFactory.getStandardArtifactTypes().iterator();
        while ( it.hasNext() )
        {
            String type = (String) it.next();
            List typeMapping = new ArrayList();
            typeMapping.add( type );
            this.typeMappings.put( type, typeMapping );
        }

    }
}
