package org.apache.maven.plugin.assembly;

import java.util.Map;

public interface AssemblyContext
{
    
    AssemblyContext setManagedVersionMap( Map managedVersions );
    
    Map getManagedVersionMap();

}
