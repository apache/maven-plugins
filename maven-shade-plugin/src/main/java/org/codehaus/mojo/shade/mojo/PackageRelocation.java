package org.codehaus.mojo.shade.mojo;

import java.util.List;

/** @author Jason van Zyl */
public class PackageRelocation
{
    private String pattern;

    private List excludes;

    public String getPattern()
    {
        return pattern;
    }

    public List getExcludes()
    {
        return excludes;
    }
}
