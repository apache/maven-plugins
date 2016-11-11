package org.apache.maven.plugins.help;

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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.ArtifactCoordinate;
import org.apache.maven.shared.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Base class with some Help Mojo functionalities.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.1
 */
public abstract class AbstractHelpMojo
    extends AbstractMojo
{
    /** The maximum length of a display line. */
    protected static final int LINE_LENGTH = 79;
    
    /** The line separator for the current OS. */
    protected static final String LS = System.getProperty( "line.separator" );
    
    /**
     * Maven Project Builder component.
     */
    @Component
    protected ProjectBuilder projectBuilder;
    
    /**
     * Component used to resolve artifacts and download their files from remote repositories.
     */
    @Component
    protected ArtifactResolver artifactResolver;
    
    /**
     * Remote repositories used for the project.
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true )
    protected List<ArtifactRepository> remoteRepositories;
    
    /**
     * Local Repository.
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    protected ArtifactRepository localRepository;
    
    /**
     * The current build session instance. This is used for
     * plugin manager API calls.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    /**
     * Optional parameter to write the output of this help in a given file, instead of writing to the console.
     * <br>
     * <b>Note</b>: Could be a relative path.
     */
    @Parameter( property = "output" )
    protected File output;

    /**
     * Utility method to write a content in a given file.
     *
     * @param output is the wanted output file.
     * @param content contains the content to be written to the file.
     * @throws IOException if any
     * @see #writeFile(File, String)
     */
    protected static void writeFile( File output, StringBuilder content )
        throws IOException
    {
        writeFile( output, content.toString() );
    }

    /**
     * Utility method to write a content in a given file.
     *
     * @param output is the wanted output file.
     * @param content contains the content to be written to the file.
     * @throws IOException if any
     */
    protected static void writeFile( File output, String content )
        throws IOException
    {
        if ( output == null )
        {
            return;
        }

        Writer out = null;
        try
        {
            output.getParentFile().mkdirs();

            out = WriterFactory.newPlatformWriter( output );

            out.write( content );

            out.close();
            out = null;
        }
        finally
        {
            IOUtil.close( out );
        }
    }
    
    /**
     * Parses the given String into GAV artifact coordinate information, adding the given type.
     * 
     * @param artifactString should respect the format <code>groupId:artifactId[:version]</code>
     * @param type The extension for the artifact, must not be <code>null</code>.
     * @return the <code>Artifact</code> object for the <code>artifactString</code> parameter.
     * @throws MojoExecutionException if the <code>artifactString</code> doesn't respect the format.
     */
    protected ArtifactCoordinate getArtifactCoordinate( String artifactString, String type )
        throws MojoExecutionException
    {
        if ( StringUtils.isEmpty( artifactString ) )
        {
            throw new IllegalArgumentException( "artifact parameter could not be empty" );
        }

        String groupId; // required
        String artifactId; // required
        String version; // optional

        String[] artifactParts = artifactString.split( ":" );
        switch ( artifactParts.length )
        {
            case 2:
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = Artifact.LATEST_VERSION;
                break;
            case 3:
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = artifactParts[2];
                break;
            default:
                throw new MojoExecutionException( "The artifact parameter '" + artifactString
                    + "' should be conform to: " + "'groupId:artifactId[:version]'." );
        }
        return getArtifactCoordinate( groupId, artifactId, version, type );
    }

    protected ArtifactCoordinate getArtifactCoordinate( String groupId, String artifactId, String version, String type )
    {
        DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
        coordinate.setGroupId( groupId );
        coordinate.setArtifactId( artifactId );
        coordinate.setVersion( version );
        coordinate.setExtension( type );
        return coordinate;
    }

    /**
     * Retrieves the Maven Project associated with the given artifact String, in the form of
     * <code>groupId:artifactId[:version]</code>. This resolves the POM artifact at those coordinates and then builds
     * the Maven project from it.
     * 
     * @param artifactString Coordinates of the Maven project to get.
     * @return New Maven project.
     * @throws MojoExecutionException If there was an error while getting the Maven project.
     */
    protected MavenProject getMavenProject( String artifactString )
        throws MojoExecutionException
    {
        ArtifactCoordinate coordinate = getArtifactCoordinate( artifactString, "pom" );
        try
        {
            ProjectBuildingRequest pbr = new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );
            pbr.setRemoteRepositories( remoteRepositories );
            pbr.setProject( null );
            pbr.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
            pbr.setResolveDependencies( true );
            Artifact artifact = artifactResolver.resolveArtifact( pbr, coordinate ).getArtifact();
            return projectBuilder.build( artifact.getFile(), pbr ).getProject();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to get the POM for the artifact '" + artifactString
                + "'. Verify the artifact parameter.", e );
        }
    }

}
