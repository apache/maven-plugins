package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.AddArtifactTask;
import org.apache.maven.plugin.assembly.archive.task.AddDependencySetsTask;
import org.apache.maven.plugin.assembly.archive.task.AddFileSetsTask;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugin.assembly.utils.ProjectUtils;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.ModuleSources;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase" role-hint="module-sets"
 */
public class ModuleSetAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    /**
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    /**
     * @plexus.requirement
     */
    private DependencyResolver dependencyResolver;

    public ModuleSetAssemblyPhase()
    {
        // needed for plexus
    }

    public ModuleSetAssemblyPhase( MavenProjectBuilder projectBuilder, DependencyResolver dependencyResolver,
                                   Logger logger )
    {
        this.projectBuilder = projectBuilder;
        this.dependencyResolver = dependencyResolver;

        enableLogging( logger );
    }

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        List moduleSets = assembly.getModuleSets();

        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();

        for ( Iterator i = moduleSets.iterator(); i.hasNext(); )
        {
            ModuleSet moduleSet = ( ModuleSet ) i.next();

            Set moduleProjects = getModuleProjects( moduleSet, configSource, moduleSet.isIncludeSubModules() );

            ModuleSources sources = moduleSet.getSources();
            ModuleBinaries binaries = moduleSet.getBinaries();

            if ( sources == null && binaries == null )
            {
                getLogger().warn( "Encountered ModuleSet with no sources or binaries specified. Skipping." );
                continue;
            }

            addModuleSourceFileSets( moduleSet.getSources(), moduleProjects, archiver, configSource,
                                     includeBaseDirectory );

            addModuleBinaries( moduleSet.getBinaries(), moduleProjects, archiver, configSource, includeBaseDirectory );
        }
    }

    protected void addModuleBinaries( ModuleBinaries binaries, Set projects, Archiver archiver,
                                      AssemblerConfigurationSource configSource, boolean includeBaseDirectory )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        if ( binaries == null )
        {
            return;
        }

        Set moduleProjects = new HashSet( projects );

        for ( Iterator it = moduleProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = ( MavenProject ) it.next();

            if ( "pom".equals( project.getPackaging() ) )
            {
                String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                getLogger().debug( "Excluding POM-packaging module: " + projectId );

                it.remove();
            }
        }

        for ( Iterator j = moduleProjects.iterator(); j.hasNext(); )
        {
            MavenProject project = ( MavenProject ) j.next();

            getLogger().debug( "Processing binary artifact for module project: " + project.getId() );

            Artifact artifact = project.getArtifact();

            addArtifact( artifact, project, archiver, configSource, binaries, includeBaseDirectory );
        }

        List depSets = binaries.getDependencySets();

        if ( ( depSets == null || depSets.isEmpty() ) && binaries.isIncludeDependencies() )
        {
            DependencySet impliedDependencySet = new DependencySet();

            impliedDependencySet.setOutputDirectory( binaries.getOutputDirectory() );
            impliedDependencySet.setOutputFileNameMapping( binaries.getOutputFileNameMapping() );
            impliedDependencySet.setFileMode( binaries.getFileMode() );
            impliedDependencySet.setDirectoryMode( binaries.getDirectoryMode() );
            impliedDependencySet.setExcludes( binaries.getExcludes() );
            impliedDependencySet.setIncludes( binaries.getIncludes() );
            impliedDependencySet.setUnpack( binaries.isUnpack() );

            depSets = Collections.singletonList( impliedDependencySet );
        }

        if ( depSets != null )
        {
            // FIXME: This will produce unpredictable results when module dependencies have a version conflict.
            getLogger().warn(
                              "NOTE: Currently, inclusion of module dependencies may produce unpredictable "
                                              + "results if a version conflict occurs." );

            for ( Iterator it = moduleProjects.iterator(); it.hasNext(); )
            {
                MavenProject moduleProject = ( MavenProject ) it.next();

                getLogger().debug( "Processing binary dependencies for module project: " + moduleProject.getId() );

                AddDependencySetsTask task =
                    new AddDependencySetsTask( depSets, moduleProject, projectBuilder, dependencyResolver, getLogger() );

                task.setIncludeBaseDirectory( includeBaseDirectory );
                task.setDefaultOutputDirectory( binaries.getOutputDirectory() );
                task.setDefaultOutputFileNameMapping( binaries.getOutputFileNameMapping() );

                task.execute( archiver, configSource );
            }
        }
    }

    protected List collectExcludesFromQueuedArtifacts( Set visitedArtifacts, List binaryExcludes )
    {
        List excludes = binaryExcludes;

        if ( excludes == null )
        {
            excludes = new ArrayList();
        }
        else
        {
            excludes = new ArrayList( excludes );
        }

        for ( Iterator it = visitedArtifacts.iterator(); it.hasNext(); )
        {
            excludes.add( it.next() );
        }

        return excludes;
    }

    protected void addArtifact( Artifact artifact, MavenProject project, Archiver archiver,
                                AssemblerConfigurationSource configSource, ModuleBinaries binaries,
                                boolean includeBaseDirectory )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        if ( artifact.getFile() == null )
        {
            throw new ArchiveCreationException( "Artifact: " + artifact.getId()
                            + " (included by module) does not have an artifact with a file. "
                            + "Please ensure the package phase is run before the assembly is generated." );
        }

        AddArtifactTask task = new AddArtifactTask( artifact );

        task.setFileNameMapping( binaries.getOutputFileNameMapping() );
        task.setIncludeBaseDirectory( includeBaseDirectory );
        task.setOutputDirectory( binaries.getOutputDirectory() );
        task.setProject( project );
        task.setDirectoryMode( binaries.getDirectoryMode() );
        task.setFileMode( binaries.getFileMode() );
        task.setUnpack( binaries.isUnpack() );

        task.execute( archiver, configSource );
    }

    protected void addModuleSourceFileSets( ModuleSources sources, Set moduleProjects, Archiver archiver,
                                            AssemblerConfigurationSource configSource, boolean includeBaseDirectory )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        if ( sources == null )
        {
            return;
        }

        List fileSets = sources.getFileSets();

        if ( fileSets == null || fileSets.isEmpty() )
        {
            FileSet fs = new FileSet();
            fs.setDirectory( "src" );

            fileSets = Collections.singletonList( fs );
        }

        for ( Iterator j = moduleProjects.iterator(); j.hasNext(); )
        {
            MavenProject moduleProject = ( MavenProject ) j.next();

            getLogger().info( "Processing sources for module project: " + moduleProject.getId() );

            for ( Iterator fsIterator = fileSets.iterator(); fsIterator.hasNext(); )
            {
                FileSet fileSet = ( FileSet ) fsIterator.next();

                FileSet moduleFileSet = createFileSet( fileSet, sources, moduleProject );

                AddFileSetsTask task = new AddFileSetsTask( Collections.singletonList( moduleFileSet ) );

                task.setProject( moduleProject );
                task.setLogger( getLogger() );
                task.setIncludeBaseDirectory( false );

                task.execute( archiver, configSource );
            }
        }
    }

    protected FileSet createFileSet( FileSet fileSet, ModuleSources sources, MavenProject moduleProject )
        throws AssemblyFormattingException
    {
        FileSet fs = new FileSet();

        String sourcePath = fileSet.getDirectory();

        File moduleBasedir = moduleProject.getBasedir();

        if ( sourcePath != null )
        {
            File sourceDir = new File( sourcePath );

            if ( !sourceDir.isAbsolute() )
            {
                sourcePath = new File( moduleBasedir, sourcePath ).getAbsolutePath();
            }
        }
        else
        {
            sourcePath = moduleBasedir.getAbsolutePath();
        }

        fs.setDirectory( sourcePath );
        fs.setDirectoryMode( fileSet.getDirectoryMode() );
        fs.setExcludes( fileSet.getExcludes() );
        fs.setFileMode( fileSet.getFileMode() );
        fs.setIncludes( fileSet.getIncludes() );
        fs.setLineEnding( fileSet.getLineEnding() );

        String destPathPrefix = "";
        if ( sources.isIncludeModuleDirectory() )
        {
            destPathPrefix =
                AssemblyFormatUtils.evaluateFileNameMapping( sources.getOutputFileNameMapping(),
                                                             moduleProject.getArtifact() );

            if ( !destPathPrefix.endsWith( "/" ) )
            {
                destPathPrefix += "/";
            }
        }

        String destPath = fileSet.getOutputDirectory();
        
        if ( destPath == null )
        {
            destPath = destPathPrefix;
        }
        else
        {
            destPath = destPathPrefix + destPath;
        }

        destPath =
            AssemblyFormatUtils.getOutputDirectory( destPath, moduleProject, "",
                                                    true );

        fs.setOutputDirectory( destPath );

        getLogger().info( "module-sources source directory is: " + sourcePath );

        return fs;
    }

    protected Set getModuleProjects( ModuleSet moduleSet, AssemblerConfigurationSource configSource,
                                     boolean includeSubModules )
        throws ArchiveCreationException
    {
        MavenProject project = configSource.getProject();

        Set moduleProjects;
        try
        {
            moduleProjects =
                ProjectUtils.getProjectModules( project, configSource.getReactorProjects(), includeSubModules,
                                                getLogger() );
        }
        catch ( IOException e )
        {
            throw new ArchiveCreationException( "Error retrieving module-set for project: " + project.getId() + ": "
                            + e.getMessage(), e );
        }

        FilterUtils.filterProjects( moduleProjects, moduleSet.getIncludes(), moduleSet.getExcludes(), true, getLogger() );
        return moduleProjects;
    }

}
