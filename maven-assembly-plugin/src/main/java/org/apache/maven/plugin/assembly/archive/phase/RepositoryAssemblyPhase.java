package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoBuilderConfigSourceWrapper;
import org.apache.maven.plugin.assembly.archive.phase.wrappers.RepoInfoWrapper;
import org.apache.maven.plugin.assembly.archive.task.AddDirectoryTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.shared.repository.RepositoryAssembler;
import org.apache.maven.shared.repository.RepositoryAssemblyException;
import org.apache.maven.shared.repository.RepositoryBuilderConfigSource;
import org.apache.maven.shared.repository.model.RepositoryInfo;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.Iterator;
import java.util.List;

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
    
    public RepositoryAssemblyPhase()
    {
        // used for plexus.
    }
    
    // introduced for testing.
    public RepositoryAssemblyPhase( RepositoryAssembler repositoryAssembler )
    {
        this.repositoryAssembler = repositoryAssembler;
    }

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List repositoriesList = assembly.getRepositories();
        
        File tempRoot = configSource.getTemporaryRootDirectory();

        for ( Iterator i = repositoriesList.iterator(); i.hasNext(); )
        {
            Repository repository = (Repository) i.next();

            File repositoryDirectory = new File( tempRoot, repository.getOutputDirectory() );

            if ( !repositoryDirectory.exists() )
            {
                repositoryDirectory.mkdirs();
            }

            try
            {
                repositoryAssembler.buildRemoteRepository( repositoryDirectory, wrap( repository ), wrap( configSource ) );
            }
            catch ( RepositoryAssemblyException e )
            {
                throw new ArchiveCreationException( "Failed to assembly repository: " + e.getMessage(), e );
            }

            String outputDirectory =
                AssemblyFormatUtils.getOutputDirectory( repository.getOutputDirectory(), configSource.getProject(),
                                                        configSource.getFinalName() );
            
            AddDirectoryTask task = new AddDirectoryTask( repositoryDirectory );
            
            task.setDirectoryMode( Integer.decode( repository.getDirectoryMode() ).intValue() );
            task.setFileMode( Integer.decode( repository.getFileMode() ).intValue() );
            task.setUseDefaultExcludes( repository.isUseDefaultExcludes() );
            task.setOutputDirectory( outputDirectory );
            
            task.execute( archiver, configSource );
        }
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
