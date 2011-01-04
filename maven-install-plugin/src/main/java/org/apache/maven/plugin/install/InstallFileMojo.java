package org.apache.maven.plugin.install;

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
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Installs a file in the local repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal install-file
 * @requiresProject false
 * @aggregator
 * @threadSafe
 */
public class InstallFileMojo
    extends AbstractInstallMojo
{

    /**
     * GroupId of the artifact to be installed. Retrieved from POM file if one is specified.
     *
     * @parameter expression="${groupId}"
     */
    protected String groupId;

    /**
     * ArtifactId of the artifact to be installed. Retrieved from POM file if one is specified.
     *
     * @parameter expression="${artifactId}"
     */
    protected String artifactId;

    /**
     * Version of the artifact to be installed. Retrieved from POM file if one is specified.
     *
     * @parameter expression="${version}"
     */
    protected String version;

    /**
     * Packaging type of the artifact to be installed. Retrieved from POM file if one is specified.
     *
     * @parameter expression="${packaging}"
     */
    protected String packaging;

    /**
     * Classifier type of the artifact to be installed. For example, "sources" or "javadoc". Defaults to none which
     * means this is the project's main artifact.
     *
     * @parameter expression="${classifier}"
     * @since 2.2
     */
    protected String classifier;

    /**
     * The file to be installed in the local repository.
     *
     * @parameter expression="${file}"
     * @required
     */
    private File file;

    /**
     * The bundled API docs for the artifact.
     *
     * @parameter expression="${javadoc}"
     * @since 2.3
     */
    private File javadoc;

    /**
     * The bundled sources for the artifact.
     *
     * @parameter expression="${sources}"
     * @since 2.3
     */
    private File sources;

    /**
     * Location of an existing POM file to be installed alongside the main artifact, given by the {@link #file}
     * parameter.
     *
     * @parameter expression="${pomFile}"
     * @since 2.1
     */
    private File pomFile;

    /**
     * Generate a minimal POM for the artifact if none is supplied via the parameter {@link #pomFile}. Defaults to
     * <code>true</code> if there is no existing POM in the local repository yet.
     *
     * @parameter expression="${generatePom}"
     * @since 2.1
     */
    private Boolean generatePom;

    /**
     * The type of remote repository layout to install to. Try <code>legacy</code> for a Maven 1.x-style repository
     * layout.
     *
     * @parameter expression="${repositoryLayout}" default-value="default"
     * @required
     * @since 2.2
     */
    private String repositoryLayout;

    /**
     * Map that contains the repository layouts.
     *
     * @component role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * The path for a specific local repository directory. If not specified the local repository path configured in the
     * Maven settings will be used.
     *
     * @parameter expression="${localRepositoryPath}"
     * @since 2.2
     */
    private File localRepositoryPath;

    /**
     * The component used to validate the user-supplied artifact coordinates.
     *
     * @component
     */
    private ModelValidator modelValidator;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // ----------------------------------------------------------------------
        // Override the default localRepository variable
        // ----------------------------------------------------------------------
        if ( localRepositoryPath != null )
        {
            try
            {
                ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) repositoryLayouts.get( repositoryLayout );
                getLog().debug( "Layout: " + layout.getClass() );

                localRepository =
                    new DefaultArtifactRepository( localRepository.getId(), localRepositoryPath.toURL().toString(),
                                                   layout );
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( "MalformedURLException: " + e.getMessage(), e );
            }
        }

        if ( pomFile != null )
        {
            processModel( readModel( pomFile ) );
        }

        validateArtifactInformation();

        Artifact artifact =
            artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, packaging, classifier );

        if ( file.equals( getLocalRepoFile( artifact ) ) )
        {
            throw new MojoFailureException( "Cannot install artifact. "
                + "Artifact is already in the local repository.\n\nFile in question is: " + file + "\n" );
        }

        File generatedPomFile = null;

        if ( !"pom".equals( packaging ) )
        {
            if ( pomFile != null )
            {
                ArtifactMetadata pomMetadata = new ProjectArtifactMetadata( artifact, pomFile );
                artifact.addMetadata( pomMetadata );
            }
            else
            {
                generatedPomFile = generatePomFile();
                ArtifactMetadata pomMetadata = new ProjectArtifactMetadata( artifact, generatedPomFile );
                if ( Boolean.TRUE.equals( generatePom )
                    || ( generatePom == null && !getLocalRepoFile( pomMetadata ).exists() ) )
                {
                    getLog().debug( "Installing generated POM" );
                    artifact.addMetadata( pomMetadata );
                }
                else if ( generatePom == null )
                {
                    getLog().debug( "Skipping installation of generated POM, already present in local repository" );
                }
            }
        }

        if ( updateReleaseInfo )
        {
            artifact.setRelease( true );
        }

        Collection metadataFiles = new LinkedHashSet();

        // TODO: maybe not strictly correct, while we should enforce that packaging has a type handler of the same id,
        // we don't
        try
        {
            installer.install( file, artifact, localRepository );
            installChecksums( artifact, metadataFiles );
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException( "Error installing artifact '" + artifact.getDependencyConflictId()
                + "': " + e.getMessage(), e );
        }
        finally
        {
            if ( generatedPomFile != null )
            {
                generatedPomFile.delete();
            }
        }

        if ( sources != null )
        {
            artifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, "jar", "sources" );
            try
            {
                installer.install( sources, artifact, localRepository );
                installChecksums( artifact, metadataFiles );
            }
            catch ( ArtifactInstallationException e )
            {
                throw new MojoExecutionException( "Error installing sources " + sources + ": " + e.getMessage(), e );
            }
        }

        if ( javadoc != null )
        {
            artifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, "jar", "javadoc" );
            try
            {
                installer.install( javadoc, artifact, localRepository );
                installChecksums( artifact, metadataFiles );
            }
            catch ( ArtifactInstallationException e )
            {
                throw new MojoExecutionException( "Error installing API docs " + javadoc + ": " + e.getMessage(), e );
            }
        }

        installChecksums( metadataFiles );
    }

    /**
     * Parses a POM.
     *
     * @param pomFile The path of the POM file to parse, must not be <code>null</code>.
     * @return The model from the POM file, never <code>null</code>.
     * @throws MojoExecutionException If the POM could not be parsed.
     */
    private Model readModel( File pomFile )
        throws MojoExecutionException
    {
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( pomFile );
            return new MavenXpp3Reader().read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "File not found " + pomFile, e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error reading POM " + pomFile, e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing POM " + pomFile, e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    /**
     * Populates missing mojo parameters from the specified POM.
     *
     * @param model The POM to extract missing artifact coordinates from, must not be <code>null</code>.
     */
    private void processModel( Model model )
    {
        Parent parent = model.getParent();

        if ( this.groupId == null )
        {
            this.groupId = model.getGroupId();
            if ( this.groupId == null && parent != null )
            {
                this.groupId = parent.getGroupId();
            }
        }
        if ( this.artifactId == null )
        {
            this.artifactId = model.getArtifactId();
        }
        if ( this.version == null )
        {
            this.version = model.getVersion();
            if ( this.version == null && parent != null )
            {
                this.version = parent.getVersion();
            }
        }
        if ( this.packaging == null )
        {
            this.packaging = model.getPackaging();
        }
    }

    /**
     * Validates the user-supplied artifact information.
     *
     * @throws MojoExecutionException If any artifact coordinate is invalid.
     */
    private void validateArtifactInformation()
        throws MojoExecutionException
    {
        Model model = generateModel();

        ModelValidationResult result = modelValidator.validate( model );

        if ( result.getMessageCount() > 0 )
        {
            throw new MojoExecutionException( "The artifact information is incomplete or not valid:\n"
                + result.render( "  " ) );
        }
    }

    /**
     * Generates a minimal model from the user-supplied artifact information.
     *
     * @return The generated model, never <code>null</code>.
     */
    private Model generateModel()
    {
        Model model = new Model();

        model.setModelVersion( "4.0.0" );

        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setPackaging( packaging );

        model.setDescription( "POM was created from install:install-file" );

        return model;
    }

    /**
     * Generates a (temporary) POM file from the plugin configuration. It's the responsibility of the caller to delete
     * the generated file when no longer needed.
     *
     * @return The path to the generated POM file, never <code>null</code>.
     * @throws MojoExecutionException If the POM file could not be generated.
     */
    private File generatePomFile()
        throws MojoExecutionException
    {
        Model model = generateModel();

        Writer writer = null;
        try
        {
            File pomFile = File.createTempFile( "mvninstall", ".pom" );

            writer = WriterFactory.newXmlWriter( pomFile );
            new MavenXpp3Writer().write( writer, model );

            return pomFile;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing temporary POM file: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    /**
     * @return the localRepositoryPath
     */
    public File getLocalRepositoryPath()
    {
        return this.localRepositoryPath;
    }

    /**
     * @param theLocalRepositoryPath the localRepositoryPath to set
     */
    public void setLocalRepositoryPath( File theLocalRepositoryPath )
    {
        this.localRepositoryPath = theLocalRepositoryPath;
    }

}
