package org.apache.maven.plugin.resources.remote;

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
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.resources.remote.io.xpp3.RemoteResourcesBundleXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectUtils;
import org.apache.maven.shared.downloader.DownloadException;
import org.apache.maven.shared.downloader.DownloadNotFoundException;
import org.apache.maven.shared.downloader.Downloader;
import org.apache.maven.model.Resource;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.velocity.VelocityComponent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Pull down resourceBundles containing remote resources and process the resources contained
 * inside the artifact.
 * <p/>
 * Resources that end in ".vm" are treated as velocity templates.  For those, the ".vm" is 
 * stripped off for the final artifact name and it's  fed through velocity to have properties
 * expanded, conditions processed, etc...
 * <p/>
 * Resources that don't end in ".vm" are copied "as is". 
 *
 * @goal process
 * @requiresDependencyResolution runtime
 * @phase generate-resources
 */
public class ProcessRemoteResourcesMojo
    extends AbstractMojo
{
    /**
     * The local repository taken from Maven's runtime. Typically $HOME/.m2/repository.
     *
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * The remote repositories used as specified in your POM.
     *
     * @parameter expression="${project.repositories}"
     */
    private List remoteRepositories;

    /**
     * The current Maven project.
     *
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * The directory where processed resources will be placed for packaging.
     *
     * @parameter expression="${project.build.directory}/maven-shared-archive-resources"
     */
    private File outputDirectory;
    
    /**
     * The directory containing extra information appended to the generated resources.
     *
     * @parameter expression="${basedir}/src/main/appended-resources"
     */
    private File appendedResourcesDirectory;

    /**
     * The resource bundles that will be retrieved and processed.
     *
     * @parameter
     * @required
     */
    private List resourceBundles;

    
    /**
     * Skip remote-resource processing
     * 
     * @parameter expression="${remoteresources.skip}" default-value="false"
     * @since 1.0-alpha-5
     */
    private boolean skip;

    /**
     * Additional properties to be passed to velocity.
     * <p/>
     * Several properties are automatically added:<br/>
     *   project - the current MavenProject <br/>
     *   projects - the list of dependency projects<br/>
     *   projectTimespan - the timespan of the current project (requires inceptionYear in pom)<br/>
     * <p/>
     * See <a href="http://maven.apache.org/ref/current/maven-project/apidocs/org/apache/maven/project/MavenProject.html">
     * the javadoc for MavenProject</a> for information about the properties on the MavenProject. 
     *
     * @parameter
     */
    private Map properties = new HashMap();
    
    /**
     * The list of resources defined for the project.
     *
     * @parameter expression="${project.resources}"
     * @required
     */
    private List resources;
    
    
    /**
     * Artifact downloader.
     *
     * @component
     */
    private Downloader downloader;

    /**
     * Velocity component.
     *
     * @component
     */
    private VelocityComponent velocity;

    // These two things make this horrible. Maven artifact is way too complicated and the relationship between
    // the model usage and maven-artifact needs to be reworked as well as all our tools that deal with it. The
    // ProjectUtils should be a component so I don't have to expose the container and artifact factory here. Can't
    // change it now because it's not released ...

    /**
     * Artifact repository factory component.
     *
     * @component
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;
    
    /**
     * Artifact factory, needed to create artifacts.
     *
     * @component
     */
    private ArtifactFactory artifactFactory;
    
    /**
     * The Maven session.
     *
     * @parameter expression="${session}"
     */
    private MavenSession mavenSession;
    
    
   /**
    * ProjectBuilder, needed to create projects from the artifacts.
    *
    * @component role="org.apache.maven.project.MavenProjectBuilder"
    * @required
    * @readonly
    */
   protected MavenProjectBuilder mavenProjectBuilder;


    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            return;
        }
        
        String inceptionYear = project.getInceptionYear();
        String year = new SimpleDateFormat( "yyyy" ).format( new Date() );
        
        if ( StringUtils.isEmpty( inceptionYear ) )
        {
            getLog().info("inceptionYear not specified, defaulting to " + year);
            inceptionYear = year;
        }

        RemoteResourcesClassLoader classLoader = new RemoteResourcesClassLoader();

        int bundleCount = 1;

        for ( Iterator i = resourceBundles.iterator(); i.hasNext(); )
        {
            String artifactDescriptor = (String) i.next();

            // groupId:artifactId:version
            String[] s = StringUtils.split( artifactDescriptor, ":" );

            if ( s.length != 3 )
            {
                String position;

                if ( bundleCount == 1 )
                {
                    position = "1st";
                }
                else if ( bundleCount == 2 )
                {
                    position = "2nd";
                }
                else if ( bundleCount == 3 )
                {
                    position = "3rd";
                }
                else
                {
                    position = bundleCount + "th";
                }

                throw new MojoExecutionException( "The " + position
                    + " resource bundle configured must specify a groupId, artifactId, and" 
                    + " version for a remote resource bundle. " 
                    + "Must be of the form <resourceBundle>groupId:artifactId:version</resourceBundle>" );
            }

            try
            {
                File artifact = downloader.download( s[0], s[1], s[2], localRepository,
                                                     ProjectUtils.buildArtifactRepositories( remoteRepositories,
                                                         artifactRepositoryFactory,
                                                         mavenSession.getContainer() ) );

                classLoader.addURL( artifact.toURI().toURL() );
            }
            catch ( DownloadException e )
            {
                throw new MojoExecutionException( "Error downloading resources JAR.", e );
            }
            catch ( DownloadNotFoundException e )
            {
                throw new MojoExecutionException( "Resources JAR cannot be found.", e );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new MojoExecutionException( "Resources JAR cannot be found.", e );
            }
            catch ( MalformedURLException e )
            {
                // Won't happen.
            }

            bundleCount++;
        }

        ClassLoader old = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader( classLoader );

        InputStreamReader reader = null;

        VelocityContext context = new VelocityContext( properties );

        context.put( "project", project );
        context.put( "projects", getProjects() );
        
        context.put( "presentYear", year );

        if ( project.getInceptionYear().equals( year ) )
        {
            context.put( "projectTimespan", year );
        }
        else
        {
            context.put( "projectTimespan", project.getInceptionYear() + "-" + year );
        }

        try
        {
            
            for ( Enumeration e = classLoader.getResources( BundleRemoteResourcesMojo.RESOURCES_MANIFEST );
                  e.hasMoreElements(); )
            {
                URL url = (URL) e.nextElement();

                URLConnection conn = url.openConnection();

                conn.connect();

                reader = new InputStreamReader( conn.getInputStream() );
                               
                try 
                {

                    RemoteResourcesBundleXpp3Reader bundleReader = new RemoteResourcesBundleXpp3Reader();
    
                    RemoteResourcesBundle bundle = bundleReader.read( reader );
    
                    for ( Iterator i = bundle.getRemoteResources().iterator(); i.hasNext(); )
                    {
                        String bundleResource = (String) i.next();
    
                        String projectResource = bundleResource;
    
                        boolean doVelocity = false;
                        if ( projectResource.endsWith( ".vm" ) )
                        {
                            projectResource = projectResource.substring( 0, projectResource.length() - 3 );
                            doVelocity = true;
                        }
    
                        // Don't overwrite resource that are already being provided.
    
                        File f = new File( outputDirectory, projectResource );
    
                        FileUtils.mkdir( f.getParentFile().getAbsolutePath() );
    
                        if ( !copyResourceIfExists( f, projectResource ) )
                        {
                            if ( doVelocity) 
                            {
                                PrintWriter writer = new PrintWriter( new FileWriter( f ) );
                                try 
                                {
                                    velocity.getEngine().mergeTemplate( bundleResource, context, writer );
        
                                    File appendedResourceFile = new File( appendedResourcesDirectory, projectResource );
                                    if ( appendedResourceFile.exists() ) 
                                    {
                                        FileReader freader = new FileReader( appendedResourceFile );
                                        BufferedReader breader = new BufferedReader( freader );
                                        
                                        String line = breader.readLine();
                               
                                        while ( line != null )
                                        {
                                            writer.println( line );
                                            line = breader.readLine();
                                        }
                                    }
                                }
                                finally
                                {
                                    writer.close();
                                }
                            }
                            else
                            {
                                URL resUrl = classLoader.getResource( bundleResource );
                                if ( resUrl != null )
                                {
                                    FileUtils.copyURLToFile( resUrl, f );
                                }
                            }
                        }
                    }
                }
                finally
                {
                    reader.close();
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error finding remote resources manifests", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing remote resource bundle descriptor.", e );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error rendering velocity resource.", e );
        }

        Thread.currentThread().setContextClassLoader( old );

        // ----------------------------------------------------------------------------
        // Push our newly generated resources directory into the MavenProject so that
        // these resources can be picked up by the process-resources phase.
        // ----------------------------------------------------------------------------

        Resource resource = new Resource();

        resource.setDirectory( outputDirectory.getAbsolutePath() );

        project.getResources().add( resource );
        project.getTestResources().add( resource );

        // ----------------------------------------------------------------------------
        // Write out archiver dot file
        // ----------------------------------------------------------------------------

        try
        {
            if ( outputDirectory.exists() )
            {
                File dotFile = new File( project.getBuild().getDirectory(), ".plxarc" );

                FileUtils.mkdir( dotFile.getParentFile().getAbsolutePath() );
                
                FileUtils.fileWrite( dotFile.getAbsolutePath(), outputDirectory.getName() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating dot file for archiving instructions.", e );
        }
    }
    
    protected List getProjects()
        throws MojoExecutionException
    {
        List projects = new ArrayList();
        for ( Iterator it = project.getArtifacts().iterator() ; it.hasNext() ; ) 
        {
            Artifact artifact = (Artifact) it.next();
            try 
            {
                if ( artifact.isSnapshot() )
                {
                    VersionRange rng = VersionRange.createFromVersion(artifact.getBaseVersion());
                    artifact = artifactFactory.createDependencyArtifact( artifact.getGroupId(),
                                                                         artifact.getArtifactId(),
                                                                         rng,
                                                                         artifact.getType(),
                                                                         artifact.getClassifier(),
                                                                         artifact.getScope(),
                                                                         null,
                                                                         artifact.isOptional() );
                }
                getLog().debug("Building project for " + artifact);
                MavenProject p = mavenProjectBuilder.buildFromRepository( artifact,
                                                        remoteRepositories,
                                                        localRepository,
                                                        true );
                projects.add( p );
            }
            catch ( ProjectBuildingException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return projects;
    }
    
    protected boolean copyResourceIfExists( File file, String relFileName )
        throws IOException 
    {
        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();
            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() )
            {
                continue;
            }
            //TODO - really should use the resource includes/excludes and name mapping
            File source = new File( resourceDirectory, relFileName );
            
            if ( source.exists() 
                && !source.equals( file ) )
            {
                //TODO - should use filters here
                FileUtils.copyFile( source, file );
                
                //exclude the original (so eclipse doesn't complain about duplicate resources)
                resource.addExclude( relFileName );
                
                return true;
            }
            
        }
        return false;
    }
}
