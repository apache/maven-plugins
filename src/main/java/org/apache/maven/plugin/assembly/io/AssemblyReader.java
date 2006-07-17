package org.apache.maven.plugin.assembly.io;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.model.Assembly;

import java.io.File;
import java.util.List;


public interface AssemblyReader
{

    public List readAssemblies( AssemblerConfigurationSource configSource )
        throws AssemblyReadException;

    public Assembly getAssemblyForDescriptorReference( String ref, AssemblerConfigurationSource configSource )
        throws AssemblyReadException;

    public Assembly getAssemblyFromDescriptorFile( File file, AssemblerConfigurationSource configSource )
        throws AssemblyReadException;

}
