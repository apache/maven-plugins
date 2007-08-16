package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoBuilderConfigSourceWrapper;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoInfoWrapper;
import org.apache.maven.plugin.assembly.archive.task.AddDirectoryTask;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.repository.RepositoryAssembler;
import org.apache.maven.shared.repository.RepositoryAssemblyException;
import org.apache.maven.shared.repository.RepositoryBuilderConfigSource;
import org.apache.maven.shared.repository.model.RepositoryInfo;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
 *                   role-hint="repositories"
 */
public class RepositoryAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    /**
     * @plexus.requirement
     */
    private RepositoryAssembler repositoryAssembler;

    /**
     * @plexus.requirement
     */
    private DependencyResolver dependencyResolver;

    public RepositoryAssemblyPhase()
    {
        // used for plexus.
    }

    // introduced for testing.
    public RepositoryAssemblyPhase( RepositoryAssembler repositoryAssembler, DependencyResolver resolver )
    {
        this.repositoryAssembler = repositoryAssembler;
        dependencyResolver = resolver;
    }

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List repositoriesList = assembly.getRepositories();

        File tempRoot = configSource.getTemporaryRootDirectory();

        for ( Iterator i = repositoriesList.iterator(); i.hasNext(); )
        {
            Repository repository = (Repository) i.next();

            resolveDependencies(repository, configSource);

            File repositoryDirectory = new File( tempRoot, repository.getOutputDirectory() );

            if ( !repositoryDirectory.exists() )
            {
                repositoryDirectory.mkdirs();
            }

            try
            {
                getLogger().debug( "Assembling repository to: " + repositoryDirectory );
                repositoryAssembler.buildRemoteRepository( repositoryDirectory, wrap( repository ), wrap( configSource ) );
                getLogger().debug( "Finished assembling repository to: " + repositoryDirectory );
            }
            catch ( RepositoryAssemblyException e )
            {
                throw new ArchiveCreationException( "Failed to assemble repository: " + e.getMessage(), e );
            }

            String outputDirectory =
                AssemblyFormatUtils.getOutputDirectory( repository.getOutputDirectory(), configSource.getProject(),
                                                        null, configSource.getFinalName() );

            AddDirectoryTask task = new AddDirectoryTask( repositoryDirectory );

            task.setDirectoryMode( TypeConversionUtils.modeToInt( repository.getDirectoryMode(), getLogger() ) );
            task.setFileMode( TypeConversionUtils.modeToInt( repository.getFileMode(), getLogger() ) );
            task.setUseDefaultExcludes( repository.isUseDefaultExcludes() );
            task.setOutputDirectory( outputDirectory );

            task.execute( archiver, configSource );
        }
    }

    private void resolveDependencies( Repository repository, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException
    {
        MavenProject project = configSource.getProject();

        ArtifactRepository localRepository = configSource.getLocalRepository();

        List additionalRemoteRepositories = configSource.getRemoteRepositories();

        Set dependencyArtifacts;
        try
        {
            dependencyArtifacts = dependencyResolver
                .resolveDependencies( project, repository.getScope(), localRepository, additionalRemoteRepositories );

            if ( ( dependencyArtifacts != null ) && !dependencyArtifacts.isEmpty() )
            {
                dependencyArtifacts = new LinkedHashSet( dependencyArtifacts );
            }
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

        project.setDependencyArtifacts( dependencyArtifacts );
    }

    private RepositoryBuilderConfigSource wrap( AssemblerConfigurationSource configSource )
    {
        return new RepoBuilderConfigSourceWrapper( configSource );
    }

    private RepositoryInfo wrap( Repository repository )
    {
        return new RepoInfoWrapper( repository );
    }

}
