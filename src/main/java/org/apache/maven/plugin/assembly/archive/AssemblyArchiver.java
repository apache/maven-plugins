package org.apache.maven.plugin.assembly.archive;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;

import java.io.File;


public interface AssemblyArchiver
{
    String ROLE = AssemblyArchiver.class.getName();

    File createArchive( Assembly assembly, String fullName, String format, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException;

}
