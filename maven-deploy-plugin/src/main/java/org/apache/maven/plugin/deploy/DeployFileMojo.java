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
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.codehaus.plexus.util.FileUtils;
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
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

/**
 * Installs the artifact in the remote repository.
 *
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 * @goal deploy-file
 * @requiresProject false
 * @threadSafe
 */
public class DeployFileMojo
    extends AbstractDeployMojo
{
    /**
     * The default Maven project created when building the plugin
     * 
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Used for attaching the source and javadoc jars to the project.
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

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
     * Type of the artifact to be deployed. 
     * Retrieved from the &lt;packaging&gt element of the POM file if a POM file specified.
     * Defaults to the file extension if it is not specified via command line or POM.<br/>
     * Maven uses two terms to refer to this datum: the &lt;packaging&gt; element 
     * for the entire POM, and the &lt;type&gt; element in a dependency specification.
     * 
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
     * The bundled API docs for the artifact.
     *
     * @parameter expression="${javadoc}"
     * @since 2.6
     */
    private File javadoc;

    /**
     * The bundled sources for the artifact.
     *
     * @parameter expression="${sources}"
     * @since 2.6
     */
    private File sources;

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
     */
    private String repositoryLayout;

    /**
     * URL where the artifact will be deployed. <br/>
     * ie ( file:///C:/m2-repo or scp://host.com/path/to/repo )
     *
     * @parameter expression="${url}"
     * @required
     */
    private String url;

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

    /**
     * The component used to validate the user-supplied artifact coordinates.
     * 
     * @component
     */
    private ModelValidator modelValidator;

    /**
     * A comma separated list of types for each of the extra side artifacts to deploy. If there is a mis-match in
     * the number of entries in {@link #files} or {@link #classifiers}, then an error will be raised.
     *
     * @parameter expression="${types}";
     */
    private String types;

    /**
     * A comma separated list of classifiers for each of the extra side artifacts to deploy. If there is a mis-match in
     * the number of entries in {@link #files} or {@link #types}, then an error will be raised.
     *
     * @parameter expression="${classifiers}";
     */
    private String classifiers;

    /**
     * A comma separated list of files for each of the extra side artifacts to deploy. If there is a mis-match in
     * the number of entries in {@link #types} or {@link #classifiers}, then an error will be raised.
     *
     * @parameter expression="${files}"
     */
    private String files;

    void initProperties()
        throws MojoExecutionException
    {
        // Process the supplied POM (if there is one)
        if ( pomFile != null )
        {
            generatePom = false;

            Model model = readModel( pomFile );

            processModel( model );
        }
        
        if ( packaging == null && file != null )
        {
            packaging = FileUtils.getExtension( file.getName() );
        }
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        failIfOffline();

        if ( !file.exists() )
        {
            throw new MojoExecutionException( file.getPath() + " not found." );
        }

        initProperties();

        validateArtifactInformation();

        ArtifactRepositoryLayout layout = getLayout( repositoryLayout );

        ArtifactRepository deploymentRepository =
            repositoryFactory.createDeploymentArtifactRepository( repositoryId, url, layout, uniqueVersion );

        String protocol = deploymentRepository.getProtocol();

        if ( StringUtils.isEmpty( protocol ) )
        {
            throw new MojoExecutionException( "No transfer protocol found." );
        }

        // Create the artifact
        Artifact artifact =
            artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, packaging, classifier );

        if ( file.equals( getLocalRepoFile( artifact ) ) )
        {
            throw new MojoFailureException( "Cannot deploy artifact from the local repository: " + file );
        }

        // Upload the POM if requested, generating one if need be
        if ( !"pom".equals( packaging ) )
        {
            if ( pomFile != null )
            {
                ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pomFile );
                artifact.addMetadata( metadata );
            }
            else if ( generatePom )
            {
                ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, generatePomFile() );
                artifact.addMetadata( metadata );
            }
        }

        if ( updateReleaseInfo )
        {
            artifact.setRelease( true );
        }
        
        project.setArtifact( artifact );

        try
        {
            deploy( file, artifact, deploymentRepository, getLocalRepository() );
        }
        catch ( ArtifactDeploymentException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        if ( sources != null )
        {
            projectHelper.attachArtifact( project, "jar", "sources", sources );
        }

        if ( javadoc != null )
        {
            projectHelper.attachArtifact( project, "jar", "javadoc", javadoc );
        }

        if ( files != null )
        {
            if ( types == null )
            {
                throw new MojoExecutionException( "You must specify 'types' if you specify 'files'" );
            }
            if ( classifiers == null )
            {
                throw new MojoExecutionException( "You must specify 'classifiers' if you specify 'files'" );
            }
            int filesLength = StringUtils.countMatches( files, "," );
            int typesLength = StringUtils.countMatches( types, "," );
            int classifiersLength = StringUtils.countMatches( classifiers, "," );
            if ( typesLength != filesLength )
            {
                throw new MojoExecutionException( "You must specify the same number of entries in 'files' and " +
                        "'types' (respectively " + filesLength + " and " + typesLength + " entries )" );
            }
            if ( classifiersLength != filesLength )
            {
                throw new MojoExecutionException( "You must specify the same number of entries in 'files' and " +
                        "'classifiers' (respectively " + filesLength + " and " + classifiersLength + " entries )" );
            }
            int fi = 0;
            int ti = 0;
            int ci = 0;
            for ( int i = 0; i <= filesLength; i++ )
            {
                int nfi = files.indexOf( ',', fi );
                if ( nfi == -1 )
                {
                    nfi = files.length();
                }
                int nti = types.indexOf( ',', ti );
                if ( nti == -1 )
                {
                    nti = types.length();
                }
                int nci = classifiers.indexOf( ',', ci );
                if ( nci == -1 )
                {
                    nci = classifiers.length();
                }
                File file = new File( files.substring( fi, nfi ) );
                if ( !file.isFile() )
                {
                    // try relative to the project basedir just in case
                    file = new File( project.getBasedir(), files.substring( fi, nfi ) );
                }
                if ( file.isFile() )
                {
                    if ( StringUtils.isWhitespace( classifiers.substring( ci, nci ) ) )
                    {
                        projectHelper.attachArtifact( project, types.substring( ti, nti ).trim(), file );
                    }
                    else
                    {
                        projectHelper.attachArtifact( project, types.substring( ti, nti).trim(), 
                                classifiers.substring( ci, nci ).trim(), file);
                    }
                }
                else
                {
                    throw new MojoExecutionException( "Specified side artifact " + file + " does not exist" );
                }
                fi = nfi + 1;
                ti = nti + 1;
                ci = nci + 1;
            }
        }
        else
        {
            if ( types != null )
            {
                throw new MojoExecutionException( "You must specify 'files' if you specify 'types'" );
            }
            if ( classifiers != null )
            {
                throw new MojoExecutionException( "You must specify 'files' if you specify 'classifiers'" );
            }
        }

        List attachedArtifacts = project.getAttachedArtifacts();

        for ( Iterator i = attachedArtifacts.iterator(); i.hasNext(); )
        {
            Artifact attached = ( Artifact ) i.next();

            try
            {
                deploy( attached.getFile(), attached, deploymentRepository, getLocalRepository() );
            }
            catch ( ArtifactDeploymentException e )
            {
                throw new MojoExecutionException( "Error deploying attached artifact " + attached.getFile() + ": " + e.getMessage(), e );
            }
        }

    }

    /**
     * Gets the path of the specified artifact within the local repository. Note that the returned path need not exist
     * (yet).
     * 
     * @param artifact The artifact whose local repo path should be determined, must not be <code>null</code>.
     * @return The absolute path to the artifact when installed, never <code>null</code>.
     */
    private File getLocalRepoFile( Artifact artifact )
    {
        String path = getLocalRepository().pathOf( artifact );
        return new File( getLocalRepository().getBasedir(), path );
    }

    /**
     * Process the supplied pomFile to get groupId, artifactId, version, and packaging
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
     * Extract the model from the specified POM file.
     * 
     * @param pomFile The path of the POM file to parse, must not be <code>null</code>.
     * @return The model from the POM file, never <code>null</code>.
     * @throws MojoExecutionException If the file doesn't exist of cannot be read.
     */
    Model readModel( File pomFile )
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
            throw new MojoExecutionException( "POM not found " + pomFile, e );
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
     * Generates a minimal POM from the user-supplied artifact information.
     *
     * @return The path to the generated POM file, never <code>null</code>.
     * @throws MojoExecutionException If the generation failed.
     */
    private File generatePomFile()
        throws MojoExecutionException
    {
        Model model = generateModel();

        Writer fw = null;
        try
        {
            File tempFile = File.createTempFile( "mvndeploy", ".pom" );
            tempFile.deleteOnExit();

            fw = WriterFactory.newXmlWriter( tempFile );
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

        model.setDescription( description );

        return model;
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
