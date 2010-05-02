package org.apache.maven.report.projectinfo;

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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributeSet;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates the Dependency Convergence report for reactor builds.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 * @goal dependency-convergence
 * @aggregator
 */
public class DependencyConvergenceReport
    extends AbstractProjectInfoReport
{
    private static final int PERCENTAGE = 100;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The projects in the current build. The effective-POM for
     * each of these projects will written.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "dependency-convergence";
    }

    protected String getI18Nsection()
    {
        return "dependency-convergence";
    }

    /** {@inheritDoc} */
    public boolean canGenerateReport()
    {
        // only generate the convergency report if we are running a reactor build
        return reactorProjects.size() > 1;
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        Sink sink = getSink();

        sink.head();
        sink.title();
        sink.text( getI18nString( locale, "title" ) );
        sink.title_();
        sink.head_();

        sink.body();

        sink.section1();

        sink.sectionTitle1();
        sink.text( getI18nString( locale, "title" ) );
        sink.sectionTitle1_();

        Map dependencyMap = getDependencyMap();

        // legend
        generateLegend( locale, sink );

        sink.lineBreak();

        // stats
        generateStats( locale, sink, dependencyMap );

        sink.section1_();

        // convergence
        generateConvergence( locale, sink, dependencyMap );

        sink.body_();
        sink.flush();
        sink.close();
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Generate the convergenec table for all dependencies
     *
     * @param locale
     * @param sink
     * @param dependencyMap
     */
    private void generateConvergence( Locale locale, Sink sink, Map dependencyMap )
    {
        sink.section2();

        sink.sectionTitle2();
        sink.text( getI18nString( locale, "convergence.caption" ) );
        sink.sectionTitle2_();

        Iterator it = dependencyMap.keySet().iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();
            List depList = (List) dependencyMap.get( key );

            sink.section3();
            sink.sectionTitle3();
            sink.text( key );
            sink.sectionTitle3_();

            generateDependencyDetails( sink, depList );

            sink.section3_();
        }

        sink.section2_();
    }

    /**
     * Generate the detail table for a given dependency
     *
     * @param sink
     * @param depList
     */
    private void generateDependencyDetails( Sink sink, List depList )
    {
        sink.table();

        Map artifactMap = getSortedUniqueArtifactMap( depList );

        sink.tableRow();

        sink.tableCell( );
        if ( artifactMap.size() > 1 )
        {
            iconError( sink );
        }
        else
        {
            iconSuccess( sink );
        }
        sink.tableCell_();

        sink.tableCell();

        sink.table();

        Iterator it = artifactMap.keySet().iterator();
        while ( it.hasNext() )
        {
            String version = (String) it.next();
            sink.tableRow();
            sink.tableCell( new SinkEventAttributeSet( new String[] {SinkEventAttributeSet.WIDTH, "25%"} ) );
            sink.text( version );
            sink.tableCell_();

            sink.tableCell();
            generateVersionDetails( sink, artifactMap, version );
            sink.tableCell_();

            sink.tableRow_();
        }
        sink.table_();
        sink.tableCell_();

        sink.tableRow_();

        sink.table_();
    }

    private void generateVersionDetails( Sink sink, Map artifactMap, String version )
    {
        sink.numberedList( 1 ); // Use lower alpha numbering
        List depList = (List) artifactMap.get( version );
        Collections.sort( depList, new ReverseDependencyLinkComparator() );
        Iterator it = depList.iterator();
        while ( it.hasNext() )
        {
            ReverseDependencyLink rdl = (ReverseDependencyLink) it.next();
            sink.numberedListItem();
            if ( StringUtils.isNotEmpty( rdl.project.getUrl() ) )
            {
                sink.link( rdl.project.getUrl() );
            }
            sink.text( rdl.project.getGroupId() + ":" + rdl.project.getArtifactId() );
            if ( StringUtils.isNotEmpty( rdl.project.getUrl() ) )
            {
                sink.link_();
            }
            sink.numberedListItem_();
        }
        sink.numberedList_();
    }

    /**
     * Produce a Map of relationships between dependencies (its version) and
     * reactor projects.
     *
     * This is the structure of the Map:
     * <pre>
     * +--------------------+----------------------------------+
     * | key                | value                            |
     * +--------------------+----------------------------------+
     * | version of a       | A List of ReverseDependencyLinks |
     * | dependency         | which each look like this:       |
     * |                    | +------------+-----------------+ |
     * |                    | | dependency | reactor project | |
     * |                    | +------------+-----------------+ |
     * +--------------------+----------------------------------+
     * </pre>
     *
     * @return A Map of sorted unique artifacts
     */
    private Map getSortedUniqueArtifactMap( List depList )
    {
        Map uniqueArtifactMap = new TreeMap();

        Iterator it = depList.iterator();
        while ( it.hasNext() )
        {
            ReverseDependencyLink rdl = (ReverseDependencyLink) it.next();
            String key = rdl.getDependency().getVersion();
            List projectList = (List) uniqueArtifactMap.get( key );
            if ( projectList == null )
            {
                projectList = new ArrayList();
            }
            projectList.add( rdl );
            uniqueArtifactMap.put( key, projectList );
        }

        return uniqueArtifactMap;
    }

    /**
     * Generate the legend table
     *
     * @param locale
     * @param sink
     */
    private void generateLegend( Locale locale, Sink sink )
    {
        sink.table();
        sink.tableCaption();
        sink.bold();
        sink.text( getI18nString( locale, "legend" ) );
        sink.bold_();
        sink.tableCaption_();

        sink.tableRow();

        sink.tableCell( );
        iconSuccess( sink );
        sink.tableCell_();
        sink.tableCell();
        sink.text( getI18nString( locale, "legend.shared" ) );
        sink.tableCell_();

        sink.tableRow_();

        sink.tableRow();

        sink.tableCell( );
        iconError( sink );
        sink.tableCell_();
        sink.tableCell();
        sink.text( getI18nString( locale, "legend.different" ) );
        sink.tableCell_();

        sink.tableRow_();

        sink.table_();
    }

    /**
     * Generate the statistic table
     *
     * @param locale
     * @param sink
     * @param dependencyMap
     */
    private void generateStats( Locale locale, Sink sink, Map dependencyMap )
    {
        int depCount = dependencyMap.size();
        int artifactCount = 0;
        int snapshotCount = 0;

        Iterator it = dependencyMap.values().iterator();
        while ( it.hasNext() )
        {
            List depList = (List) it.next();
            Map artifactMap = getSortedUniqueArtifactMap( depList );
            snapshotCount += countSnapshots( artifactMap );
            artifactCount += artifactMap.size();
        }

        int convergence = (int) ( ( (double) depCount / (double) artifactCount ) * PERCENTAGE );

        // Create report
        sink.table();
        sink.tableCaption();
        sink.bold();
        sink.text( getI18nString( locale, "stats.caption" ) );
        sink.bold_();
        sink.tableCaption_();

        sink.tableRow();
        sink.tableHeaderCell( );
        sink.text( getI18nString( locale, "stats.subprojects" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        sink.text( String.valueOf( reactorProjects.size() ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell( );
        sink.text( getI18nString( locale, "stats.dependencies" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        sink.text( String.valueOf( depCount ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell( );
        sink.text( getI18nString( locale, "stats.artifacts" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        sink.text( String.valueOf( artifactCount ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell( );
        sink.text( getI18nString( locale, "stats.snapshots" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        sink.text( String.valueOf( snapshotCount ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell( );
        sink.text( getI18nString( locale, "stats.convergence" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        if ( convergence < PERCENTAGE )
        {
            iconError( sink );
        }
        else
        {
            iconSuccess( sink );
        }
        sink.nonBreakingSpace();
        sink.bold();
        sink.text( String.valueOf( convergence ) + "%" );
        sink.bold_();
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell( );
        sink.text( getI18nString( locale, "stats.readyrelease" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        if ( convergence >= PERCENTAGE && snapshotCount <= 0 )
        {
            iconSuccess( sink );
            sink.nonBreakingSpace();
            sink.bold();
            sink.text( getI18nString( locale, "stats.readyrelease.success" ) );
            sink.bold_();
        }
        else
        {
            iconError( sink );
            sink.nonBreakingSpace();
            sink.bold();
            sink.text( getI18nString( locale, "stats.readyrelease.error" ) );
            sink.bold_();
            if ( convergence < PERCENTAGE )
            {
                sink.lineBreak();
                sink.text( getI18nString( locale, "stats.readyrelease.error.convergence" ) );
            }
            if ( snapshotCount > 0 )
            {
                sink.lineBreak();
                sink.text( getI18nString( locale, "stats.readyrelease.error.snapshots" ) );
            }
        }
        sink.tableCell_();
        sink.tableRow_();

        sink.table_();
    }

    private int countSnapshots( Map artifactMap )
    {
        int count = 0;
        Iterator it = artifactMap.keySet().iterator();
        while ( it.hasNext() )
        {
            String version = (String) it.next();
            boolean isReactorProject = false;

            Iterator iterator = ( (List) artifactMap.get( version ) ).iterator();
            // It if enough to check just the first dependency here, because
            // the dependency is the same in all the RDLs in the List. It's the
            // reactorProjects that are different.
            if ( iterator.hasNext() )
            {
                ReverseDependencyLink rdl = (ReverseDependencyLink) iterator.next();
                if ( isReactorProject( rdl.getDependency() ) )
                {
                    isReactorProject = true;
                }
            }

            if ( version.endsWith( "-SNAPSHOT" ) && !isReactorProject )
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Check to see if the specified dependency is among the reactor projects.
     *
     * @param dependency The dependency to check
     * @return true if and only if the dependency is a reactor project
     */
    private boolean isReactorProject( Dependency dependency )
    {
        Iterator iterator = reactorProjects.iterator();
        while ( iterator.hasNext() )
        {
            MavenProject mavenProject = (MavenProject) iterator.next();
            if ( mavenProject.getGroupId().equals( dependency.getGroupId() )
                && mavenProject.getArtifactId().equals( dependency.getArtifactId() ) )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( dependency + " is a reactor project" );
                }
                return true;
            }
        }
        return false;
    }

    private void iconSuccess( Sink sink )
    {
        sink.figure();
        sink.figureCaption();
        sink.text( "success" );
        sink.figureCaption_();
        sink.figureGraphics( "images/icon_success_sml.gif" );
        sink.figure_();
    }

    private void iconError( Sink sink )
    {
        sink.figure();
        sink.figureCaption();
        sink.text( "error" );
        sink.figureCaption_();
        sink.figureGraphics( "images/icon_error_sml.gif" );
        sink.figure_();
    }

    /**
     * Produce a Map of relationships between dependencies
     * (its groupId:artifactId) and reactor projects.
     *
     * This is the structure of the Map:
     * <pre>
     * +--------------------+----------------------------------+
     * | key                | value                            |
     * +--------------------+----------------------------------+
     * | groupId:artifactId | A List of ReverseDependencyLinks |
     * | of a dependency    | which each look like this:       |
     * |                    | +------------+-----------------+ |
     * |                    | | dependency | reactor project | |
     * |                    | +------------+-----------------+ |
     * +--------------------+----------------------------------+
     * </pre>
     *
     * @return A Map of relationships between dependencies and reactor projects
     */
    private Map getDependencyMap()
    {
        Iterator it = reactorProjects.iterator();

        Map dependencyMap = new TreeMap();

        while ( it.hasNext() )
        {
            MavenProject reactorProject = (MavenProject) it.next();

            Iterator itdep = reactorProject.getDependencies().iterator();
            while ( itdep.hasNext() )
            {
                Dependency dep = (Dependency) itdep.next();
                String key = dep.getGroupId() + ":" + dep.getArtifactId();
                List depList = (List) dependencyMap.get( key );
                if ( depList == null )
                {
                    depList = new ArrayList();
                }
                depList.add( new ReverseDependencyLink( dep, reactorProject ) );
                dependencyMap.put( key, depList );
            }
        }

        return dependencyMap;
    }

    /**
     * Internal object
     */
    private static class ReverseDependencyLink
    {
        private Dependency dependency;

        protected MavenProject project;

        ReverseDependencyLink( Dependency dependency, MavenProject project )
        {
            this.dependency = dependency;
            this.project = project;
        }

        public Dependency getDependency()
        {
            return dependency;
        }

        public MavenProject getProject()
        {
            return project;
        }

        /** {@inheritDoc} */
        public String toString()
        {
            return project.getId();
        }
    }

    /**
     * Internal ReverseDependencyLink comparator
     */
    static class ReverseDependencyLinkComparator
        implements Comparator
    {
        /** {@inheritDoc} */
        public int compare( Object o1, Object o2 )
        {
            if ( o1 instanceof ReverseDependencyLink && o2 instanceof ReverseDependencyLink )
            {
                ReverseDependencyLink p1 = (ReverseDependencyLink) o1;
                ReverseDependencyLink p2 = (ReverseDependencyLink) o2;
                return p1.getProject().getId().compareTo( p2.getProject().getId() );
            }

            return 0;
        }
    }
}
