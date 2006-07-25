package org.apache.maven.plugin.assembly.archive.phase;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.AddArtifactTask;
import org.apache.maven.plugin.assembly.filter.AssemblyScopeArtifactFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugin.assembly.utils.ProjectUtils;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;


/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
 *                   role-hint="dependency-sets"
 */
public class DependencySetAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
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

            int dirMode = Integer.parseInt( dependencySet.getDirectoryMode(), 8 );
            int fileMode = Integer.parseInt( dependencySet.getFileMode(), 8 );

            getLogger().debug(
                "DependencySet[" + output + "]" + " dir perms: "
                    + Integer.toString( archiver.getDefaultDirectoryMode(), 8 ) + " file perms: "
                    + Integer.toString( archiver.getDefaultFileMode(), 8 ) );

            Set allDependencyArtifacts = ProjectUtils.getDependencies( project );
            Set dependencyArtifacts = new HashSet( allDependencyArtifacts );

            AssemblyScopeArtifactFilter scopeFilter = new AssemblyScopeArtifactFilter( dependencySet.getScope() );

            FilterUtils.filterArtifacts( dependencyArtifacts, dependencySet.getIncludes(), dependencySet.getExcludes(),
                true, Collections.singletonList( scopeFilter ) );

            for ( Iterator j = dependencyArtifacts.iterator(); j.hasNext(); )
            {
                Artifact artifact = (Artifact) j.next();

                String fileNameMapping = AssemblyFormatUtils.evaluateFileNameMapping( dependencySet
                    .getOutputFileNameMapping(), artifact );
                
                String outputLocation = output + fileNameMapping;

                AddArtifactTask task = new AddArtifactTask( artifact, outputLocation );
                
                task.setDirectoryMode( dirMode );
                task.setFileMode( fileMode );
                task.setUnpack( dependencySet.isUnpack() );
                
                task.execute( archiver, configSource );
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

}
