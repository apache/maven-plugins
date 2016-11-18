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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.StringModelSource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.validation.ModelValidator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.shared.project.install.ProjectInstaller;
import org.apache.maven.shared.project.install.ProjectInstallerRequest;
import org.apache.maven.shared.utils.ReaderFactory;
import org.apache.maven.shared.utils.WriterFactory;
import org.apache.maven.shared.utils.io.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Installs a file in the local repository.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
@Mojo( name = "install-file", requiresProject = false, aggregator = true, threadSafe = true )
public class InstallFileMojo
    extends AbstractInstallMojo
{

    /**
     * GroupId of the artifact to be installed. Retrieved from POM file if one is specified or extracted from
     * {@code pom.xml} in jar if available.
     */
    @Parameter( property = "groupId" )
    private String groupId;

    /**
     * ArtifactId of the artifact to be installed. Retrieved from POM file if one is specified or extracted from
     * {@code pom.xml} in jar if available.
     */
    @Parameter( property = "artifactId" )
    private String artifactId;

    /**
     * Version of the artifact to be installed. Retrieved from POM file if one is specified or extracted from
     * {@code pom.xml} in jar if available.
     */
    @Parameter( property = "version" )
    private String version;

    /**
     * Packaging type of the artifact to be installed. Retrieved from POM file if one is specified or extracted from
     * {@code pom.xml} in jar if available.
     */
    @Parameter( property = "packaging" )
    private String packaging;

    /**
     * Classifier type of the artifact to be installed. For example, "sources" or "javadoc". Defaults to none which
     * means this is the project's main artifact.
     * 
     * @since 2.2
     */
    @Parameter( property = "classifier" )
    private String classifier;

    /**
     * The file to be installed in the local repository.
     */
    @Parameter( property = "file", required = true )
    private File file;

    /**
     * The bundled API docs for the artifact.
     * 
     * @since 2.3
     */
    @Parameter( property = "javadoc" )
    private File javadoc;

    /**
     * The bundled sources for the artifact.
     * 
     * @since 2.3
     */
    @Parameter( property = "sources" )
    private File sources;

    /**
     * Location of an existing POM file to be installed alongside the main artifact, given by the {@link #file}
     * parameter.
     * 
     * @since 2.1
     */
    @Parameter( property = "pomFile" )
    private File pomFile;

    /**
     * Generate a minimal POM for the artifact if none is supplied via the parameter {@link #pomFile}. Defaults to
     * <code>true</code> if there is no existing POM in the local repository yet.
     * 
     * @since 2.1
     */
    @Parameter( property = "generatePom" )
    private Boolean generatePom;

    /**
     * The path for a specific local repository directory. If not specified the local repository path configured in the
     * Maven settings will be used.
     * 
     * @since 2.2
     */
    @Parameter( property = "localRepositoryPath" )
    private File localRepositoryPath;

    /**
    * The component used to validate the user-supplied artifact coordinates.
    */
    @Component
    private ModelValidator modelValidator;
    
    /**
     * Used for attaching the artifacts to install to the project.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Used for creating the project to which the artifacts to install will be attached.
     */
    @Component
    private ProjectBuilder projectBuilder;
    
    /**
     * Used to install the project created.
     */
    @Component
    private ProjectInstaller installer;
    
    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( !file.exists() )
        {
            String message = "The specified file '" + file.getPath() + "' not exists";
            getLog().error( message );
            throw new MojoFailureException( message );
        }

        ProjectBuildingRequest buildingRequest = session.getProjectBuildingRequest();
        
        // ----------------------------------------------------------------------
        // Override the default localRepository variable
        // ----------------------------------------------------------------------
        if ( localRepositoryPath != null )
        {
            buildingRequest = repositoryManager.setLocalRepositoryBasedir( buildingRequest, localRepositoryPath );
            
            getLog().debug( "localRepoPath: " + repositoryManager.getLocalRepositoryBasedir( buildingRequest ) );
        }

        File temporaryPom = null;

        if ( pomFile != null )
        {
            processModel( readModel( pomFile ) );
        }
        else
        {
            temporaryPom = readingPomFromJarFile();
            pomFile = temporaryPom;
        }

        validateArtifactInformation();
        
        MavenProject project = createMavenProject();
        Artifact artifact = project.getArtifact();
        
        if ( file.equals( getLocalRepoFile( buildingRequest, artifact ) ) )
        {
            throw new MojoFailureException( "Cannot install artifact. "
                + "Artifact is already in the local repository.\n\nFile in question is: " + file + "\n" );
        }
        
        if ( classifier == null )
        {
            artifact.setFile( file );
            if ( "pom".equals( packaging ) )
            {
                project.setFile( file );
            }
        }
        else
        {
            projectHelper.attachArtifact( project, packaging, classifier, file );
        }

        if ( !"pom".equals( packaging ) )
        {
            if ( pomFile != null )
            {
                if ( classifier == null )
                {
                    artifact.addMetadata( new ProjectArtifactMetadata( artifact, pomFile ) );
                }
                else
                {
                    project.setFile( pomFile );
                }
            }
            else
            {
                temporaryPom = generatePomFile();
                ProjectArtifactMetadata pomMetadata = new ProjectArtifactMetadata( artifact, temporaryPom );
                if ( Boolean.TRUE.equals( generatePom )
                    || ( generatePom == null && !getLocalRepoFile( buildingRequest, pomMetadata ).exists() ) )
                {
                    getLog().debug( "Installing generated POM" );
                    if ( classifier == null )
                    {
                        artifact.addMetadata( pomMetadata );
                    }
                    else
                    {
                        project.setFile( temporaryPom );
                    }
                }
                else if ( generatePom == null )
                {
                    getLog().debug( "Skipping installation of generated POM, already present in local repository" );
                }
            }
        }

        if ( sources != null )
        {
            projectHelper.attachArtifact( project, "jar", "sources", sources );
        }

        if ( javadoc != null )
        {
            projectHelper.attachArtifact( project, "jar", "javadoc", javadoc );
        }
        
        try
        {
            // CHECKSTYLE_OFF: LineLength
            ProjectInstallerRequest projectInstallerRequest =
                new ProjectInstallerRequest().setProject( project ).setCreateChecksum( createChecksum ).setUpdateReleaseInfo( updateReleaseInfo );
            // CHECKSTYLE_ON: LineLength

            installer.install( buildingRequest, projectInstallerRequest );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            if ( temporaryPom != null )
            {
                // noinspection ResultOfMethodCallIgnored
                temporaryPom.delete();
            }
        }
    }
    
    /**
     * Creates a Maven project in-memory from the user-supplied groupId, artifactId and version. When a classifier is
     * supplied, the packaging must be POM because the project with only have attachments. This project serves as basis
     * to attach the artifacts to install to.
     * 
     * @return The created Maven project, never <code>null</code>.
     * @throws MojoFailureException When building the project failed.
     */
    private MavenProject createMavenProject()
        throws MojoFailureException
    {
        ModelSource modelSource =
            new StringModelSource( "<project>" + "<modelVersion>4.0.0</modelVersion>" + "<groupId>" + groupId
                + "</groupId>" + "<artifactId>" + artifactId + "</artifactId>" + "<version>" + version + "</version>"
                + "<packaging>" + ( classifier == null ? packaging : "pom" ) + "</packaging>" + "</project>" );
        DefaultProjectBuildingRequest buildingRequest =
            new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );
        buildingRequest.setProcessPlugins( false );
        try
        {
            return projectBuilder.build( modelSource, buildingRequest ).getProject();
        }
        catch ( ProjectBuildingException e )
        {
            throw new MojoFailureException( e.getMessage(), e );
        }
    }

    private File readingPomFromJarFile()
        throws MojoExecutionException
    {
        File pomFile = null;

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
                        pomFile = File.createTempFile( base, ".pom" );
                        
                        pomOutputStream = new FileOutputStream( pomFile );
                        
                        IOUtil.copy( pomInputStream, pomOutputStream );

                        pomOutputStream.close();
                        pomOutputStream = null;

                        pomInputStream.close();
                        pomInputStream = null;

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

            if ( pomFile == null )
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
        return pomFile;
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
            final Model model = new MavenXpp3Reader().read( reader );
            reader.close();
            reader = null;
            return model;
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

        ModelBuildingRequest buildingRequest = new DefaultModelBuildingRequest();
        
        InstallModelProblemCollector problemCollector = new InstallModelProblemCollector();
        
        modelValidator.validateEffectiveModel( model, buildingRequest , problemCollector );

        if ( problemCollector.getMessageCount() > 0 )
        {
            throw new MojoExecutionException( "The artifact information is incomplete or not valid:\n"
                + problemCollector.render( "  " ) );
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
            writer.close();
            writer = null;

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

    private static class InstallModelProblemCollector implements ModelProblemCollector
    {
        /** */
        private static final String NEWLINE = System.getProperty( "line.separator" );

        /** */
        private List<String> messages = new ArrayList<String>();
        
        @Override
        public void add( Severity severity, String message, InputLocation location, Exception cause )
        {
            messages.add( message );
        }

        public int getMessageCount()
        {
            return messages.size();
        }

        public String render( String indentation )
        {
            if ( messages.size() == 0 )
            {
                return indentation + "There were no validation errors.";
            }

            StringBuilder message = new StringBuilder();

            for ( int i = 0; i < messages.size(); i++ )
            {
                message.append( indentation + "[" + i + "]  " + messages.get( i ).toString() + NEWLINE );
            }

            return message.toString();
        }
    };
}
