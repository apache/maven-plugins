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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.Map;

/**
 * Installs a file in local repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal install-file
 * @requiresProject false
 * @aggregator
 */
public class InstallFileMojo
    extends AbstractInstallMojo
{
    /**
     * GroupId of the artifact to be installed. Retrieved from POM file if specified.
     *
     * @parameter expression="${groupId}"
     */
    protected String groupId;

    /**
     * ArtifactId of the artifact to be installed. Retrieved from POM file if specified.
     *
     * @parameter expression="${artifactId}"
     */
    protected String artifactId;

    /**
     * Version of the artifact to be installed. Retrieved from POM file if specified
     *
     * @parameter expression="${version}"
     */
    protected String version;

    /**
     * Packaging type of the artifact to be installed. Retrieved from POM file if specified
     *
     * @parameter expression="${packaging}"
     */
    protected String packaging;

    /**
     * Classifier type of the artifact to be installed.  For example, "sources" or "javadoc".
     * Defaults to none which means this is the project's main jar.
     *
     * @parameter expression="${classifier}"
     */
    protected String classifier;

    /**
     * The file to be deployed
     *
     * @parameter expression="${file}"
     * @required
     */
    private File file;

    /**
     * Location of an existing POM file to be deployed alongside the main
     * artifact, given by the ${file} parameter.
     *
     * @parameter expression="${pomFile}"
     */
    private File pomFile;

    /**
     * Install a POM for this artifact.  Will generate a default POM if none is
     * supplied with the pomFile argument.
     *
     * @parameter expression="${generatePom}" default-value="false"
     */
    private boolean generatePom;

    /**
     * The type of remote repository layout to deploy to. Try <i>legacy</i> for 
     * a Maven 1.x-style repository layout.
     * 
     * @parameter expression="${repositoryLayout}" default-value="default"
     * @required
     */
    private String repositoryLayout;

    /**
     * Map that contains the layouts
     *
     * @component role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * The path for a specific local repository directory. It will wrap into an <code>ArtifactRepository</code>
     * with <code>localRepoId</code> as <code>id</code> and with default <code>repositoryLayout</code>
     *
     * @parameter expression="${localRepositoryPath}"
     */
    private File localRepositoryPath;

    /**
     * The <code>id</code> for the <code>localRepo</code>
     *
     * @parameter expression="${localRepositoryId}"
     */
    private String localRepositoryId;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // ----------------------------------------------------------------------
        // Override the default localRepository variable
        // ----------------------------------------------------------------------
        if ( StringUtils.isNotEmpty( localRepositoryId ) && ( localRepositoryPath != null ) )
        {
            try
            {
                ArtifactRepositoryLayout layout;

                layout = ( ArtifactRepositoryLayout ) repositoryLayouts.get( repositoryLayout );

                getLog().info("Layout: " + layout.getClass());
                localRepository = new DefaultArtifactRepository( localRepositoryId, localRepositoryPath.toURL()
                    .toString(), layout );
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( "MalformedURLException: " + e.getMessage(), e );
            }
        }

        ArtifactMetadata metadata = null;

        Artifact pomArtifact = null;

        if ( pomFile != null )
        {
            if ( pomFile.isFile() )
            {
                processModel( readPom( pomFile ) );

                pomArtifact = artifactFactory.createArtifact( groupId, artifactId, version, null, "pom" );
            }
            else
            {
                getLog().warn( "Ignored non-existent POM file " + pomFile );
            }
        }

        if ( StringUtils.isEmpty( groupId ) )
        {
            throw new MojoExecutionException( "Missing group identifier, please specify -DgroupId=..." );
        }
        if ( StringUtils.isEmpty( artifactId ) )
        {
            throw new MojoExecutionException( "Missing artifact identifier, please specify -DartifactId=..." );
        }
        if ( StringUtils.isEmpty( version ) )
        {
            throw new MojoExecutionException( "Missing version, please specify -Dversion=..." );
        }
        if ( StringUtils.isEmpty( packaging ) )
        {
            throw new MojoExecutionException( "Missing packaging type, please specify -Dpackaging=..." );
        }

        Artifact artifact =
            artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, packaging, classifier );

        File generatedPomFile = null;

        // TODO: check if it exists first, and default to true if not
        if ( generatePom )
        {
            Writer fw = null;
            try
            {
                generatedPomFile = File.createTempFile( "mvninstall", ".pom" );
                generatedPomFile.deleteOnExit();

                Model model = new Model();
                model.setModelVersion( "4.0.0" );
                model.setGroupId( groupId );
                model.setArtifactId( artifactId );
                model.setVersion( version );
                model.setPackaging( packaging );
                model.setDescription( "POM was created from install:install-file" );

                fw = WriterFactory.newXmlWriter( generatedPomFile );
                new MavenXpp3Writer().write( fw, model );
                metadata = new ProjectArtifactMetadata( artifact, generatedPomFile );
                artifact.addMetadata( metadata );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error writing temporary pom file: " + e.getMessage(), e );
            }
            finally
            {
                IOUtil.close( fw );
            }
        }

        // TODO: validate
        // TODO: maybe not strictly correct, while we should enfore that packaging has a type handler of the same id, we don't
        try
        {
            String localPath = localRepository.pathOf( artifact );

            File destination = new File( localRepository.getBasedir(), localPath );

            if ( !file.equals( destination ) )
            {
                installer.install( file, artifact, localRepository );
                installChecksums( file, getLocalRepoFile( artifact ) );
                if ( generatePom )
                {
                    installChecksums( generatedPomFile, getLocalRepoFile( metadata ) );
                }

                if ( pomFile != null && pomFile.isFile() )
                {
                    installer.install( pomFile, pomArtifact, localRepository );
                    installChecksums( pomFile, getLocalRepoFile( pomArtifact ) );
                }
            }
            else
            {
                throw new MojoFailureException(
                    "Cannot install artifact. Artifact is already in the local repository.\n\nFile in question is: " +
                        file + "\n" );
            }
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException(
                "Error installing artifact '" + artifact.getDependencyConflictId() + "': " + e.getMessage(), e );
        }
    }

    /**
     * @param aFile
     * @return the model from a file
     * @throws MojoExecutionException if any
     */
    private Model readPom( File aFile )
        throws MojoExecutionException
    {
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( aFile );

            MavenXpp3Reader mavenReader = new MavenXpp3Reader();

            return mavenReader.read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "File not found " + aFile, e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error reading pom", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error reading pom", e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private void processModel( Model model )
    {
        Parent parent = model.getParent();

        if ( this.groupId == null )
        {
            if ( parent != null && parent.getGroupId() != null )
            {
                this.groupId = parent.getGroupId();
            }
            if ( model.getGroupId() != null )
            {
                this.groupId = model.getGroupId();
            }
        }
        if ( this.artifactId == null && model.getArtifactId() != null )
        {
            this.artifactId = model.getArtifactId();
        }
        if ( this.version == null )
        {
            if ( parent != null && parent.getVersion() != null )
            {
                this.version = parent.getVersion();
            }
            if ( model.getVersion() != null )
            {
                this.version = model.getVersion();
            }
        }
        if ( this.packaging == null && model.getPackaging() != null )
        {
            this.packaging = model.getPackaging();
        }
    }

    /**
     * @return the localRepositoryId
     */
    public String getLocalRepositoryId()
    {
        return this.localRepositoryId;
    }

    /**
     * @param theLocalRepositoryId the localRepositoryId to set
     */
    public void setLocalRepositoryId( String theLocalRepositoryId )
    {
        this.localRepositoryId = theLocalRepositoryId;
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
