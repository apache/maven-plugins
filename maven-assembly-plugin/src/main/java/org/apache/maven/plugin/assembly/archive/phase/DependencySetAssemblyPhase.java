package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.AddDependencySetsTask;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
 *                   role-hint="dependency-sets"
 */
public class DependencySetAssemblyPhase
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
    
    public DependencySetAssemblyPhase()
    {
        // used for plexus init
    }
    
    public DependencySetAssemblyPhase( MavenProjectBuilder projectBuilder, DependencyResolver dependencyResolver, Logger logger )
    {
        this.projectBuilder = projectBuilder;
        this.dependencyResolver = dependencyResolver;
        
        enableLogging( logger );
    }

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        AddDependencySetsTask task =
            new AddDependencySetsTask( assembly.getDependencySets(), configSource.getProject(), projectBuilder,
                                       dependencyResolver, getLogger() );
        
        task.execute( archiver, configSource );
    }
}
