package org.apache.maven.plugin.assembly.io;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.model.Assembly;

import java.io.File;
import java.io.Reader;
import java.util.List;


public interface AssemblyReader
{

    public List readAssemblies( AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException;

    public Assembly getAssemblyForDescriptorReference( String ref, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException;

    public Assembly getAssemblyFromDescriptorFile( File file, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException;

    public Assembly readAssembly( Reader reader, String locationDescription, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException;

    public void includeSiteInAssembly( Assembly assembly, AssemblerConfigurationSource configSource )
        throws MojoFailureException, InvalidAssemblerConfigurationException;

}
