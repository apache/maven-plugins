package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.AddArtifactTask;
import org.apache.maven.plugin.assembly.archive.task.AddFileSetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugin.assembly.utils.ProjectUtils;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.ModuleSources;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

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
    
    public ModuleSetAssemblyPhase()
    {
        // needed for plexus
    }
    
    public ModuleSetAssemblyPhase( MavenProjectBuilder projectBuilder )
    {
        this.projectBuilder = projectBuilder;
    }

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List moduleSets = assembly.getModuleSets();

        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();

        for ( Iterator i = moduleSets.iterator(); i.hasNext(); )
        {
            ModuleSet moduleSet = ( ModuleSet ) i.next();

            Set moduleProjects = getModuleProjects( moduleSet, configSource );
            
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

    protected void addModuleBinaries( ModuleBinaries binaries, Set moduleProjects, Archiver archiver,
                                      AssemblerConfigurationSource configSource, boolean includeBaseDirectory )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        if ( binaries == null )
        {
            return;
        }

        Set visitedArtifacts = new HashSet();
        
        for ( Iterator j = moduleProjects.iterator(); j.hasNext(); )
        {
            MavenProject project = ( MavenProject ) j.next();
            
            getLogger().debug( "Processing binary artifact for module project: " + project.getId() );
            
            Artifact artifact = project.getArtifact();

            addArtifact( artifact, project, archiver, configSource, binaries, includeBaseDirectory );
            
            visitedArtifacts.add( artifact.getDependencyConflictId() );
        }
        
        if ( binaries.isIncludeDependencies() )
        {
            // FIXME: This will produce unpredictable results when module dependencies have a version conflict.
            getLogger().warn( "NOTE: Currently, inclusion of module dependencies may produce unpredictable " +
                    "results if a version conflict occurs." );
            
            for ( Iterator it = moduleProjects.iterator(); it.hasNext(); )
            {
                MavenProject moduleProject = ( MavenProject ) it.next();

                getLogger().debug( "Processing binary dependencies for module project: " + moduleProject.getId() );
                
                Set binaryDependencies = moduleProject.getArtifacts();

                List includes = binaries.getIncludes();
                
                // we don't need to include dependencies which have already been found.
//                List excludes = collectExcludesFromQueuedArtifacts( visitedArtifacts, binaries.getExcludes() );
                List excludes = binaries.getExcludes();

                FilterUtils.filterArtifacts( binaryDependencies, includes, excludes, true, Collections.EMPTY_LIST,
                                             getLogger() );

                for ( Iterator binDepIterator = binaryDependencies.iterator(); binDepIterator.hasNext(); )
                {
                    Artifact artifact = ( Artifact ) binDepIterator.next();
                    MavenProject project;
                    try
                    {
                        project = projectBuilder.buildFromRepository( artifact, configSource.getRemoteRepositories(),
                                                            configSource.getLocalRepository() );
                    }
                    catch ( ProjectBuildingException e )
                    {
                        throw new ArchiveCreationException( "Error retrieving POM of module-dependency: " + artifact.getId()
                                        + "; Reason: " + e.getMessage(), e );
                    }
                    
                    addArtifact( artifact, project, archiver, configSource, binaries, includeBaseDirectory );
                    
                    visitedArtifacts.add( artifact.getDependencyConflictId() );
                }
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
                              AssemblerConfigurationSource configSource, ModuleBinaries binaries, boolean includeBaseDirectory )
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

        for ( Iterator j = moduleProjects.iterator(); j.hasNext(); )
        {
            MavenProject moduleProject = ( MavenProject ) j.next();

            getLogger().debug( "Processing sources for module project: " + moduleProject.getId() );
            
            String sourcePath = sources.getDirectory();
            
            File moduleBasedir = moduleProject.getBasedir();
            
            if ( sourcePath != null )
            {
                File sourceDir = new File( sourcePath );
                
                if ( !sourceDir.isAbsolute() )
                {
                    sourcePath = new File( moduleBasedir, sourcePath ).getAbsolutePath();
                    sources.setDirectory( sourcePath );
                }
            }
            else
            {
                sourcePath = moduleBasedir.getAbsolutePath();
            }

            AddFileSetsTask task = new AddFileSetsTask( Collections.singletonList( sources ) );

            task.setProject( moduleProject );
            task.setLogger( getLogger() );
            task.setIncludeBaseDirectory( includeBaseDirectory );

            task.execute( archiver, configSource );
        }
    }

    protected Set getModuleProjects( ModuleSet moduleSet, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException
    {
        MavenProject project = configSource.getProject();

        Set moduleProjects;
        try
        {
            moduleProjects = ProjectUtils.getProjectModules( project, configSource.getReactorProjects(), getLogger() );
        }
        catch ( IOException e )
        {
            throw new ArchiveCreationException( "Error retrieving module-set for project: " + project.getId() + ": "
                            + e.getMessage(), e );
        }

        FilterUtils.filterProjects( moduleProjects, moduleSet.getIncludes(), moduleSet.getExcludes(), true,
                                    getLogger() );
        return moduleProjects;
    }

}
