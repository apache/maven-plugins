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
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.StringUtils;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
 * @version $Id $
 * @goal dependency-convergence
 * @aggregator
 */
public class DependencyConvergenceReport
    extends AbstractProjectInfoReport
{
    private static final int PERCENTAGE = 100;

    private static final List SUPPORTED_FONT_FAMILY_NAMES = Arrays.asList( GraphicsEnvironment
        .getLocalGraphicsEnvironment().getAvailableFontFamilyNames() );

    /**
     * The projects in the current build. The effective-POM for
     * each of these projects will written.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "dependency-convergence";
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getI18nString( locale, "name" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getI18nString( locale, "description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
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
    }

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

        // To know the cell width for version
        String biggestVersion = getBiggestStringVersion( dependencyMap );
        int cellWidth = getMavenTableCell( biggestVersion );

        Iterator it = dependencyMap.keySet().iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();
            List depList = (List) dependencyMap.get( key );

            sink.section3();
            sink.sectionTitle3();
            sink.text( key );
            sink.sectionTitle3_();

            generateDependencyDetails( sink, depList, cellWidth );

            sink.section3_();
        }

        sink.section2_();
    }

    /**
     * Generate the detail table for a given dependency
     *
     * @param sink
     * @param depList
     * @param cellWidth
     */
    private void generateDependencyDetails( Sink sink, List depList, int cellWidth )
    {
        sink.table();

        Map artifactMap = getSortedUniqueArtifactMap( depList );

        sink.tableRow();

        sink.tableCell( "15px" ); // according /images/icon_success_sml.gif and /images/icon_error_sml.gif
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
            sink.tableCell( String.valueOf( cellWidth ) + "px" );
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
        sink.numberedList( 1 ); // parameter value is apparently irrelevant here.
        List depList = (List) artifactMap.get( version );
        Collections.sort( depList, new ProjectComparator() );
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

        sink.tableCell( "15px" ); // according /images/icon_success_sml.gif
        iconSuccess( sink );
        sink.tableCell_();
        sink.tableCell();
        sink.text( getI18nString( locale, "legend.shared" ) );
        sink.tableCell_();

        sink.tableRow_();

        sink.tableRow();

        sink.tableCell( "15px" ); // according /images/icon_error_sml.gif
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

        // To know the header cell width
        List l = new ArrayList();
        l.add( getI18nString( locale, "stats.subprojects" ) );
        l.add( getI18nString( locale, "stats.dependencies" ) );
        l.add( getI18nString( locale, "stats.artifacts" ) );
        l.add( getI18nString( locale, "stats.snapshots" ) );
        l.add( getI18nString( locale, "stats.convergence" ) );
        l.add( getI18nString( locale, "stats.readyrelease" ) );

        String biggest = getBiggestString( l );
        String headerCellWidth = getMavenTableHeaderCell( biggest ) + "px";

        // Create report
        sink.table();
        sink.tableCaption();
        sink.bold();
        sink.text( getI18nString( locale, "stats.caption" ) );
        sink.bold_();
        sink.tableCaption_();

        sink.tableRow();
        sink.tableHeaderCell( headerCellWidth );
        sink.text( getI18nString( locale, "stats.subprojects" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        sink.text( String.valueOf( reactorProjects.size() ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell( headerCellWidth );
        sink.text( getI18nString( locale, "stats.dependencies" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        sink.text( String.valueOf( depCount ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell( headerCellWidth );
        sink.text( getI18nString( locale, "stats.artifacts" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        sink.text( String.valueOf( artifactCount ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell( headerCellWidth );
        sink.text( getI18nString( locale, "stats.snapshots" ) );
        sink.tableHeaderCell_();
        sink.tableCell();
        sink.text( String.valueOf( snapshotCount ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell( headerCellWidth );
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
        sink.tableHeaderCell( headerCellWidth );
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
            if ( version.endsWith( "-SNAPSHOT" ) )
            {
                count++;
            }
        }
        return count;
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

    public Map getDependencyMap()
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

    private String getI18nString( Locale locale, String key )
    {
        return i18n.getString( "project-info-report", locale, "report.dependency-convergence." + key );
    }

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

        /**
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return project.getId();
        }
    }

    private static class ProjectComparator
        implements Comparator
    {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
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

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        // only generate the convergency report if we are running a reactor build
        return reactorProjects.size() > 1;
    }

    /**
     * @param dependencyMap a map with <code>version</code> as key
     * @return the biggest string of <code>version</code>
     */
    private String getBiggestStringVersion( Map dependencyMap )
    {
        String biggestVersion = "";

        Iterator it = dependencyMap.keySet().iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();
            List depList = (List) dependencyMap.get( key );
            Map artifactMap = getSortedUniqueArtifactMap( depList );

            String biggestTmp = getBiggestString( artifactMap.keySet() );
            if ( biggestVersion.length() < biggestTmp.length() )
            {
                biggestVersion = biggestTmp;
            }
        }

        return biggestVersion;
    }

    /**
     * @param l a collection of String
     * @return the biggest <code>String</code> in the collection
     */
    private static String getBiggestString( Collection l )
    {
        String biggest = "";

        Iterator it = l.iterator();
        while ( it.hasNext() )
        {
            String text = (String) it.next();
            if ( biggest.length() < text.length() )
            {
                biggest = text;
            }
        }

        return biggest;
    }

    /**
     * The header cell style has the following hierarchy:
     * <pre>
     * html > body .composite > div #bodyColumn > div #contentBox > div .section > div .section > table .bodyTable > tbody > tr .a > td > table .bodyTable > tbody > tr .b > td
     * </pre>
     * Extract from <code>maven-theme.css</code>:
     * <pre>
     * body, td, select, input, li{
     *   font-family: Verdana, Helvetica, Arial, sans-serif;
     *   font-size: 13px;
     * }
     * </pre>
     *
     * @param s a String
     * @return the total advance width for showing the specified <code>String</code> using the Maven table cell.
     * @see #getStringWidth(String, int, int)
     */
    private static int getMavenTableCell( String s )
    {
        return getStringWidth( s, Font.PLAIN, 13 );
    }

    /**
     * The header cell style has the following hierarchy:
     * <pre>
     * html > body .composite > div #bodyColumn > div #contentBox > div .section > table .bodyTable > tbody > tr .a > th
     * </pre>
     * Extract from <code>maven-theme.css</code>:
     * <pre>
     * body, td, select, input, li{
     *   font-family: Verdana, Helvetica, Arial, sans-serif;
     *   font-size: 13px;
     * }
     * table.bodyTable th{
     *   color: white;
     *   background-color: #bbbbbb;
     *   text-align: left;
     *   font-weight: bold;
     * }
     * </pre>
     *
     * @param s a String
     * @return the total advance width for showing the specified <code>String</code> using the Maven table header cell.
     * @see #getStringWidth(String, int, int)
     */
    private static int getMavenTableHeaderCell( String s )
    {
        return getStringWidth( s, Font.BOLD, 13 );
    }

    /**
     * @param s a String
     * @param style an AWT style
     * @param size an AWT size
     * @return the total advance width for showing the specified <code>String</code> depending
     * the Maven CSS, ie the font family and the specified <code>style</code> and <code>size</code>.
     * @see #getMavenFontFamily()
     */
    private static int getStringWidth( String s, int style, int size )
    {
        Font font = new Font( getMavenFontFamily(), style, size );

        return Toolkit.getDefaultToolkit().getFontMetrics( font ).stringWidth( s );
    }

    /**
     * Extract from <code>maven-theme.css</code>:
     * <pre>
     * body, td, select, input, li{
     *   font-family: Verdana, Helvetica, Arial, sans-serif;
     *   font-size: 13px;
     * }
     * </pre>
     *
     * @todo maybe use batik-css to parse the maven-theme.css
     *
     * @return a AWT font family name
     */
    private static String getMavenFontFamily()
    {
        if ( SUPPORTED_FONT_FAMILY_NAMES.contains( "Verdana" ) )
        {
            return "Verdana";
        }
        else if ( SUPPORTED_FONT_FAMILY_NAMES.contains( "Helvetica" ) )
        {
            return "Helvetica";
        }
        else if ( SUPPORTED_FONT_FAMILY_NAMES.contains( "Arial" ) )
        {
            return "Arial";
        }
        else if ( SUPPORTED_FONT_FAMILY_NAMES.contains( "SansSerif" ) )
        {
            return "SansSerif";
        }

        return "Default";
    }
}
