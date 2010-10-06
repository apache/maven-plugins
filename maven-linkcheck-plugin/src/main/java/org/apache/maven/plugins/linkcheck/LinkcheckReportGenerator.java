/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.maven.plugins.linkcheck;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;

import org.apache.maven.doxia.linkcheck.model.LinkcheckFile;
import org.apache.maven.doxia.linkcheck.model.LinkcheckFileResult;
import org.apache.maven.doxia.linkcheck.model.LinkcheckModel;
import org.apache.maven.doxia.sink.Sink;

import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 * @author ltheussl
 * @since 1.1
 */
public class LinkcheckReportGenerator
{
    private final I18N i18n;

    private String httpMethod;
    private boolean offline;
    private String[] excludedLinks;
    private Integer[] excludedHttpStatusErrors;
    private Integer[] excludedHttpStatusWarnings;
    private String[] excludedPages;
    private boolean httpFollowRedirect;

    /**
     *
     * @param i18n not null.
     */
    public LinkcheckReportGenerator( I18N i18n )
    {
        this.i18n = i18n;
    }

    /**
     *
     * @param excludedHttpStatusErrors may be null.
     */
    public void setExcludedHttpStatusErrors( Integer[] excludedHttpStatusErrors )
    {
        this.excludedHttpStatusErrors = excludedHttpStatusErrors;
    }

    /**
     *
     * @param excludedHttpStatusWarnings may be null.
     */
    public void setExcludedHttpStatusWarnings( Integer[] excludedHttpStatusWarnings )
    {
        this.excludedHttpStatusWarnings = excludedHttpStatusWarnings;
    }

    /**
     *
     * @param excludedLinks may be null.
     */
    public void setExcludedLinks( String[] excludedLinks )
    {
        this.excludedLinks = excludedLinks;
    }

    /**
     *
     * @param excludedPages may be null.
     */
    public void setExcludedPages( String[] excludedPages )
    {
        this.excludedPages = excludedPages;
    }

    /**
     *
     * @param httpFollowRedirect default is false.
     */
    public void setHttpFollowRedirect( boolean httpFollowRedirect )
    {
        this.httpFollowRedirect = httpFollowRedirect;
    }

    /**
     *
     * @param httpMethod may be null.
     */
    public void setHttpMethod( String httpMethod )
    {
        this.httpMethod = httpMethod;
    }

    /**
     *
     * @param offline default is false.
     */
    public void setOffline( boolean offline )
    {
        this.offline = offline;
    }

    /**
     * Genarate a report for the given LinkcheckModel and emit it into a Sink.
     * <strong>Note</strong> that the Sink is flushed and closed.
     *
     * @param locale not null.
     * @param linkcheckModel may be null.
     * @param sink not null.
     */
    public void generateReport( Locale locale, LinkcheckModel linkcheckModel, Sink sink )
    {
        String name = i18n.getString( "linkcheck-report", locale, "report.linkcheck.name" );

        sink.head();
        sink.title();
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.title" ) );
        sink.title_();
        sink.head_();

        sink.body();

        if ( linkcheckModel == null )
        {
            sink.section1();
            sink.sectionTitle1();
            sink.text( name );
            sink.sectionTitle1_();

            sink.paragraph();
            sink.rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.empty" ) );
            sink.paragraph_();

            sink.section1_();

            sink.body_();
            sink.flush();
            sink.close();

            return;
        }

        // Overview
        sink.section1();
        sink.sectionTitle1();
        sink.text( name );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.overview" ) );
        sink.paragraph_();

        sink.section1_();

        // Statistics
        generateSummarySection( locale, linkcheckModel, sink );

        if ( linkcheckModel.getFiles().size() > 0 )
        {
            // Details
            generateDetailsSection( locale, linkcheckModel, sink );
        }

        sink.body_();
        sink.flush();
        sink.close();
    }

