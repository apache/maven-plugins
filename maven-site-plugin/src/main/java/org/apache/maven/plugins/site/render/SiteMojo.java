package org.apache.maven.plugins.site.render;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.DoxiaDocumentRenderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.exec.MavenReportExecution;

/**
 * Generates the site for a single project.
 * <p>
 * Note that links between module sites in a multi module build will <b>not</b> work, since local build directory
 * structure doesn't match deployed site.
 * </p>
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
@Mojo( name = "site", requiresDependencyResolution = ResolutionScope.TEST, requiresReports = true )
public class SiteMojo
    extends AbstractSiteRenderingMojo
{
    /**
     * Directory where the project sites and report distributions will be generated.
     */
    @Parameter( property = "siteOutputDirectory", defaultValue = "${project.reporting.outputDirectory}" )
    protected File outputDirectory;

    /**
     * Convenience parameter that allows you to disable report generation.
     */
    @Parameter( property = "generateReports", defaultValue = "true" )
    private boolean generateReports;

    /**
     * Generate a sitemap. The result will be a "sitemap.html" file at the site root.
     *
     * @since 2.1
     */
    @Parameter( property = "generateSitemap", defaultValue = "false" )
    private boolean generateSitemap;

    /**
     * Whether to validate xml input documents. If set to true, <strong>all</strong> input documents in xml format (in
     * particular xdoc and fml) will be validated and any error will lead to a build failure.
     *
     * @since 2.1.1
     */
    @Parameter( property = "validate", defaultValue = "false" )
    private boolean validate;

    /**
     * {@inheritDoc} Generate the project site
     * <p/>
     * throws MojoExecutionException if any
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "maven.site.skip = true: Skipping site generation" );
            return;
        }

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "executing Site Mojo" );
        }

        checkInputEncoding();

        List<MavenReportExecution> reports;
        if ( generateReports )
        {
            reports = getReports();
        }
        else
        {
            reports = Collections.emptyList();
        }

        try
        {
            List<Locale> localesList = getLocales();

            // Default is first in the list
            Locale defaultLocale = localesList.get( 0 );
            Locale.setDefault( defaultLocale );

            for ( Locale locale : localesList )
            {
                renderLocale( locale, reports );
            }
        }
        catch ( RendererException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error during site generation", e );
        }
    }

    private void renderLocale( Locale locale, List<MavenReportExecution> reports )
        throws IOException, RendererException, MojoFailureException, MojoExecutionException
    {
        SiteRenderingContext context = createSiteRenderingContext( locale );
        // MSITE-723 add generated site directory, in case some content has been put in pre-site phase
        context.addSiteDirectory( generatedSiteDirectory );

        context.setInputEncoding( getInputEncoding() );
        context.setOutputEncoding( getOutputEncoding() );
        context.setValidate( validate );
        if ( validate )
        {
            getLog().info( "Validation is switched on, xml input documents will be validated!" );
        }

        File outputDir = getOutputDirectory( locale );

        Map<String, DocumentRenderer> documents = locateDocuments( context, reports, locale );

        // copy resources
        siteRenderer.copyResources( context, outputDir );

        // 1. render Doxia documents first
        List<DocumentRenderer> reportDocuments = renderDoxiaDocuments( documents, context, outputDir, false );

        // 2. then reports
        // prepare external reports
        for ( MavenReportExecution mavenReportExecution : reports )
        {
            MavenReport report = mavenReportExecution.getMavenReport();
            report.setReportOutputDirectory( outputDir );
        }

        siteRenderer.render( reportDocuments, context, outputDir );

        if ( generateSitemap )
        {
            getLog().info( "Generating Sitemap." );

            new SiteMap( getOutputEncoding(), i18n ).generate( context.getDecoration(), generatedSiteDirectory,
                                                               locale );
        }

        // 3. Generated docs must be (re-)done afterwards as they are often generated by reports
        context.getSiteDirectories().clear();
        context.addSiteDirectory( generatedSiteDirectory );

        Map<String, DocumentRenderer> generatedDocuments = siteRenderer.locateDocumentFiles( context );

        renderDoxiaDocuments( generatedDocuments, context, outputDir, true );

        // copy generated resources also
        siteRenderer.copyResources( context, outputDir );
    }

    /**
     * Render Doxia documents from the list given, but not reports.
     * 
     * @param documents a collection of documents containing both Doxia source files and reports
     * @return the sublist of documents that are not Doxia source files
     */
    private List<DocumentRenderer> renderDoxiaDocuments( Map<String, DocumentRenderer> documents,
                                                         SiteRenderingContext context, File outputDir,
                                                         boolean generated )
                                                             throws RendererException, IOException
    {
        Map<String, DocumentRenderer> doxiaDocuments = new TreeMap<String, DocumentRenderer>();
        List<DocumentRenderer> nonDoxiaDocuments = new ArrayList<DocumentRenderer>();

        Map<String, Integer> counts = new TreeMap<String, Integer>();

        for ( Map.Entry<String, DocumentRenderer> entry : documents.entrySet() )
        {
            DocumentRenderer doc = entry.getValue();

            if ( doc instanceof DoxiaDocumentRenderer )
            {
                doxiaDocuments.put( entry.getKey(), doc );

                DoxiaDocumentRenderer doxia = (DoxiaDocumentRenderer) doc;

                // count documents per parserId
                String parserId = doxia.getRenderingContext().getParserId();
                Integer count = counts.get( parserId );
                if ( count == null )
                {
                    count = 1;
                }
                else
                {
                    count++;
                }
                counts.put( parserId, count );
            }
            else
            {
                nonDoxiaDocuments.add( doc );
            }
        }

        if ( doxiaDocuments.size() > 0 )
        {
            StringBuilder sb = new StringBuilder( 15 * counts.size() );
            for ( Map.Entry<String, Integer> entry : counts.entrySet() )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( ", " );
                }
                sb.append( entry.getValue() );
                sb.append( ' ' );
                sb.append( entry.getKey() );
            }

            getLog().info( "Rendering " + doxiaDocuments.size() + ( generated ? " generated" : "" ) + " Doxia document"
                + ( doxiaDocuments.size() > 1 ? "s" : "" ) + ": " + sb.toString() );

            siteRenderer.render( doxiaDocuments.values(), context, outputDir );
        }

        return nonDoxiaDocuments;
    }

    private File getOutputDirectory( Locale locale )
    {
        File file;
        if ( locale.getLanguage().equals( Locale.getDefault().getLanguage() ) )
        {
            file = outputDirectory;
        }
        else
        {
            file = new File( outputDirectory, locale.getLanguage() );
        }

        // Safety
        if ( !file.exists() )
        {
            file.mkdirs();
        }

        return file;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public MavenSession getSession()
    {
        return mavenSession;
    }
}
