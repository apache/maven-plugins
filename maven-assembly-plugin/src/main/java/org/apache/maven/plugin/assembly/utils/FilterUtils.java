package org.apache.maven.plugin.assembly.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.StatisticsReportingArtifactFilter;
import org.codehaus.plexus.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @version $Id$
 */
public final class FilterUtils
{

    private FilterUtils()
    {
    }

    public static void filterProjects( final Set<MavenProject> projects, final List<String> includes,
                                       final List<String> excludes, final boolean actTransitively, final Logger logger )
    {
        final List<PatternIncludesArtifactFilter> allFilters = new ArrayList<PatternIncludesArtifactFilter>();

        final AndArtifactFilter filter = new AndArtifactFilter();

        if ( !includes.isEmpty() )
        {
            final PatternIncludesArtifactFilter includeFilter =
                new PatternIncludesArtifactFilter( includes, actTransitively );

            filter.add( includeFilter );
            allFilters.add( includeFilter );
        }
        if ( !excludes.isEmpty() )
        {
            final PatternExcludesArtifactFilter excludeFilter =
                new PatternExcludesArtifactFilter( excludes, actTransitively );

            filter.add( excludeFilter );
            allFilters.add( excludeFilter );
        }

        for ( final Iterator<MavenProject> it = projects.iterator(); it.hasNext(); )
        {
            final MavenProject project = it.next();
            final Artifact artifact = project.getArtifact();

            if ( !filter.include( artifact ) )
            {
                it.remove();
            }
        }

        for (final PatternIncludesArtifactFilter f : allFilters) {
            if (f instanceof StatisticsReportingArtifactFilter) {
                ((StatisticsReportingArtifactFilter) f).reportMissedCriteria(logger);
            }
        }
    }

    public static void filterArtifacts( final Set<Artifact> artifacts, final List<String> includes,
                                        final List<String> excludes, final boolean strictFiltering,
                                        final boolean actTransitively, final Logger logger,
                                        final ArtifactFilter... additionalFilters )
        throws InvalidAssemblerConfigurationException
    {
        final List<ArtifactFilter> allFilters = new ArrayList<ArtifactFilter>();

        final AndArtifactFilter filter = new AndArtifactFilter();

        if ( additionalFilters != null && additionalFilters.length > 0 )
        {
            for ( final ArtifactFilter additionalFilter : additionalFilters )
            {
                if ( additionalFilter != null )
                {
                    filter.add( additionalFilter );
                }
            }
        }

        if ( !includes.isEmpty() )
        {
            final ArtifactFilter includeFilter = new PatternIncludesArtifactFilter( includes, actTransitively );

            filter.add( includeFilter );

            allFilters.add( includeFilter );
        }

        if ( !excludes.isEmpty() )
        {
            final ArtifactFilter excludeFilter = new PatternExcludesArtifactFilter( excludes, actTransitively );

            filter.add( excludeFilter );

            allFilters.add( excludeFilter );
        }

        // FIXME: Why is this added twice??
        // if ( additionalFilters != null && !additionalFilters.isEmpty() )
        // {
        // allFilters.addAll( additionalFilters );
        // }

        for ( final Iterator<Artifact> it = artifacts.iterator(); it.hasNext(); )
        {
            final Artifact artifact = it.next();

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

        for (final ArtifactFilter f : allFilters) {
            if (f instanceof StatisticsReportingArtifactFilter) {
                final StatisticsReportingArtifactFilter sFilter = (StatisticsReportingArtifactFilter) f;

                if (strictFiltering && sFilter.hasMissedCriteria()) {
                    throw new InvalidAssemblerConfigurationException(
                            "One or more filters had unmatched criteria. Check debug log for more information.");
                }
            }
        }
    }

    public static void reportFilteringStatistics( final Collection<ArtifactFilter> filters, final Logger logger )
    {
        for (final ArtifactFilter f : filters) {
            if (f instanceof StatisticsReportingArtifactFilter) {
                final StatisticsReportingArtifactFilter sFilter = (StatisticsReportingArtifactFilter) f;

                if (logger.isDebugEnabled()) {
                    logger.debug("Statistics for " + sFilter + "\n");
                }

                sFilter.reportMissedCriteria(logger);
                sFilter.reportFilteredArtifacts(logger);
            }
        }
    }

}