    private void generateSummarySection( Locale locale, LinkcheckModel linkcheckModel, Sink sink )
    {
        // Calculus
        List linkcheckFiles = linkcheckModel.getFiles();

        int totalFiles = linkcheckFiles.size();

        int totalLinks = 0;
        int totalValidLinks = 0;
        int totalErrorLinks = 0;
        int totalWarningLinks = 0;
        for ( Iterator it = linkcheckFiles.iterator(); it.hasNext(); )
        {
            LinkcheckFile linkcheckFile = (LinkcheckFile) it.next();

            totalLinks += linkcheckFile.getNumberOfLinks();
            totalValidLinks += linkcheckFile.getNumberOfLinks( LinkcheckFileResult.VALID_LEVEL );
            totalErrorLinks += linkcheckFile.getNumberOfLinks( LinkcheckFileResult.ERROR_LEVEL );
            totalWarningLinks += linkcheckFile.getNumberOfLinks( LinkcheckFileResult.WARNING_LEVEL );
        }

        sink.section1();
        sink.sectionTitle1();
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary" ) );
        sink.sectionTitle1_();

        // Summary of the analysis parameters
        sink.paragraph();
        sink.rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.overview1" ) );
        sink.paragraph_();

        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.table.parameter" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.table.value" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        sink.rawText(
                           i18n.getString( "linkcheck-report", locale,
                                           "report.linkcheck.summary.table.httpFollowRedirect" ) );
        sink.tableCell_();
        sink.tableCell();
        sink.text( String.valueOf( httpFollowRedirect ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        sink
                 .rawText(
                           i18n
                               .getString( "linkcheck-report", locale, "report.linkcheck.summary.table.httpMethod" ) );
        sink.tableCell_();
        sink.tableCell();
        if ( StringUtils.isEmpty( httpMethod ) )
        {
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.table.none" ) );
        }
        else
        {
            sink.text( httpMethod );
        }
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        sink.rawText(
                           i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.table.offline" ) );
        sink.tableCell_();
        sink.tableCell();
        sink.text( String.valueOf( offline ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        sink.rawText(
                           i18n.getString( "linkcheck-report", locale,
                                           "report.linkcheck.summary.table.excludedPages" ) );
        sink.tableCell_();
        sink.tableCell();
        if ( excludedPages == null || excludedPages.length == 0 )
        {
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.table.none" ) );
        }
        else
        {
            sink.text( StringUtils.join( excludedPages, "," ) );
        }
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        sink.rawText(
                           i18n.getString( "linkcheck-report", locale,
                                           "report.linkcheck.summary.table.excludedLinks" ) );
        sink.tableCell_();
        sink.tableCell();
        if ( excludedLinks == null || excludedLinks.length == 0 )
        {
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.table.none" ) );
        }
        else
        {
            sink.text( StringUtils.join( excludedLinks, "," ) );
        }
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        sink.rawText(
                           i18n.getString( "linkcheck-report", locale,
                                           "report.linkcheck.summary.table.excludedHttpStatusErrors" ) );
        sink.tableCell_();
        sink.tableCell();
        if ( excludedHttpStatusErrors == null || excludedHttpStatusErrors.length == 0 )
        {
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.table.none" ) );
        }
        else
        {
            sink.text( toString( excludedHttpStatusErrors ) );
        }
        sink.tableCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableCell();
        sink.rawText(
                           i18n.getString( "linkcheck-report", locale,
                                           "report.linkcheck.summary.table.excludedHttpStatusWarnings" ) );
        sink.tableCell_();
        sink.tableCell();
        if ( excludedHttpStatusWarnings == null || excludedHttpStatusWarnings.length == 0 )
        {
            sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.table.none" ) );
        }
        else
        {
            sink.text( toString( excludedHttpStatusWarnings ) );
        }
        sink.tableCell_();
        sink.tableRow_();

        sink.table_();

        // Summary of the checked files
        sink.paragraph();
        sink.rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.overview2" ) );
        sink.paragraph_();

        sink.table();

        // Header
        generateTableHeader( locale, false, sink );

        // Content
        sink.tableRow();

        sink.tableCell();
        sink.bold();
        sink.text( totalFiles + "" );
        sink.bold_();
        sink.tableCell_();
        sink.tableCell();
        sink.bold();
        sink.text( totalLinks + "" );
        sink.bold_();
        sink.tableCell_();
        sink.tableCell();
        sink.bold();
        sink.text( String.valueOf( totalValidLinks ) );
        sink.bold_();
        sink.tableCell_();
        sink.tableCell();
        sink.bold();
        sink.text( String.valueOf( totalWarningLinks ) );
        sink.bold_();
        sink.tableCell_();
        sink.tableCell();
        sink.bold();
        sink.text( String.valueOf( totalErrorLinks ) );
        sink.bold_();
        sink.tableCell_();

        sink.tableRow_();

        sink.table_();

        sink.section1_();
    }

    private void generateDetailsSection( Locale locale, LinkcheckModel linkcheckModel, Sink sink )
    {
        sink.section1();
        sink.sectionTitle1();
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.detail" ) );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.detail.overview" ) );
        sink.paragraph_();

        sink.table();

        // Header
        generateTableHeader( locale, true, sink );

        // Content
        List linkcheckFiles = linkcheckModel.getFiles();
        for ( Iterator it = linkcheckFiles.iterator(); it.hasNext(); )
        {
            LinkcheckFile linkcheckFile = (LinkcheckFile) it.next();

            sink.tableRow();

            sink.tableCell();
            if ( linkcheckFile.getUnsuccessful() == 0 )
            {
                iconValid( locale, sink );
            }
            else
            {
                iconError( locale, sink );
            }
            sink.tableCell_();

            // tableCell( createLinkPatternedText( linkcheckFile.getRelativePath(), "./"
            // + linkcheckFile.getRelativePath() ) );
            sink.tableCell();
            sink.link( linkcheckFile.getRelativePath() );
            sink.text( linkcheckFile.getRelativePath() );
            sink.link_();
            sink.tableCell_();
            sink.tableCell();
            sink.text( String.valueOf( linkcheckFile.getNumberOfLinks() ) );
            sink.tableCell_();
            sink.tableCell();
            sink.text( String.valueOf( linkcheckFile.getNumberOfLinks( LinkcheckFileResult.VALID_LEVEL ) ) );
            sink.tableCell_();
            sink.tableCell();
            sink.text( String.valueOf( linkcheckFile.getNumberOfLinks( LinkcheckFileResult.WARNING_LEVEL ) ) );
            sink.tableCell_();
            sink.tableCell();
            sink.text( String.valueOf( linkcheckFile.getNumberOfLinks( LinkcheckFileResult.ERROR_LEVEL ) ) );
            sink.tableCell_();

            sink.tableRow_();

            // Detail error
            if ( linkcheckFile.getUnsuccessful() != 0 )
            {
                sink.tableRow();

                sink.tableCell();
                sink.text( "" );
                sink.tableCell_();

                // TODO it is due to DOXIA-78
                sink.rawText( "<td colspan=\"5\">" );

                sink.table();

                for ( Iterator it2 = linkcheckFile.getResults().iterator(); it2.hasNext(); )
                {
                    LinkcheckFileResult linkcheckFileResult = (LinkcheckFileResult) it2.next();

                    if ( linkcheckFileResult.getStatusLevel() == LinkcheckFileResult.VALID_LEVEL )
                    {
                        continue;
                    }

                    sink.tableRow();

                    sink.tableCell();
                    if ( linkcheckFileResult.getStatusLevel() == LinkcheckFileResult.WARNING_LEVEL )
                    {
                        iconWarning( locale, sink );
                    }
                    else if ( linkcheckFileResult.getStatusLevel() == LinkcheckFileResult.ERROR_LEVEL )
                    {
                        iconError( locale, sink );
                    }
                    sink.tableCell_();

                    sink.tableCell();
                    sink.italic();
                    if ( linkcheckFileResult.getTarget().startsWith( "#" ) )
                    {
                        sink.link( linkcheckFile.getRelativePath() + linkcheckFileResult.getTarget() );
                    }
                    else if ( linkcheckFileResult.getTarget().startsWith( "." ) )
                    {
                        // We need to calculate a correct absolute path here, because target is a relative path
                        String absolutePath = FilenameUtils.getFullPath( linkcheckFile.getRelativePath() )
                            + linkcheckFileResult.getTarget();
                        String normalizedPath = FilenameUtils.normalize( absolutePath );
                        if ( normalizedPath == null )
                        {
                            normalizedPath = absolutePath;
                        }
                        sink.link( normalizedPath );
                    }
                    else
                    {
                        sink.link( linkcheckFileResult.getTarget() );
                    }
                    // Show the link as it was written to make it easy for
                    // the author to find it in the source document
                    sink.text( linkcheckFileResult.getTarget() );
                    sink.link_();
                    sink.text( ": " );
                    sink.text( linkcheckFileResult.getErrorMessage() );
                    sink.italic_();
                    sink.tableCell_();

                    sink.tableRow_();
                }

                sink.table_();

                sink.tableCell_();

                sink.tableRow_();
            }
        }

        sink.table_();

        sink.section1_();
    }

    private void generateTableHeader( Locale locale, boolean detail, Sink sink )
    {
        sink.tableRow();
        if ( detail )
        {
            sink.rawText( "<th rowspan=\"2\">" );
            sink.text( "" );
            sink.tableHeaderCell_();
        }
        sink.rawText( "<th rowspan=\"2\">" );
        sink.text(
                        detail ? i18n.getString( "linkcheck-report", locale,
                                                 "report.linkcheck.detail.table.documents" )
                                        : i18n.getString( "linkcheck-report", locale,
                                                          "report.linkcheck.summary.table.documents" ) );
        sink.tableHeaderCell_();
        // TODO it is due to DOXIA-78
        sink.rawText( "<th colspan=\"4\" align=\"center\">" );
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.links" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.totalLinks" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        iconValid( locale, sink );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        iconWarning( locale, sink );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        iconError( locale, sink );
        sink.tableHeaderCell_();
        sink.tableRow_();
    }

    private void iconError( Locale locale, Sink sink )
    {
        sink.figure();
        sink.figureCaption();
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.icon.error" ) );
        sink.figureCaption_();
        // should be defined in skins
        sink.figureGraphics( "images/icon_error_sml.gif" );
        sink.figure_();
    }

    private void iconValid( Locale locale, Sink sink )
    {
        sink.figure();
        sink.figureCaption();
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.icon.valid" ) );
        sink.figureCaption_();
        // should be defined in skins
        sink.figureGraphics( "images/icon_success_sml.gif" );
        sink.figure_();
    }

    private void iconWarning( Locale locale, Sink sink )
    {
        sink.figure();
        sink.figureCaption();
        sink.text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.icon.warning" ) );
        sink.figureCaption_();
        // should be defined in skins
        sink.figureGraphics( "images/icon_warning_sml.gif" );
        sink.figure_();
    }

    // ----------------------------------------------------------------------
    // static methods
    // ----------------------------------------------------------------------

    /**
     * Similar to {@link Arrays#toString(int[])} in 1.5.
     *
     * @param a not null
     * @return the array comma separated.
     */
    private static String toString( Object[] a )
    {
        if ( a == null || a.length == 0 )
        {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        buf.append( a[0] );

        for ( int i = 1; i < a.length; i++ )
        {
            buf.append( ", " );
            buf.append( a[i] );
        }

        return buf.toString();
    }
}
