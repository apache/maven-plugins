package org.apache.maven.plugin.assembly.archive.task;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.filter.AssemblyScopeArtifactFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AddDependencySetsTask
    implements ArchiverTask
{

    private final List dependencySets;

    private boolean includeBaseDirectory = true;

    private final Logger logger;

    private final MavenProject project;

    private final MavenProjectBuilder projectBuilder;

    private String defaultOutputDirectory;

    private String defaultOutputFileNameMapping;

    public AddDependencySetsTask( List dependencySets, MavenProject project, MavenProjectBuilder projectBuilder, Logger logger )
    {
        this.dependencySets = dependencySets;
        this.project = project;
        this.projectBuilder = projectBuilder;
        this.logger = logger;
    }

    public void execute( Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        if ( dependencySets == null || dependencySets.isEmpty() )
        {
            logger.debug( "No dependency sets specified." );
            return;
        }
        
        for ( Iterator i = dependencySets.iterator(); i.hasNext(); )
        {
            DependencySet dependencySet = ( DependencySet ) i.next();

            addDependencySet( dependencySet, archiver, configSource, includeBaseDirectory );
        }
    }

    protected void addDependencySet( DependencySet dependencySet, Archiver archiver,
                                     AssemblerConfigurationSource configSource, boolean includeBaseDirectory )
        throws AssemblyFormattingException, ArchiveCreationException
    {
        logger.info( "Processing DependencySet" );

        Set dependencyArtifacts = getDependencyArtifacts( project, dependencySet );

        for ( Iterator j = dependencyArtifacts.iterator(); j.hasNext(); )
        {
            Artifact depArtifact = ( Artifact ) j.next();
            
            if ( "pom".equals( depArtifact.getType() ) )
            {
                logger.info( "Skipping POM artifact (found among dependencies for " + project.getId() + "): " + depArtifact.getId() );
                continue;
            }

            MavenProject depProject;
            try
            {
                depProject = projectBuilder.buildFromRepository( depArtifact, configSource.getRemoteRepositories(),
                                                    configSource.getLocalRepository() );
            }
            catch ( ProjectBuildingException e )
            {
                throw new ArchiveCreationException( "Error retrieving POM of module-dependency: " + depArtifact.getId()
                                + "; Reason: " + e.getMessage(), e );
            }
            
            AddArtifactTask task = new AddArtifactTask( depArtifact );
            
            task.setProject( depProject );
            task.setOutputDirectory( dependencySet.getOutputDirectory(), defaultOutputDirectory );
            task.setFileNameMapping( dependencySet.getOutputFileNameMapping(), defaultOutputFileNameMapping );
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
                                     true, Collections.singletonList( scopeFilter ), logger );

        return dependencyArtifacts;
    }

    public boolean isIncludeBaseDirectory()
    {
        return includeBaseDirectory;
    }

    public void setIncludeBaseDirectory( boolean includeBaseDirectory )
    {
        this.includeBaseDirectory = includeBaseDirectory;
    }

    public List getDependencySets()
    {
        return dependencySets;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public String getDefaultOutputDirectory()
    {
        return defaultOutputDirectory;
    }

    public void setDefaultOutputDirectory( String defaultOutputDirectory )
    {
        this.defaultOutputDirectory = defaultOutputDirectory;
    }

    public String getDefaultOutputFileNameMapping()
    {
        return defaultOutputFileNameMapping;
    }

    public void setDefaultOutputFileNameMapping( String defaultOutputFileNameMapping )
    {
        this.defaultOutputFileNameMapping = defaultOutputFileNameMapping;
    }
}
