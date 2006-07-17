package org.apache.maven.plugin.assembly.archive;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.filter.AssemblyExcludesArtifactFilter;
import org.apache.maven.plugin.assembly.filter.AssemblyIncludesArtifactFilter;
import org.apache.maven.plugin.assembly.filter.AssemblyScopeArtifactFilter;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.format.FileFormatter;
import org.apache.maven.plugin.assembly.format.FileSetFormatter;
import org.apache.maven.plugin.assembly.repository.RepositoryAssembler;
import org.apache.maven.plugin.assembly.repository.RepositoryAssemblyException;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugin.assembly.utils.ProjectUtils;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.ModuleSources;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.ArchiveCreator"
 *                   role-hint="default"
 */
public class DefaultAssemblyArchiver
    extends AbstractLogEnabled
    implements AssemblyArchiver
{

    /**
     * @plexus.requirement
     */
    private ArchiverManager archiverManager;

    /**
     * @plexus.requirement
     */
    private RepositoryAssembler repositoryAssembler;

    public File createArchive( Assembly assembly, String fullName, String format,
                               AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        File destFile = null;

        String filename = fullName + "." + format;

        try
        {
            Archiver archiver = createArchiver( format, configSource.getTarLongFileMode() );

            destFile = createArchive( archiver, assembly, filename, configSource );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new ArchiveCreationException( "Unable to obtain archiver for extension '" + format + "'" );
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveCreationException( "Error creating assembly: " + e.getMessage(), e );
        }

        return destFile;
    }

    /**
     * Creates the necessary archiver to build the distribution file.
     * 
     * @param format
     *            Archive format
     * @param tarLongFileMode
     * @return archiver Archiver generated
     * @throws org.codehaus.plexus.archiver.ArchiverException
     * @throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException
     */
    protected Archiver createArchiver( String format, String tarLongFileMode )
        throws ArchiverException, NoSuchArchiverException
    {
        Archiver archiver;
        if ( format.startsWith( "tar" ) )
        {
            TarArchiver tarArchiver = (TarArchiver) this.archiverManager.getArchiver( "tar" );
            archiver = tarArchiver;
            int index = format.indexOf( '.' );
            if ( index >= 0 )
            {
                // TODO: this needs a cleanup in plexus archiver - use a real
                // typesafe enum
                TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
                // TODO: this should accept gz and bz2 as well so we can skip
                // over the switch
                String compression = format.substring( index + 1 );
                if ( "gz".equals( compression ) )
                {
                    tarCompressionMethod.setValue( "gzip" );
                }
                else if ( "bz2".equals( compression ) )
                {
                    tarCompressionMethod.setValue( "bzip2" );
                }
                else
                {
                    // TODO: better handling
                    throw new IllegalArgumentException( "Unknown compression format: " + compression );
                }
                tarArchiver.setCompression( tarCompressionMethod );

                TarLongFileMode tarFileMode = new TarLongFileMode();

                tarFileMode.setValue( tarLongFileMode );

                tarArchiver.setLongfile( tarFileMode );
            }
        }
        else if ( "war".equals( format ) )
        {
            WarArchiver warArchiver = (WarArchiver) this.archiverManager.getArchiver( "war" );
            warArchiver.setIgnoreWebxml( false ); // See MNG-1274
            archiver = warArchiver;
        }
        else
        {
            archiver = this.archiverManager.getArchiver( format );
        }

        return archiver;
    }

    protected File createArchive( Archiver archiver, Assembly assembly, String filename,
                                  AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        ComponentsXmlArchiverFileFilter componentsXmlFilter = new ComponentsXmlArchiverFileFilter();

        addAssemblyRepositories( archiver, assembly, configSource );
        addAssemblyDependencySets( archiver, assembly, configSource, componentsXmlFilter );
        addAssemblyModules( archiver, assembly, configSource, componentsXmlFilter );
        addAssemblyFileSets( archiver, assembly, configSource, componentsXmlFilter );
        addAssemblyFileList( archiver, assembly, configSource, componentsXmlFilter );

        MavenArchiveConfiguration archive = configSource.getJarArchiveConfiguration();

        try
        {
            componentsXmlFilter.addToArchive( archiver );
        }
        catch ( IOException e )
        {
            throw new ArchiveCreationException( "Error adding component descriptors to assembly archive: "
                + e.getMessage(), e );
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveCreationException( "Error adding component descriptors to assembly archive: "
                + e.getMessage(), e );
        }

        File outputDirectory = configSource.getOutputDirectory();
        File destFile = new File( outputDirectory, filename );

        if ( archiver instanceof JarArchiver )
        {
            // TODO: I'd really prefer to rewrite MavenArchiver as either a
            // separate manifest creation utility (and to
            // create an include pom.properties etc into another archiver), or
            // an implementation of an archiver
            // (the first is preferable).
            MavenArchiver mavenArchiver = new MavenArchiver();

            if ( archive != null )
            {
                try
                {
                    Manifest manifest;
                    File manifestFile = archive.getManifestFile();

                    if ( manifestFile != null )
                    {
                        try
                        {
                            manifest = new Manifest( new FileReader( manifestFile ) );
                        }
                        catch ( FileNotFoundException e )
                        {
                            throw new ArchiveCreationException( "Manifest not found: " + e.getMessage() );
                        }
                        catch ( IOException e )
                        {
                            throw new ArchiveCreationException( "Error processing manifest: " + e.getMessage(), e );
                        }
                    }
                    else
                    {
                        manifest = mavenArchiver.getManifest( configSource.getProject(), archive.getManifest() );
                    }

                    if ( manifest != null )
                    {
                        JarArchiver jarArchiver = (JarArchiver) archiver;
                        jarArchiver.addConfiguredManifest( manifest );
                    }
                }
                catch ( ManifestException e )
                {
                    throw new ArchiveCreationException( "Error creating manifest: " + e.getMessage(), e );
                }
                catch ( DependencyResolutionRequiredException e )
                {
                    throw new ArchiveCreationException( "Dependencies were not resolved: " + e.getMessage(), e );
                }
            }
        }

        archiver.setDestFile( destFile );
        try
        {
            archiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveCreationException( "Error creating assembly archive: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ArchiveCreationException( "Error creating assembly archive: " + e.getMessage(), e );
        }

        return destFile;
    }

    protected void addAssemblyRepositories( Archiver archiver, Assembly assembly,
                                            AssemblerConfigurationSource configSource )
        throws ArchiveCreationException
    {
        List modulesList = assembly.getModuleSets();
        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();
        File tempRoot = configSource.getTemporaryRootDirectory();

        for ( Iterator i = modulesList.iterator(); i.hasNext(); )
        {
            Repository repository = (Repository) i.next();

            File repositoryDirectory = new File( tempRoot, repository.getOutputDirectory() );

            if ( !repositoryDirectory.exists() )
            {
                repositoryDirectory.mkdirs();
            }

            try
            {
                repositoryAssembler.assemble( repositoryDirectory, repository, configSource );
            }
            catch ( RepositoryAssemblyException e )
            {
                throw new ArchiveCreationException( "Failed to assembly repository: " + e.getMessage(), e );
            }

            try
            {
                if ( includeBaseDirectory )
                {
                    archiver.addDirectory( repositoryDirectory, repository.getOutputDirectory() + "/" );
                }
                else
                {
                    archiver.addDirectory( repositoryDirectory );
                }
            }
            catch ( ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding directory to archive: " + e.getMessage(), e );
            }
        }
    }

    protected void addAssemblyDependencySets( Archiver archiver, Assembly assembly,
                                              AssemblerConfigurationSource configSource,
                                              ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List dependencySets = assembly.getDependencySets();
        MavenProject project = configSource.getProject();
        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();

        for ( Iterator i = dependencySets.iterator(); i.hasNext(); )
        {
            DependencySet dependencySet = (DependencySet) i.next();
            String output = dependencySet.getOutputDirectory();
            output = AssemblyFormatUtils.getOutputDirectory( output, project, configSource.getFinalName(),
                includeBaseDirectory );

            archiver.setDefaultDirectoryMode( Integer.parseInt( dependencySet.getDirectoryMode(), 8 ) );

            archiver.setDefaultFileMode( Integer.parseInt( dependencySet.getFileMode(), 8 ) );

            getLogger().debug(
                "DependencySet[" + output + "]" + " dir perms: "
                    + Integer.toString( archiver.getDefaultDirectoryMode(), 8 ) + " file perms: "
                    + Integer.toString( archiver.getDefaultFileMode(), 8 ) );

            Set allDependencyArtifacts = ProjectUtils.getDependencies( project );
            Set dependencyArtifacts = new HashSet( allDependencyArtifacts );

            AssemblyScopeArtifactFilter scopeFilter = new AssemblyScopeArtifactFilter( dependencySet.getScope() );

            FilterUtils.filterArtifacts( dependencyArtifacts, dependencySet.getIncludes(), dependencySet.getExcludes(),
                true, Collections.singletonList( scopeFilter ) );

            File workDirectory = configSource.getWorkingDirectory();

            for ( Iterator j = dependencyArtifacts.iterator(); j.hasNext(); )
            {
                Artifact artifact = (Artifact) j.next();

                String fileNameMapping = AssemblyFormatUtils.evaluateFileNameMapping( dependencySet
                    .getOutputFileNameMapping(), artifact );
                if ( dependencySet.isUnpack() )
                {
                    // TODO: something like zipfileset in plexus-archiver
                    // archiver.addJar( )

                    File tempLocation = new File( workDirectory, fileNameMapping );
                    boolean process = false;
                    if ( !tempLocation.exists() )
                    {
                        tempLocation.mkdirs();
                        process = true;
                    }
                    else if ( artifact.getFile().lastModified() > tempLocation.lastModified() )
                    {
                        process = true;
                    }

                    if ( process )
                    {
                        try
                        {
                            ArchiveAssemblyUtils.unpack( artifact.getFile(), tempLocation, archiverManager );
                        }
                        catch ( NoSuchArchiverException e )
                        {
                            throw new ArchiveCreationException( "Unable to obtain unarchiver for file '"
                                + artifact.getFile() + "'" );
                        }
                        catch ( ArchiveExpansionException e )
                        {
                            throw new ArchiveCreationException( "Unable to expand archive: '" + artifact.getFile()
                                + "'" );
                        }

                        /*
                         * If the assembly is 'jar-with-dependencies', remove
                         * the security files in all dependencies that will
                         * prevent the uberjar to execute. Please see
                         * MASSEMBLY-64 for details.
                         */
                        if ( archiver instanceof JarArchiver )
                        {
                            String[] securityFiles = { "*.RSA", "*.DSA", "*.SF", "*.rsa", "*.dsa", "*.sf" };
                            org.apache.maven.shared.model.fileset.FileSet securityFileSet = new org.apache.maven.shared.model.fileset.FileSet();
                            securityFileSet.setDirectory( tempLocation.getAbsolutePath() + "/META-INF/" );

                            for ( int sfsi = 0; sfsi < securityFiles.length; sfsi++ )
                            {
                                securityFileSet.addInclude( securityFiles[sfsi] );
                            }

                            FileSetManager fsm = new FileSetManager( getLogger() );
                            try
                            {
                                fsm.delete( securityFileSet );
                            }
                            catch ( IOException e )
                            {
                                throw new ArchiveCreationException( "Failed to delete security files: "
                                    + e.getMessage(), e );
                            }
                        }
                    }

                    ArchiveAssemblyUtils.addDirectory( archiver, tempLocation, output, null, FileUtils
                        .getDefaultExcludesAsList(), componentsXmlFilter );
                }
                else
                {
                    try
                    {
                        archiver.addFile( artifact.getFile(), output + fileNameMapping );
                    }
                    catch ( ArchiverException e )
                    {
                        throw new ArchiveCreationException( "Error adding file '" + artifact.getFile()
                            + "' to archive: " + e.getMessage(), e );
                    }
                }
            }

            allDependencyArtifacts.removeAll( dependencyArtifacts );

            for ( Iterator it = allDependencyArtifacts.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                // would be better to have a way to find out when a specified
                // include or exclude
                // is never triggered and warn() it.
                getLogger().debug( "artifact: " + artifact + " not included" );
            }
        }
    }

    /**
     * Process Files that will be included in the distribution.
     * 
     * @param archiver
     * @param componentsXmlFilter
     * @param fileSets
     * @param includeBaseDirecetory
     * @throws ArchiveCreationException
     * @throws IOException
     * @throws AssemblyFormattingException
     * @throws IOException
     */
    protected void addAssemblyFileSets( Archiver archiver, Assembly assembly,
                                        AssemblerConfigurationSource configSource,
                                        ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List fileSets = assembly.getFileSets();
        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();

        addFileSets( archiver, fileSets, includeBaseDirectory, configSource, componentsXmlFilter );
    }

    private void addFileSets( Archiver archiver, List fileSets, boolean includeBaseDirectory,
                              AssemblerConfigurationSource configSource,
                              ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        File archiveBaseDir = configSource.getArchiveBaseDirectory();

        if ( archiveBaseDir != null )
        {
            if ( !archiveBaseDir.exists() )
            {
                throw new ArchiveCreationException( "The archive base directory '" + archiveBaseDir.getAbsolutePath()
                    + "' does not exist" );
            }
            else if ( !archiveBaseDir.isDirectory() )
            {
                throw new ArchiveCreationException( "The archive base directory '" + archiveBaseDir.getAbsolutePath()
                    + "' exists, but it is not a directory" );
            }
        }

        File basedir = configSource.getBasedir();
        MavenProject project = configSource.getProject();

        FileSetFormatter fileSetFormatter = new FileSetFormatter( configSource, getLogger() );

        for ( Iterator i = fileSets.iterator(); i.hasNext(); )
        {
            FileSet fileSet = (FileSet) i.next();
            String directory = fileSet.getDirectory();
            String output = fileSet.getOutputDirectory();

            if ( output == null )
            {
                output = directory;
            }

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "FileSet[" + output + "]" + " dir perms: "
                        + Integer.toString( archiver.getDefaultDirectoryMode(), 8 ) + " file perms: "
                        + Integer.toString( archiver.getDefaultFileMode(), 8 )
                        + (fileSet.getLineEnding() == null ? "" : " lineEndings: " + fileSet.getLineEnding()) );
            }

            output = AssemblyFormatUtils.getOutputDirectory( output, project, configSource.getFinalName(),
                includeBaseDirectory );

            archiver.setDefaultDirectoryMode( Integer.parseInt( fileSet.getDirectoryMode(), 8 ) );

            archiver.setDefaultFileMode( Integer.parseInt( fileSet.getFileMode(), 8 ) );

            if ( directory == null )
            {
                directory = basedir.getAbsolutePath();
            }

            File fileSetDir = null;

            if ( archiveBaseDir == null )
            {
                fileSetDir = new File( directory );
            }
            else
            {
                fileSetDir = new File( archiveBaseDir, directory );
            }

            getLogger().debug( "The archive base directory is '" + archiveBaseDir.getAbsolutePath() + "'" );

            if ( !fileSetDir.isAbsolute() )
            {
                fileSetDir = new File( basedir, directory );
            }

            if ( archiveBaseDir.exists() )
            {
                try
                {
                    archiveBaseDir = fileSetFormatter.formatFileSetForAssembly( archiveBaseDir, fileSet );
                }
                catch ( IOException e )
                {
                    throw new ArchiveCreationException( "Error fixing file-set line endings for assembly: "
                        + e.getMessage(), e );
                }

                getLogger().debug( "Archive base directory: '" + archiveBaseDir.getAbsolutePath() + "'" );

                ArchiveAssemblyUtils.addDirectory( archiver, archiveBaseDir, output, fileSet.getIncludes(), fileSet
                    .getExcludes(), componentsXmlFilter );
            }
        }
    }

    /**
     * Copy files to the distribution with option to change destination name
     * 
     * @param archiver
     * @param componentsXmlFilter
     * @param fileList
     * @throws ArchiveCreationException
     * @throws AssemblyFormattingException
     */
    protected void addAssemblyFileList( Archiver archiver, Assembly assembly,
                                        AssemblerConfigurationSource configSource,
                                        ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List fileList = assembly.getFiles();
        File basedir = configSource.getBasedir();

        FileFormatter fileFormatter = new FileFormatter( configSource, getLogger() );
        for ( Iterator i = fileList.iterator(); i.hasNext(); )
        {
            FileItem fileItem = (FileItem) i.next();

            String sourcePath = fileItem.getSource();

            // ensure source file is in absolute path for reactor build to work
            File source = new File( sourcePath );

            // save the original sourcefile's name, because filtration may
            // create a temp file with a different name.
            String sourceName = source.getName();

            if ( !source.isAbsolute() )
            {
                source = new File( basedir, sourcePath );
            }

            fileFormatter.format( source, fileItem.isFiltered(), fileItem.getLineEnding() );

            String destName = fileItem.getDestName();

            if ( destName == null )
            {
                destName = sourceName;
            }

            String outputDirectory = AssemblyFormatUtils.getOutputDirectory( fileItem.getOutputDirectory(),
                configSource.getProject(), configSource.getFinalName(), assembly.isIncludeBaseDirectory() );

            String target;

            // omit the last char if ends with / or \\
            if ( outputDirectory.endsWith( "/" ) || outputDirectory.endsWith( "\\" ) )
            {
                target = outputDirectory + destName;
            }
            else
            {
                target = outputDirectory + "/" + destName;
            }

            try
            {
                archiver.addFile( source, target, Integer.parseInt( fileItem.getFileMode() ) );
            }
            catch ( ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
            }
        }
    }

    private void addAssemblyModules( Archiver archiver, Assembly assembly, AssemblerConfigurationSource configSource,
                                     ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List moduleSets = assembly.getModuleSets();
        MavenProject project = configSource.getProject();
        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();
        File workDirectory = configSource.getWorkingDirectory();

        for ( Iterator i = moduleSets.iterator(); i.hasNext(); )
        {
            ModuleSet moduleSet = (ModuleSet) i.next();

            AndArtifactFilter moduleFilter = new AndArtifactFilter();

            if ( !moduleSet.getIncludes().isEmpty() )
            {
                moduleFilter.add( new AssemblyIncludesArtifactFilter( moduleSet.getIncludes() ) );
            }
            if ( !moduleSet.getExcludes().isEmpty() )
            {
                moduleFilter.add( new AssemblyExcludesArtifactFilter( moduleSet.getExcludes() ) );
            }

            Set allModuleProjects;
            try
            {
                allModuleProjects = ProjectUtils.getProjectModules( project, configSource.getReactorProjects() );
            }
            catch ( IOException e )
            {
                throw new ArchiveCreationException( "Error retrieving module-set for project: " + project.getId()
                    + ": " + e.getMessage(), e );
            }

            // FIXME: Check source from here down to bum redundant code and
            // consolidate with existing utils.
            Set moduleProjects = new HashSet( allModuleProjects );

            FilterUtils.filterProjects( moduleProjects, moduleSet.getIncludes(), moduleSet.getExcludes(), false );

            List moduleFileSets = new ArrayList();

            for ( Iterator j = moduleProjects.iterator(); j.hasNext(); )
            {
                MavenProject moduleProject = (MavenProject) j.next();

                String name = moduleProject.getBuild().getFinalName();

                ModuleSources sources = moduleSet.getSources();

                if ( sources != null )
                {
                    String output = sources.getOutputDirectory();
                    output = AssemblyFormatUtils.getOutputDirectory( output, moduleProject, configSource
                        .getFinalName(), includeBaseDirectory );

                    FileSet moduleFileSet = new FileSet();

                    moduleFileSet.setDirectory( moduleProject.getBasedir().getAbsolutePath() );
                    moduleFileSet.setOutputDirectory( output );

                    List excludesList = new ArrayList();
                    excludesList.add( PathUtils.toRelative( moduleProject.getBasedir(), moduleProject.getBuild()
                        .getDirectory() )
                        + "/**" );
                    excludesList.add( PathUtils.toRelative( moduleProject.getBasedir(), moduleProject.getBuild()
                        .getOutputDirectory() )
                        + "/**" );
                    excludesList.add( PathUtils.toRelative( moduleProject.getBasedir(), moduleProject.getBuild()
                        .getTestOutputDirectory() )
                        + "/**" );
                    excludesList.add( PathUtils.toRelative( moduleProject.getBasedir(), moduleProject.getReporting()
                        .getOutputDirectory() )
                        + "/**" );
                    moduleFileSet.setExcludes( excludesList );

                    moduleFileSets.add( moduleFileSet );
                }

                ModuleBinaries binaries = moduleSet.getBinaries();

                if ( binaries != null )
                {
                    Artifact moduleArtifact = moduleProject.getArtifact();

                    if ( moduleArtifact.getFile() == null )
                    {
                        throw new ArchiveCreationException(
                            "Included module: "
                                + moduleProject.getId()
                                + " does not have an artifact with a file. Please ensure the package phase is run before the assembly is generated." );
                    }

                    String output = binaries.getOutputDirectory();
                    output = AssemblyFormatUtils.getOutputDirectory( output, moduleProject, configSource
                        .getFinalName(), includeBaseDirectory );

                    archiver.setDefaultDirectoryMode( Integer.parseInt( binaries.getDirectoryMode(), 8 ) );

                    archiver.setDefaultFileMode( Integer.parseInt( binaries.getFileMode(), 8 ) );

                    getLogger().debug(
                        "ModuleSet[" + output + "]" + " dir perms: "
                            + Integer.toString( archiver.getDefaultDirectoryMode(), 8 ) + " file perms: "
                            + Integer.toString( archiver.getDefaultFileMode(), 8 ) );

                    Set binaryDependencies = moduleProject.getArtifacts();

                    List includes = binaries.getIncludes();
                    List excludes = binaries.getExcludes();

                    FilterUtils.filterArtifacts( binaryDependencies, includes, excludes, true, Collections.EMPTY_LIST );

                    if ( binaries.isUnpack() )
                    {
                        // TODO: something like zipfileset in plexus-archiver
                        // archiver.addJar( )

                        // TODO refactor into the AbstractUnpackMojo
                        File tempLocation = new File( workDirectory, name );
                        boolean process = false;
                        if ( !tempLocation.exists() )
                        {
                            tempLocation.mkdirs();
                            process = true;
                        }
                        else if ( moduleArtifact.getFile().lastModified() > tempLocation.lastModified() )
                        {
                            process = true;
                        }

                        if ( process )
                        {
                            try
                            {
                                try
                                {
                                    ArchiveAssemblyUtils.unpack( moduleArtifact.getFile(), tempLocation,
                                        archiverManager );
                                }
                                catch ( ArchiveExpansionException e )
                                {
                                    throw new ArchiveCreationException( "Unable to unpack module artifact: '"
                                        + moduleArtifact.getFile() + "'" );
                                }

                                if ( binaries.isIncludeDependencies() )
                                {
                                    for ( Iterator dependencyIterator = binaryDependencies.iterator(); dependencyIterator
                                        .hasNext(); )
                                    {
                                        Artifact dependencyArtifact = (Artifact) dependencyIterator.next();

                                        try
                                        {
                                            ArchiveAssemblyUtils.unpack( dependencyArtifact.getFile(), tempLocation,
                                                archiverManager );
                                        }
                                        catch ( ArchiveExpansionException e )
                                        {
                                            throw new ArchiveCreationException( "Unable to unpack dependency: '"
                                                + dependencyArtifact.getFile() + "' of module: '"
                                                + moduleProject.getId() + "'" );
                                        }
                                    }
                                }
                            }
                            catch ( NoSuchArchiverException e )
                            {
                                throw new ArchiveCreationException( "Unable to obtain unarchiver: " + e.getMessage(), e );
                            }

                            /*
                             * If the assembly is 'jar-with-dependencies',
                             * remove the security files in all dependencies
                             * that will prevent the uberjar to execute. Please
                             * see MASSEMBLY-64 for details.
                             */
                            if ( archiver instanceof JarArchiver )
                            {
                                String[] securityFiles = { "*.RSA", "*.DSA", "*.SF", "*.rsa", "*.dsa", "*.sf" };
                                org.apache.maven.shared.model.fileset.FileSet securityFileSet = new org.apache.maven.shared.model.fileset.FileSet();
                                securityFileSet.setDirectory( tempLocation.getAbsolutePath() + "/META-INF/" );

                                for ( int sfsi = 0; sfsi < securityFiles.length; sfsi++ )
                                {
                                    securityFileSet.addInclude( securityFiles[sfsi] );
                                }

                                FileSetManager fsm = new FileSetManager( getLogger() );
                                try
                                {
                                    fsm.delete( securityFileSet );
                                }
                                catch ( IOException e )
                                {
                                    throw new ArchiveCreationException( "Failed to delete security files: "
                                        + e.getMessage(), e );
                                }
                            }
                        }

                        ArchiveAssemblyUtils.addDirectory( archiver, tempLocation, output, null, FileUtils
                            .getDefaultExcludesAsList(), componentsXmlFilter );
                    }
                    else
                    {
                        try
                        {
                            String outputFileNameMapping = binaries.getOutputFileNameMapping();

                            archiver
                                .addFile( moduleArtifact.getFile(), output
                                    + AssemblyFormatUtils.evaluateFileNameMapping( outputFileNameMapping,
                                        moduleArtifact ) );

                            if ( binaries.isIncludeDependencies() )
                            {
                                for ( Iterator artifacts = binaryDependencies.iterator(); artifacts.hasNext(); )
                                {
                                    Artifact dependencyArtifact = (Artifact) artifacts.next();

                                    archiver.addFile( dependencyArtifact.getFile(), output
                                        + AssemblyFormatUtils.evaluateFileNameMapping( outputFileNameMapping,
                                            dependencyArtifact ) );
                                }
                            }
                        }
                        catch ( ArchiverException e )
                        {
                            throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
                        }
                    }
                }

                if ( !moduleFileSets.isEmpty() )
                {
                    // TODO: includes and excludes
                    addFileSets( archiver, moduleFileSets, includeBaseDirectory, configSource, componentsXmlFilter );
                }
            }

            allModuleProjects.removeAll( moduleProjects );

            for ( Iterator it = allModuleProjects.iterator(); it.hasNext(); )
            {
                MavenProject excludedProject = (MavenProject) it.next();

                // would be better to have a way to find out when a specified
                // include or exclude
                // is never triggered and warn() it.
                getLogger().debug( "module: " + excludedProject.getId() + " not included" );
            }
        }
    }

}
