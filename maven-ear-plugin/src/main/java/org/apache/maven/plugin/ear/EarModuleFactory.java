package org.apache.maven.plugin.ear;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.ear.util.ArtifactTypeMappingService;
import org.apache.maven.plugin.ear.util.JavaEEVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds an {@link EarModule} based on an <tt>Artifact</tt>.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public final class EarModuleFactory
{
    /**
     * The list of artifact types.
     */
    public static final List<String> STANDARD_ARTIFACT_TYPE;

    static
    {
        List<String> temp = new ArrayList<String>();
        temp.add( "jar" );
        temp.add( "ejb" );
        temp.add( "ejb3" );
        temp.add( "par" );
        temp.add( "ejb-client" );
        temp.add( "app-client" );
        temp.add( "rar" );
        temp.add( "war" );
        temp.add( "sar" );
        temp.add( "wsr" );
        temp.add( "har" );
        STANDARD_ARTIFACT_TYPE = Collections.unmodifiableList( temp );
    }

    /**
     * Creates a new {@link EarModule} based on the specified {@link Artifact} and the specified execution
     * configuration.
     * 
     * @param artifact the artifact
     * @param javaEEVersion the javaEE version to use
     * @param defaultLibBundleDir the default bundle dir for {@link org.apache.maven.plugin.ear.JarModule}
     * @param includeInApplicationXml should {@link org.apache.maven.plugin.ear.JarModule} be included in application
     *            Xml
     * @param typeMappingService The artifact type mapping service
     * @return an ear module for this artifact
     * @throws UnknownArtifactTypeException if the artifact is not handled
     */
    @SuppressWarnings( "deprecation" )
    // CHECKSTYLE_OFF: LineLength
    public static EarModule newEarModule( Artifact artifact, JavaEEVersion javaEEVersion, String defaultLibBundleDir,
                                          Boolean includeInApplicationXml, ArtifactTypeMappingService typeMappingService )
    // CHECKSTYLE_ON: LineLength
        throws UnknownArtifactTypeException
    {
        // Get the standard artifact type based on default config and user-defined mapping(s)
        final String artifactType;
        try
        {
            artifactType = typeMappingService.getStandardType( artifact.getType() );
        }
        catch ( UnknownArtifactTypeException e )
        {
            throw new UnknownArtifactTypeException( e.getMessage() + " for " + artifact.getArtifactId() );
        }

        if ( "jar".equals( artifactType ) )
        {
            return new JarModule( artifact, defaultLibBundleDir, includeInApplicationXml );
        }
        else if ( "ejb".equals( artifactType ) )
        {
            return new EjbModule( artifact );
        }
        else if ( "ejb3".equals( artifactType ) )
        {
            return new Ejb3Module( artifact );
        }
        else if ( "par".equals( artifactType ) )
        {
            return new ParModule( artifact );
        }
        else if ( "ejb-client".equals( artifactType ) )
        {
            // Somewhat weird way to tackle the problem described in MEAR-85
            if ( javaEEVersion.le( JavaEEVersion.ONE_DOT_FOUR ) )
            {
                return new EjbClientModule( artifact, null );
            }
            else
            {
                return new EjbClientModule( artifact, defaultLibBundleDir );
            }
        }
        else if ( "app-client".equals( artifactType ) )
        {
            return new AppClientModule( artifact );
        }
        else if ( "rar".equals( artifactType ) )
        {
            return new RarModule( artifact );
        }
        else if ( "war".equals( artifactType ) )
        {
            return new WebModule( artifact );
        }
        else if ( "sar".equals( artifactType ) )
        {
            return new SarModule( artifact );
        }
        else if ( "wsr".equals( artifactType ) )
        {
            return new WsrModule( artifact );
        }
        else if ( "har".equals( artifactType ) )
        {
            return new HarModule( artifact );
        }
        else
        {
            throw new IllegalStateException( "Could not handle artifact type[" + artifactType + "]" );
        }
    }

    /**
     * Returns a list of standard artifact types.
     * 
     * @return the standard artifact types
     */
    public static List<String> getStandardArtifactTypes()
    {
        return STANDARD_ARTIFACT_TYPE;
    }

    /**
     * Specify whether the specified type is standard artifact type.
     * 
     * @param type the type to check
     * @return true if the specified type is a standard artifact type
     */
    public static boolean isStandardArtifactType( final String type )
    {
        return STANDARD_ARTIFACT_TYPE.contains( type );
    }

}
