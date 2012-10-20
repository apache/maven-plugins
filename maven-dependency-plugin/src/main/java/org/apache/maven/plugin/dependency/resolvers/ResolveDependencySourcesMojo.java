package org.apache.maven.plugin.dependency.resolvers;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Goal that resolves the project source dependencies from the repository.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 2.0-alpha2
 */
@Mojo( name = "sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
       requiresDependencyResolution = ResolutionScope.TEST )
public class ResolveDependencySourcesMojo
    extends ResolveDependenciesMojo
{

    private static final String SOURCE_CLASSIFIER = "sources";

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through
     * resolving the source jars.
     *
     * @throws MojoExecutionException with a message if an error occurs.
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( StringUtils.isEmpty( this.classifier ) )
        {
            this.classifier = SOURCE_CLASSIFIER;
        }

        super.execute();
    }
}
