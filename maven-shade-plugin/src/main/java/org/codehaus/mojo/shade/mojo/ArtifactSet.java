package org.codehaus.mojo.shade.mojo;

import java.util.Set;

/** @author Jason van Zyl */
public class ArtifactSet
{
    private Set includes;

    private Set excludes;

    public Set getIncludes()
    {
        return includes;
    }

    public Set getExcludes()
    {
        return excludes;
    }
}
