package org.apache.maven.plugin.assembly.archive.task;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

    private final Logger logger;

    private final MavenProject project;

    private final MavenProjectBuilder projectBuilder;

    private String defaultOutputDirectory;

    private String defaultOutputFileNameMapping;

    private final DependencyResolver dependencyResolver;

    public AddDependencySetsTask( List dependencySets, MavenProject project, MavenProjectBuilder projectBuilder,
                                  DependencyResolver dependencyResolver, Logger logger )
    {
        this.dependencySets = dependencySets;
        this.project = project;
        this.projectBuilder = projectBuilder;
        this.dependencyResolver = dependencyResolver;
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

        List deps = project.getDependencies();
        if ( deps == null || deps.isEmpty() )
        {
            logger.debug( "Project " + project.getId() + " has no dependencies. Skipping dependency set addition." );
        }

        for ( Iterator i = dependencySets.iterator(); i.hasNext(); )
        {
            DependencySet dependencySet = (DependencySet) i.next();

            addDependencySet( dependencySet, archiver, configSource );
        }
    }

    protected void addDependencySet( DependencySet dependencySet, Archiver archiver,
                                     AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException, ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        logger.info( "Processing DependencySet (output=" + dependencySet.getOutputDirectory() + ")" );

        Set dependencyArtifacts = resolveDependencyArtifacts( dependencySet, configSource );

        for ( Iterator j = dependencyArtifacts.iterator(); j.hasNext(); )
        {
            Artifact depArtifact = (Artifact) j.next();

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
                task.setDirectoryMode( dependencySet.getDirectoryMode() );
                task.setFileMode( dependencySet.getFileMode() );
                task.setUnpack( dependencySet.isUnpack() );

                task.execute( archiver, configSource );
            }
        }
    }

    protected Set resolveDependencyArtifacts( DependencySet dependencySet, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, InvalidAssemblerConfigurationException
    {
        ArtifactRepository localRepository = configSource.getLocalRepository();

        List additionalRemoteRepositories = configSource.getRemoteRepositories();

        Set dependencyArtifacts;
        try
        {
            dependencyArtifacts = dependencyResolver
                .resolveDependencies( project, dependencySet.getScope(), localRepository, additionalRemoteRepositories );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ArchiveCreationException( "Failed to resolve dependencies for project: " + project.getId(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ArchiveCreationException( "Failed to resolve dependencies for project: " + project.getId(), e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new ArchiveCreationException( "Failed to resolve dependencies for project: " + project.getId(), e );
        }

        FilterUtils.filterArtifacts( dependencyArtifacts, dependencySet.getIncludes(), dependencySet.getExcludes(),
                                     dependencySet.isUseStrictFiltering(), true, Collections.EMPTY_LIST, logger );

        return dependencyArtifacts;
    }

    protected void addNonArchiveDependency( Artifact depArtifact, MavenProject depProject, DependencySet dependencySet,
                                            Archiver archiver )
        throws AssemblyFormattingException, ArchiveCreationException
    {
        File source = depArtifact.getFile();

        String outputDirectory = dependencySet.getOutputDirectory();

        outputDirectory = AssemblyFormatUtils.getOutputDirectory( outputDirectory, depProject, depProject.getBuild()
            .getFinalName() );
        String destName = AssemblyFormatUtils.evaluateFileNameMapping( dependencySet.getOutputFileNameMapping(),
                                                                       depArtifact );

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
