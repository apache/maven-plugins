package org.apache.maven.plugin.antlr;

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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates Antlr documentation from grammar files.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal html
 * @see <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
 */
public class AntlrHtmlReport
    extends AbstractAntlrMojo
    implements MavenReport
{
    // ----------------------------------------------------------------------
    // Report Parameters
    // ----------------------------------------------------------------------

    /**
     * Generates the site report
     *
     * @component
     */
    private Renderer siteRenderer;

    /**
     * Internationalization.
     *
     * @component
     */
    protected I18N i18n;

    /**
     * Specifies the destination directory where Antlr generates HTML files.
     *
     * @parameter expression="${project.build.directory}/generated-site/antlr"
     * @required
     */
    private File reportOutputDirectory;

    /**
     * The name of the Antlr report.
     *
     * @parameter expression="${name}" default-value="Antlr Grammars"
     */
    private String name;

    /**
     * The description of the Antlr report.
     *
     * @parameter expression="${description}" default-value="Generated Antlr report from grammars."
     */
    private String description;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        if ( StringUtils.isEmpty( name ) )
        {
            return i18n.getString( "antlr-report", locale, "report.name" );
        }

        return "Antlr Grammars";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        if ( StringUtils.isEmpty( description ) )
        {
            return i18n.getString( "antlr-report", locale, "report.description" );
        }

        return description;
    }

    /**
     * @see org.apache.maven.plugin.antlr.AbstractAntlrMojo#addArgs(java.util.List)
     */
    protected void addArgs( List arguments )
    {
        // ----------------------------------------------------------------------
        // See http://www.antlr.org/doc/options.html#Command%20Line%20Options
        // ----------------------------------------------------------------------

        arguments.add( "-html" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#generate(org.codehaus.doxia.sink.Sink, java.util.Locale)
     */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        outputDirectory = getReportOutputDirectory();

        try
        {
            executeAntlr();
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( "Antlr execution failed: " + e.getMessage(), e );
        }

        AntlrRenderer r = new AntlrRenderer( sink, outputDirectory, i18n, Locale.ENGLISH );
        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "antlr/index";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#isExternalReport()
     */
    public boolean isExternalReport()
    {
        return false;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return true;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_REPORTS;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getReportOutputDirectory()
     */
    public File getReportOutputDirectory()
    {
        if ( reportOutputDirectory == null )
        {
            return outputDirectory;
        }

        return reportOutputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#setReportOutputDirectory(java.io.File)
     */
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        if ( ( reportOutputDirectory != null ) && ( !reportOutputDirectory.getAbsolutePath().endsWith( "antlr" ) ) )
        {
            this.reportOutputDirectory = new File( reportOutputDirectory, "antlr" );
        }
        else
        {
            this.reportOutputDirectory = reportOutputDirectory;
        }
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            SiteRendererSink sink = siteRenderer.createSink( getReportOutputDirectory(), getOutputName() + ".html" );

            generate( sink, Locale.getDefault() );
        }
        catch ( RendererException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        catch ( IOException e )
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

    /**
     * Renderer report
     */
    private static class AntlrRenderer
        extends AbstractMavenReportRenderer
    {
        private File outputDirectory;

        private I18N i18n;

        private Locale locale;

        AntlrRenderer( Sink sink, File outputDirectory, I18N i18n, Locale locale )
        {
            super( sink );

            this.outputDirectory = outputDirectory;

            this.i18n = i18n;

            this.locale = locale;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return i18n.getString( "antlr-report", locale, "report.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            startSection( i18n.getString( "antlr-report", locale, "report.overview.title" ) );

            paragraph( i18n.getString( "antlr-report", locale, "report.overview.intro" ) );

            endSection();

            startSection( i18n.getString( "antlr-report", locale, "report.grammars.title" ) );

            try
            {
                List htmlFiles = FileUtils.getFiles( outputDirectory, "**/*.html", "**/*index.html" );

                if ( htmlFiles.isEmpty() )
                {
                    sink.text( i18n.getString( "antlr-report", locale, "report.grammars.noreport" ) );
                }
                else
                {
                    sink.list();
                    for ( Iterator it = htmlFiles.iterator(); it.hasNext(); )
                    {
                        File current = (File) it.next();

                        sink.listItem();
                        sink.link( PathUtils.toRelative( outputDirectory, current.getAbsolutePath() ) );
                        sink.text( StringUtils.replace( current.getName(), ".html", "" ) );
                        sink.link_();
                        sink.listItem_();
                    }
                    sink.list_();
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "IOException: " + e.getMessage(), e );
            }

            endSection();

            sink.flush();
            sink.close();
        }
    }
}
