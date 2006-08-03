package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.AddArtifactTask;
import org.apache.maven.plugin.assembly.filter.AssemblyScopeArtifactFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();

        for ( Iterator i = dependencySets.iterator(); i.hasNext(); )
        {
            DependencySet dependencySet = (DependencySet) i.next();

            addDependencySet( dependencySet, archiver, configSource, includeBaseDirectory );
        }
    }

    protected void addDependencySet( DependencySet dependencySet, Archiver archiver,
                                     AssemblerConfigurationSource configSource, boolean includeBaseDirectory )
        throws AssemblyFormattingException, ArchiveCreationException
    {
        MavenProject project = configSource.getProject();

        getLogger().info( "Processing DependencySet" );

        Set dependencyArtifacts = getDependencyArtifacts( project, dependencySet );

        for ( Iterator j = dependencyArtifacts.iterator(); j.hasNext(); )
        {
            Artifact artifact = (Artifact) j.next();

            AddArtifactTask task = new AddArtifactTask( artifact );

            task.setProject( project );
            task.setOutputDirectory( dependencySet.getOutputDirectory() );
            task.setFileNameMapping( dependencySet.getOutputFileNameMapping() );
            task.setIncludeBaseDirectory( includeBaseDirectory );
            task.setDirectoryMode( dependencySet.getDirectoryMode() );
            task.setFileMode( dependencySet.getFileMode() );
            task.setUnpack( dependencySet.isUnpack() );

            task.execute( archiver, configSource );
        }
    }

    protected Set getDependencyArtifacts( MavenProject project, DependencySet dependencySet )
    {
        Set dependencyArtifacts = new HashSet();

        Set projectArtifacts = project.getArtifacts();
        if ( projectArtifacts != null )
        {
            dependencyArtifacts.addAll( projectArtifacts );
        }

        AssemblyScopeArtifactFilter scopeFilter = new AssemblyScopeArtifactFilter( dependencySet.getScope() );

        FilterUtils.filterArtifacts( dependencyArtifacts, dependencySet.getIncludes(), dependencySet.getExcludes(),
                                     true, Collections.singletonList( scopeFilter ), getLogger() );

        return dependencyArtifacts;
    }

}
