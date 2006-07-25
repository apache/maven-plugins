package org.apache.maven.plugin.assembly.archive.task;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.codehaus.plexus.archiver.Archiver;

public interface ArchiverTask
{
    
    public void execute( Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException;

}
