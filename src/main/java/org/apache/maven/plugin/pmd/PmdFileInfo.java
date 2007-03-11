package org.apache.maven.plugin.pmd;

import java.io.File;

import org.apache.maven.project.MavenProject;

public class PmdFileInfo {
    
    MavenProject project;
    File sourceDir;
    String xref;
    
    
    public PmdFileInfo(MavenProject project,
                       File dir,
                       String x) 
    {
        this.project = project;
        this.sourceDir = dir;
        this.xref = x;
    }
    
    
    public String getXrefLocation()
    {
        return xref;
    }
    
    public File getSourceDirectory() 
    {
        return sourceDir;
    }
    
    public MavenProject getProject() 
    {
        return project;
    }
    

}
