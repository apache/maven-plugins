package org.apache.maven.plugin.ear;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.ear.util.ArtifactTypeMappingService;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds an {@link EarModule} based on an <tt>Artifact</tt>.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public final class EarModuleFactory
{
    public final static List standardArtifactTypes = new ArrayList();

    static
    {
        standardArtifactTypes.add( "jar" );
        standardArtifactTypes.add( "ejb" );
        standardArtifactTypes.add( "ejb3" );
        standardArtifactTypes.add( "par" );
        standardArtifactTypes.add( "ejb-client" );
        standardArtifactTypes.add( "rar" );
        standardArtifactTypes.add( "war" );
        standardArtifactTypes.add( "sar" );
        standardArtifactTypes.add( "wsr" );
        standardArtifactTypes.add( "har" );
    }

    /**
     * Creates a new {@link EarModule} based on the
     * specified {@link Artifact} and the specified
     * execution configuration.
     *
     * @param artifact             the artifact
     * @param defaultJavaBundleDir the default bundle dir for {@link JavaModule}
     * @return an ear module for this artifact
     */
    public static EarModule newEarModule( Artifact artifact, String defaultJavaBundleDir )
        throws UnknownArtifactTypeException
    {
        // Get the standard artifact type based on default config and user-defined mapping(s)
        final String artifactType =
            ArtifactTypeMappingService.getInstance().getStandardType( artifact.getType());

        if ( "jar".equals( artifactType ) )
        {
            return new JavaModule( artifact, defaultJavaBundleDir );
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
            return new EjbClientModule( artifact );
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
    public static List getStandardArtifactTypes()
    {
        return standardArtifactTypes;
    }

    /**
     * Specify whether the specified type is standard artifact
     * type.
     *
     * @param type the type to check
     * @return true if the specified type is a standard artifact type
     */
    public static boolean isStandardArtifactType( final String type )
    {
        return standardArtifactTypes.contains( type );
    }

}
