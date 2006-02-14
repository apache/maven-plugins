package org.apache.maven.plugin.clean;

import org.apache.maven.shared.model.fileset.FileSet;

public class Fileset
    extends FileSet
{
    
    public String toString()
    {
        return "file-set: " + getDirectory() + " (included: " + getIncludes() + ", excluded: " + getExcludes() + ")";
    }

}
