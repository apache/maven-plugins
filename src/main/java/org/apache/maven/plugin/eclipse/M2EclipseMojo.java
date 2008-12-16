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
package org.apache.maven.plugin.eclipse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Creates an eclipse project that is ready to use with the M2Elipse plugin.
 * 
 * @goal m2eclipse
 * @execute phase="generate-resources"
 * @since 2.4
 */
public class M2EclipseMojo
    extends EclipsePlugin
{

    protected static final String M2ECLIPSE_NATURE = "org.maven.ide.eclipse.maven2Nature";

    protected static final String M2ECLIPSE_BUILD_COMMAND = "org.maven.ide.eclipse.maven2Builder";

    protected static final String M2ECLIPSE_CLASSPATH_CONTAINER = "org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER";

    protected void setupExtras()
        throws MojoExecutionException
    {
        // disable normal dependency resolution; the m2eclipse plugin will handle it.
        setResolveDependencies( false );

        if ( getAdditionalProjectnatures() != null )
        {
            getAdditionalProjectnatures().add( M2ECLIPSE_NATURE );
        }
        else
        {
            setAdditionalProjectnatures( new ArrayList( Collections.singletonList( M2ECLIPSE_NATURE ) ) );
        }

        if ( getAdditionalBuildcommands() != null )
        {
            getAdditionalBuildcommands().add( M2ECLIPSE_BUILD_COMMAND );
        }
        else
        {
            setAdditionalBuildcommands( new ArrayList( Collections.singletonList( M2ECLIPSE_BUILD_COMMAND ) ) );
        }

        List classpathContainers = getClasspathContainers();
        if ( classpathContainers == null )
        {
            classpathContainers = new ArrayList();

            classpathContainers.add( COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER );

            if ( isPdeProject() )
            {
                classpathContainers.add( REQUIRED_PLUGINS_CONTAINER );
            }
        }

        classpathContainers.add( M2ECLIPSE_CLASSPATH_CONTAINER );

        setClasspathContainers( classpathContainers );
    }

}
