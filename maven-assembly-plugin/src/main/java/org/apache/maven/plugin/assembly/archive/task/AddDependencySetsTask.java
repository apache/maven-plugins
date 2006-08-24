package org.apache.maven.plugin.assembly.archive.task;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.filter.AssemblyScopeArtifactFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AddDependencySetsTask
    implements ArchiverTask
{

    private static final List NON_ARCHIVE_DEPENDENCY_TYPES;

    static
    {
        List nonArch = new ArrayList();

        nonArch.add( "pom" );

        NON_ARCHIVE_DEPENDENCY_TYPES = Collections.unmodifiableList( nonArch );
    }

    private final List dependencySets;

    private boolean includeBaseDirectory = true;

    private final Logger logger;

    private final MavenProject project;

    private final MavenProjectBuilder projectBuilder;

    private String defaultOutputDirectory;

    private String defaultOutputFileNameMapping;

    public AddDependencySetsTask( List dependencySets, MavenProject project, MavenProjectBuilder projectBuilder,
                                  Logger logger )
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

            MavenProject depProject;
            try
            {
                depProject =
                    projectBuilder.buildFromRepository( depArtifact, configSource.getRemoteRepositories(),
                                                        configSource.getLocalRepository() );
            }
            catch ( ProjectBuildingException e )
            {
                throw new ArchiveCreationException( "Error retrieving POM of module-dependency: " + depArtifact.getId()
                                + "; Reason: " + e.getMessage(), e );
            }

            if ( NON_ARCHIVE_DEPENDENCY_TYPES.contains( depArtifact.getType() ) )
            {
                addNonArchiveDependency( depArtifact, depProject, dependencySet, archiver );
            }
            else
            {
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
    }

    private void addNonArchiveDependency( Artifact depArtifact, MavenProject depProject, DependencySet dependencySet,
                                          Archiver archiver )
        throws AssemblyFormattingException, ArchiveCreationException
    {
        File source = depArtifact.getFile();

        String outputDirectory = dependencySet.getOutputDirectory();

        outputDirectory =
            AssemblyFormatUtils.getOutputDirectory( outputDirectory, depProject, depProject.getBuild().getFinalName(),
                                                    includeBaseDirectory );
        String destName =
            AssemblyFormatUtils.evaluateFileNameMapping( dependencySet.getOutputFileNameMapping(), depArtifact );

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
            archiver.addFile( source, target, Integer.parseInt( dependencySet.getFileMode(), 8 ) );
        }
        catch ( ArchiverException e )
        {
            throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
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
