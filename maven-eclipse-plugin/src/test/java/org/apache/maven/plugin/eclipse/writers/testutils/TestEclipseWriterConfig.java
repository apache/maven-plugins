package org.apache.maven.plugin.eclipse.writers.testutils;

import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.writers.EclipseWriterConfig;
import org.apache.maven.plugin.ide.IdeDependency;

import java.util.ArrayList;
import java.util.List;

public class TestEclipseWriterConfig
    extends EclipseWriterConfig
{

    public List getBuildCommands()
    {
        List result = super.getBuildCommands();
        
        if ( result == null )
        {
            result = new ArrayList();
        }
        
        return result;
    }

    public List getClasspathContainers()
    {
        List result = super.getClasspathContainers();
        
        if ( result == null )
        {
            result = new ArrayList();
        }
        
        return result;
    }

    public IdeDependency[] getDeps()
    {
        IdeDependency[] deps = super.getDeps();
        
        if ( deps == null )
        {
            deps = new IdeDependency[0];
        }
        
        return deps;
    }

    public List getProjectnatures()
    {
        List result = super.getProjectnatures();
        
        if ( result == null )
        {
            result = new ArrayList();
        }
        
        return result;
    }

    public EclipseSourceDir[] getSourceDirs()
    {
        EclipseSourceDir[] dirs = super.getSourceDirs();
        
        if ( dirs == null )
        {
            dirs = new EclipseSourceDir[0];
        }
        
        return dirs;
    }

}
