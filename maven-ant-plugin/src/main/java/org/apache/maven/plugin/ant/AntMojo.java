package org.apache.maven.plugin.ant;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Generate Ant build files.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo change this to use the artifact ant tasks instead of :get
 */
@Mojo( name = "ant", requiresDependencyResolution = ResolutionScope.TEST )
public class AntMojo
    extends AbstractMojo
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Used for resolving artifacts.
     */
    @Component
    private ArtifactResolver resolver;

    /**
     * Factory for creating artifact objects.
     */
    @Component
    private ArtifactFactory factory;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The project to create a build for.
     */
    @Component
    private MavenProject project;

    /**
     * The local repository where the artifacts are located.
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;

    /**
     * The remote repositories where artifacts are located.
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true )
    private List remoteRepositories;

    /**
     * The current user system settings for use in Maven.
     */
    @Component
    private Settings settings;

    /**
     * Whether or not to overwrite the <code>build.xml</code> file.
     */
    @Parameter( property = "overwrite", defaultValue = "false" )
    private boolean overwrite;

    /**
     * The current Maven session.
     */
    @Component
    private MavenSession session;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        ArtifactResolverWrapper artifactResolverWrapper = ArtifactResolverWrapper.getInstance( resolver, factory,
                                                                                       localRepository,
                                                                                       remoteRepositories );

        Properties executionProperties = ( session != null ) ? session.getExecutionProperties() : null;

        AntBuildWriter antBuildWriter =
            new AntBuildWriter( project, artifactResolverWrapper, settings, overwrite, executionProperties );

        try
        {
            antBuildWriter.writeBuildXmls();
            antBuildWriter.writeBuildProperties();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error building Ant script: " + e.getMessage(), e );
        }

        getLog().info(
                       "Wrote Ant project for " + project.getArtifactId() + " to "
                           + project.getBasedir().getAbsolutePath() );
    }
}
