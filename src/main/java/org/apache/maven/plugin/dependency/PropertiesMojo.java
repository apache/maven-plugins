package org.apache.maven.plugin.dependency;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.util.Set;

/**
 * Goal that sets a property pointing to the artifact file for each project dependency.
 * For each dependency (direct and transitive) a project property will be set which follows the
 * <code>groupId:artifactId:type:[classifier]</code> form and contains the path to the resolved artifact.
 *
 * @author Paul Gier
 * @version $Id$
 * @since 2.2
 */
@Mojo( name = "properties", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true )
public class PropertiesMojo
    extends AbstractMojo
{

    /**
     * The current Maven project
     */
    @Component
    protected MavenProject project;

    /**
     * Skip plugin execution completely.
     *
     * @since 2.7
     */
    @Parameter( property = "mdep.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through setting a property for each artifact.
     *
     * @throws MojoExecutionException with a message if an error occurs.
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( isSkip() )
        {
            getLog().info( "Skipping plugin execution" );
            return;
        }

        @SuppressWarnings( "unchecked" ) Set<Artifact> artifacts = getProject().getArtifacts();

        for ( Artifact artifact : artifacts )
        {
            project.getProperties().setProperty( artifact.getDependencyConflictId(),
                                                 artifact.getFile().getAbsolutePath() );
        }
    }

    public MavenProject getProject()
    {
        return project;
    }

    public boolean isSkip()
    {
        return skip;
    }

    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }
}
