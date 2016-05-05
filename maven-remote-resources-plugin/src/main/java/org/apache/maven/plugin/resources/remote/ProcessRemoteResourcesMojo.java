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

import java.io.ByteArrayInputStream;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * <p>
 * Pull down resourceBundles containing remote resources and process the resources contained inside. When that is done
 * the resources are injected into the current (in-memory) Maven project, making them available to the process-resources
 * phase.
 * </p>
 * <p>
 * Resources that end in ".vm" are treated as velocity templates. For those, the ".vm" is stripped off for the final
 * artifact name and it's fed through velocity to have properties expanded, conditions processed, etc...
 * </p>
 * <p/>
 * Resources that don't end in ".vm" are copied "as is".
 */
// NOTE: Removed the following in favor of maven-artifact-resolver library, for MRRESOURCES-41
// If I leave this intact, interdependent projects within the reactor that haven't been built
// (remember, this runs in the generate-resources phase) will cause the build to fail.
//
// @requiresDependencyResolution test
@Mojo( name = "process", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true )
public class ProcessRemoteResourcesMojo
    extends AbstractMojo
    implements LogChute
{

    private static final String TEMPLATE_SUFFIX = ".vm";

    /**
     * <p>
     * In cases where a local resource overrides one from a remote resource bundle, that resource should be filtered if
     * the resource set specifies it. In those cases, this parameter defines the list of delimiters for filterable
     * expressions. These delimiters are specified in the form 'beginToken*endToken'. If no '*' is given, the delimiter
     * is assumed to be the same for start and end.
     * </p>
     * <p>
     * So, the default filtering delimiters might be specified as:
     * </p>
     * 
     * <pre>
     * &lt;delimiters&gt;
     *   &lt;delimiter&gt;${*}&lt/delimiter&gt;
     *   &lt;delimiter&gt;@&lt/delimiter&gt;
     * &lt;/delimiters&gt;
     * </pre>
     * <p/>
     * Since the '@' delimiter is the same on both ends, we don't need to specify '@*@' (though we can).
     *
     * @since 1.1
     */
    @Parameter
    protected List<String> filterDelimiters;

    /**
     * @since 1.1
     */
    @Parameter( defaultValue = "true" )
    protected boolean useDefaultFilterDelimiters;

    /**
     * If true, only generate resources in the directory of the root project in a multimodule build.
     * Dependencies from all modules will be aggregated before resource-generation takes place.
     *
     * @since 1.1
     */
    @Parameter( defaultValue = "false" )
    protected boolean runOnlyAtExecutionRoot;

    /**
     * Used for calculation of execution-root for {@link ProcessRemoteResourcesMojo#runOnlyAtExecutionRoot}.
     */
    @Parameter( defaultValue = "${basedir}", readonly = true, required = true )
    protected File basedir;

    /**
     * The character encoding scheme to be applied when filtering resources.
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    protected String encoding;

    /**
     * The local repository taken from Maven's runtime. Typically $HOME/.m2/repository.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver.
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true )
    private List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * The current Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * The directory where processed resources will be placed for packaging.
     */
    @Parameter( defaultValue = "${project.build.directory}/maven-shared-archive-resources" )
    private File outputDirectory;

    /**
     * The directory containing extra information appended to the generated resources.
     */
    @Parameter( defaultValue = "${basedir}/src/main/appended-resources" )
    private File appendedResourcesDirectory;

    /**
     * Supplemental model data. Useful when processing
     * artifacts with incomplete POM metadata.
     * <p/>
     * By default, this Mojo looks for supplemental model data in the file
     * "${appendedResourcesDirectory}/supplemental-models.xml".
     *
     * @since 1.0-alpha-5
     */
    @Parameter
    private String[] supplementalModels;

    /**
     * List of artifacts that are added to the search path when looking
     * for supplementalModels, expressed with <code>groupId:artifactId:version[:type[:classifier]]</code> format.
     *
     * @since 1.1
     */
    @Parameter
    private List<String> supplementalModelArtifacts;

    /**
     * Map of artifacts to supplemental project object models.
     */
    private Map<String, Model> supplementModels;

    /**
     * Merges supplemental data model with artifact
     * metadata. Useful when processing artifacts with
     * incomplete POM metadata.
     */
    @Component
    private ModelInheritanceAssembler inheritanceAssembler;

    /**
     * The resource bundles that will be retrieved and processed,
     * expressed with <code>groupId:artifactId:version[:type[:classifier]]</code> format.
     */
    @Parameter( required = true )
    private List<String> resourceBundles;

    /**
     * Skip remote-resource processing
     *
     * @since 1.0-alpha-5
     */
    @Parameter( property = "remoteresources.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Attaches the resources to the main build of the project as a resource
     * directory.
     *
     * @since 1.5
     */
    @Parameter( defaultValue = "true", property = "attachToMain" )
    private boolean attachToMain;

    /**
     * Attaches the resources to the test build of the project as a resource
     * directory.
     *
     * @since 1.5
     */
    @Parameter( defaultValue = "true", property = "attachToTest" )
    private boolean attachToTest;

    /**
     * Additional properties to be passed to velocity.
     * <p/>
     * Several properties are automatically added:<br/>
     * project - the current MavenProject <br/>
     * projects - the list of dependency projects<br/>
     * projectTimespan - the timespan of the current project (requires inceptionYear in pom)<br/>
     * locator - the ResourceManager that can be used to retrieve additional resources<br/>
     * <p/>
     * See <a
     * href="http://maven.apache.org/ref/current/maven-project/apidocs/org/apache/maven/project/MavenProject.html"> the
     * javadoc for MavenProject</a> for information about the properties on the MavenProject.
     */
    @Parameter
    private Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Whether to include properties defined in the project when filtering resources.
     *
     * @since 1.2
     */
    @Parameter( defaultValue = "false" )
    protected boolean includeProjectProperties = false;

    /**
     * When the result of velocity transformation fits in memory, it is compared with the actual contents on disk
     * to eliminate unnecessary destination file overwrite. This improves build times since further build steps
     * typically rely on the modification date.
     *
     * @since 1.6
     */
    @Parameter( defaultValue = "5242880" )
    protected int velocityFilterInMemoryThreshold = 5 * 1024 * 1024;

    /**
     * The list of resources defined for the project.
     */
    @Parameter( defaultValue = "${project.resources}", readonly = true, required = true )
    private List<Resource> resources;

    /**
     * Artifact Resolver, needed to resolve and download the {@code resourceBundles}.
     */
    @Component
    private ArtifactResolver artifactResolver;

    /**
     * Filtering support, for local resources that override those in the remote bundle.
     */
    @Component
    private MavenFileFilter fileFilter;

    /**
     * Artifact factory, needed to create artifacts.
     */
    @Component
    private ArtifactFactory artifactFactory;

    /**
     * The Maven session.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession mavenSession;

    /**
     * ProjectBuilder, needed to create projects from the artifacts.
     */
    @Component( role = MavenProjectBuilder.class )
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     */
    @Component
    private ResourceManager locator;

    /**
     * Scope to include. An Empty string indicates all scopes (default is "runtime").
     *
     * @since 1.0
     */
    @Parameter( property = "includeScope", defaultValue = "runtime" )
    protected String includeScope;

    /**
     * Scope to exclude. An Empty string indicates no scopes (default).
     *
     * @since 1.0
     */
    @Parameter( property = "excludeScope", defaultValue = "" )
    protected String excludeScope;

    /**
     * When resolving project dependencies, specify the scopes to include.
     * The default is the same as "includeScope" if there are no exclude scopes set.
     * Otherwise, it defaults to "test" to grab all the dependencies so the
     * exclude filters can filter out what is not needed.
     * 
     * @since 1.5
     */
    @Parameter
    private String[] resolveScopes;

    /**
     * Comma separated list of Artifact names too exclude.
     *
     * @since 1.0
     */
    @Parameter( property = "excludeArtifactIds", defaultValue = "" )
    protected String excludeArtifactIds;

    /**
     * Comma separated list of Artifact names to include.
     *
     * @since 1.0
     */
    @Parameter( property = "includeArtifactIds", defaultValue = "" )
    protected String includeArtifactIds;

    /**
     * Comma separated list of GroupId Names to exclude.
     *
     * @since 1.0
     */
    @Parameter( property = "excludeGroupIds", defaultValue = "" )
    protected String excludeGroupIds;

    /**
     * Comma separated list of GroupIds to include.
     *
     * @since 1.0
     */
    @Parameter( property = "includeGroupIds", defaultValue = "" )
    protected String includeGroupIds;

    /**
     * If we should exclude transitive dependencies
     *
     * @since 1.0
     */
    @Parameter( property = "excludeTransitive", defaultValue = "false" )
    protected boolean excludeTransitive;

    /**
     */
    @Component( hint = "default" )
    protected ProjectDependenciesResolver dependencyResolver;

    private VelocityEngine velocity;

    @SuppressWarnings( "unchecked" )
    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Skipping remote resources execution." );
            return;
        }

        if ( StringUtils.isEmpty( encoding ) )
        {
            getLog().warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                               + ", i.e. build is platform dependent!" );
        }

        if ( runOnlyAtExecutionRoot && !project.isExecutionRoot() )
        {
            getLog().info( "Skipping remote-resource generation in this project because it's not the Execution Root" );
            return;
        }
        if ( resolveScopes == null )
        {
            if ( excludeScope == null || "".equals( excludeScope ) )
            {
                resolveScopes = new String[] { this.includeScope };
            }
            else
            {
                resolveScopes = new String[] { Artifact.SCOPE_TEST };
            }
        }
        velocity = new VelocityEngine();
        velocity.setProperty( VelocityEngine.RUNTIME_LOG_LOGSYSTEM, this );
        velocity.setProperty( "resource.loader", "classpath" );
        velocity.setProperty( "classpath.resource.loader.class", ClasspathResourceLoader.class.getName() );
        velocity.init();

        if ( supplementalModels == null )
        {
            File sups = new File( appendedResourcesDirectory, "supplemental-models.xml" );
            if ( sups.exists() )
            {
                try
                {
                    supplementalModels = new String[] { sups.toURI().toURL().toString() };
                }
                catch ( MalformedURLException e )
                {
                    // ignore
                    getLog().debug( "URL issue with supplemental-models.xml: " + e.toString() );
                }
            }
        }

        addSupplementalModelArtifacts();
        locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );
        if ( appendedResourcesDirectory != null )
        {
            locator.addSearchPath( FileResourceLoader.ID, appendedResourcesDirectory.getAbsolutePath() );
        }
        locator.addSearchPath( "url", "" );
        locator.setOutputDirectory( new File( project.getBuild().getDirectory() ) );

        if ( includeProjectProperties )
        {
            final Properties projectProperties = project.getProperties();
            for ( Object key : projectProperties.keySet() )
            {
                properties.put( key.toString(), projectProperties.get( key ).toString() );
            }
        }

        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );

            validate();

            List<File> resourceBundleArtifacts = downloadBundles( resourceBundles );
            supplementModels = loadSupplements( supplementalModels );

            VelocityContext context = new VelocityContext( properties );
            configureVelocityContext( context );

            RemoteResourcesClassLoader classLoader = new RemoteResourcesClassLoader( null );

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
                    Resource resource = new Resource();
                    resource.setDirectory( outputDirectory.getAbsolutePath() );
                    // MRRESOURCES-61 handle main and test resources separately
                    if ( attachToMain )
                    {
                        project.getResources().add( resource );
                    }
                    if ( attachToTest )
                    {
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

    private void addSupplementalModelArtifacts()
        throws MojoExecutionException
    {
        if ( supplementalModelArtifacts != null && !supplementalModelArtifacts.isEmpty() )
        {
            List<File> artifacts = downloadBundles( supplementalModelArtifacts );

            for ( File artifact : artifacts )
            {
                if ( artifact.isDirectory() )
                {
                    locator.addSearchPath( FileResourceLoader.ID, artifact.getAbsolutePath() );
                }
                else
                {
                    try
                    {
                        locator.addSearchPath( "jar", "jar:" + artifact.toURI().toURL().toExternalForm() );
                    }
                    catch ( MalformedURLException e )
                    {
                        throw new MojoExecutionException( "Could not use jar " + artifact.getAbsolutePath(), e );
                    }
                }
            }

        }
    }

    @SuppressWarnings( "unchecked" )
    protected List<MavenProject> getProjects()
        throws MojoExecutionException
    {
        List<MavenProject> projects = new ArrayList<MavenProject>();

        // add filters in well known order, least specific to most specific
        FilterArtifacts filter = new FilterArtifacts();

        Set<Artifact> artifacts = resolveProjectArtifacts();
        if ( this.excludeTransitive )
        {
            Set<Artifact> depArtifacts;
            if ( runOnlyAtExecutionRoot )
            {
                depArtifacts = aggregateProjectDependencyArtifacts();
            }
            else
            {
                depArtifacts = project.getDependencyArtifacts();
            }
            filter.addFilter( new ProjectTransitivityFilter( depArtifacts, true ) );
        }

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
            throw new MojoExecutionException( e.getMessage(), e );
        }

        for ( Artifact artifact : artifacts )
        {
            try
            {
                List<ArtifactRepository> remoteRepo = remoteArtifactRepositories;
                if ( artifact.isSnapshot() )
                {
                    VersionRange rng = VersionRange.createFromVersion( artifact.getBaseVersion() );
                    artifact =
                        artifactFactory.createDependencyArtifact( artifact.getGroupId(), artifact.getArtifactId(), rng,
                                                                  artifact.getType(), artifact.getClassifier(),
                                                                  artifact.getScope(), null, artifact.isOptional() );
                }

                getLog().debug( "Building project for " + artifact );
                MavenProject p;
                try
                {
                    p = mavenProjectBuilder.buildFromRepository( artifact, remoteRepo, localRepository );
                }
                catch ( InvalidProjectModelException e )
                {
                    getLog().warn( "Invalid project model for artifact [" + artifact.getArtifactId() + ":"
                                       + artifact.getGroupId() + ":" + artifact.getVersion() + "]. "
                                       + "It will be ignored by the remote resources Mojo." );
                    continue;
                }

                String supplementKey =
                    generateSupplementMapKey( p.getModel().getGroupId(), p.getModel().getArtifactId() );

                if ( supplementModels.containsKey( supplementKey ) )
                {
                    Model mergedModel = mergeModels( p.getModel(), supplementModels.get( supplementKey ) );
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
        }
        Collections.sort( projects, new ProjectComparator() );
        return projects;
    }

    private Set<Artifact> resolveProjectArtifacts()
        throws MojoExecutionException
    {
        // CHECKSTYLE_OFF: LineLength
        try
        {
            if ( runOnlyAtExecutionRoot )
            {
                List<MavenProject> projects = mavenSession.getSortedProjects();
                return dependencyResolver.resolve( projects, Arrays.asList( resolveScopes ), mavenSession );
            }
            else
            {
                return dependencyResolver.resolve( project, Arrays.asList( resolveScopes ), mavenSession );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException(
                                              "Failed to resolve dependencies for one or more projects in the reactor. Reason: "
                                                  + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException(
                                              "Failed to resolve dependencies for one or more projects in the reactor. Reason: "
                                                  + e.getMessage(), e );
        }
        // CHECKSTYLE_ON: LineLength
    }

    @SuppressWarnings( "unchecked" )
    private Set<Artifact> aggregateProjectDependencyArtifacts()
        throws MojoExecutionException
    {
        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();

        List<MavenProject> projects = mavenSession.getSortedProjects();
        for ( MavenProject p : projects )
        {
            if ( p.getDependencyArtifacts() == null )
            {
                try
                {
                    Set<Artifact> depArtifacts = p.createArtifacts( artifactFactory, null, null );

                    if ( depArtifacts != null && !depArtifacts.isEmpty() )
                    {
                        for ( Artifact artifact : depArtifacts )
                        {
                            if ( artifact.getVersion() == null && artifact.getVersionRange() != null )
                            {
                                // Version is required for equality comparison between artifacts,
                                // but it is not needed for our purposes of filtering out
                                // transitive dependencies (which requires only groupId/artifactId).
                                // Therefore set an arbitrary version if missing.
                                artifact.setResolvedVersion( Artifact.LATEST_VERSION );
                            }
                            artifacts.add( artifact );
                        }
                    }
                }
                catch ( InvalidDependencyVersionException e )
                {
                    throw new MojoExecutionException( "Failed to create dependency artifacts for: " + p.getId()
                        + ". Reason: " + e.getMessage(), e );
                }
            }
        }

        return artifacts;
    }

    protected Map<Organization, List<MavenProject>> getProjectsSortedByOrganization( List<MavenProject> projects )
        throws MojoExecutionException
    {
        Map<Organization, List<MavenProject>> organizations =
            new TreeMap<Organization, List<MavenProject>>( new OrganizationComparator() );
        List<MavenProject> unknownOrganization = new ArrayList<MavenProject>();

        for ( MavenProject p : projects )
        {
            if ( p.getOrganization() != null && StringUtils.isNotEmpty( p.getOrganization().getName() ) )
            {
                List<MavenProject> sortedProjects = organizations.get( p.getOrganization() );
                if ( sortedProjects == null )
                {
                    sortedProjects = new ArrayList<MavenProject>();
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
        for ( Resource resource : resources )
        {
            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() )
            {
                continue;
            }

            // TODO - really should use the resource includes/excludes and name mapping
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
                    DeferredFileOutputStream os = new DeferredFileOutputStream( velocityFilterInMemoryThreshold, file );
                    try
                    {

                        if ( encoding != null )
                        {
                            reader = new InputStreamReader( new FileInputStream( source ), encoding );
                            writer = new OutputStreamWriter( os, encoding );
                        }
                        else
                        {
                            reader = ReaderFactory.newPlatformReader( source );
                            writer = WriterFactory.newPlatformWriter( os );
                        }

                        velocity.evaluate( context, writer, "", reader );
                        writer.close();
                        writer = null;
                        reader.close();
                        reader = null;
                    }
                    catch ( ParseErrorException e )
                    {
                        throw new MojoExecutionException( "Error rendering velocity resource: " + source, e );
                    }
                    catch ( MethodInvocationException e )
                    {
                        throw new MojoExecutionException( "Error rendering velocity resource: " + source, e );
                    }
                    catch ( ResourceNotFoundException e )
                    {
                        throw new MojoExecutionException( "Error rendering velocity resource: " + source, e );
                    }
                    finally
                    {
                        IOUtil.close( writer );
                        IOUtil.close( reader );
                    }
                    fileWriteIfDiffers( os );
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

                // exclude the original (so eclipse doesn't complain about duplicate resources)
                resource.addExclude( relFileName );

                return true;
            }

        }
        return false;
    }

    /**
     * If the transformation result fits in memory and the destination file already exists
     * then both are compared.
     * <p>If destination file is byte-by-byte equal, then it is not overwritten.
     * This improves subsequent compilation times since upstream plugins property see that
     * the resource was not modified.
     * <p>Note: the method should be called after {@link org.apache.commons.io.output.DeferredFileOutputStream#close}
     *
     * @param outStream Deferred stream
     * @throws IOException
     */
    private void fileWriteIfDiffers( DeferredFileOutputStream outStream ) throws IOException
    {
        File file = outStream.getFile();
        if ( outStream.isThresholdExceeded() )
        {
            getLog().info( "File " + file + " was overwritten due to content limit threshold "
                    + outStream.getThreshold() + " reached" );
            return;
        }
        boolean needOverwrite = true;

        if ( file.exists() )
        {
            InputStream is = null;
            try
            {
                is = new FileInputStream( file );
                final InputStream newContents = new ByteArrayInputStream( outStream.getData() );
                needOverwrite = !IOUtil.contentEquals( is, newContents );
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "File " + file + " contents "
                                        + ( needOverwrite ? "differs" : "does not differ" ) );
                }

                is.close();
                is = null;
            }
            finally
            {
                IOUtil.close( is );
            }
        }

        if ( !needOverwrite )
        {
            getLog().info( "File " + file + " is up to date" );
            return;
        }
        getLog().info( "Writing " + file );
        OutputStream os = new FileOutputStream( file );
        try
        {
            outStream.writeTo( os );
            os.close();
            os = null;
        }
        finally
        {
            IOUtil.close( os );
        }
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
            LinkedHashSet<String> delims = new LinkedHashSet<String>();
            if ( useDefaultFilterDelimiters )
            {
                delims.addAll( req.getDelimiters() );
            }

            for ( String delim : filterDelimiters )
            {
                if ( delim == null )
                {
                    delims.add( "${*}" );
                }
                else
                {
                    delims.add( delim );
                }
            }

            req.setDelimiters( delims );
        }

        return req;
    }

    protected void validate()
        throws MojoExecutionException
    {
        int bundleCount = 1;

        for ( String artifactDescriptor : resourceBundles )
        {
            // groupId:artifactId:version, groupId:artifactId:version:type
            // or groupId:artifactId:version:type:classifier
            String[] s = StringUtils.split( artifactDescriptor, ":" );

            if ( s.length < 3 || s.length > 5 )
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
                    + " resource bundle configured must specify a groupId, artifactId, "
                    + " version and, optionally, type and classifier for a remote resource bundle. "
                    + "Must be of the form <resourceBundle>groupId:artifactId:version</resourceBundle>, "
                    + "<resourceBundle>groupId:artifactId:version:type</resourceBundle> or "
                    + "<resourceBundle>groupId:artifactId:version:type:classifier</resourceBundle>" );
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
        List<MavenProject> projects = getProjects();
        context.put( "projects", projects );
        context.put( "projectsSortedByOrganization", getProjectsSortedByOrganization( projects ) );
        context.put( "presentYear", year );
        context.put( "locator", locator );

        if ( inceptionYear.equals( year ) )
        {
            context.put( "projectTimespan", year );
        }
        else
        {
            context.put( "projectTimespan", inceptionYear + "-" + year );
        }
    }

    private List<File> downloadBundles( List<String> bundles )
        throws MojoExecutionException
    {
        List<File> bundleArtifacts = new ArrayList<File>();

        try
        {
            for ( String artifactDescriptor : bundles )
            {
                // groupId:artifactId:version[:type[:classifier]]
                String[] s = artifactDescriptor.split( ":" );

                File artifactFile = null;
                // check if the artifact is part of the reactor
                if ( mavenSession != null )
                {
                    List<MavenProject> list = mavenSession.getSortedProjects();
                    for ( MavenProject p : list )
                    {
                        if ( s[0].equals( p.getGroupId() ) && s[1].equals( p.getArtifactId() )
                            && s[2].equals( p.getVersion() ) )
                        {
                            if ( s.length >= 4 && "test-jar".equals( s[3] ) )
                            {
                                artifactFile = new File( p.getBuild().getTestOutputDirectory() );
                            }
                            else
                            {
                                artifactFile = new File( p.getBuild().getOutputDirectory() );
                            }
                        }
                    }
                }
                if ( artifactFile == null || !artifactFile.exists() )
                {
                    String type = ( s.length >= 4 ? s[3] : "jar" );
                    String classifier = ( s.length == 5 ? s[4] : null );
                    Artifact artifact =
                        artifactFactory.createDependencyArtifact( s[0], s[1], VersionRange.createFromVersion( s[2] ),
                                                                  type, classifier, Artifact.SCOPE_RUNTIME );

                    artifactResolver.resolve( artifact, remoteArtifactRepositories, localRepository );

                    artifactFile = artifact.getFile();
                }
                bundleArtifacts.add( artifactFile );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Error downloading resources archive.", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Resources archive cannot be found.", e );
        }

        return bundleArtifacts;
    }

    private void initalizeClassloader( RemoteResourcesClassLoader cl, List<File> artifacts )
        throws MojoExecutionException
    {
        try
        {
            for ( File artifact : artifacts )
            {
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
        try
        {
            // CHECKSTYLE_OFF: LineLength
            for ( Enumeration<URL> e = classLoader.getResources( BundleRemoteResourcesMojo.RESOURCES_MANIFEST ); e.hasMoreElements(); )
            {
                URL url = e.nextElement();

                InputStream in = null;
                OutputStream out = null;
                Reader reader = null;
                Writer writer = null;
                try
                {
                    reader = new InputStreamReader( url.openStream() );

                    RemoteResourcesBundleXpp3Reader bundleReader = new RemoteResourcesBundleXpp3Reader();

                    RemoteResourcesBundle bundle = bundleReader.read( reader );

                    reader.close();
                    reader = null;

                    for ( String bundleResource : bundle.getRemoteResources() )
                    {
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
                                DeferredFileOutputStream os =
                                    new DeferredFileOutputStream( velocityFilterInMemoryThreshold, f );

                                writer = bundle.getSourceEncoding() == null
                                             ? new OutputStreamWriter( os )
                                             : new OutputStreamWriter( os, bundle.getSourceEncoding() );

                                if ( bundle.getSourceEncoding() == null )
                                {
                                    // TODO: Is this correct? Shouldn't we behave like the rest of maven and fail
                                    // down to JVM default instead ISO-8859-1 ?
                                    velocity.mergeTemplate( bundleResource, "ISO-8859-1", context, writer );
                                }
                                else
                                {
                                    velocity.mergeTemplate( bundleResource, bundle.getSourceEncoding(), context,
                                                            writer );

                                }

                                writer.close();
                                writer = null;
                                fileWriteIfDiffers( os );
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
                            File appendedVmResourceFile =
                                new File( appendedResourcesDirectory, projectResource + ".vm" );

                            if ( appendedResourceFile.exists() )
                            {
                                in = new FileInputStream( appendedResourceFile );
                                out = new FileOutputStream( f, true );
                                IOUtil.copy( in, out );
                                out.close();
                                out = null;
                                in.close();
                                in = null;
                            }
                            else if ( appendedVmResourceFile.exists() )
                            {
                                reader = new FileReader( appendedVmResourceFile );

                                if ( bundle.getSourceEncoding() == null )
                                {
                                    writer = new PrintWriter( new FileWriter( f, true ) );
                                }
                                else
                                {
                                    writer =
                                        new PrintWriter( new OutputStreamWriter( new FileOutputStream( f, true ),
                                                                                 bundle.getSourceEncoding() ) );
                                }

                                Velocity.init();
                                Velocity.evaluate( context, writer, "remote-resources", reader );
                                writer.close();
                                writer = null;
                                reader.close();
                                reader = null;
                            }
                        }
                    }
                }
                finally
                {
                    IOUtil.close( out );
                    IOUtil.close( in );
                    IOUtil.close( writer );
                    IOUtil.close( reader );
                }
                // CHECKSTYLE_ON: LineLength
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
                throw new MojoExecutionException( "Supplemental project XML "
                    + "requires that a <groupId> element be present." );
            }

            if ( artifactId == null || artifactId.trim().equals( "" ) )
            {
                throw new MojoExecutionException( "Supplemental project XML "
                    + "requires that a <artifactId> element be present." );
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

    private Map<String, Model> loadSupplements( String models[] )
        throws MojoExecutionException
    {
        if ( models == null )
        {
            getLog().debug( "Supplemental data models won't be loaded.  " + "No models specified." );
            return Collections.emptyMap();
        }

        List<Supplement> supplements = new ArrayList<Supplement>();
        for ( String set : models )
        {
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
                    throw new MojoExecutionException( "Supplemental data models won't be loaded. " + "File "
                        + f.getAbsolutePath() + " cannot be read, check permissions on the file." );
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

        Map<String, Model> supplementMap = new HashMap<String, Model>();
        for ( Supplement sd : supplements )
        {
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
        implements Comparator<Organization>
    {
        public int compare( Organization org1, Organization org2 )
        {
            int i = compareStrings( org1.getName(), org2.getName() );
            if ( i == 0 )
            {
                i = compareStrings( org1.getUrl(), org2.getUrl() );
            }
            return i;
        }

        public boolean equals( Organization o1, Organization o2 )
        {
            return compare( o1, o2 ) == 0;
        }

        private int compareStrings( String s1, String s2 )
        {
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
        implements Comparator<MavenProject>
    {
        public int compare( MavenProject p1, MavenProject p2 )
        {
            return p1.getArtifact().compareTo( p2.getArtifact() );
        }

        public boolean equals( MavenProject p1, MavenProject p2 )
        {
            return p1.getArtifact().equals( p2.getArtifact() );
        }
    }

    /* LogChute methods */
    public void init( RuntimeServices rs )
        throws Exception
    {
    }

    public void log( int level, String message )
    {
        switch ( level )
        {
            case LogChute.WARN_ID:
                getLog().warn( message );
                break;
            case LogChute.INFO_ID:
                // velocity info messages are too verbose, just consider them as debug messages...
                getLog().debug( message );
                break;
            case LogChute.DEBUG_ID:
                getLog().debug( message );
                break;
            case LogChute.ERROR_ID:
                getLog().error( message );
                break;
            default:
                getLog().debug( message );
                break;
        }
    }

    public void log( int level, String message, Throwable t )
    {
        switch ( level )
        {
            case LogChute.WARN_ID:
                getLog().warn( message, t );
                break;
            case LogChute.INFO_ID:
                // velocity info messages are too verbose, just consider them as debug messages...
                getLog().debug( message, t );
                break;
            case LogChute.DEBUG_ID:
                getLog().debug( message, t );
                break;
            case LogChute.ERROR_ID:
                getLog().error( message, t );
                break;
            default:
                getLog().debug( message, t );
                break;
        }
    }

    public boolean isLevelEnabled( int level )
    {
        return false;
    }

}
