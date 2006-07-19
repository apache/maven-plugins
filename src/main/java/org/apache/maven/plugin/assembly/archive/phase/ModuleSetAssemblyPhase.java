package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveAssemblyUtils;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.filter.AssemblyExcludesArtifactFilter;
import org.apache.maven.plugin.assembly.filter.AssemblyIncludesArtifactFilter;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugin.assembly.utils.ProjectUtils;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.ModuleSources;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
 *                   role-hint="module-sets"
 */
public class ModuleSetAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{
    
    /**
     * @plexus.requirement
     */
    private ArchiverManager archiverManager;

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource,
                         ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List moduleSets = assembly.getModuleSets();
        MavenProject project = configSource.getProject();
        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();

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
                allModuleProjects = ProjectUtils.getProjectModules( project, configSource.getReactorProjects(), getLogger() );
            }
            catch ( IOException e )
            {
                throw new ArchiveCreationException( "Error retrieving module-set for project: " + project.getId()
                    + ": " + e.getMessage(), e );
            }

            Set moduleProjects = new HashSet( allModuleProjects );

            FilterUtils.filterProjects( moduleProjects, moduleSet.getIncludes(), moduleSet.getExcludes(), false );

            List moduleFileSets = new ArrayList();

            for ( Iterator j = moduleProjects.iterator(); j.hasNext(); )
            {
                MavenProject moduleProject = (MavenProject) j.next();

                ModuleSources sources = moduleSet.getSources();
                if ( sources != null )
                {
                    sources.setDirectory( PathUtils.toRelative( moduleProject.getBasedir(), sources.getDirectory() ) );

                    moduleFileSets.add( sources );
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

                    String fileNameMapping = AssemblyFormatUtils.evaluateFileNameMapping( binaries
                        .getOutputFileNameMapping(), moduleArtifact );

                    String output = binaries.getOutputDirectory();
                    output = AssemblyFormatUtils.getOutputDirectory( output, moduleProject,
                        configSource.getFinalName(), includeBaseDirectory );

                    int fileMode = Integer.parseInt( binaries.getFileMode(), 8 );
                    int dirMode = Integer.parseInt( binaries.getDirectoryMode(), 8 );

                    getLogger().debug(
                        "ModuleSet[" + output + "]" + " dir perms: "
                            + Integer.toString( archiver.getDefaultDirectoryMode(), 8 ) + " file perms: "
                            + Integer.toString( archiver.getDefaultFileMode(), 8 ) );

                    ArchiveAssemblyUtils.addArtifactToArchive( moduleArtifact, archiver, archiverManager, output,
                        fileNameMapping, binaries.isUnpack(), dirMode, fileMode, configSource, componentsXmlFilter,
                        getLogger() );

                    if ( binaries.isIncludeDependencies() )
                    {
                        Set binaryDependencies = moduleProject.getArtifacts();

                        List includes = binaries.getIncludes();
                        List excludes = binaries.getExcludes();

                        FilterUtils.filterArtifacts( binaryDependencies, includes, excludes, true,
                            Collections.EMPTY_LIST );

                        for ( Iterator binDepIterator = binaryDependencies.iterator(); binDepIterator.hasNext(); )
                        {
                            Artifact binaryDependency = (Artifact) binDepIterator.next();

                            String depFileNameMapping = AssemblyFormatUtils.evaluateFileNameMapping( binaries
                                .getOutputFileNameMapping(), binaryDependency );

                            ArchiveAssemblyUtils.addArtifactToArchive( binaryDependency, archiver, archiverManager,
                                output, depFileNameMapping, includeBaseDirectory, dirMode, fileMode, configSource,
                                componentsXmlFilter, getLogger() );
                        }
                    }
                }

                if ( !moduleFileSets.isEmpty() )
                {
                    ArchiveAssemblyUtils.addFileSets( archiver, moduleFileSets, includeBaseDirectory, configSource, componentsXmlFilter, getLogger() );
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
