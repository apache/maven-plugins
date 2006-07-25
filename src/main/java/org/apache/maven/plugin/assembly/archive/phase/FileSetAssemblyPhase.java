package org.apache.maven.plugin.assembly.archive.phase;

import java.util.List;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveAssemblyUtils;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;

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
        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();

        ArchiveAssemblyUtils.addFileSets( archiver, fileSets, includeBaseDirectory, configSource, getLogger() );
    }

}
