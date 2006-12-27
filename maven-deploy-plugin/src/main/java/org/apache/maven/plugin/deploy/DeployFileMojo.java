package org.apache.maven.plugin.deploy;

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
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Installs the artifact in the remote repository.
 *
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 * @goal deploy-file
 * @requiresProject false
 */
public class DeployFileMojo
    extends AbstractDeployMojo
{
    /**
     * GroupId of the artifact to be deployed.  Retrieved from POM file if specified.
     *
     * @parameter expression="${groupId}"
     */
    private String groupId;

    /**
     * ArtifactId of the artifact to be deployed.  Retrieved from POM file if specified.
     *
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * Version of the artifact to be deployed.  Retrieved from POM file if specified.
     *
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * Type of the artifact to be deployed.  Retrieved from POM file if specified.
     *
     * @parameter expression="${packaging}"
     */
    private String packaging;

    /**
     * Description passed to a generated POM file (in case of generatePom=true)
     *
     * @parameter expression="${generatePom.description}"
     */
    private String description;

    /**
     * File to be deployed.
     *
     * @parameter expression="${file}"
     * @required
     */
    private File file;

    /**
     * Server Id to map on the &lt;id&gt; under &lt;server&gt; section of settings.xml
     * In most cases, this parameter will be required for authentication.
     *
     * @parameter expression="${repositoryId}" default-value="remote-repository"
     * @required
     */
    private String repositoryId;

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
     * URL where the artifact will be deployed. <br/>
     * ie ( file://C:\m2-repo or scp://host.com/path/to/repo )
     *
     * @parameter expression="${url}"
     * @required
     */
    private String url;

    /**
     * Component used to create an artifact
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * Component used to create a repository
     *
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * Location of an existing POM file to be deployed alongside the main
     * artifact, given by the ${file} parameter.
     * 
     * @parameter expression="${pomFile}"
     */
    private File pomFile;

    /**
     * Upload a POM for this artifact.  Will generate a default POM if none is
     * supplied with the pomFile argument.
     *
     * @parameter expression="${generatePom}" default-value="true"
     */
    private boolean generatePom;

    /**
     * Add classifier to the artifact
     *
     * @parameter expression="${classifier}";
     */
    private String classifier;

    /**
     * Whether to deploy snapshots with a unique version or not.
     *
     * @parameter expression="${uniqueVersion}" default-value="true"
     */
    private boolean uniqueVersion;

    protected void initProperties()
        throws MojoExecutionException
    {
        // Process the supplied POM (if there is one)
        if ( pomFile != null )
        {
            generatePom = false;

            Model model = readModel( pomFile );

            processModel( model );
        }

        // Verify arguments
        if ( groupId == null || artifactId == null || version == null || packaging == null )
        {
            throw new MojoExecutionException( "Missing group, artifact, version, or packaging information" );
        }
    }

    public void execute()
        throws MojoExecutionException
    {
        initProperties();

        if ( !file.exists() )
        {
            throw new MojoExecutionException( file.getPath() + " not found." );
        }

        ArtifactRepositoryLayout layout;

        layout = ( ArtifactRepositoryLayout ) repositoryLayouts.get( repositoryLayout );

        ArtifactRepository deploymentRepository =
            repositoryFactory.createDeploymentArtifactRepository( repositoryId, url, layout, uniqueVersion );

        String protocol = deploymentRepository.getProtocol();

        if ( "".equals( protocol ) || protocol == null )
        {
            throw new MojoExecutionException( "No transfer protocol found." );
        }

        // Create the artifact
        Artifact artifact =
            artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, packaging, classifier );
        
        // Upload the POM if requested, generating one if need be
        if ( generatePom )
        {
            ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, generatePomFile() );
            artifact.addMetadata( metadata );
        }
        else
        {
            ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pomFile );
            artifact.addMetadata( metadata );
        }

        try
        {
            getDeployer().deploy( file, artifact, deploymentRepository, getLocalRepository() );
        }
        catch ( ArtifactDeploymentException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    /**
     * Process the supplied pomFile to get groupId, artifactId, version, and packaging
     *
     * @throws NullPointerException if model is <code>null</code>
     */
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
        if ( this.version == null && model.getVersion() != null )
        {
            this.version = model.getVersion();
        }
        if ( this.packaging == null && model.getPackaging() != null )
        {
            this.packaging = model.getPackaging();
        }
    }

    /**
     * Extract the Model from the specified file.
     *
     * @param pomFile
     * @return
     * @throws MojoExecutionException if the file doesn't exist of cannot be read.
     */
    protected Model readModel( File pomFile )
        throws MojoExecutionException
    {

        if ( !pomFile.exists() )
        {
            throw new MojoExecutionException( "Specified pomFile does not exist" );
        }

        Reader reader = null;
        try
        {
            reader = new FileReader( pomFile );
            MavenXpp3Reader modelReader = new MavenXpp3Reader();
            return modelReader.read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Error reading specified POM file: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error reading specified POM file: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error reading specified POM file: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private File generatePomFile()
        throws MojoExecutionException
    {
        FileWriter fw = null;
        try
        {
            File tempFile = File.createTempFile( "mvninstall", ".pom" );
            tempFile.deleteOnExit();

            Model model = new Model();
            model.setModelVersion( "4.0.0" );
            model.setGroupId( groupId );
            model.setArtifactId( artifactId );
            model.setVersion( version );
            model.setPackaging( packaging );
            model.setDescription( description );

            fw = new FileWriter( tempFile );
            new MavenXpp3Writer().write( fw, model );

            return tempFile;
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

    void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    void setVersion( String version )
    {
        this.version = version;
    }

    void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    void setPomFile( File pomFile )
    {
        this.pomFile = pomFile;
    }

    String getGroupId()
    {
        return groupId;
    }

    String getArtifactId()
    {
        return artifactId;
    }

    String getVersion()
    {
        return version;
    }

    String getPackaging()
    {
        return packaging;
    }

    File getFile()
    {
        return file;
    }

    String getClassifier()
    {
        return classifier;
    }

    void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }
}
