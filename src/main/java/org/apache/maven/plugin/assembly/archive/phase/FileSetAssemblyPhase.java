package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.AddFileSetsTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.List;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
 *                   role-hint="file-sets"
 */
public class FileSetAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List fileSets = assembly.getFileSets();
        
        if ( fileSets != null && !fileSets.isEmpty() )
        {
            AddFileSetsTask task = new AddFileSetsTask( fileSets );
            
            task.setLogger( getLogger() );
            task.setProject( configSource.getProject() );
            
            task.execute( archiver, configSource );
        }
    }

}
