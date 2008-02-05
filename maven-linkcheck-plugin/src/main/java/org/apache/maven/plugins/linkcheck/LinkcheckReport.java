package org.apache.maven.plugins.linkcheck;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.linkcheck.DefaultLinkCheck;
import org.apache.maven.doxia.linkcheck.HttpBean;
import org.apache.maven.doxia.linkcheck.LinkCheck;
import org.apache.maven.doxia.linkcheck.model.LinkcheckFile;
import org.apache.maven.doxia.linkcheck.model.LinkcheckFileResult;
import org.apache.maven.doxia.linkcheck.model.LinkcheckModel;
import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.Body;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.LinkItem;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates a <code>Linkcheck</code> report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 1.0
 * @goal linkcheck
 */
public class LinkcheckReport
    extends AbstractMavenReport
{
    // ----------------------------------------------------------------------
    // Report Components
    // ----------------------------------------------------------------------

    /**
     * Internationalization.
     *
     * @component
     */
    protected I18N i18n;

    /**
     * Doxia Site Renderer.
     *
     * @component
     */
    protected Renderer siteRenderer;

    /**
     * SiteTool component.
     *
     * @component
     */
    protected SiteTool siteTool;

    /**
     * SiteTool component.
     *
     * @component
     */
    protected LinkCheck linkCheck;

    // ----------------------------------------------------------------------
    // Report Parameters
    // ----------------------------------------------------------------------

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The reactor projects.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    /**
     * Local Repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * Remote repositories used for the project.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     */
    protected List repositories;

    /**
     * Report output directory.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * The Maven Settings.
     *
     * @parameter default-value="${settings}"
     * @required
     * @readonly
     */
    protected Settings settings;

    /**
     * Directory containing the <code>site.xml</code> file.
     *
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    protected File siteDirectory;

    // ----------------------------------------------------------------------
    // Linkcheck parameters
    // ----------------------------------------------------------------------

    /**
     * The settings offline paramater.
     *
     * @parameter expression="${settings.offline}"
     * @required
     * @readonly
     */
    private boolean offline;

    /**
     * If online, the HTTP method should automatically follow HTTP redirects,
     * <tt>false</tt> otherwise.
     *
     * @parameter default-value="false"
     */
    protected boolean httpFollowRedirect;

    /**
     * The location of the linkcheck cache.
     *
     * @parameter expression="${project.build.directory}/linkcheck/linkcheck.cache"
     * @required
     */
    protected String linkcheckCache;

    /**
     * The location of the linkcheck report.
     *
     * @parameter expression="${project.build.directory}/linkcheck/linkcheck.xml"
     * @required
     */
    protected String linkcheckOutput;

    /**
     * The HTTP method to use. Currently supported are "GET" and "HEAD".
     * <dl>
     * <dt>HTTP GET</dt>
     * <dd>
     * The HTTP GET method is defined in section 9.3 of
     * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a>:
     * The GET method means retrieve whatever information (in the form of an
     * entity) is identified by the Request-URI.
     * </dd>
     * <dt>HTTP HEAD</dt>
     * <dd>
     * The HTTP HEAD method is defined in section 9.4 of
     * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a>:
     * The HEAD method is identical to GET except that the server MUST NOT
     * return a message-body in the response.
     * </dd>
     * </dl>
     *
     * @parameter default-value="head"
     */
    protected String httpMethod;

    /**
     * The list of HTTP errors to ignored.
     *
     * @parameter
     * @see {@link HttpStatus} for all defined values.
     */
    protected int[] excludedHttpStatusErrors;

    /**
     * The list of HTTP warnings to ignored.
     *
     * @parameter
     * @see {@link HttpStatus} for all defined values.
     */
    protected int[] excludedHttpStatusWarnings;

    /**
     * The list of pages to exclude.
     *
     * @parameter
     */
    protected String[] excludedPages;

    /**
     * The list of links to exclude.
     *
     * @parameter
     */
    protected String[] excludedLinks;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "linkcheck-report", locale, "report.linkcheck.description" );
    }

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return i18n.getString( "linkcheck-report", locale, "report.linkcheck.name" );
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "linkcheck";
    }

    /** {@inheritDoc} */
    public boolean canGenerateReport()
    {
        boolean can = outputDirectory.exists();

        if ( !can )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "Linkcheck report skipped. You need to call 'mvn site' before." );
            }
        }

        return can;
    }

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( !canGenerateReport() )
        {
            return;
        }

        try
        {
            DecorationModel model = new DecorationModel();
            model.setBody( new Body() );
            Map attributes = new HashMap();
            attributes.put( "outputEncoding", "UTF-8" );
            attributes.put( "project", project );
            Locale locale = Locale.getDefault();
            Artifact skinArtifact = siteTool.getDefaultSkinArtifact( localRepository, repositories );
            SiteRenderingContext siteContext = siteRenderer.createContextForSkin( skinArtifact.getFile(), attributes,
                                                                                  model, getName( locale ), locale );

            RenderingContext context = new RenderingContext( outputDirectory, getOutputName() + ".html" );

            SiteRendererSink sink = new SiteRendererSink( context );
            generate( sink, locale );

            outputDirectory.mkdirs();

            Writer writer = new FileWriter( new File( outputDirectory, getOutputName() + ".html" ) );

            siteRenderer.generateDocument( writer, sink, siteContext );

            siteRenderer.copyResources( siteContext, new File( project.getBasedir(), "src/site/resources" ),
                                        outputDirectory );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        catch ( SiteToolException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        catch ( RendererException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    /** {@inheritDoc} */
    protected MavenProject getProject()
    {
        return project;
    }

    /** {@inheritDoc} */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /** {@inheritDoc} */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        try
        {
            LinkcheckModel result = executeLinkCheck( locale );

            generateReport( locale, result );
        }
        catch ( Exception e )
        {
            throw new MavenReportException( "IOException: " + e.getMessage(), e );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Execute the <code>Linkcheck</code> tool.
     */
    private LinkcheckModel executeLinkCheck( Locale locale )
        throws Exception
    {
        // Wrap linkcheck
        LinkCheck lc = new DefaultLinkCheck();
        lc.setOnline( !offline );
        lc.setBasedir( outputDirectory );
        lc.setReportOutput( new File( linkcheckOutput ) );
        lc.setLinkCheckCache( new File( linkcheckCache ) );
        lc.setExcludedLinks( getExcludedLinks( locale ) );
        lc.setExcludedPages( getExcludedPages() );
        lc.setExcludedHttpStatusErrors( excludedHttpStatusErrors );
        lc.setExcludedHttpStatusWarnings( excludedHttpStatusWarnings );

        HttpBean bean = new HttpBean();
        bean.setMethod( httpMethod );
        bean.setFollowRedirects( httpFollowRedirect );

        Proxy proxy = settings.getActiveProxy();
        if ( proxy != null )
        {
            bean.setProxyHost( proxy.getHost() );
            bean.setProxyPort( proxy.getPort() );
            bean.setProxyUser( proxy.getUsername() );
            bean.setProxyPassword( proxy.getPassword() );
        }
        lc.setHttp( bean );

        return lc.execute();
    }

    private String[] getExcludedLinks( Locale locale )
        throws Exception
    {
        List linksToExclude = ( excludedLinks != null ? new ArrayList( Arrays.asList( excludedLinks ) ) : new ArrayList() );

        if ( project.getUrl() != null )
        {
            // Using interpolated references in the decoration model
            DecorationModel site = siteTool.getDecorationModel( project, reactorProjects, localRepository,
                                                                repositories, siteDirectory, locale, "ISO-8859-1",
                                                                "ISO-8859-1" );

            String baseUrl = project.getUrl();
            if ( site.getBannerLeft() != null && StringUtils.isNotEmpty( site.getBannerLeft().getHref() ) )
            {
                linksToExclude.add( siteTool.getRelativePath( site.getBannerLeft().getHref(), baseUrl ) );
            }
            if ( site.getBannerRight() != null && StringUtils.isNotEmpty( site.getBannerRight().getHref() ) )
            {
                linksToExclude.add( siteTool.getRelativePath( site.getBannerRight().getHref(), baseUrl ) );
            }
            if ( site.getBody() != null && site.getBody().getLinks() != null )
            {
                for ( Iterator it = site.getBody().getLinks().iterator(); it.hasNext(); )
                {
                    LinkItem link = (LinkItem) it.next();
                    linksToExclude.add( siteTool.getRelativePath( link.getHref(), baseUrl ) );
                }
            }
        }

        return (String[]) linksToExclude.toArray( new String[0] );
    }

    private String[] getExcludedPages()
    {
        List linksToExclude;

        if ( excludedPages != null )
        {
            linksToExclude = Arrays.asList( excludedPages );
        }
        else
        {
            linksToExclude = new ArrayList();
        }

        // Exclude this report
        linksToExclude.add( getOutputName() + ".html" );

        return (String[]) linksToExclude.toArray( new String[0] );
    }

    /**
     * Generate the Linkcheck report.
     *
     * @param locale the wanted locale
     * @param linkcheckModel the result of the analysis
     */
    private void generateReport( Locale locale, LinkcheckModel linkcheckModel )
    {
        getSink().head();
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.name" ) );
        getSink().head_();

        getSink().body();

        if ( linkcheckModel == null )
        {
            getSink().section1();
            getSink().sectionTitle1();
            getSink().text( getName( locale ) );
            getSink().sectionTitle1_();

            getSink().paragraph();
            getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.empty" ) );
            getSink().paragraph_();

            getSink().section1_();

            getSink().body_();
            getSink().flush();
            getSink().close();

            return;
        }

        // Overview
        getSink().section1();
        getSink().sectionTitle1();
        getSink().text( getName( locale ) );
        getSink().sectionTitle1_();

        getSink().paragraph();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.overview" ) );
        getSink().paragraph_();

        getSink().section1_();

        //Statistics
        generateSummarySection( locale, linkcheckModel );

        //Details
        generateDetailsSection( locale, linkcheckModel );

        getSink().body_();
        getSink().flush();
        getSink().close();
    }

    private void generateSummarySection( Locale locale, LinkcheckModel linkcheckModel )
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

        getSink().section1();
        getSink().sectionTitle1();
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary" ) );
        getSink().sectionTitle1_();

        // Summary of the analysis parameters
        getSink().paragraph();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.overview1" ) );
        getSink().paragraph_();

        getSink().table();

        getSink().tableRow();
        getSink().tableHeaderCell();
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.parameter" ) );
        getSink().tableHeaderCell_();
        getSink().tableHeaderCell();
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.value" ) );
        getSink().tableHeaderCell_();
        getSink().tableRow_();

        getSink().tableRow();
        getSink().tableCell();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.httpFollowRedirect" ) );
        getSink().tableCell_();
        getSink().tableCell();
        getSink().text( String.valueOf( httpFollowRedirect ) );
        getSink().tableCell_();
        getSink().tableRow_();

        getSink().tableRow();
        getSink().tableCell();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.httpMethod" ) );
        getSink().tableCell_();
        getSink().tableCell();
        if ( StringUtils.isEmpty( httpMethod ) )
        {
            getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.none" ) );
        }
        else
        {
            getSink().text( httpMethod );
        }
        getSink().tableCell_();
        getSink().tableRow_();

        getSink().tableRow();
        getSink().tableCell();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.excludedPages" ) );
        getSink().tableCell_();
        getSink().tableCell();
        if ( excludedPages == null || excludedPages.length == 0 )
        {
            getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.none" ) );
        }
        else
        {
            getSink().text( StringUtils.join( excludedPages, "," ) );
        }
        getSink().tableCell_();
        getSink().tableRow_();

        getSink().tableRow();
        getSink().tableCell();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.excludedLinks" ) );
        getSink().tableCell_();
        getSink().tableCell();
        if ( excludedLinks == null || excludedLinks.length == 0 )
        {
            getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.none" ) );
        }
        else
        {
            getSink().text( StringUtils.join( excludedLinks, "," ) );
        }
        getSink().tableCell_();
        getSink().tableRow_();

        getSink().tableRow();
        getSink().tableCell();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.excludedHttpStatusErrors" ) );
        getSink().tableCell_();
        getSink().tableCell();
        if ( excludedHttpStatusErrors == null || excludedHttpStatusErrors.length == 0 )
        {
            getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.none" ) );
        }
        else
        {
            getSink().text( toString( excludedHttpStatusErrors ) );
        }
        getSink().tableCell_();
        getSink().tableRow_();

        getSink().tableRow();
        getSink().tableCell();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.excludedHttpStatusWarnings" ) );
        getSink().tableCell_();
        getSink().tableCell();
        if ( excludedHttpStatusWarnings == null || excludedHttpStatusWarnings.length == 0 )
        {
            getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.none" ) );
        }
        else
        {
            getSink().text( toString( excludedHttpStatusWarnings ) );
        }
        getSink().tableCell_();
        getSink().tableRow_();

        getSink().table_();

        // Summary of the checked files
        getSink().paragraph();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.summary.overview2" ) );
        getSink().paragraph_();

        getSink().table();

        //Header
        generateTableHeader( locale, false );

        //Content
        getSink().tableRow();

        getSink().tableCell();
        getSink().bold();
        getSink().text( totalFiles + "" );
        getSink().bold_();
        getSink().tableCell_();
        getSink().tableCell();
        getSink().bold();
        getSink().text( totalLinks + "" );
        getSink().bold_();
        getSink().tableCell_();
        getSink().tableCell();
        getSink().bold();
        getSink().text( String.valueOf( totalValidLinks ) );
        getSink().bold_();
        getSink().tableCell_();
        getSink().tableCell();
        getSink().bold();
        getSink().text( String.valueOf( totalWarningLinks ) );
        getSink().bold_();
        getSink().tableCell_();
        getSink().tableCell();
        getSink().bold();
        getSink().text( String.valueOf( totalErrorLinks ) );
        getSink().bold_();
        getSink().tableCell_();

        getSink().tableRow_();

        getSink().table_();

        getSink().section1_();
    }

    private void generateDetailsSection( Locale locale, LinkcheckModel linkcheckModel )
    {
        getSink().section1();
        getSink().sectionTitle1();
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.detail" ) );
        getSink().sectionTitle1_();

        getSink().paragraph();
        getSink().rawText( i18n.getString( "linkcheck-report", locale, "report.linkcheck.detail.overview" ) );
        getSink().paragraph_();

        getSink().table();

        //Header
        generateTableHeader( locale, true );

        // Content
        List linkcheckFiles = linkcheckModel.getFiles();
        for ( Iterator it = linkcheckFiles.iterator(); it.hasNext(); )
        {
            LinkcheckFile linkcheckFile = (LinkcheckFile) it.next();

            getSink().tableRow();

            getSink().tableCell();
            if ( linkcheckFile.getUnsuccessful() == 0 )
            {
                iconValid( locale );
            }
            else
            {
                iconError( locale );
            }
            getSink().tableCell_();

            //            tableCell( createLinkPatternedText( linkcheckFile.getRelativePath(), "./"
            //                + linkcheckFile.getRelativePath() ) );
            getSink().tableCell();
            getSink().link( linkcheckFile.getRelativePath() );
            getSink().text( linkcheckFile.getRelativePath() );
            getSink().link_();
            getSink().tableCell_();
            getSink().tableCell();
            getSink().text( String.valueOf( linkcheckFile.getNumberOfLinks() ) );
            getSink().tableCell_();
            getSink().tableCell();
            getSink().text( String.valueOf( linkcheckFile.getNumberOfLinks( LinkcheckFileResult.VALID_LEVEL ) ) );
            getSink().tableCell_();
            getSink().tableCell();
            getSink().text( String.valueOf( linkcheckFile.getNumberOfLinks( LinkcheckFileResult.WARNING_LEVEL ) ) );
            getSink().tableCell_();
            getSink().tableCell();
            getSink().text( String.valueOf( linkcheckFile.getNumberOfLinks( LinkcheckFileResult.ERROR_LEVEL ) ) );
            getSink().tableCell_();

            getSink().tableRow_();

            // Detail error
            if ( linkcheckFile.getUnsuccessful() != 0 )
            {
                getSink().tableRow();

                getSink().tableCell();
                getSink().text( "" );
                getSink().tableCell_();

                // TODO it is due to DOXIA-78
                getSink().rawText( "<td colspan=\"5\">" );

                getSink().table();

                for ( Iterator it2 = linkcheckFile.getResults().iterator(); it2.hasNext(); )
                {
                    LinkcheckFileResult linkcheckFileResult = (LinkcheckFileResult) it2.next();

                    if ( linkcheckFileResult.getStatusLevel() == LinkcheckFileResult.VALID_LEVEL )
                    {
                        continue;
                    }

                    getSink().tableRow();

                    getSink().tableCell();
                    if ( linkcheckFileResult.getStatusLevel() == LinkcheckFileResult.WARNING_LEVEL )
                    {
                        iconWarning( locale );
                    }
                    else if ( linkcheckFileResult.getStatusLevel() == LinkcheckFileResult.ERROR_LEVEL )
                    {
                        iconError( locale );
                    }
                    getSink().tableCell_();

                    getSink().tableCell();
                    getSink().italic();
                    getSink().link( linkcheckFileResult.getTarget() );
                    getSink().text( linkcheckFileResult.getTarget() );
                    getSink().link_();
                    getSink().text( ": " );
                    getSink().text( linkcheckFileResult.getErrorMessage() );
                    getSink().italic_();
                    getSink().tableCell_();

                    getSink().tableRow_();
                }

                getSink().table_();

                getSink().tableCell_();

                getSink().tableRow_();
            }
        }

        getSink().tableRow();

        getSink().table_();

        getSink().section1_();
    }

    private void generateTableHeader( Locale locale, boolean detail )
    {
        getSink().tableRow();
        if ( detail )
        {
            getSink().rawText( "<th rowspan=\"2\">" );
            getSink().text( "" );
            getSink().tableHeaderCell_();
        }
        getSink().rawText( "<th rowspan=\"2\">" );
        getSink().text( detail ? i18n.getString( "linkcheck-report", locale, "report.linkcheck.detail.table.documents" ) : i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.summary.documents" ) );
        getSink().tableHeaderCell_();
        // TODO it is due to DOXIA-78
        getSink().rawText( "<th colspan=\"4\" center>" );
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.links" ) );
        getSink().tableHeaderCell_();
        getSink().rawText( "</th>" );
        getSink().tableRow_();

        getSink().tableRow();
        getSink().tableHeaderCell();
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.table.totalLinks" ) );
        getSink().tableHeaderCell_();
        getSink().tableHeaderCell();
        iconValid( locale );
        getSink().tableHeaderCell_();
        getSink().tableHeaderCell();
        iconWarning( locale );
        getSink().tableHeaderCell_();
        getSink().tableHeaderCell();
        iconError( locale );
        getSink().tableHeaderCell_();
        getSink().tableRow_();
    }

    private void iconError( Locale locale )
    {
        getSink().figure();
        getSink().figureCaption();
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.icon.error" ) );
        getSink().figureCaption_();
        getSink().figureGraphics( "images/icon_error_sml.gif" );
        getSink().figure_();
    }

    private void iconValid( Locale locale )
    {
        getSink().figure();
        getSink().figureCaption();
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.icon.valid" ) );
        getSink().figureCaption_();
        getSink().figureGraphics( "images/icon_success_sml.gif" );
        getSink().figure_();
    }

    private void iconWarning( Locale locale )
    {
        getSink().figure();
        getSink().figureCaption();
        getSink().text( i18n.getString( "linkcheck-report", locale, "report.linkcheck.icon.warning" ) );
        getSink().figureCaption_();
        getSink().figureGraphics( "images/icon_warning_sml.gif" );
        getSink().figure_();
    }

    private String toString( int[] a )
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
