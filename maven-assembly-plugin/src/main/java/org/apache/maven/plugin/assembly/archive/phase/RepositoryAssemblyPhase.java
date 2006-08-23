package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.AddDirectoryTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.repository.RepositoryAssembler;
import org.apache.maven.plugin.assembly.repository.RepositoryAssemblyException;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.Repository;
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
                repositoryAssembler.assemble( repositoryDirectory, repository, configSource );
            }
            catch ( RepositoryAssemblyException e )
            {
                throw new ArchiveCreationException( "Failed to assembly repository: " + e.getMessage(), e );
            }

            String outputDirectory =
                AssemblyFormatUtils.getOutputDirectory( repository.getOutputDirectory(), configSource.getProject(),
                                                        configSource.getFinalName(),
                                                        assembly.isIncludeBaseDirectory() );
            
            AddDirectoryTask task = new AddDirectoryTask( repositoryDirectory );
            
            task.setDirectoryMode( Integer.parseInt( repository.getDirectoryMode(), 8 ) );
            task.setFileMode( Integer.parseInt( repository.getFileMode(), 8 ) );
            task.setOutputDirectory( outputDirectory );
            
            task.execute( archiver, configSource );
        }
    }

}
