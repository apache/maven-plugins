package org.apache.maven.plugin.assembly.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.StatisticsReportingArtifactFilter;
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
            PatternIncludesArtifactFilter includeFilter = new PatternIncludesArtifactFilter( includes,
                                                                                               actTransitively );

            filter.add( includeFilter );
            allFilters.add( includeFilter );
        }
        if ( !excludes.isEmpty() )
        {
            PatternExcludesArtifactFilter excludeFilter = new PatternExcludesArtifactFilter( excludes,
                                                                                               actTransitively );

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

            if ( f instanceof StatisticsReportingArtifactFilter )
            {
                ( (StatisticsReportingArtifactFilter) f ).reportMissedCriteria( logger );
            }
        }
    }

    public static void filterArtifacts( Set artifacts, List includes, List excludes, boolean strictFiltering,
                                        boolean actTransitively, List additionalFilters, Logger logger )
        throws InvalidAssemblerConfigurationException
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
            ArtifactFilter includeFilter = new PatternIncludesArtifactFilter( includes, actTransitively );

            filter.add( includeFilter );

            allFilters.add( includeFilter );
        }

        if ( !excludes.isEmpty() )
        {
            ArtifactFilter excludeFilter = new PatternExcludesArtifactFilter( excludes, actTransitively );

            filter.add( excludeFilter );

            allFilters.add( excludeFilter );
        }

        if ( additionalFilters != null && !additionalFilters.isEmpty() )
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

        reportFilteringStatistics( allFilters, logger );

        for ( Iterator it = allFilters.iterator(); it.hasNext(); )
        {
            ArtifactFilter f = (ArtifactFilter) it.next();

            if ( f instanceof StatisticsReportingArtifactFilter )
            {
                StatisticsReportingArtifactFilter sFilter = (StatisticsReportingArtifactFilter) f;

                if ( strictFiltering && sFilter.hasMissedCriteria() )
                {
                    throw new InvalidAssemblerConfigurationException(
                                                                      "One or more filters had unmatched criteria. Check debug log for more information." );
                }
            }
        }
    }

    public static void reportFilteringStatistics( Collection filters, Logger logger )
    {
        for ( Iterator it = filters.iterator(); it.hasNext(); )
        {
            ArtifactFilter f = (ArtifactFilter) it.next();

            if ( f instanceof StatisticsReportingArtifactFilter )
            {
                StatisticsReportingArtifactFilter sFilter = (StatisticsReportingArtifactFilter) f;

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Statistics for " + sFilter + "\n" );
                }

                sFilter.reportMissedCriteria( logger );
                sFilter.reportFilteredArtifacts( logger );
            }
        }
    }

}
