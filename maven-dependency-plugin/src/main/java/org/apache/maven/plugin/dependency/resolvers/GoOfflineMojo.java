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

import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractResolveMojo;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

/**
 * Goal that resolves all project dependencies, including plugins and reports
 * and their dependencies.
 * 
 * @goal go-offline
 * @execute goal=resolve-plugins
 * @requiresDependencyResolution test
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @since 2.0
 */
public class GoOfflineMojo
    extends AbstractResolveMojo
{

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through
     * displaying the resolved version.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     */
    public void execute()
        throws MojoExecutionException
    {
        Set artifacts = project.getArtifacts();

        if ( !silent )
        {
            for ( Iterator i = artifacts.iterator(); i.hasNext(); )
            {
                this.getLog().info( "Resolved: " + DependencyUtil.getFormattedFileName( (Artifact) i.next(), false ) );
            }
        }
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
