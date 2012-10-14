package org.apache.maven.plugin.assembly.mojos;

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
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.io.AssemblyReadException;
import org.apache.maven.plugin.assembly.io.AssemblyReader;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
    /**
     * The character encoding scheme to be applied when filtering resources.
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    protected String encoding;

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
    @Parameter( defaultValue = "${project.build.finalName}", required = true )
    private String finalName;

    /**
     * Directory to unpack JARs into if needed
     */
    @Parameter( defaultValue = "${project.build.directory}/assembly/work", required = true )
    private File workDirectory;

    /**
     * Specifies the formats of the assembly.
     * Multiple formats can be supplied and the Assembly Plugin will generate an archive for each desired formats.
     * When deploying your project, all file formats specified will also be deployed. A format is specified by supplying one of the following
     * values in a &lt;format&gt; subelement:
     * <ul>
     * <li><em>dir</em> - Creates a directory</li>
     * <li><em>zip</em> - Creates a ZIP file format</li>
     * <li><em>tar</em> - Creates a TAR format</li>
     * <li><em>tar.gz</em> - Creates a gzip'd TAR format</li>
     * <li><em>tar.bz2</em> - Creates a bzip'd TAR format</li>
     * </ul>
     */
    @Parameter
    private List<String> formats;

    /**
     * This is the artifact classifier to be used for the resultant assembly artifact. Normally, you would use the
     * assembly-id instead of specifying this here.
     *
     * @deprecated Please use the Assembly's id for classifier instead
     */
    @Deprecated
    @SuppressWarnings( "unused" )
    @Parameter( property = "classifier" )
    private String classifier;

    /**
     * A list of descriptor files to generate from.
     */
    @Parameter
    private String[] descriptors;

    /**
     * A list of references to assembly descriptors available on the plugin's classpath. The default classpath
     * includes these built-in descriptors: <code>bin</code>,
     * <code>jar-with-dependencies</code>, <code>src</code>, and
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
     * Predefined Assembly Descriptor Id's. You can select bin, jar-with-dependencies, or src.
     *
     * @deprecated Please use descriptorRefs instead
     */
    @Deprecated
    @Parameter( property = "descriptorId" )
    protected String descriptorId;

    /**
     * Assembly XML Descriptor file. This must be the path to your customized descriptor file.
     *
     * @deprecated Please use descriptors instead
     */
    @Deprecated
    @Parameter( property = "descriptor" )
    protected String descriptor;

    /**
     * Sets the TarArchiver behavior on file paths with more than 100 characters length. Valid values are: "warn"
     * (default), "fail", "truncate", "gnu", or "omit".
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
    private MavenFileFilter mavenFileFilter;

    /**
     * The Maven Session Object
     */
    @Component
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
     * Set to true to include the site generated by site:site goal.
     *
     * @deprecated Please set this variable in the assembly descriptor instead
     */
    @Deprecated
    @Parameter( property = "includeSite", defaultValue = "false" )
    private boolean includeSite;

    /**
     * Set to false to exclude the assembly id from the assembly final name.
     */
    @Parameter( property = "assembly.appendAssemblyId", defaultValue = "true" )
    protected boolean appendAssemblyId;

    /**
     * Set to true in order to not fail when a descriptor is missing.
     */
    @Parameter( property = "assembly.ignoreMissingDescriptor", defaultValue = "false" )
    protected boolean ignoreMissingDescriptor;

    /**
     * This is a set of instructions to the archive builder, especially for building .jar files. It enables you to
     * specify a Manifest file for the jar, in addition to other options.
     */
    @Parameter
    private MavenArchiveConfiguration archive;

    /**
     * The list of extra filter properties files to be used along with System properties, project
     * properties, and filter properties files specified in the POM build/filters section, which
     * should be used for the filtering during the current mojo execution.
     * <br/>
     * Normally, these will be configured from a plugin's execution section, to provide a different
     * set of filters for a particular execution.
     */
    @Parameter
    protected List<String> filters;

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
    @Parameter( defaultValue = "false" )
    private boolean recompressZippedFiles;

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
     *
     * @since 2.2
     */
    @Parameter( property = "assembly.updatOnly", defaultValue = "false" )
    private boolean updateOnly;

    /**
     * <p>
     * will use the jvm chmod, this is available for user and all level group level will be ignored
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
     * Create the binary distribution.
     * 
     * @throws org.apache.maven.plugin.MojoExecutionException
     * 
     */
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
            throw new MojoFailureException( assemblyReader, e.getMessage(), "Mojo configuration is invalid: "
                            + e.getMessage() );
        }

        // TODO: include dependencies marked for distribution under certain formats
        // TODO: how, might we plug this into an installer, such as NSIS?

        boolean warnedAboutMainProjectArtifact = false;
        for ( final Iterator<Assembly> assemblyIterator = assemblies.iterator(); assemblyIterator.hasNext(); )
        {
            final Assembly assembly = assemblyIterator.next();
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
                    throw new MojoFailureException( "No formats specified in the execution parameters or the assembly descriptor.");
                }

                for ( final String format : effectiveFormats )
                {
                    final File destFile = assemblyArchiver.createArchive( assembly, fullName, format, this, isRecompressZippedFiles());

                    final MavenProject project = getProject();
                    final String classifier = getClassifier();
                    final String type = project.getArtifact()
                                               .getType();

                    if ( attach && destFile.isFile() )
                    {
                        if ( isAssemblyIdAppended() )
                        {
                            projectHelper.attachArtifact( project, format, assembly.getId(), destFile );
                        }
                        else if ( classifier != null )
                        {
                            projectHelper.attachArtifact( project, format, classifier, destFile );
                        }
                        else if ( !"pom".equals( type ) && format.equals( type ) )
                        {
                            if ( !warnedAboutMainProjectArtifact )
                            {
                                final StringBuilder message = new StringBuilder();

                                message.append( "Configuration options: 'appendAssemblyId' is set to false, and 'classifier' is missing." );
                                message.append( "\nInstead of attaching the assembly file: " )
                                       .append( destFile )
                                       .append( ", it will become the file for main project artifact." );
                                message.append( "\nNOTE: If multiple descriptors or descriptor-formats are provided for this project, the value of this file will be non-deterministic!" );

                                getLog().warn( message );
                                warnedAboutMainProjectArtifact = true;
                            }

                            final File existingFile = project.getArtifact()
                                                             .getFile();
                            if ( ( existingFile != null ) && existingFile.exists() )
                            {
                                getLog().warn( "Replacing pre-existing project main-artifact file: " + existingFile
                                                               + "\nwith assembly file: " + destFile );
                            }

                            project.getArtifact()
                                   .setFile( destFile );
                        }
                        else
                        {
                            projectHelper.attachArtifact( project, format, null, destFile );
                        }
                    }
                    else if ( attach )
                    {
                        getLog().warn( "Assembly file: "
                                                       + destFile
                                                       + " is not a regular file (it may be a directory). It cannot be attached to the project build for installation or deployment." );
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

    /**
     * Returns true if the current project is located at the Execution Root Directory (where mvn was launched)
     * 
     * @return
     */
    protected boolean isThisTheExecutionRoot()
    {
        final Log log = getLog();
        log.debug( "Root Folder:" + mavenSession.getExecutionRootDirectory() );
        log.debug( "Current Folder:" + basedir );
        final boolean result = mavenSession.getExecutionRootDirectory()
                                           .equalsIgnoreCase( basedir.toString() );
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

    protected AssemblyArchiver getAssemblyArchiver()
    {
        return assemblyArchiver;
    }

    protected AssemblyReader getAssemblyReader()
    {
        return assemblyReader;
    }

    public File getBasedir()
    {
        return basedir;
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated This has been replaced by {@link #getDescriptors()}
     */
    @Deprecated
    public String getDescriptor()
    {
        return descriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated This has been replaced by {@link #getDescriptorReferences()}
     */
    @Deprecated
    public String getDescriptorId()
    {
        return descriptorId;
    }

    public String[] getDescriptorReferences()
    {
        return descriptorRefs;
    }

    public File getDescriptorSourceDirectory()
    {
        return descriptorSourceDirectory;
    }

    public String[] getDescriptors()
    {
        return descriptors;
    }

    public abstract MavenProject getProject();

    public File getSiteDirectory()
    {
        return siteDirectory;
    }

    public boolean isSiteIncluded()
    {
        return includeSite;
    }

    public String getFinalName()
    {
        return finalName;
    }

    public boolean isAssemblyIdAppended()
    {
        return appendAssemblyId;
    }

    public String getTarLongFileMode()
    {
        return tarLongFileMode;
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public MavenArchiveConfiguration getJarArchiveConfiguration()
    {
        return archive;
    }

    public File getWorkingDirectory()
    {
        return workDirectory;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public File getTemporaryRootDirectory()
    {
        return tempRoot;
    }

    public File getArchiveBaseDirectory()
    {
        return archiveBaseDirectory;
    }

    public List<String> getFilters()
    {
        if ( filters == null )
        {
            filters = getProject().getBuild()
                                  .getFilters();
            if ( filters == null )
            {
                filters = Collections.emptyList();
            }
        }
        return filters;
    }

    public List<MavenProject> getReactorProjects()
    {
        return reactorProjects;
    }

    public String getClassifier()
    {
        // TODO Auto-generated method stub
        return null;
    }

    protected MavenProjectHelper getProjectHelper()
    {
        return projectHelper;
    }

    public void setAppendAssemblyId( final boolean appendAssemblyId )
    {
        this.appendAssemblyId = appendAssemblyId;
    }

    public void setArchive( final MavenArchiveConfiguration archive )
    {
        this.archive = archive;
    }

    public void setArchiveBaseDirectory( final File archiveBaseDirectory )
    {
        this.archiveBaseDirectory = archiveBaseDirectory;
    }

    public void setAssemblyArchiver( final AssemblyArchiver assemblyArchiver )
    {
        this.assemblyArchiver = assemblyArchiver;
    }

    public void setAssemblyReader( final AssemblyReader assemblyReader )
    {
        this.assemblyReader = assemblyReader;
    }

    public void setBasedir( final File basedir )
    {
        this.basedir = basedir;
    }

    public void setClassifier( final String classifier )
    {
        this.classifier = classifier;
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated This has been replaced by {@link #setDescriptors(String[])}
     */
    @Deprecated
    public void setDescriptor( final String descriptor )
    {
        this.descriptor = descriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated This has been replaced by {@link #setDescriptorRefs(String[])}
     */
    @Deprecated
    public void setDescriptorId( final String descriptorId )
    {
        this.descriptorId = descriptorId;
    }

    public void setDescriptorRefs( final String[] descriptorRefs )
    {
        this.descriptorRefs = descriptorRefs;
    }

    public void setDescriptors( final String[] descriptors )
    {
        this.descriptors = descriptors;
    }

    public void setDescriptorSourceDirectory( final File descriptorSourceDirectory )
    {
        this.descriptorSourceDirectory = descriptorSourceDirectory;
    }

    public void setFilters( final List<String> filters )
    {
        this.filters = filters;
    }

    public void setFinalName( final String finalName )
    {
        this.finalName = finalName;
    }

    public void setIncludeSite( final boolean includeSite )
    {
        this.includeSite = includeSite;
    }

    public void setLocalRepository( final ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public void setOutputDirectory( final File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setProjectHelper( final MavenProjectHelper projectHelper )
    {
        this.projectHelper = projectHelper;
    }

    public void setReactorProjects( final List<MavenProject> reactorProjects )
    {
        this.reactorProjects = reactorProjects;
    }

    public void setSiteDirectory( final File siteDirectory )
    {
        this.siteDirectory = siteDirectory;
    }

    public void setTarLongFileMode( final String tarLongFileMode )
    {
        this.tarLongFileMode = tarLongFileMode;
    }

    public void setTempRoot( final File tempRoot )
    {
        this.tempRoot = tempRoot;
    }

    public void setWorkDirectory( final File workDirectory )
    {
        this.workDirectory = workDirectory;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public boolean isIgnoreDirFormatExtensions()
    {
        return ignoreDirFormatExtensions;
    }

    public boolean isIgnoreMissingDescriptor()
    {
        return ignoreMissingDescriptor;
    }

    public void setIgnoreMissingDescriptor( final boolean ignoreMissingDescriptor )
    {
        this.ignoreMissingDescriptor = ignoreMissingDescriptor;
    }

    public MavenSession getMavenSession()
    {
        return mavenSession;
    }

    public String getArchiverConfig()
    {
        return archiverConfig == null ? null : archiverConfig.toString();
    }

    public MavenFileFilter getMavenFileFilter()
    {
        return mavenFileFilter;
    }

    public boolean isUpdateOnly()
    {
        return updateOnly;
    }

    public boolean isUseJvmChmod()
    {
        return useJvmChmod;
    }

    public boolean isIgnorePermissions()
    {
        return ignorePermissions;
    }
    
    public String getEncoding() {
    	return encoding;
    }

    protected boolean isRecompressZippedFiles() {
        return recompressZippedFiles;
    }
}
