package org.apache.maven.plugin.assembly.archive.phase.wrappers;

import org.apache.maven.plugins.assembly.model.GroupVersionAlignment;

import java.util.List;

public class GroupVersionAlignmentWrapper
{

    private final GroupVersionAlignment alignment;

    public GroupVersionAlignmentWrapper( GroupVersionAlignment alignment )
    {
        this.alignment = alignment;
    }

    public List getExcludes()
    {
        return alignment.getExcludes();
    }

    public String getId()
    {
        return alignment.getId();
    }

    public String getVersion()
    {
        return alignment.getVersion();
    }
    
}
