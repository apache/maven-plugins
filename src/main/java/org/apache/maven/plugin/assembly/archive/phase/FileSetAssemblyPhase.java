package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveAssemblyUtils;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
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

    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource,
                         ComponentsXmlArchiverFileFilter componentsXmlFilter )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List fileSets = assembly.getFileSets();
        boolean includeBaseDirectory = assembly.isIncludeBaseDirectory();

        ArchiveAssemblyUtils.addFileSets( archiver, fileSets, includeBaseDirectory, configSource, componentsXmlFilter, getLogger() );
    }

}
