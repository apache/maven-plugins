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

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.resources.remote.io.xpp3.RemoteResourcesBundleXpp3Reader;
import org.apache.maven.plugin.resources.remote.io.xpp3.SupplementalDataModelXpp3Reader;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectUtils;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TransitivityFilter;
import org.apache.maven.shared.downloader.DownloadException;
import org.apache.maven.shared.downloader.DownloadNotFoundException;
import org.apache.maven.shared.downloader.Downloader;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.velocity.VelocityComponent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * <p>
 * Pull down resourceBundles containing remote resources and process the
 * resources contained inside. When that is done the resources are injected
 * into the current (in-memory) Maven project, making them available to the
 * process-resources phase.
 * </p>
 * <p>
 * Resources that end in ".vm" are treated as velocity templates.  For those, the ".vm" is
 * stripped off for the final artifact name and it's  fed through velocity to have properties
 * expanded, conditions processed, etc...
 * </p>
 * <p>
 * Resources that don't end in ".vm" are copied "as is".
 * </p>
 *
 * @goal process
 * @requiresDependencyResolution test
 * @phase generate-resources
 */
public class ProcessRemoteResourcesMojo
    extends AbstractMojo
{
    
    private static final String TEMPLATE_SUFFIX = ".vm";
    
    /**
     * <p>
     * In cases where a local resource overrides one from a remote resource bundle, that resource
     * should be filtered if the resource set specifies it. In those cases, this parameter defines
     * the list of delimiters for filterable expressions. These delimiters are specified in the
     * form 'beginToken*endToken'. If no '*' is given, the delimiter is assumed to be the same for start and end.
     * </p>
     * <p>
     * So, the default filtering delimiters might be specified as:
     * </p>
     * <pre>
     * &lt;delimiters&gt;
     *   &lt;delimiter&gt;${*}&lt/delimiter&gt;
     *   &lt;delimiter&gt;@&lt/delimiter&gt;
     * &lt;/delimiters&gt;
     * </pre>
     * <p>
     * Since the '@' delimiter is the same on both ends, we don't need to specify '@*@' (though we can).
     * </p>
     * @parameter
     * @since 1.1
     */
    protected List filterDelimiters;

    /**
     * @parameter default-value="true"
     * @since 1.1
     */
    protected boolean filterUsesDefaultDelimiters;
    
    /**
     * If true, only generate resources in the directory of the root project in a multimodule build.
     * Dependencies from all modules will be aggregated before resource-generation takes place.
     * 
     * @parameter default-value="false"
     * @since 1.1
     */
    protected boolean runOnlyAtExecutionRoot;
    
    /**
     * The character encoding scheme to be applied when filtering resources.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    protected String encoding;

    /**
     * The local repository taken from Maven's runtime. Typically $HOME/.m2/repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * The remote repositories used as specified in your POM.
     *
     * @parameter expression="${project.repositories}"
     * @readonly
     * @required
     */
    private List repositories;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private List remoteArtifactRepositories;

    /**
     * The current Maven project.
     *
     * @parameter expression="${project}"
     * @readonly
     * @required
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
     * Supplemental model data.  Useful when processing
     * artifacts with incomplete POM metadata.
     * <p/>
     * By default, this Mojo looks for supplemental model
     * data in the file "${appendedResourcesDirectory}/supplemental-models.xml".
     *
     * @parameter
     * @since 1.0-alpha-5
     */
    private String[] supplementalModels;

    /**
     * Map of artifacts to supplemental project object models.
     */
    private Map supplementModels;

    /**
     * Merges supplemental data model with artifact
     * metadata.  Useful when processing artifacts with
     * incomplete POM metadata.
     *
     * @component
     * @readonly
     * @required
     */
    private ModelInheritanceAssembler inheritanceAssembler;

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
     * Skip remote-resource processing
     *
     * @parameter default-value="true"
     * @since 1.0-beta-1
     */
    private boolean attached = true;

    /**
     * Additional properties to be passed to velocity.
     * <p/>
     * Several properties are automatically added:<br/>
     * project - the current MavenProject <br/>
     * projects - the list of dependency projects<br/>
     * projectTimespan - the timespan of the current project (requires inceptionYear in pom)<br/>
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
     * @readonly
     * @required
     */
    private List resources;

    /**
     * Artifact downloader.
     *
     * @component
     * @readonly
     * @required
     */
    private Downloader downloader;

    /**
     * Velocity component.
     *
     * @component
     * @readonly
     * @required
     */
    private VelocityComponent velocity;
    
    /**
     * Filtering support, for local resources that override those in the remote bundle.
     * 
     * @component
     */
    private MavenFileFilter fileFilter;

    // These two things make this horrible. Maven artifact is way too complicated and the relationship between
    // the model usage and maven-artifact needs to be reworked as well as all our tools that deal with it. The
    // ProjectUtils should be a component so I don't have to expose the container and artifact factory here. Can't
    // change it now because it's not released ...

    /**
     * Artifact repository factory component.
     *
     * @component
     * @readonly
     * @required
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * Artifact factory, needed to create artifacts.
     *
     * @component
     * @readonly
     * @required
     */
    private ArtifactFactory artifactFactory;

    /**
     * The Maven session.
     *
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    private MavenSession mavenSession;

    /**
     * ProjectBuilder, needed to create projects from the artifacts.
     *
     * @component role="org.apache.maven.project.MavenProjectBuilder"
     * @required
     * @readonly
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * @component
     * @required
     * @readonly
     */
    private ResourceManager locator;


    /**
     * Scope to include. An Empty string indicates all scopes (default).
     *
     * @since 1.0
     * @parameter expression="${includeScope}" default-value="runtime"
     * @optional
     */
    protected String includeScope;

    /**
     * Scope to exclude. An Empty string indicates no scopes (default).
     *
     * @since 1.0
     * @parameter expression="${excludeScope}" default-value=""
     * @optional
     */
    protected String excludeScope;

    /**
     * Comma separated list of Artifact names too exclude.
     *
     * @since 1.0
     * @optional
     * @parameter expression="${excludeArtifactIds}" default-value=""
     */
    protected String excludeArtifactIds;

    /**
     * Comma separated list of Artifact names to include.
     *
     * @since 1.0
     * @optional
     * @parameter expression="${includeArtifactIds}" default-value=""
     */
    protected String includeArtifactIds;

    /**
     * Comma separated list of GroupId Names to exclude.
     *
     * @since 1.0
     * @optional
     * @parameter expression="${excludeGroupIds}" default-value=""
     */
    protected String excludeGroupIds;

    /**
     * Comma separated list of GroupIds to include.
     *
     * @since 1.0
     * @optional
     * @parameter expression="${includeGroupIds}" default-value=""
     */
    protected String includeGroupIds;

    /**
     * If we should exclude transitive dependencies
     *
     * @since 1.0
     * @optional
     * @parameter expression="${excludeTransitive}" default-value="false"
     */
    protected boolean excludeTransitive;
    
    /**
     * @component role-hint="default"
     */
    protected ProjectDependenciesResolver dependencyResolver;

    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            return;
        }
        if ( supplementalModels == null )
        {
            File sups = new File( appendedResourcesDirectory, "supplemental-models.xml" );
            if ( sups.exists() )
            {
                try
                {
                    supplementalModels = new String[]{sups.toURL().toString()};
                }
                catch ( MalformedURLException e )
                {
                    //ignore
                    getLog().debug( "URL issue with supplemental-models.xml: " + e.toString() );
                }
            }
        }

        locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );
        if ( appendedResourcesDirectory != null )
        {
            locator.addSearchPath( FileResourceLoader.ID, appendedResourcesDirectory.getAbsolutePath() );
        }
        locator.addSearchPath( "url", "" );
        locator.setOutputDirectory( new File( project.getBuild().getDirectory() ) );

        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );

            validate();

            List resourceBundleArtifacts = downloadResourceBundles( resourceBundles );
            supplementModels = loadSupplements( supplementalModels );

            VelocityContext context = new VelocityContext( properties );
            configureVelocityContext( context );

            RemoteResourcesClassLoader classLoader
                = new RemoteResourcesClassLoader( null );
            
            initalizeClassloader( classLoader, resourceBundleArtifacts );
            Thread.currentThread().setContextClassLoader( classLoader );
            
            processResourceBundles( classLoader, context );

            try
            {
                if ( outputDirectory.exists() )
                {
                    // ----------------------------------------------------------------------------
                    // Push our newly generated resources directory into the MavenProject so that
                    // these resources can be picked up by the process-resources phase.
                    // ----------------------------------------------------------------------------
                    if ( attached )
                    {
                        Resource resource = new Resource();
                        resource.setDirectory( outputDirectory.getAbsolutePath() );

                        project.getResources().add( resource );
                        project.getTestResources().add( resource );
                    }

                    // ----------------------------------------------------------------------------
                    // Write out archiver dot file
                    // ----------------------------------------------------------------------------
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
        finally
        {
            Thread.currentThread().setContextClassLoader( origLoader );
        }
    }

    protected List getProjects()
        throws MojoExecutionException
    {
        List projects = new ArrayList();

        // add filters in well known order, least specific to most specific
        FilterArtifacts filter = new FilterArtifacts();
        
        Set depArtifacts;
        Set artifacts;
        if ( runOnlyAtExecutionRoot )
        {
            depArtifacts = aggregateProjectDependencyArtifacts();
            artifacts = resolveProjectArtifacts();
        }
        else
        {
            depArtifacts = project.getDependencyArtifacts();
            artifacts = project.getArtifacts();
        }

        filter.addFilter( new TransitivityFilter( depArtifacts, this.excludeTransitive ) );
        filter.addFilter( new ScopeFilter( this.includeScope, this.excludeScope ) );
        filter.addFilter( new GroupIdFilter( this.includeGroupIds, this.excludeGroupIds ) );
        filter.addFilter( new ArtifactIdFilter( this.includeArtifactIds, this.excludeArtifactIds ) );

        // perform filtering
        try
        {
            artifacts = filter.filter( artifacts );
        }
        catch ( ArtifactFilterException e )
        {
            throw new MojoExecutionException(e.getMessage(),e);
        }


        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            try
            {
                List remoteRepo = null;
                if ( artifact.isSnapshot() )
                {
                    VersionRange rng = VersionRange.createFromVersion( artifact.getBaseVersion() );
                    artifact = artifactFactory.createDependencyArtifact( artifact.getGroupId(),
                                                                         artifact.getArtifactId(), rng,
                                                                         artifact.getType(), artifact.getClassifier(),
                                                                         artifact.getScope(), null,
                                                                         artifact.isOptional() );
                    remoteRepo = remoteArtifactRepositories;
                }
                else
                {
                    remoteRepo =
                        ProjectUtils.buildArtifactRepositories( repositories,
                                                                artifactRepositoryFactory,
                                                                mavenSession.getContainer() );

                }

                getLog().debug( "Building project for " + artifact );
                MavenProject p = null;
                try
                {
                    p = mavenProjectBuilder.buildFromRepository( artifact, remoteRepo, localRepository );
                }
                catch ( InvalidProjectModelException e )
                {
                    getLog().warn( "Invalid project model for artifact [" + artifact.getArtifactId() + ":" +
                        artifact.getGroupId() + ":" + artifact.getVersion() + "]. " +
                        "It will be ignored by the remote resources Mojo." );
                    continue;
                }

                String supplementKey =
                    generateSupplementMapKey( p.getModel().getGroupId(), p.getModel().getArtifactId() );

                if ( supplementModels.containsKey( supplementKey ) )
                {
                    Model mergedModel = mergeModels( p.getModel(), (Model) supplementModels.get( supplementKey ) );
                    MavenProject mergedProject = new MavenProject( mergedModel );
                    projects.add( mergedProject );
                    mergedProject.setArtifact( artifact );
                    mergedProject.setVersion( artifact.getVersion() );
                    getLog().debug( "Adding project with groupId [" + mergedProject.getGroupId() + "] (supplemented)" );
                }
                else
                {
                    projects.add( p );
                    getLog().debug( "Adding project with groupId [" + p.getGroupId() + "]" );
                }
            }
            catch ( ProjectBuildingException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }
        Collections.sort( projects, new ProjectComparator() );
        return projects;
    }

    private Set resolveProjectArtifacts()
        throws MojoExecutionException
    {
        List projects = mavenSession.getSortedProjects();

        try
        {
            return dependencyResolver.resolve( projects, Collections.singleton( Artifact.SCOPE_TEST ), mavenSession );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Failed to resolve dependencies for one or more projects in the reactor. Reason: "
                + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Failed to resolve dependencies for one or more projects in the reactor. Reason: "
                + e.getMessage(), e );
        }
    }

    private Set aggregateProjectDependencyArtifacts()
        throws MojoExecutionException
    {
        Set artifacts = new LinkedHashSet();
        
        List projects = mavenSession.getSortedProjects();
        for ( Iterator it = projects.iterator(); it.hasNext(); )
        {
            MavenProject p = (MavenProject) it.next();
            if ( p.getDependencyArtifacts() == null )
            {
                try
                {
                    Set depArtifacts = p.createArtifacts( artifactFactory, null, null );
                    p.setDependencyArtifacts( depArtifacts );
                    
                    if ( depArtifacts != null && !depArtifacts.isEmpty() )
                    {
                        artifacts.addAll( depArtifacts );
                    }
                }
                catch ( InvalidDependencyVersionException e )
                {
                    throw new MojoExecutionException( "Failed to create dependency artifacts for: " + p.getId() + ". Reason: "
                        + e.getMessage(), e );
                }
            }
        }
        
        return artifacts;
    }

    protected Map getProjectsSortedByOrganization( List projects )
        throws MojoExecutionException
    {
        Map organizations = new TreeMap( new OrganizationComparator() );
        List unknownOrganization = new ArrayList();

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject p = (MavenProject) i.next();

            if ( p.getOrganization() != null && StringUtils.isNotEmpty( p.getOrganization().getName() ) )
            {
                List sortedProjects = (List) organizations.get( p.getOrganization() );
                if ( sortedProjects == null )
                {
                    sortedProjects = new ArrayList();
                }
                sortedProjects.add( p );

                organizations.put( p.getOrganization(), sortedProjects );
            }
            else
            {
                unknownOrganization.add( p );
            }
        }
        if ( !unknownOrganization.isEmpty() )
        {
            Organization unknownOrg = new Organization();
            unknownOrg.setName( "an unknown organization" );
            organizations.put( unknownOrg, unknownOrganization );
        }

        return organizations;
    }

    protected boolean copyResourceIfExists( File file, String relFileName, VelocityContext context )
        throws IOException, MojoExecutionException
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
            File templateSource = new File( resourceDirectory, relFileName + TEMPLATE_SUFFIX );
            
            if ( !source.exists() && templateSource.exists() )
            {
                source = templateSource;
            }

            if ( source.exists() && !source.equals( file ) )
            {
                if ( source == templateSource )
                {
                    Reader reader = null;
                    Writer writer = null;
                    try
                    {
                        if ( encoding != null )
                        {
                            reader = new InputStreamReader( new FileInputStream( source ), encoding );
                            writer = new OutputStreamWriter( new FileOutputStream( file ), encoding );
                        }
                        else
                        {
                            reader = ReaderFactory.newPlatformReader( source );
                            writer = WriterFactory.newPlatformWriter( file );
                        }
                        
                        velocity.getEngine().evaluate( context, writer, "", reader );
                        velocity.getEngine().evaluate( context, writer, "", reader );
                    }
                    catch ( ParseErrorException e )
                    {
                        throw new MojoExecutionException( "Error rendering velocity resource.", e );
                    }
                    catch ( MethodInvocationException e )
                    {
                        throw new MojoExecutionException( "Error rendering velocity resource.", e );
                    }
                    catch ( ResourceNotFoundException e )
                    {
                        throw new MojoExecutionException( "Error rendering velocity resource.", e );
                    }
                    finally
                    {
                        IOUtil.close( writer );
                        IOUtil.close( reader );
                    }
                }
                else if ( resource.isFiltering() )
                {
                    
                    MavenFileFilterRequest req = setupRequest( resource, source, file );
                    
                    try
                    {
                        fileFilter.copyFile( req );
                    }
                    catch ( MavenFilteringException e )
                    {
                        throw new MojoExecutionException( "Error filtering resource: " + source, e );
                    }
                }
                else
                {
                    FileUtils.copyFile( source, file );
                }

                //exclude the original (so eclipse doesn't complain about duplicate resources)
                resource.addExclude( relFileName );

                return true;
            }

        }
        return false;
    }

    private MavenFileFilterRequest setupRequest( Resource resource, File source, File file )
    {
        MavenFileFilterRequest req = new MavenFileFilterRequest();
        req.setFrom( source );
        req.setTo( file );
        req.setFiltering( resource.isFiltering() );
        
        req.setMavenProject( project );
        req.setMavenSession( mavenSession );
        req.setInjectProjectBuildFilters( true );
        
        if ( encoding != null )
        {
            req.setEncoding( encoding );
        }
        
        if ( filterDelimiters != null && !filterDelimiters.isEmpty() )
        {
            LinkedHashSet delims = new LinkedHashSet();
            if ( filterUsesDefaultDelimiters )
            {
                delims.addAll( req.getDelimiters() );
            }
            delims.addAll( filterDelimiters );
            
            req.setDelimiters( delims );
        }
        
        return req;
    }

    protected void validate()
        throws MojoExecutionException
    {
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

                throw new MojoExecutionException( "The " + position +
                    " resource bundle configured must specify a groupId, artifactId, and" +
                    " version for a remote resource bundle. " +
                    "Must be of the form <resourceBundle>groupId:artifactId:version</resourceBundle>" );
            }

            bundleCount++;
        }

    }

    protected void configureVelocityContext( VelocityContext context )
        throws MojoExecutionException
    {
        String inceptionYear = project.getInceptionYear();
        String year = new SimpleDateFormat( "yyyy" ).format( new Date() );

        if ( StringUtils.isEmpty( inceptionYear ) )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "inceptionYear not specified, defaulting to " + year );
            }
            
            inceptionYear = year;
        }
        context.put( "project", project );
        List projects = getProjects();
        context.put( "projects", projects );
        context.put( "projectsSortedByOrganization", getProjectsSortedByOrganization( projects ) );

        context.put( "presentYear", year );

        if ( inceptionYear.equals( year ) )
        {
            context.put( "projectTimespan", year );
        }
        else
        {
            context.put( "projectTimespan", inceptionYear + "-" + year );
        }
    }

    private List downloadResourceBundles( List resourceBundles )
        throws MojoExecutionException
    {
        List resourceBundleArtifacts = new ArrayList();

        try
        {
            for ( Iterator i = resourceBundles.iterator(); i.hasNext(); )
            {
                String artifactDescriptor = (String) i.next();
                // groupId:artifactId:version
                String[] s = artifactDescriptor.split( ":" );
                File artifact = downloader.download( s[0], s[1], s[2], localRepository,
                                                     ProjectUtils.buildArtifactRepositories( repositories,
                                                                                             artifactRepositoryFactory,
                                                                                             mavenSession.getContainer() ) );

                resourceBundleArtifacts.add( artifact );
            }
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

        return resourceBundleArtifacts;
    }

    private void initalizeClassloader( RemoteResourcesClassLoader cl, List artifacts )
        throws MojoExecutionException
    {
        try
        {
            for ( Iterator i = artifacts.iterator(); i.hasNext(); )
            {
                File artifact = (File) i.next();
                cl.addURL( artifact.toURI().toURL() );
            }
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Unable to configure resources classloader: " + e.getMessage(), e );
        }
    }

    protected void processResourceBundles( RemoteResourcesClassLoader classLoader, VelocityContext context )
        throws MojoExecutionException
    {
        InputStreamReader reader = null;
        
        try
        {

            for ( Enumeration e = classLoader.getResources( BundleRemoteResourcesMojo.RESOURCES_MANIFEST );
                  e.hasMoreElements(); )
            {
                URL url = (URL) e.nextElement();

                try
                {
                    reader = new InputStreamReader( url.openStream() );

                    RemoteResourcesBundleXpp3Reader bundleReader = new RemoteResourcesBundleXpp3Reader();

                    RemoteResourcesBundle bundle = bundleReader.read( reader );

                    for ( Iterator i = bundle.getRemoteResources().iterator(); i.hasNext(); )
                    {
                        String bundleResource = (String) i.next();

                        String projectResource = bundleResource;

                        boolean doVelocity = false;
                        if ( projectResource.endsWith( TEMPLATE_SUFFIX ) )
                        {
                            projectResource = projectResource.substring( 0, projectResource.length() - 3 );
                            doVelocity = true;
                        }

                        // Don't overwrite resource that are already being provided.

                        File f = new File( outputDirectory, projectResource );

                        FileUtils.mkdir( f.getParentFile().getAbsolutePath() );

                        if ( !copyResourceIfExists( f, projectResource, context ) )
                        {
                            if ( doVelocity )
                            {
                                PrintWriter writer;
                                if ( bundle.getSourceEncoding() == null )
                                {
                                    writer = new PrintWriter( new FileWriter( f ) );
                                }
                                else
                                {
                                    writer = new PrintWriter( new OutputStreamWriter( new FileOutputStream( f ),
                                                                                      bundle.getSourceEncoding() ) );

                                }

                                try
                                {
                                    if ( bundle.getSourceEncoding() == null )
                                    {
                                        velocity.getEngine().mergeTemplate( bundleResource, context, writer );
                                    }
                                    else
                                    {
                                        velocity.getEngine().mergeTemplate( bundleResource, bundle.getSourceEncoding(),
                                                                            context, writer );

                                    }
                                }
                                finally
                                {
                                    IOUtil.close(writer);
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
                            File appendedResourceFile = new File( appendedResourcesDirectory, projectResource );
                            File appendedVmResourceFile = new File( appendedResourcesDirectory,
                                                                    projectResource + ".vm" );
                            if ( appendedResourceFile.exists() )
                            {
                                final InputStream in = new FileInputStream( appendedResourceFile );
                                final OutputStream append = new FileOutputStream( f, true );

                                try
                                {
                                    IOUtil.copy( in, append );
                                }
                                finally
                                {
                                    IOUtil.close( in );
                                    IOUtil.close( append );
                                }
                            }
                            else if ( appendedVmResourceFile.exists() )
                            {
                                PrintWriter writer;
                                FileReader freader = new FileReader( appendedVmResourceFile );

                                if ( bundle.getSourceEncoding() == null )
                                {
                                    writer = new PrintWriter( new FileWriter( f, true ) );
                                }
                                else
                                {
                                    writer = new PrintWriter( new OutputStreamWriter( new FileOutputStream( f, true ),
                                                                                      bundle.getSourceEncoding() ) );

                                }

                                try
                                {
                                    Velocity.init();
                                    Velocity.evaluate( context, writer, "remote-resources", freader );
                                }
                                finally
                                {
                                    IOUtil.close(writer);
                                    IOUtil.close(freader);
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
    }

    protected Model getSupplement( Xpp3Dom supplementModelXml )
        throws MojoExecutionException
    {
        MavenXpp3Reader modelReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model = modelReader.read( new StringReader( supplementModelXml.toString() ) );
            String groupId = model.getGroupId();
            String artifactId = model.getArtifactId();

            if ( groupId == null || groupId.trim().equals( "" ) )
            {
                throw new MojoExecutionException(
                    "Supplemental project XML " + "requires that a <groupId> element be present." );
            }

            if ( artifactId == null || artifactId.trim().equals( "" ) )
            {
                throw new MojoExecutionException(
                    "Supplemental project XML " + "requires that a <artifactId> element be present." );
            }
        }
        catch ( IOException e )
        {
            getLog().warn( "Unable to read supplemental XML: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            getLog().warn( "Unable to parse supplemental XML: " + e.getMessage(), e );
        }

        return model;
    }

    protected Model mergeModels( Model parent, Model child )
    {
        inheritanceAssembler.assembleModelInheritance( child, parent );
        return child;
    }

    private static String generateSupplementMapKey( String groupId, String artifactId )
    {
        return groupId.trim() + ":" + artifactId.trim();
    }

    private Map loadSupplements( String models[] )
        throws MojoExecutionException
    {
        if ( models == null )
        {
            getLog().debug( "Supplemental data models won't be loaded.  " + "No models specified." );
            return Collections.EMPTY_MAP;
        }

        List supplements = new ArrayList();
        for ( int idx = 0; idx < models.length; idx++ )
        {
            String set = models[idx];
            getLog().debug( "Preparing ruleset: " + set );
            try
            {
                File f = locator.getResourceAsFile( set, getLocationTemp( set ) );

                if ( null == f || !f.exists() )
                {
                    throw new MojoExecutionException( "Cold not resolve " + set );
                }
                if ( !f.canRead() )
                {
                    throw new MojoExecutionException( "Supplemental data models won't be loaded. " + "File " +
                        f.getAbsolutePath() + " cannot be read, check permissions on the file." );
                }

                getLog().debug( "Loading supplemental models from " + f.getAbsolutePath() );

                SupplementalDataModelXpp3Reader reader = new SupplementalDataModelXpp3Reader();
                SupplementalDataModel supplementalModel = reader.read( new FileReader( f ) );
                supplements.addAll( supplementalModel.getSupplement() );
            }
            catch ( Exception e )
            {
                String msg = "Error loading supplemental data models: " + e.getMessage();
                getLog().error( msg, e );
                throw new MojoExecutionException( msg, e );
            }
        }

        getLog().debug( "Loading supplements complete." );

        Map supplementMap = new HashMap();
        for ( Iterator i = supplements.iterator(); i.hasNext(); )
        {
            Supplement sd = (Supplement) i.next();

            Xpp3Dom dom = (Xpp3Dom) sd.getProject();

            Model m = getSupplement( dom );
            supplementMap.put( generateSupplementMapKey( m.getGroupId(), m.getArtifactId() ), m );
        }

        return supplementMap;
    }

    /**
     * Convenience method to get the location of the specified file name.
     *
     * @param name the name of the file whose location is to be resolved
     * @return a String that contains the absolute file name of the file
     */
    private String getLocationTemp( String name )
    {
        String loc = name;
        if ( loc.indexOf( '/' ) != -1 )
        {
            loc = loc.substring( loc.lastIndexOf( '/' ) + 1 );
        }
        if ( loc.indexOf( '\\' ) != -1 )
        {
            loc = loc.substring( loc.lastIndexOf( '\\' ) + 1 );
        }
        getLog().debug( "Before: " + name + " After: " + loc );
        return loc;
    }

    class OrganizationComparator
        implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            Organization org1 = (Organization) o1;
            Organization org2 = (Organization) o2;
            int i = compareStrings( org1.getName(), org2.getName() );
            if (i == 0)
            {
                i = compareStrings( org1.getUrl(), org2.getUrl() );
            }
            return i;
        }

        public boolean equals( Object o1, Object o2 )
        {
            return compare(o1, o2) == 0;
        }

        private int compareStrings( String s1, String s2 ) {
            if ( s1 == null && s2 == null )
            {
                return 0;
            }
            else if ( s1 == null && s2 != null )
            {
                return 1;
            }
            else if ( s1 != null && s2 == null )
            {
                return -1;
            }

            return s1.compareToIgnoreCase( s2 );
        }
    }

    class ProjectComparator
        implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            MavenProject p1 = (MavenProject) o1;
            MavenProject p2 = (MavenProject) o2;

            return p1.getArtifact().compareTo( p2.getArtifact() );
        }

        public boolean equals( Object o1, Object o2 )
        {
            MavenProject p1 = (MavenProject) o1;
            MavenProject p2 = (MavenProject) o2;

            return p1.getArtifact().equals( p2.getArtifact() );
        }
    }

}
