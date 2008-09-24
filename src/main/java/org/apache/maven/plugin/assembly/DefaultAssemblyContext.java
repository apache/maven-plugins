package org.apache.maven.plugin.assembly;

import java.util.Map;

public class DefaultAssemblyContext
    implements AssemblyContext
{

    private Map managedVersions;

    public Map getManagedVersionMap()
    {
        return managedVersions;
    }

    public AssemblyContext setManagedVersionMap( Map managedVersions )
    {
        this.managedVersions = managedVersions;
        return this;
    }

}
