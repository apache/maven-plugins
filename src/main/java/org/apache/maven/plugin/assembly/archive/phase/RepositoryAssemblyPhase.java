package org.apache.maven.plugin.assembly.archive.phase;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.repository.RepositoryAssembler;
import org.apache.maven.plugin.assembly.repository.RepositoryAssemblyException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.Repository;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
 *                   role-hint="repositories"
 */
public class RepositoryAssemblyPhase
    implements AssemblyArchiverPhase
{

    /**
     * @plexus.requirement
     */
    private RepositoryAssembler repositoryAssembler;

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List modulesList = assembly.getRepositories();
        
        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();
        File tempRoot = configSource.getTemporaryRootDirectory();

        for ( Iterator i = modulesList.iterator(); i.hasNext(); )
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

            try
            {
                if ( includeBaseDirectory )
                {
                    archiver.addDirectory( repositoryDirectory, repository.getOutputDirectory() + "/" );
                }
                else
                {
                    archiver.addDirectory( repositoryDirectory );
                }
            }
            catch ( ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding directory to archive: " + e.getMessage(), e );
            }
        }
    }

}
