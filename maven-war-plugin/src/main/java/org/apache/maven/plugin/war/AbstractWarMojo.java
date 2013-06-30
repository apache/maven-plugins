package org.apache.maven.plugin.war;

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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.war.overlay.OverlayManager;
import org.apache.maven.plugin.war.packaging.DependenciesAnalysisPackagingTask;
import org.apache.maven.plugin.war.packaging.OverlayPackagingTask;
import org.apache.maven.plugin.war.packaging.SaveWebappStructurePostPackagingTask;
import org.apache.maven.plugin.war.packaging.WarPackagingContext;
import org.apache.maven.plugin.war.packaging.WarPackagingTask;
import org.apache.maven.plugin.war.packaging.WarPostPackagingTask;
import org.apache.maven.plugin.war.packaging.WarProjectPackagingTask;
import org.apache.maven.plugin.war.util.WebappStructure;
import org.apache.maven.plugin.war.util.WebappStructureSerializer;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.AbstractMavenFilteringRequest;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Contains common jobs for WAR mojos.
 *
 * @version $Id$
 */
public abstract class AbstractWarMojo
    extends AbstractMojo
{
    public static final String DEFAULT_FILE_NAME_MAPPING = "@{artifactId}@-@{version}@.@{extension}@";

    public static final String DEFAULT_FILE_NAME_MAPPING_CLASSIFIER =
        "@{artifactId}@-@{version}@-@{classifier}@.@{extension}@";

    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final String META_INF = "META-INF";

    private static final String WEB_INF = "WEB-INF";

    /**
     * The Maven project.
     */
    @Component
    private MavenProject project;

    /**
     * The directory containing compiled classes.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true, readonly = true )
    private File classesDirectory;

    /**
     * Whether a JAR file will be created for the classes in the webapp. Using this optional configuration
     * parameter will make the compiled classes to be archived into a JAR file
     * and the classes directory will then be excluded from the webapp.
     *
     * @since 2.0.1
     */
    @Parameter( property = "archiveClasses", defaultValue = "false" )
    private boolean archiveClasses;

    /**
     * The encoding to use when copying filtered web resources.
     *
     * @since 2.3
     */
    @Parameter( property = "resourceEncoding", defaultValue = "${project.build.sourceEncoding}" )
    private String resourceEncoding;

    /**
     * The JAR archiver needed for archiving the classes directory into a JAR file under WEB-INF/lib.
     */
    @Component( role = Archiver.class, hint = "jar" )
    private JarArchiver jarArchiver;

    /**
     * The directory where the webapp is built.
     */
    @Parameter( defaultValue = "${project.build.directory}/${project.build.finalName}", required = true )
    private File webappDirectory;

    /**
     * Single directory for extra files to include in the WAR. This is where
     * you place your JSP files.
     */
    @Parameter( defaultValue = "${basedir}/src/main/webapp", required = true )
    private File warSourceDirectory;

    /**
     * The list of webResources we want to transfer.
     */
    @Parameter
    private Resource[] webResources;

    /**
     * Filters (property files) to include during the interpolation of the pom.xml.
     */
    @Parameter
    private List<String> filters;

    /**
     * The path to the web.xml file to use.
     */
    @Parameter( property = "maven.war.webxml" )
    private File webXml;

    /**
     * The path to a configuration file for the servlet container. Note that
     * the file name may be different for different servlet containers.
     * Apache Tomcat uses a configuration file named context.xml. The file will
     * be copied to the META-INF directory.
     */
    @Parameter( property = "maven.war.containerConfigXML" )
    private File containerConfigXML;

    /**
     * Directory to unpack dependent WARs into if needed.
     */
    @Parameter( defaultValue = "${project.build.directory}/war/work", required = true )
    private File workDirectory;

    /**
     * The file name mapping to use when copying libraries and TLDs. If no file mapping is
     * set (default) the files are copied with their standard names.
     *
     * @since 2.1-alpha-1
     */
    @Parameter
    private String outputFileNameMapping;

    /**
     * The file containing the webapp structure cache.
     *
     * @since 2.1-alpha-1
     */
    @Parameter( defaultValue = "${project.build.directory}/war/work/webapp-cache.xml", required = true )
    private File cacheFile;

    /**
     * Whether the cache should be used to save the status of the webapp
     * across multiple runs. Experimental feature so disabled by default.
     *
     * @since 2.1-alpha-1
     */
    @Parameter( property = "useCache", defaultValue = "false" )
    private boolean useCache = false;

    /**
     */
    @Component( role = ArtifactFactory.class )
    private ArtifactFactory artifactFactory;

    /**
     * To look up Archiver/UnArchiver implementations.
     */
    @Component( role = ArchiverManager.class )
    private ArchiverManager archiverManager;

    /**
     */
    @Component( role = MavenFileFilter.class, hint = "default" )
    private MavenFileFilter mavenFileFilter;

    /**
     */
    @Component( role = MavenResourcesFiltering.class, hint = "default" )
    private MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * The comma separated list of tokens to include when copying the content
     * of the warSourceDirectory.
     */
    @Parameter( alias = "includes", defaultValue = "**" )
    private String warSourceIncludes;

    /**
     * The comma separated list of tokens to exclude when copying the content
     * of the warSourceDirectory.
     */
    @Parameter( alias = "excludes" )
    private String warSourceExcludes;

    /**
     * The comma separated list of tokens to include when doing
     * a WAR overlay.
     * Default is '**'
     *
     * @deprecated Use &lt;overlay&gt;/&lt;includes&gt; instead
     */
    @Parameter
    private String dependentWarIncludes = "**/**";

    /**
     * The comma separated list of tokens to exclude when doing
     * a WAR overlay.
     *
     * @deprecated Use &lt;overlay&gt;/&lt;excludes&gt; instead
     */
    @Parameter
    private String dependentWarExcludes = "META-INF/**";

    /**
     * The overlays to apply.
     *
     * Each &lt;overlay&gt; element may contain:
     * <ul>
     *     <li>id (defaults to <tt>currentBuild</tt>)</li>
     *     <li>groupId (if this and artifactId are null, then the current project is treated as its own overlay)</li>
     *     <li>artifactId (see above)</li>
     *     <li>classifier</li>
     *     <li>type</li>
     *     <li>includes (a list of string patterns)</li>
     *     <li>excludes (a list of string patterns)</li>
     *     <li>filtered (defaults to false)</li>
     *     <li>skip (defaults to false)</li>
     *     <li>targetPath (defaults to root of webapp structure)</li>
     *
     * </ul>
     *
     *
     *
     * @since 2.1-alpha-1
     */
    @Parameter
    private List<Overlay> overlays = new ArrayList<Overlay>();

    /**
     * A list of file extensions that should not be filtered.
     * <b>Will be used when filtering webResources and overlays.</b>
     *
     * @since 2.1-alpha-2
     */
    @Parameter
    private List<String> nonFilteredFileExtensions;

    /**
     * @since 2.1-alpha-2
     */
    @Component
    private MavenSession session;

    /**
     * To filter deployment descriptors. <b>Disabled by default.</b>
     *
     * @since 2.1-alpha-2
     */
    @Parameter( property = "maven.war.filteringDeploymentDescriptors", defaultValue = "false" )
    private boolean filteringDeploymentDescriptors = false;

    /**
     * To escape interpolated values with Windows path
     * <code>c:\foo\bar</code> will be replaced with <code>c:\\foo\\bar</code>.
     *
     * @since 2.1-alpha-2
     */
    @Parameter( property = "maven.war.escapedBackslashesInFilePath", defaultValue = "false" )
    private boolean escapedBackslashesInFilePath = false;

    /**
     * Expression preceded with this String won't be interpolated.
     * <code>\${foo}</code> will be replaced with <code>${foo}</code>.
     *
     * @since 2.1-beta-1
     */
    @Parameter( property = "maven.war.escapeString" )
    protected String escapeString;

    /**
     * Indicates if zip archives (jar,zip etc) being added to the war should be
     * compressed again. Compressing again can result in smaller archive size, but
     * gives noticeably longer execution time.
     *
     * @since 2.3
     */
    @Parameter( defaultValue = "true" )
    private boolean recompressZippedFiles;

    /**
     * Stop searching endToken at the end of line
     * @since 2.4
     */
    @Parameter(property = "maven.war.supportMultiLineFiltering", defaultValue = "false" )
    private boolean supportMultiLineFiltering = false;

    /**
     * use jvmChmod rather that cli chmod and forking process
     * @since 2.4
     */
    @Parameter(property = "maven.war.useJvmChmod", defaultValue = "true" )
    private boolean useJvmChmod;


    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    private final WebappStructureSerializer webappStructureSerialier = new WebappStructureSerializer();

    private final Overlay currentProjectOverlay = Overlay.createInstance();


    public Overlay getCurrentProjectOverlay()
    {
        return currentProjectOverlay;
    }

    /**
     * Returns a string array of the excludes to be used
     * when copying the content of the WAR source directory.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getExcludes()
    {
        List<String> excludeList = new ArrayList<String>();
        if ( StringUtils.isNotEmpty( warSourceExcludes ) )
        {
            excludeList.addAll( Arrays.asList( StringUtils.split( warSourceExcludes, "," ) ) );
        }

        // if webXML is specified, omit the one in the source directory
        if ( webXml != null && StringUtils.isNotEmpty( webXml.getName() ) )
        {
            excludeList.add( "**/" + WEB_INF + "/web.xml" );
        }

        // if contextXML is specified, omit the one in the source directory
        if ( containerConfigXML != null && StringUtils.isNotEmpty( containerConfigXML.getName() ) )
        {
            excludeList.add( "**/" + META_INF + "/" + containerConfigXML.getName() );
        }

        return (String[]) excludeList.toArray( EMPTY_STRING_ARRAY );
    }

    /**
     * Returns a string array of the includes to be used
     * when assembling/copying the WAR.
     *
     * @return an array of tokens to include
     */
    protected String[] getIncludes()
    {
        return StringUtils.split( StringUtils.defaultString( warSourceIncludes ), "," );
    }

    /**
     * Returns a string array of the excludes to be used
     * when adding dependent WAR as an overlay onto this WAR.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getDependentWarExcludes()
    {
        String[] excludes;
        if ( StringUtils.isNotEmpty( dependentWarExcludes ) )
        {
            excludes = StringUtils.split( dependentWarExcludes, "," );
        }
        else
        {
            excludes = EMPTY_STRING_ARRAY;
        }
        return excludes;
    }

    /**
     * Returns a string array of the includes to be used
     * when adding dependent WARs as an overlay onto this WAR.
     *
     * @return an array of tokens to include
     */
    protected String[] getDependentWarIncludes()
    {
        return StringUtils.split( StringUtils.defaultString( dependentWarIncludes ), "," );
    }

    public void buildExplodedWebapp( File webappDirectory )
        throws MojoExecutionException, MojoFailureException
    {
        webappDirectory.mkdirs();

        try
        {
            buildWebapp( project, webappDirectory );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not build webapp", e );
        }
    }


    /**
     * Builds the webapp for the specified project with the new packaging task
     * thingy
     * <p/>
     * Classes, libraries and tld files are copied to
     * the <tt>webappDirectory</tt> during this phase.
     *
     * @param project         the maven project
     * @param webappDirectory the target directory
     * @throws MojoExecutionException if an error occurred while packaging the webapp
     * @throws MojoFailureException   if an unexpected error occurred while packaging the webapp
     * @throws IOException            if an error occurred while copying the files
     */
    @SuppressWarnings( "unchecked" )
    public void buildWebapp( MavenProject project, File webappDirectory )
        throws MojoExecutionException, MojoFailureException, IOException
    {

        WebappStructure cache;
        if ( useCache && cacheFile.exists() )
        {
            cache = new WebappStructure( project.getDependencies(), webappStructureSerialier.fromXml( cacheFile ) );
        }
        else
        {
            cache = new WebappStructure( project.getDependencies(), null );
        }

        final long startTime = System.currentTimeMillis();
        getLog().info( "Assembling webapp [" + project.getArtifactId() + "] in [" + webappDirectory + "]" );

        final OverlayManager overlayManager =
            new OverlayManager( overlays, project, dependentWarIncludes, dependentWarExcludes, currentProjectOverlay );
        final List<WarPackagingTask> packagingTasks = getPackagingTasks( overlayManager );
        List<FileUtils.FilterWrapper> defaultFilterWrappers = null;
        try
        {
            MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
            mavenResourcesExecution.setEscapeString( escapeString );
            mavenResourcesExecution.setSupportMultiLineFiltering( supportMultiLineFiltering );
            mavenResourcesExecution.setMavenProject( project );
            mavenResourcesExecution.setFilters( filters );
            mavenResourcesExecution.setEscapedBackslashesInFilePath( escapedBackslashesInFilePath );
            mavenResourcesExecution.setMavenSession( this.session );
            mavenResourcesExecution.setEscapeString( this.escapeString );
            mavenResourcesExecution.setSupportMultiLineFiltering( supportMultiLineFiltering );

            defaultFilterWrappers = mavenFileFilter.getDefaultFilterWrappers( mavenResourcesExecution );

        }
        catch ( MavenFilteringException e )
        {
            getLog().error( "fail to build filering wrappers " + e.getMessage() );
            throw new MojoExecutionException( e.getMessage(), e );
        }

        final WarPackagingContext context = new DefaultWarPackagingContext( webappDirectory, cache, overlayManager,
                                                                            defaultFilterWrappers,
                                                                            getNonFilteredFileExtensions(),
                                                                            filteringDeploymentDescriptors,
                                                                            this.artifactFactory, resourceEncoding,
                                                                            useJvmChmod);
        for ( WarPackagingTask warPackagingTask : packagingTasks )
        {
            warPackagingTask.performPackaging( context );
        }

        // Post packaging
        final List<WarPostPackagingTask> postPackagingTasks = getPostPackagingTasks();
        for( WarPostPackagingTask task  : postPackagingTasks )
        {
            task.performPostPackaging( context );
        }
        getLog().info( "Webapp assembled in [" + ( System.currentTimeMillis() - startTime ) + " msecs]" );

    }

    /**
     * Returns a <tt>List</tt> of the {@link org.apache.maven.plugin.war.packaging.WarPackagingTask}
     * instances to invoke to perform the packaging.
     *
     * @param overlayManager the overlay manager
     * @return the list of packaging tasks
     * @throws MojoExecutionException if the packaging tasks could not be built
     */
    private List<WarPackagingTask> getPackagingTasks( OverlayManager overlayManager )
        throws MojoExecutionException
    {
        final List<WarPackagingTask> packagingTasks = new ArrayList<WarPackagingTask>();
        if ( useCache )
        {
            packagingTasks.add( new DependenciesAnalysisPackagingTask() );
        }

        final List<Overlay> resolvedOverlays = overlayManager.getOverlays();
        for ( Overlay overlay : resolvedOverlays )
        {
            if ( overlay.isCurrentProject() )
            {
                packagingTasks.add( new WarProjectPackagingTask( webResources, webXml, containerConfigXML,
                                                                 currentProjectOverlay ) );
            }
            else
            {
                packagingTasks.add( new OverlayPackagingTask( overlay, currentProjectOverlay ) );
            }
        }
        return packagingTasks;
    }


    /**
     * Returns a <tt>List</tt> of the {@link org.apache.maven.plugin.war.packaging.WarPostPackagingTask}
     * instances to invoke to perform the post-packaging.
     *
     * @return the list of post packaging tasks
     */
    private List<WarPostPackagingTask> getPostPackagingTasks()
    {
        final List<WarPostPackagingTask> postPackagingTasks = new ArrayList<WarPostPackagingTask>();
        if ( useCache )
        {
            postPackagingTasks.add( new SaveWebappStructurePostPackagingTask( cacheFile ) );
        }
        // TODO add lib scanning to detect duplicates
        return postPackagingTasks;
    }

    /**
     * WarPackagingContext default implementation
     */
    private class DefaultWarPackagingContext
        implements WarPackagingContext
    {

        private final ArtifactFactory artifactFactory;

        private final String resourceEncoding;

        private final WebappStructure webappStructure;

        private final File webappDirectory;

        private final OverlayManager overlayManager;

        private final List<FileUtils.FilterWrapper> filterWrappers;

        private List<String> nonFilteredFileExtensions;

        private boolean filteringDeploymentDescriptors;

        private boolean useJvmChmod = true;

        public DefaultWarPackagingContext( File webappDirectory, final WebappStructure webappStructure,
                                           final OverlayManager overlayManager, List<FileUtils.FilterWrapper> filterWrappers,
                                           List<String> nonFilteredFileExtensions, boolean filteringDeploymentDescriptors,
                                           ArtifactFactory artifactFactory, String resourceEncoding, boolean useJvmChmod )
        {
            this.webappDirectory = webappDirectory;
            this.webappStructure = webappStructure;
            this.overlayManager = overlayManager;
            this.filterWrappers = filterWrappers;
            this.artifactFactory = artifactFactory;
            this.filteringDeploymentDescriptors = filteringDeploymentDescriptors;
            this.nonFilteredFileExtensions = nonFilteredFileExtensions == null ? Collections.<String>emptyList()
                                                                              : nonFilteredFileExtensions;
            this.resourceEncoding = resourceEncoding;
            // This is kinda stupid but if we loop over the current overlays and we request the path structure
            // it will register it. This will avoid wrong warning messages in a later phase
            for ( String overlayId : overlayManager.getOverlayIds() )
            {
                webappStructure.getStructure( overlayId );
            }
            this.useJvmChmod = useJvmChmod;
        }

        public MavenProject getProject()
        {
            return project;
        }

        public File getWebappDirectory()
        {
            return webappDirectory;
        }

        public File getClassesDirectory()
        {
            return classesDirectory;
        }

        public Log getLog()
        {
            return AbstractWarMojo.this.getLog();
        }

        public String getOutputFileNameMapping()
        {
            return outputFileNameMapping;
        }

        public File getWebappSourceDirectory()
        {
            return warSourceDirectory;
        }

        public String[] getWebappSourceIncludes()
        {
            return getIncludes();
        }

        public String[] getWebappSourceExcludes()
        {
            return getExcludes();
        }

        public boolean archiveClasses()
        {
            return archiveClasses;
        }

        public File getOverlaysWorkDirectory()
        {
            return workDirectory;
        }

        public ArchiverManager getArchiverManager()
        {
            return archiverManager;
        }

        public MavenArchiveConfiguration getArchive()
        {
            return archive;
        }

        public JarArchiver getJarArchiver()
        {
            return jarArchiver;
        }

        public List<String> getFilters()
        {
            return filters;
        }

        public WebappStructure getWebappStructure()
        {
            return webappStructure;
        }

        public List<String> getOwnerIds()
        {
            return overlayManager.getOverlayIds();
        }

        public MavenFileFilter getMavenFileFilter()
        {
            return mavenFileFilter;
        }

        public List<FileUtils.FilterWrapper> getFilterWrappers()
        {
            return filterWrappers;
        }

        public boolean isNonFilteredExtension( String fileName )
        {
            return !mavenResourcesFiltering.filteredFileExtension( fileName, nonFilteredFileExtensions );
        }

        public boolean isFilteringDeploymentDescriptors()
        {
            return filteringDeploymentDescriptors;
        }

        public ArtifactFactory getArtifactFactory()
        {
            return this.artifactFactory;
        }

        public MavenSession getSession()
        {
            return session;
        }

        public String getResourceEncoding()
        {
            return resourceEncoding;
        }

        public boolean isUseJvmChmod()
        {
            return useJvmChmod;
        }
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public File getClassesDirectory()
    {
        return classesDirectory;
    }

    public void setClassesDirectory( File classesDirectory )
    {
        this.classesDirectory = classesDirectory;
    }

    public File getWebappDirectory()
    {
        return webappDirectory;
    }

    public void setWebappDirectory( File webappDirectory )
    {
        this.webappDirectory = webappDirectory;
    }

    public File getWarSourceDirectory()
    {
        return warSourceDirectory;
    }

    public void setWarSourceDirectory( File warSourceDirectory )
    {
        this.warSourceDirectory = warSourceDirectory;
    }

    public File getWebXml()
    {
        return webXml;
    }

    public void setWebXml( File webXml )
    {
        this.webXml = webXml;
    }

    public File getContainerConfigXML()
    {
        return containerConfigXML;
    }

    public void setContainerConfigXML( File containerConfigXML )
    {
        this.containerConfigXML = containerConfigXML;
    }

    public String getOutputFileNameMapping()
    {
        return outputFileNameMapping;
    }

    public void setOutputFileNameMapping( String outputFileNameMapping )
    {
        this.outputFileNameMapping = outputFileNameMapping;
    }

    public List<Overlay> getOverlays()
    {
        return overlays;
    }

    public void setOverlays( List<Overlay> overlays )
    {
        this.overlays = overlays;
    }

    public void addOverlay( Overlay overlay )
    {
        overlays.add( overlay );
    }

    public boolean isArchiveClasses()
    {
        return archiveClasses;
    }

    public void setArchiveClasses( boolean archiveClasses )
    {
        this.archiveClasses = archiveClasses;
    }

    public JarArchiver getJarArchiver()
    {
        return jarArchiver;
    }

    public void setJarArchiver( JarArchiver jarArchiver )
    {
        this.jarArchiver = jarArchiver;
    }

    public Resource[] getWebResources()
    {
        return webResources;
    }

    public void setWebResources( Resource[] webResources )
    {
        this.webResources = webResources;
    }

    public List<String> getFilters()
    {
        return filters;
    }

    public void setFilters( List<String> filters )
    {
        this.filters = filters;
    }

    public File getWorkDirectory()
    {
        return workDirectory;
    }

    public void setWorkDirectory( File workDirectory )
    {
        this.workDirectory = workDirectory;
    }

    public File getCacheFile()
    {
        return cacheFile;
    }

    public void setCacheFile( File cacheFile )
    {
        this.cacheFile = cacheFile;
    }

    public String getWarSourceIncludes()
    {
        return warSourceIncludes;
    }

    public void setWarSourceIncludes( String warSourceIncludes )
    {
        this.warSourceIncludes = warSourceIncludes;
    }

    public String getWarSourceExcludes()
    {
        return warSourceExcludes;
    }

    public void setWarSourceExcludes( String warSourceExcludes )
    {
        this.warSourceExcludes = warSourceExcludes;
    }


    public boolean isUseCache()
    {
        return useCache;
    }

    public void setUseCache( boolean useCache )
    {
        this.useCache = useCache;
    }

    public MavenArchiveConfiguration getArchive()
    {
        return archive;
    }

    public List<String> getNonFilteredFileExtensions()
    {
        return nonFilteredFileExtensions;
    }

    public void setNonFilteredFileExtensions( List<String> nonFilteredFileExtensions )
    {
        this.nonFilteredFileExtensions = nonFilteredFileExtensions;
    }

    public ArtifactFactory getArtifactFactory()
    {
        return this.artifactFactory;
    }

    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }
    
    protected MavenSession getSession()
    {
        return this.session;
    }

    protected boolean isRecompressZippedFiles()
    {
        return recompressZippedFiles;
    }
}
