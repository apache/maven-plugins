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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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

/**
 * Installs the artifact in the remote repository.
 * 
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
@Mojo( name = "deploy-file", requiresProject = false, threadSafe = true )
public class DeployFileMojo
    extends AbstractDeployMojo
{
    /**
     * The default Maven project created when building the plugin
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * Used for attaching the source and javadoc jars to the project.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * GroupId of the artifact to be deployed. Retrieved from POM file if specified.
     */
    @Parameter( property = "groupId" )
    private String groupId;

    /**
     * ArtifactId of the artifact to be deployed. Retrieved from POM file if specified.
     */
    @Parameter( property = "artifactId" )
    private String artifactId;

    /**
     * Version of the artifact to be deployed. Retrieved from POM file if specified.
     */
    @Parameter( property = "version" )
    private String version;

    /**
     * Type of the artifact to be deployed. Retrieved from the &lt;packaging&gt element of the POM file if a POM file
     * specified. Defaults to the file extension if it is not specified via command line or POM.<br/>
     * Maven uses two terms to refer to this datum: the &lt;packaging&gt; element for the entire POM, and the
     * &lt;type&gt; element in a dependency specification.
     */
    @Parameter( property = "packaging" )
    private String packaging;

    /**
     * Description passed to a generated POM file (in case of generatePom=true)
     */
    @Parameter( property = "generatePom.description" )
    private String description;

    /**
     * File to be deployed.
     */
    @Parameter( property = "file", required = true )
    private File file;

    /**
     * The bundled API docs for the artifact.
     * 
     * @since 2.6
     */
    @Parameter( property = "javadoc" )
    private File javadoc;

    /**
     * The bundled sources for the artifact.
     * 
     * @since 2.6
     */
    @Parameter( property = "sources" )
    private File sources;

    /**
     * Server Id to map on the &lt;id&gt; under &lt;server&gt; section of settings.xml In most cases, this parameter
     * will be required for authentication.
     */
    @Parameter( property = "repositoryId", defaultValue = "remote-repository", required = true )
    private String repositoryId;

    /**
     * The type of remote repository layout to deploy to. Try <i>legacy</i> for a Maven 1.x-style repository layout.
     */
    @Parameter( property = "repositoryLayout", defaultValue = "default" )
    private String repositoryLayout;

    /**
     * URL where the artifact will be deployed. <br/>
     * ie ( file:///C:/m2-repo or scp://host.com/path/to/repo )
     */
    @Parameter( property = "url", required = true )
    private String url;

    /**
     * Location of an existing POM file to be deployed alongside the main artifact, given by the ${file} parameter.
     */
    @Parameter( property = "pomFile" )
    private File pomFile;

    /**
     * Upload a POM for this artifact. Will generate a default POM if none is supplied with the pomFile argument.
     */
    @Parameter( property = "generatePom", defaultValue = "true" )
    private boolean generatePom;

    /**
     * Add classifier to the artifact
     */
    @Parameter( property = "classifier" )
    private String classifier;

    /**
     * Whether to deploy snapshots with a unique version or not.
     */
    @Parameter( property = "uniqueVersion", defaultValue = "true" )
    private boolean uniqueVersion;

    /**
     * The component used to validate the user-supplied artifact coordinates.
     */
    @Component
    private ModelValidator modelValidator;

    /**
     * A comma separated list of types for each of the extra side artifacts to deploy. If there is a mis-match in the
     * number of entries in {@link #files} or {@link #classifiers}, then an error will be raised.
     */
    @Parameter( property = "types" )
    private String types;

    /**
     * A comma separated list of classifiers for each of the extra side artifacts to deploy. If there is a mis-match in
     * the number of entries in {@link #files} or {@link #types}, then an error will be raised.
     */
    @Parameter( property = "classifiers" )
    private String classifiers;

    /**
     * A comma separated list of files for each of the extra side artifacts to deploy. If there is a mis-match in the
     * number of entries in {@link #types} or {@link #classifiers}, then an error will be raised.
     */
    @Parameter( property = "files" )
    private String files;

    void initProperties()
        throws MojoExecutionException
    {
        if ( pomFile != null )
        {
            processModel( readModel( pomFile ) );
        }
        else
        {
            boolean foundPom = false;

            JarFile jarFile = null;
            try
            {
                Pattern pomEntry = Pattern.compile( "META-INF/maven/.*/pom\\.xml" );

                jarFile = new JarFile( file );

                Enumeration<JarEntry> jarEntries = jarFile.entries();

                while ( jarEntries.hasMoreElements() )
                {
                    JarEntry entry = jarEntries.nextElement();

                    if ( pomEntry.matcher( entry.getName() ).matches() )
                    {
                        getLog().debug( "Using " + entry.getName() + " as pomFile" );

                        foundPom = true;

                        InputStream pomInputStream = null;
                        OutputStream pomOutputStream = null;

                        try
                        {
                            pomInputStream = jarFile.getInputStream( entry );
                            
                            String base = file.getName();
                            if ( base.indexOf( '.' ) > 0 )
                            {
                                base = base.substring( 0, base.lastIndexOf( '.' ) );
                            }
                            pomFile = new File( file.getParentFile(), base + ".pom" );
                            
                            pomOutputStream = new FileOutputStream( pomFile );
                            
                            IOUtil.copy( pomInputStream, pomOutputStream );

                            processModel( readModel( pomFile ) );

                            break;
                        }
                        finally
                        {
                            IOUtil.close( pomInputStream );
                            IOUtil.close( pomOutputStream );
                        }
                    }
                }

                if ( !foundPom )
                {
                    getLog().info( "pom.xml not found in " + file.getName() );
                }
            }
            catch ( IOException e )
            {
                // ignore, artifact not packaged by Maven
            }
            finally
            {
                if ( jarFile != null )
                {
                    try
                    {
                        jarFile.close();
                    }
                    catch ( IOException e )
                    {
                        // we did our best
                    }
                }
            }
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
            deploy( file, artifact, deploymentRepository, getLocalRepository(), getRetryFailedDeploymentCount() );
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
                throw new MojoExecutionException( "You must specify the same number of entries in 'files' and "
                    + "'types' (respectively " + filesLength + " and " + typesLength + " entries )" );
            }
            if ( classifiersLength != filesLength )
            {
                throw new MojoExecutionException( "You must specify the same number of entries in 'files' and "
                    + "'classifiers' (respectively " + filesLength + " and " + classifiersLength + " entries )" );
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
                        projectHelper.attachArtifact( project, types.substring( ti, nti ).trim(),
                                                      classifiers.substring( ci, nci ).trim(), file );
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

        @SuppressWarnings( "unchecked" )
        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();

        for ( Artifact attached : attachedArtifacts )
        {
            try
            {
                deploy( attached.getFile(), attached, deploymentRepository, getLocalRepository(),
                        getRetryFailedDeploymentCount() );
            }
            catch ( ArtifactDeploymentException e )
            {
                throw new MojoExecutionException( "Error deploying attached artifact " + attached.getFile() + ": "
                    + e.getMessage(), e );
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
