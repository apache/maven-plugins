package org.apache.maven.plugins.repository;

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

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Goal which creates an upload bundle for a project built with Maven.
 *
 * @goal bundle-create
 * @execute phase="package"
 * @since 2.0
 */
public class BundleCreateMojo
    extends AbstractMojo
{
    public static final String POM = "pom.xml";

    /**
     * Base directory.
     *
     * @parameter default-value="${basedir}"
     * @readonly
     */
    private String basedir;

    /**
     * The current Maven project.
     *
     * @parameter default-value="${project}"
     * @readonly
     */
    private MavenProject project;
    
    /**
     * Disable validations to make sure bundle supports project materialization.
     * <br/>
     * <b>WARNING: This means your project will be MUCH harder to use.</b>
     * @parameter expression="${bundle.disableMaterialization}" default-value="false"
     */
    private boolean disableMaterialization;

    /**
     * Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * @component
     */
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * @component
     */
    protected InputHandler inputHandler;
    
    /**
     * @parameter default-value="${settings}"
     * @readonly
     */
    protected Settings settings;

    public void execute()
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------
        // Check the mandatory elements of the POM
        //
        // modelVersion
        // groupId
        // artifactId
        // packaging
        // name
        // version
        // description
        // url
        // licenses
        // dependencies
        // ----------------------------------------------------------------------

        // We don't have to validate modelVersion, groupId, artifactId or version here,
        // it is done by DefaultMaven and maven-artifact

        validate( project.getName(), "project.name" );

        validate( project.getDescription(), "project.description" );

        validate( project.getUrl(), "project.url" );

        if ( project.getLicenses().isEmpty() )
        {
            throw new MojoExecutionException( "At least one license must be defined." );
        }
        
        if ( disableMaterialization )
        {
            getLog().warn( "Validations to confirm support for project materialization have been DISABLED." +
                "\n\nYour project may not provide the POM elements necessary to allow users to retrieve sources on-demand," +
                "\nor to easily checkout your project in an IDE. THIS CAN SERIOUSLY INCONVENIENCE YOUR USERS." +
                "\n\nContinue? [y/N]" );
            
            try
            {
                if ( 'y' != inputHandler.readLine().toLowerCase().charAt( 0 ) )
                {
                    disableMaterialization = false;
                }
            }
            catch ( IOException e )
            {
                getLog().debug( "Error reading confirmation: " + e.getMessage(), e );
            }
            
        }
        
        if ( !disableMaterialization )
        {
            if ( project.getScm() == null )
            {
                throw new MojoExecutionException( "You must supply a valid <scm> section, with at least "
                    + "<url> (viewing URL) and <connection> (read-only tooling connection) specified." );
            }
            else
            {
                validate( project.getScm().getUrl(), "project.scm.url" );
                
                validate( project.getScm().getConnection(), "project.scm.connection" );
            }
        }

        // ----------------------------------------------------------------------
        // Create the bundle archive
        // ----------------------------------------------------------------------

        File pom = new File( basedir, POM );

        final String finalName = project.getBuild().getFinalName();

        String outputDirectory = project.getBuild().getDirectory();
        
        boolean batchMode = settings == null ? false : !settings.isInteractiveMode();
        List<File> files = BundleUtils.selectProjectFiles( new File( outputDirectory ), inputHandler, finalName, pom, getLog(), batchMode );

        String extension = artifactHandlerManager.getArtifactHandler( project.getPackaging() ).getExtension();

        File bundle = new File( outputDirectory, finalName + "-bundle.jar" );

        try
        {
            jarArchiver.addFile( pom, POM );

            boolean artifactChecks = !"pom".equals( project.getPackaging() );
            boolean sourcesFound = false;
            boolean javadocsFound = false;
            
            for ( File f : files )
            {
                if ( artifactChecks && f.getName().endsWith( finalName + "-sources." + extension ) )
                {
                    sourcesFound = true;
                }
                else if ( artifactChecks && f.getName().equals( finalName + "-javadoc." + extension ) )
                {
                    javadocsFound = true;
                }
                
                jarArchiver.addFile( f, f.getName() );
            }
            
            if ( artifactChecks && !sourcesFound )
            {
                getLog().warn( "Sources not included in upload bundle. In order to add sources please run"
                    + " \"mvn source:jar javadoc:jar repository:bundle-create\"" );
            }

            if ( artifactChecks && !javadocsFound )
            {
                getLog().warn( "Javadoc not included in upload bundle. In order to add javadocs please run"
                    + " \"mvn source:jar javadoc:jar repository:bundle-create\"" );
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
