package org.apache.maven.plugin.assembly.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.assembly.filter.AssemblyExcludesArtifactFilter;
import org.apache.maven.plugin.assembly.filter.AssemblyIncludesArtifactFilter;
import org.apache.maven.project.MavenProject;

public final class FilterUtils
{
    
    private FilterUtils()
    {
    }

    public static void filterProjects( Set projects, List includes, List excludes, boolean actTransitively )
    {
        AndArtifactFilter filter = new AndArtifactFilter();

        if ( !includes.isEmpty() )
        {
            filter.add( new AssemblyIncludesArtifactFilter( includes, actTransitively ) );
        }
        if ( !excludes.isEmpty() )
        {
            filter.add( new AssemblyExcludesArtifactFilter( excludes, actTransitively ) );
        }
        
        for ( Iterator it = projects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();
            Artifact artifact = project.getArtifact();
            
            if ( !filter.include( artifact ) )
            {
                it.remove();
            }
        }
    }

    public static void filterArtifacts( Set artifacts, List includes, List excludes, boolean actTransitively, List additionalFilters )
    {
        AndArtifactFilter filter = new AndArtifactFilter();
        
        if ( additionalFilters != null && !additionalFilters.isEmpty() )
        {
            for ( Iterator it = additionalFilters.iterator(); it.hasNext(); )
            {
                ArtifactFilter additionalFilter = (ArtifactFilter) it.next();
                
                filter.add( additionalFilter );
            }
        }

        if ( !includes.isEmpty() )
        {
            filter.add( new AssemblyIncludesArtifactFilter( includes, actTransitively ) );
        }
        if ( !excludes.isEmpty() )
        {
            filter.add( new AssemblyExcludesArtifactFilter( excludes, actTransitively ) );
        }
        
        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            
            if ( !filter.include( artifact ) )
            {
                it.remove();
            }
        }
    }

}
