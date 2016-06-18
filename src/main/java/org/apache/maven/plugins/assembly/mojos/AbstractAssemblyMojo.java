package org.apache.maven.plugins.assembly.mojos;

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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.io.AssemblyReadException;
import org.apache.maven.plugins.assembly.io.AssemblyReader;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugins.assembly.utils.InterpolationConstants;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenReaderFilter;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.fixed.PropertiesBasedValueSource;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @threadSafe
 */
public abstract class AbstractAssemblyMojo
    extends AbstractMojo
    implements AssemblerConfigurationSource
{
    protected FixedStringSearchInterpolator commanndLinePropertiesInterpolator;

    protected FixedStringSearchInterpolator envInterpolator;

    protected FixedStringSearchInterpolator mainProjectInterpolator;

    protected FixedStringSearchInterpolator rootInterpolator;

    /**
     * Set to false to exclude the assembly id from the assembly final name.
     */
    @Parameter( property = "assembly.appendAssemblyId", defaultValue = "true" )
    boolean appendAssemblyId;

    /**
     * The character encoding scheme to be applied when filtering resources.
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

    /**
     * Expressions preceded with this String won't be interpolated.
     * If you use "\" as the escape string then \${foo} will be replaced with ${foo}.
     *
     * @since 2.4
     */
    @Parameter( property = "assembly.escapeString" )
    private String escapeString;

    /**
     * Flag allowing one or more executions of the assembly plugin to be configured as skipped for a particular build.
     * This makes the assembly plugin more controllable from profiles.
     */
    @Parameter( property = "assembly.skipAssembly", defaultValue = "false" )
    private boolean skipAssembly;

    /**
     * If this flag is set, everything up to the call to Archiver.createArchive() will be executed.
     */
    @Parameter( property = "assembly.dryRun", defaultValue = "false" )
    private boolean dryRun;

    /**
     * If this flag is set, the ".dir" suffix will be suppressed in the output directory name when using assembly/format
     * == 'dir' and other formats that begin with 'dir'. <br/>
     * <b>NOTE:</b> Since 2.2-beta-3, the default-value for this is true, NOT false as it used to be.
     */
    @Parameter( defaultValue = "true" )
    private boolean ignoreDirFormatExtensions;

    /**
     * Local Maven repository where artifacts are cached during the build process.
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;

    /**
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true )
    private List<ArtifactRepository> remoteRepositories;

    /**
     * Contains the full list of projects in the reactor.
     */
    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> reactorProjects;

    /**
     * The output directory of the assembled distribution file.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true )
    private File outputDirectory;

    /**
     * The filename of the assembled distribution file.
     */
    @Parameter( defaultValue = "${project.build.finalName}", required = true, readonly = true )
    private String finalName;

    /**
     * Directory to unpack JARs into if needed
     */
    @Parameter( defaultValue = "${project.build.directory}/assembly/work", required = true )
    private File workDirectory;

    /**
     * Specifies the formats of the assembly.
     * Multiple formats can be supplied and the Assembly Plugin will generate an archive for each desired formats.
     * When deploying your project, all file formats specified will also be deployed. A format is specified by supplying
     * one of the following
     * values in a &lt;format&gt; subelement:
     * <ul>
     * <li><em>dir</em> - Creates a directory</li>
     * <li><em>zip</em> - Creates a ZIP file format</li>
     * <li><em>tar</em> - Creates a TAR format</li>
     * <li><em>tar.gz</em> or <em>tgz</em> - Creates a gzip'd TAR format</li>
     * <li><em>tar.bz2</em> or <em>tbz2</em> - Creates a bzip'd TAR format</li>
     * </ul>
     */
    @Parameter
    private List<String> formats;

    /**
     * A list of descriptor files to generate from.
     */
    @Parameter
    private String[] descriptors;

    /**
     * A list of references to assembly descriptors available on the plugin's classpath. The default classpath
     * includes these built-in descriptors: <code>bin</code>, <code>jar-with-dependencies</code>, <code>src</code>, and
     * <code>project</code>. You can add others by adding dependencies
     * to the plugin.
     */
    @Parameter
    private String[] descriptorRefs;

    /**
     * Directory to scan for descriptor files in. <b>NOTE:</b> This may not work correctly with assembly components.
     */
    @Parameter
    private File descriptorSourceDirectory;

    /**
     * This is the base directory from which archive files are created. This base directory pre-pended to any
     * <code>&lt;directory&gt;</code> specifications in the assembly descriptor. This is an optional parameter.
     */
    @Parameter
    private File archiveBaseDirectory;

    /**
     * Sets the TarArchiver behavior on file paths with more than 100 characters length. Valid values are: "warn"
     * (default), "fail", "truncate", "gnu", "posix", "posix_warn" or "omit".
     */
    @Parameter( property = "assembly.tarLongFileMode", defaultValue = "warn" )
    private String tarLongFileMode;

    /**
     * Base directory of the project.
     */
    @Parameter( defaultValue = "${project.basedir}", required = true, readonly = true )
    private File basedir;

    /**
     * Maven ProjectHelper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Maven shared filtering utility.
     */
    @Component
    private MavenReaderFilter mavenReaderFilter;

    /**
     * The Maven Session Object
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession mavenSession;

    /**
     * Temporary directory that contain the files to be assembled.
     */
    @Parameter( defaultValue = "${project.build.directory}/archive-tmp", required = true, readonly = true )
    private File tempRoot;

    /**
     * Directory for site generated.
     */
    @Parameter( defaultValue = "${project.reporting.outputDirectory}", readonly = true )
    private File siteDirectory;

    /**
     * Set to true in order to not fail when a descriptor is missing.
     */
    @Parameter( property = "assembly.ignoreMissingDescriptor", defaultValue = "false" )
    private boolean ignoreMissingDescriptor;

    /**
     * This is a set of instructions to the archive builder, especially for building .jar files. It enables you to
     * specify a Manifest file for the jar, in addition to other options.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     */
    @Parameter
    private MavenArchiveConfiguration archive;

    /**
     * The list of extra filter properties files to be used along with System properties, project
     * properties, and filter properties files specified in the POM build/filters section, which
     * should be used for the filtering during the current mojo execution. <br/>
     * Normally, these will be configured from a plugin's execution section, to provide a different
     * set of filters for a particular execution.
     */
    @Parameter
    private List<String> filters;

    /**
     * If True (default) then the ${project.build.filters} are also used in addition to any
     * further filters defined for the Assembly.
     *
     * @since 2.4.2
     */
    @Parameter( property = "assembly.includeProjectBuildFilters", defaultValue = "true" )
    private boolean includeProjectBuildFilters;

    /**
     * Controls whether the assembly plugin tries to attach the resulting assembly to the project.
     *
     * @since 2.2-beta-1
     */
    @Parameter( property = "assembly.attach", defaultValue = "true" )
    private boolean attach;

    /**
     * Indicates if zip archives (jar,zip etc) being added to the assembly should be compressed again.
     * Compressing again can result in smaller archive size, but gives noticeably longer execution time.
     *
     * @since 2.4
     */
    @Parameter( defaultValue = "true" )
    private boolean recompressZippedFiles;

    /**
     * sets the merge manifest mode in the JarArchiver
     * @since 3
     */
    @Parameter
    private String mergeManifestMode;

    /**
     */
    @Component
    private AssemblyArchiver assemblyArchiver;

    /**
     */
    @Component
    private AssemblyReader assemblyReader;

    /**
     * Allows additional configuration options that are specific to a particular type of archive format. This is
     * intended to capture an XML configuration that will be used to reflectively setup the options on the archiver
     * instance. <br/>
     * For instance, to direct an assembly with the "ear" format to use a particular deployment descriptor, you should
     * specify the following for the archiverConfig value in your plugin configuration: <br/>
     * <p/>
     * <p/>
     * <pre>
     * &lt;appxml&gt;${project.basedir}/somepath/app.xml&lt;/appxml&gt;
     * </pre>
     *
     * @since 2.2-beta-3
     */
    @Parameter
    private PlexusConfiguration archiverConfig;

    /**
     * This will cause the assembly to run only at the top of a given module tree. That is, run in the project contained
     * in the same folder where the mvn execution was launched.
     *
     * @since 2.2-beta-4
     */
    @Parameter( property = "assembly.runOnlyAtExecutionRoot", defaultValue = "false" )
    private boolean runOnlyAtExecutionRoot;

    /**
     * This will cause the assembly to only update an existing archive, if it exists.
     * <p>
     * <strong>Note:</strong> The property that can be used on the command line was misspelled as "assembly.updatOnly"
     * in versions prior to version 2.4.
     * </p>
     *
     * @since 2.2
     */
    @Parameter( property = "assembly.updateOnly", defaultValue = "false" )
    private boolean updateOnly;

    /**
     * <p>
     * will use the jvm chmod, this is available for user and all level group level will be ignored As of
     * assembly-plugin 2.5, this flag is ignored for users of java7+
     * </p>
     *
     * @since 2.2
     */
    @Parameter( property = "assembly.useJvmChmod", defaultValue = "false" )
    private boolean useJvmChmod;

    /**
     * <p>
     * Set to <code>true</code> in order to avoid all chmod calls.
     * </p>
     * <p/>
     * <p>
     * <b>NOTE:</b> This will cause the assembly plugin to <b>DISREGARD</b> all fileMode/directoryMode settings in the
     * assembly descriptor, and all file permissions in unpacked dependencies!
     * </p>
     *
     * @since 2.2
     */
    @Parameter( property = "assembly.ignorePermissions", defaultValue = "false" )
    private boolean ignorePermissions;

    /**
     * <p>
     * Set of delimiters for expressions to filter within the resources. These delimiters are specified in the form
     * 'beginToken*endToken'. If no '*' is given, the delimiter is assumed to be the same for start and end.
     * </p>
     * <p>
     * So, the default filtering delimiters might be specified as:
     * </p>
     * <p/>
     * <pre>
     * &lt;delimiters&gt;
     *   &lt;delimiter&gt;${*}&lt;/delimiter&gt;
     *   &lt;delimiter&gt;@&lt;/delimiter&gt;
     * &lt;/delimiters&gt;
     * </pre>
     * <p>
     * Since the '@' delimiter is the same on both ends, we don't need to specify '@*@' (though we can).
     * </p>
     *
     * @since 2.4
     */
    @Parameter
    private List<String> delimiters;

    public static FixedStringSearchInterpolator mainProjectInterpolator( MavenProject mainProject )
    {
        if ( mainProject != null )
        {
            // 5
            return FixedStringSearchInterpolator.create(
                new org.codehaus.plexus.interpolation.fixed.PrefixedObjectValueSource(
                    InterpolationConstants.PROJECT_PREFIXES, mainProject, true ),

                // 6
                new org.codehaus.plexus.interpolation.fixed.PrefixedPropertiesValueSource(
                    InterpolationConstants.PROJECT_PROPERTIES_PREFIXES, mainProject.getProperties(), true ) );
        }
        else
        {
            return FixedStringSearchInterpolator.empty();
        }
    }

    /**
     * Create the binary distribution.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( skipAssembly )
        {
            getLog().info( "Assemblies have been skipped per configuration of the skipAssembly parameter." );
            return;
        }

        // run only at the execution root.
        if ( runOnlyAtExecutionRoot && !isThisTheExecutionRoot() )
        {
            getLog().info( "Skipping the assembly in this project because it's not the Execution Root" );
            return;
        }

        List<Assembly> assemblies;
        try
        {
            assemblies = assemblyReader.readAssemblies( this );
        }
        catch ( final AssemblyReadException e )
        {
            throw new MojoExecutionException( "Error reading assemblies: " + e.getMessage(), e );
        }
        catch ( final InvalidAssemblerConfigurationException e )
        {
            throw new MojoFailureException( assemblyReader, e.getMessage(),
                                            "Mojo configuration is invalid: " + e.getMessage() );
        }

        // TODO: include dependencies marked for distribution under certain formats
        // TODO: how, might we plug this into an installer, such as NSIS?

        boolean warnedAboutMainProjectArtifact = false;
        for ( final Assembly assembly : assemblies )
        {
            try
            {
                final String fullName = AssemblyFormatUtils.getDistributionName( assembly, this );

                List<String> effectiveFormats = formats;
                if ( effectiveFormats == null || effectiveFormats.size() == 0 )
                {
                    effectiveFormats = assembly.getFormats();
                }
                if ( effectiveFormats == null || effectiveFormats.size() == 0 )
                {
                    throw new MojoFailureException(
                        "No formats specified in the execution parameters or the assembly descriptor." );
                }

                for ( final String format : effectiveFormats )
                {
                    final File destFile =
                        assemblyArchiver.createArchive( assembly, fullName, format,
                            this, isRecompressZippedFiles(), getMergeManifestMode() );

                    final MavenProject project = getProject();
                    final String type = project.getArtifact().getType();

                    if ( attach && destFile.isFile() )
                    {
                        if ( isAssemblyIdAppended() )
                        {
                            projectHelper.attachArtifact( project, format, assembly.getId(), destFile );
                        }
                        else if ( !"pom".equals( type ) && format.equals( type ) )
                        {
                            if ( !warnedAboutMainProjectArtifact )
                            {
                                final StringBuilder message = new StringBuilder();

                                message.append( "Configuration options: 'appendAssemblyId' is set to false, "
                                                    + "and 'classifier' is missing." );
                                message.append( "\nInstead of attaching the assembly file: " ).append( destFile );
                                message.append( ", it will become the file for main project artifact." );
                                message.append( "\nNOTE: If multiple descriptors or descriptor-formats are provided "
                                                    + "for this project, the value of this file will be "
                                                    + "non-deterministic!" );

                                getLog().warn( message );
                                warnedAboutMainProjectArtifact = true;
                            }

                            final File existingFile = project.getArtifact().getFile();
                            if ( ( existingFile != null ) && existingFile.exists() )
                            {
                                getLog().warn( "Replacing pre-existing project main-artifact file: " + existingFile
                                                   + "\nwith assembly file: " + destFile );
                            }

                            project.getArtifact().setFile( destFile );
                        }
                        else
                        {
                            projectHelper.attachArtifact( project, format, null, destFile );
                        }
                    }
                    else if ( attach )
                    {
                        getLog().warn( "Assembly file: " + destFile + " is not a regular file (it may be a directory). "
                                           + "It cannot be attached to the project build for installation or "
                                           + "deployment." );
                    }
                }
            }
            catch ( final ArchiveCreationException e )
            {
                throw new MojoExecutionException( "Failed to create assembly: " + e.getMessage(), e );
            }
            catch ( final AssemblyFormattingException e )
            {
                throw new MojoExecutionException( "Failed to create assembly: " + e.getMessage(), e );
            }
            catch ( final InvalidAssemblerConfigurationException e )
            {
                throw new MojoFailureException( assembly, "Assembly is incorrectly configured: " + assembly.getId(),
                                                "Assembly: " + assembly.getId() + " is not configured correctly: "
                                                    + e.getMessage() );
            }
        }
    }

    private FixedStringSearchInterpolator createRepositoryInterpolator()
    {
        final Properties settingsProperties = new Properties();
        final MavenSession session = getMavenSession();

        if ( getLocalRepository() != null )
        {
            settingsProperties.setProperty( "localRepository", getLocalRepository().getBasedir() );
            settingsProperties.setProperty( "settings.localRepository", getLocalRepository().getBasedir() );
        }
        else if ( session != null && session.getSettings() != null )
        {
            settingsProperties.setProperty( "localRepository", session.getSettings().getLocalRepository() );
            settingsProperties.setProperty( "settings.localRepository", getLocalRepository().getBasedir() );
        }

        return FixedStringSearchInterpolator.create( new PropertiesBasedValueSource( settingsProperties ) );

    }

    private FixedStringSearchInterpolator createCommandLinePropertiesInterpolator()
    {
        Properties commandLineProperties = System.getProperties();
        final MavenSession session = getMavenSession();

        if ( session != null )
        {
            commandLineProperties = new Properties();
            commandLineProperties.putAll( session.getSystemProperties() );
            commandLineProperties.putAll( session.getUserProperties() );
        }

        PropertiesBasedValueSource cliProps = new PropertiesBasedValueSource( commandLineProperties );
        return FixedStringSearchInterpolator.create( cliProps );

    }

    private FixedStringSearchInterpolator createEnvInterpolator()
    {
        PrefixedPropertiesValueSource envProps = new PrefixedPropertiesValueSource( Collections.singletonList( "env." ),
                                                                                    CommandLineUtils.getSystemEnvVars(
                                                                                        false ), true );
        return FixedStringSearchInterpolator.create( envProps );
    }

    /**
     * Returns true if the current project is located at the Execution Root Directory (where mvn was launched)
     *
     * @return if this is the execution root
     */
    boolean isThisTheExecutionRoot()
    {
        final Log log = getLog();
        log.debug( "Root Folder:" + mavenSession.getExecutionRootDirectory() );
        log.debug( "Current Folder:" + basedir );
        final boolean result = mavenSession.getExecutionRootDirectory().equalsIgnoreCase( basedir.toString() );
        if ( result )
        {
            log.debug( "This is the execution root." );
        }
        else
        {
            log.debug( "This is NOT the execution root." );
        }

        return result;
    }

    @Override
    public File getBasedir()
    {
        return basedir;
    }

    public void setBasedir( final File basedir )
    {
        this.basedir = basedir;
    }

    @Override
    public String[] getDescriptorReferences()
    {
        return descriptorRefs;
    }

    @Override
    public File getDescriptorSourceDirectory()
    {
        return descriptorSourceDirectory;
    }

    @Override
    public String[] getDescriptors()
    {
        return descriptors;
    }

    public void setDescriptors( final String[] descriptors )
    {
        this.descriptors = descriptors;
    }

    @Override
    public abstract MavenProject getProject();

    @Override
    public File getSiteDirectory()
    {
        return siteDirectory;
    }

    public void setSiteDirectory( final File siteDirectory )
    {
        this.siteDirectory = siteDirectory;
    }

    @Override
    public String getFinalName()
    {
        return finalName;
    }

    public void setFinalName( final String finalName )
    {
        this.finalName = finalName;
    }

    @Override
    public boolean isAssemblyIdAppended()
    {
        return appendAssemblyId;
    }

    @Override
    public String getTarLongFileMode()
    {
        return tarLongFileMode;
    }

    public void setTarLongFileMode( final String tarLongFileMode )
    {
        this.tarLongFileMode = tarLongFileMode;
    }

    @Override
    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( final File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public MavenArchiveConfiguration getJarArchiveConfiguration()
    {
        return archive;
    }

    @Override
    public File getWorkingDirectory()
    {
        return workDirectory;
    }

    @Override
    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( final ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    @Override
    public File getTemporaryRootDirectory()
    {
        return tempRoot;
    }

    @Override
    public File getArchiveBaseDirectory()
    {
        return archiveBaseDirectory;
    }

    @Override
    public List<String> getFilters()
    {
        if ( filters == null )
        {
            filters = getProject().getBuild().getFilters();
            if ( filters == null )
            {
                filters = Collections.emptyList();
            }
        }
        return filters;
    }

    public void setFilters( final List<String> filters )
    {
        this.filters = filters;
    }

    @Override
    public boolean isIncludeProjectBuildFilters()
    {
        return includeProjectBuildFilters;
    }

    @Override
    public List<MavenProject> getReactorProjects()
    {
        return reactorProjects;
    }

    public void setReactorProjects( final List<MavenProject> reactorProjects )
    {
        this.reactorProjects = reactorProjects;
    }

    public void setAppendAssemblyId( final boolean appendAssemblyId )
    {
        this.appendAssemblyId = appendAssemblyId;
    }

    public void setArchive( final MavenArchiveConfiguration archive )
    {
        this.archive = archive;
    }

    public void setDescriptorRefs( final String[] descriptorRefs )
    {
        this.descriptorRefs = descriptorRefs;
    }

    public void setTempRoot( final File tempRoot )
    {
        this.tempRoot = tempRoot;
    }

    public void setWorkDirectory( final File workDirectory )
    {
        this.workDirectory = workDirectory;
    }

    @Override
    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    @Override
    public boolean isDryRun()
    {
        return dryRun;
    }

    @Override
    public boolean isIgnoreDirFormatExtensions()
    {
        return ignoreDirFormatExtensions;
    }

    @Override
    public boolean isIgnoreMissingDescriptor()
    {
        return ignoreMissingDescriptor;
    }

    @Override
    public MavenSession getMavenSession()
    {
        return mavenSession;
    }

    @Override
    public String getArchiverConfig()
    {
        return archiverConfig == null ? null : archiverConfig.toString();
    }

    @Override
    public MavenReaderFilter getMavenReaderFilter()
    {
        return mavenReaderFilter;
    }

    @Override
    public boolean isUpdateOnly()
    {
        return updateOnly;
    }

    @Override
    public boolean isUseJvmChmod()
    {
        return useJvmChmod;
    }

    @Override
    public boolean isIgnorePermissions()
    {
        return ignorePermissions;
    }

    @Override
    public String getEncoding()
    {
        return encoding;
    }

    boolean isRecompressZippedFiles()
    {
        return recompressZippedFiles;
    }

    public String getMergeManifestMode()
    {
        return mergeManifestMode;
    }

    @Override
    public String getEscapeString()
    {
        return escapeString;
    }

    @Override
    public List<String> getDelimiters()
    {
        return delimiters;
    }

    public void setDelimiters( List<String> delimiters )
    {
        this.delimiters = delimiters;
    }

    @Override
    @Nonnull
    public FixedStringSearchInterpolator getCommandLinePropsInterpolator()
    {
        if ( commanndLinePropertiesInterpolator == null )
        {
            this.commanndLinePropertiesInterpolator = createCommandLinePropertiesInterpolator();
        }
        return commanndLinePropertiesInterpolator;
    }

    @Override
    @Nonnull
    public FixedStringSearchInterpolator getEnvInterpolator()
    {
        if ( envInterpolator == null )
        {
            this.envInterpolator = createEnvInterpolator();
        }
        return envInterpolator;
    }

    @Override
    @Nonnull
    public FixedStringSearchInterpolator getRepositoryInterpolator()
    {
        if ( rootInterpolator == null )
        {
            this.rootInterpolator = createRepositoryInterpolator();
        }
        return rootInterpolator;
    }

    @Override
    @Nonnull
    public FixedStringSearchInterpolator getMainProjectInterpolator()
    {
        if ( mainProjectInterpolator == null )
        {
            this.mainProjectInterpolator = mainProjectInterpolator( getProject() );
        }
        return mainProjectInterpolator;
    }

}
