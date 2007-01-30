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
package org.apache.maven.plugins.repository;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

/**
 * Goal which creates an upload bundle for a project built with maven.
 *
 * @goal bundle-create
 * @execute phase="package"
 * 
 */
public class BundleCreateMojo
    extends AbstractMojo
{
    public static final String POM = "pom.xml";

    /**
     * Base directory.
     * @parameter expression="${basedir}"
     */
    private String basedir;

    /**
     * The current maven project.
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * Jar archiver.
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     * @readonly
     */
    private JarArchiver jarArchiver;

    /**
     * @component role="org.apache.maven.artifact.handler.manager.ArtifactHandlerManager"
     * @required
     * @readonly
     */
    private ArtifactHandlerManager artifactHandlerManager;

    public void execute()
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------
        // Make sure we have a packaging != pom
        // ----------------------------------------------------------------------

        if ( project.getPackaging().equals( "pom" ) )
        {
            throw new MojoExecutionException( "Packaging cannot be POM when creating an upload bundle." );
        }

        // ----------------------------------------------------------------------
        // Check the mandatory elements of the POM
        //
        // groupId
        // artifactId
        // packaging
        // name
        // version
        // url
        // description
        // dependencies
        // licenses
        // ----------------------------------------------------------------------

        validate( project.getName(), "project.name" );

        validate( project.getUrl(), "project.url" );

        validate( project.getDescription(), "project.description" );

        if ( project.getLicenses().isEmpty() )
        {
            throw new MojoExecutionException( "At least one license must be defined." );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        File pom = new File( basedir, POM );

        String finalName = project.getBuild().getFinalName();

        String outputDirectory = project.getBuild().getDirectory();

        String extension = artifactHandlerManager.getArtifactHandler( project.getPackaging() ).getExtension();

        File artifact = new File( outputDirectory, finalName + "." + extension );

        File sourceArtifact = new File( outputDirectory, finalName + "-sources." + extension );

        File javadocArtifact = new File( outputDirectory, finalName + "-javadoc." + extension );

        File bundle = new File( outputDirectory, finalName + "-bundle.jar" );

        try
        {
            jarArchiver.addFile( pom, POM );

            jarArchiver.addFile( artifact, artifact.getName() );

            if ( sourceArtifact.exists() )
            {
                jarArchiver.addFile( sourceArtifact, sourceArtifact.getName() );
            }
            else
            {
                getLog()
                    .warn(
                           "Sources not included in upload bundle. In order to add sources please run \"mvn source:jar javadoc:jar repository:bundle-create\"" );
            }

            if ( javadocArtifact.exists() )
            {
                jarArchiver.addFile( javadocArtifact, javadocArtifact.getName() );
            }
            else
            {
                getLog()
                    .warn(
                           "Javadoc not included in upload bundle. In order to add javadocs please run \"mvn source:jar javadoc:jar repository:bundle-create\"" );
            }

            jarArchiver.setDestFile( bundle );

            jarArchiver.createArchive();
        }
        catch ( Exception e )
        {
            e.printStackTrace();

            throw new MojoExecutionException( "Error creating upload bundle archive." );
        }
    }

    private void validate( String data, String expression )
        throws MojoExecutionException
    {
        if ( StringUtils.isEmpty( data ) )
        {
            throw new MojoExecutionException( expression + " must be present." );
        }
    }
}
