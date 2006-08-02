package org.apache.maven.plugin.assembly.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.assembly.filter.AssemblyExcludesArtifactFilter;
import org.apache.maven.plugin.assembly.filter.AssemblyIncludesArtifactFilter;
import org.apache.maven.plugin.assembly.filter.StatisticsReportingFilter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;

public final class FilterUtils
{

    private FilterUtils()
    {
    }

    public static void filterProjects( Set projects, List includes, List excludes, boolean actTransitively,
                                       Logger logger )
    {
        List allFilters = new ArrayList();
        
        AndArtifactFilter filter = new AndArtifactFilter();

        if ( !includes.isEmpty() )
        {
            AssemblyIncludesArtifactFilter includeFilter =
                new AssemblyIncludesArtifactFilter( includes, actTransitively );
            
            filter.add( includeFilter );
            allFilters.add( includeFilter );
        }
        if ( !excludes.isEmpty() )
        {
            AssemblyExcludesArtifactFilter excludeFilter =
                new AssemblyExcludesArtifactFilter( excludes, actTransitively );
                
            filter.add( excludeFilter );
            allFilters.add( excludeFilter );
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
        
        for ( Iterator it = allFilters.iterator(); it.hasNext(); )
        {
            ArtifactFilter f = (ArtifactFilter) it.next();
            
            if ( f instanceof StatisticsReportingFilter )
            {
                ((StatisticsReportingFilter) f).reportMissedCriteria( logger );
            }
        }
    }

    public static void filterArtifacts( Set artifacts, List includes, List excludes, boolean actTransitively,
                                        List additionalFilters, Logger logger )
    {
        List allFilters = new ArrayList();
        
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
            ArtifactFilter includeFilter = new AssemblyIncludesArtifactFilter( includes, actTransitively );
            
            filter.add( includeFilter );
            
            allFilters.add( includeFilter );
        }
        if ( !excludes.isEmpty() )
        {
            ArtifactFilter excludeFilter = new AssemblyExcludesArtifactFilter( excludes, actTransitively );
            
            filter.add( excludeFilter );
            
            allFilters.add( excludeFilter );
        }

        if ( !additionalFilters.isEmpty() )
        {
            allFilters.addAll( additionalFilters );
        }
        
        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            if ( !filter.include( artifact ) )
            {
                it.remove();
                
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( artifact.getId() + " was removed by one or more filters." );
                }
            }
        }
        
        for ( Iterator it = allFilters.iterator(); it.hasNext(); )
        {
            ArtifactFilter f = (ArtifactFilter) it.next();
            
            if ( f instanceof StatisticsReportingFilter )
            {
                StatisticsReportingFilter sFilter = (StatisticsReportingFilter) f;
                
                if( logger.isDebugEnabled() )
                {
                    logger.debug( "Statistics for " + sFilter + "\n" );
                }
                
                sFilter.reportMissedCriteria( logger );
                sFilter.reportFilteredArtifacts( logger );
            }
        }
    }

}
