package org.apache.maven.plugin.checkstyle;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.checkstyle.rss.CheckstyleRssGenerator;
import org.apache.maven.plugin.checkstyle.rss.CheckstyleRssGeneratorRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringUtils;

import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/**
 * Base abstract class for Checkstyle reports.
 *
 * @version $Id: CheckstyleReport.java 1155028 2011-08-08 17:53:46Z olamy $
 * @since 2.8
 */
public abstract class AbstractCheckstyleReport
    extends AbstractMavenReport
{
    public static final String PLUGIN_RESOURCES = "org/apache/maven/plugin/checkstyle";

    /**
     * Skip entire check.
     *
     * @parameter expression="${checkstyle.skip}" default-value="false"
     * @since 2.2
     */
    protected boolean skip;

    /**
     * The output directory for the report. Note that this parameter is only
     * evaluated if the goal is run directly from the command line. If the goal
     * is run indirectly as part of a site generation, the output directory
     * configured in Maven Site Plugin is used instead.
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Specifies the path and filename to save the checkstyle output. The format
     * of the output file is determined by the <code>outputFileFormat</code>
     * parameter.
     *
     * @parameter expression="${checkstyle.output.file}"
     *            default-value="${project.build.directory}/checkstyle-result.xml"
     */
    private File outputFile;

    /**
     * If <code>null</code>, the Checkstyle plugin will display violations on stdout.
     * Otherwise, a text file will be created with the violations.
     *
     * @parameter
     */
    private File useFile;

    /**
     * Specifies the format of the output to be used when writing to the output
     * file. Valid values are "plain" and "xml".
     *
     * @parameter expression="${checkstyle.output.format}" default-value="xml"
     */
    private String outputFileFormat;

    /**
     * Specifies if the Rules summary should be enabled or not.
     *
     * @parameter expression="${checkstyle.enable.rules.summary}"
     *            default-value="true"
     */
    private boolean enableRulesSummary;

    /**
     * Specifies if the Severity summary should be enabled or not.
     *
     * @parameter expression="${checkstyle.enable.severity.summary}"
     *            default-value="true"
     */
    private boolean enableSeveritySummary;

    /**
     * Specifies if the Files summary should be enabled or not.
     *
     * @parameter expression="${checkstyle.enable.files.summary}"
     *            default-value="true"
     */
    private boolean enableFilesSummary;

    /**
     * Specifies if the RSS should be enabled or not.
     *
     * @parameter expression="${checkstyle.enable.rss}" default-value="true"
     */
    private boolean enableRSS;

    /**
     * SiteTool.
     *
     * @since 2.2
     * @component role="org.apache.maven.doxia.tools.SiteTool"
     * @required
     * @readonly
     */
    protected SiteTool siteTool;

    /**
     * The Maven Project Object.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Link the violation line numbers to the source xref. Will link
     * automatically if Maven JXR plugin is being used.
     *
     * @parameter expression="${linkXRef}" default-value="true"
     * @since 2.1
     */
    private boolean linkXRef;

    /**
     * Location of the Xrefs to link to.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref"
     */
    private File xrefLocation;

    /**
     * @component
     * @required
     * @readonly
     */
    private Renderer siteRenderer;

    /**
     * @component
     * @required
     * @readonly
     */
    protected ResourceManager locator;

    /**
     * CheckstyleRssGenerator.
     *
     * @since 2.4
     * @component role="org.apache.maven.plugin.checkstyle.rss.CheckstyleRssGenerator" role-hint="default"
     * @required
     * @readonly
     */
    protected CheckstyleRssGenerator checkstyleRssGenerator;

    /**
     * @since 2.5
     * @component role="org.apache.maven.plugin.checkstyle.CheckstyleExecutor" role-hint="default"
     * @required
     * @readonly
     */
    protected CheckstyleExecutor checkstyleExecutor;

    protected ByteArrayOutputStream stringOutputStream;

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.checkstyle.name" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.checkstyle.description" );
    }

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
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );
        locator.addSearchPath( "url", "" );

        locator.setOutputDirectory( new File( project.getBuild().getDirectory() ) );

        // for when we start using maven-shared-io and
        // maven-shared-monitor...
        // locator = new Locator( new MojoLogMonitorAdaptor( getLog() ) );

        // locator = new Locator( getLog(), new File(
        // project.getBuild().getDirectory() ) );

        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            CheckstyleExecutorRequest request = createRequest();

            CheckstyleResults results = checkstyleExecutor.executeCheckstyle( request );

            ResourceBundle bundle = getBundle( locale );
            generateReportStatics();
            generateMainReport( results, bundle );
            if ( enableRSS )
            {
                CheckstyleRssGeneratorRequest checkstyleRssGeneratorRequest =
                    new CheckstyleRssGeneratorRequest( this.project, this.getCopyright(), outputDirectory, getLog() );
                checkstyleRssGenerator.generateRSS( results, checkstyleRssGeneratorRequest );
            }

        }
        catch ( CheckstyleException e )
        {
            throw new MavenReportException( "Failed during checkstyle configuration", e );
        }
        catch ( CheckstyleExecutorException e )
        {
            throw new MavenReportException( "Failed during checkstyle execution", e );
        }
        finally
        {
            //be sure to restore original context classloader
            Thread.currentThread().setContextClassLoader( currentClassLoader );
        }
    }

    /**
     * Create the Checkstyle executor request.
     *
     * @return The executor request.
     * @throws MavenReportException If something goes wrong during creation.
     */
    protected abstract CheckstyleExecutorRequest createRequest()
            throws MavenReportException;

    /**
     * Creates and returns the report generation listener.
     *
     * @return The audit listener.
     * @throws MavenReportException If something goes wrong.
     */
    protected AuditListener getListener()
        throws MavenReportException
    {
        AuditListener listener = null;

        if ( StringUtils.isNotEmpty( outputFileFormat ) )
        {
            File resultFile = outputFile;

            OutputStream out = getOutputStream( resultFile );

            if ( "xml".equals( outputFileFormat ) )
            {
                listener = new XMLLogger( out, true );
            }
            else if ( "plain".equals( outputFileFormat ) )
            {
                listener = new DefaultLogger( out, true );
            }
            else
            {
                // TODO: failure if not a report
                throw new MavenReportException( "Invalid output file format: (" + outputFileFormat
                    + "). Must be 'plain' or 'xml'." );
            }
        }

        return listener;
    }

    private OutputStream getOutputStream( File file )
        throws MavenReportException
    {
        File parentFile = file.getAbsoluteFile().getParentFile();

        if ( !parentFile.exists() )
        {
            parentFile.mkdirs();
        }

        FileOutputStream fileOutputStream;
        try
        {
            fileOutputStream = new FileOutputStream( file );
        }
        catch ( FileNotFoundException e )
        {
            throw new MavenReportException( "Unable to create output stream: " + file, e );
        }
        return fileOutputStream;
    }

    /**
     * Creates and returns the console listener.
     *
     * @return The console listener.
     * @throws MavenReportException If something goes wrong.
     */
    protected DefaultLogger getConsoleListener()
        throws MavenReportException
    {
        DefaultLogger consoleListener;

        if ( useFile == null )
        {
            stringOutputStream = new ByteArrayOutputStream();
            consoleListener = new DefaultLogger( stringOutputStream, false );
        }
        else
        {
            OutputStream out = getOutputStream( useFile );

            consoleListener = new DefaultLogger( out, true );
        }

        return consoleListener;
    }

    private void generateReportStatics()
        throws MavenReportException
    {
        ReportResource rresource = new ReportResource( PLUGIN_RESOURCES, outputDirectory );
        try
        {
            rresource.copy( "images/rss.png" );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to copy static resources.", e );
        }
    }


    private String getCopyright()
    {
        String copyright;
        int currentYear = Calendar.getInstance().get( Calendar.YEAR );
        if ( StringUtils.isNotEmpty( project.getInceptionYear() )
            && !String.valueOf( currentYear ).equals( project.getInceptionYear() ) )
        {
            copyright = project.getInceptionYear() + " - " + currentYear;
        }
        else
        {
            copyright = String.valueOf( currentYear );
        }

        if ( ( project.getOrganization() != null ) && StringUtils.isNotEmpty( project.getOrganization().getName() ) )
        {
            copyright = copyright + " " + project.getOrganization().getName();
        }
        return copyright;
    }

    private void generateMainReport( CheckstyleResults results, ResourceBundle bundle )
    {
        CheckstyleReportGenerator generator =
            new CheckstyleReportGenerator( getSink(), bundle, project.getBasedir(), siteTool );

        generator.setLog( getLog() );
        generator.setEnableRulesSummary( enableRulesSummary );
        generator.setEnableSeveritySummary( enableSeveritySummary );
        generator.setEnableFilesSummary( enableFilesSummary );
        generator.setEnableRSS( enableRSS );
        generator.setCheckstyleConfig( results.getConfiguration() );
        if ( linkXRef )
        {
            String relativePath = PathTool.getRelativePath( getOutputDirectory(), xrefLocation.getAbsolutePath() );
            if ( StringUtils.isEmpty( relativePath ) )
            {
                relativePath = ".";
            }
            relativePath = relativePath + "/" + xrefLocation.getName();
            if ( xrefLocation.exists() )
            {
                // XRef was already generated by manual execution of a lifecycle
                // binding
                generator.setXrefLocation( relativePath );
            }
            else
            {
                // Not yet generated - check if the report is on its way
                for ( Iterator<ReportPlugin> reports = getProject().getReportPlugins().iterator(); reports.hasNext(); )
                {
                    ReportPlugin report = reports.next();

                    String artifactId = report.getArtifactId();
                    if ( "maven-jxr-plugin".equals( artifactId ) || "jxr-maven-plugin".equals( artifactId ) )
                    {
                        generator.setXrefLocation( relativePath );
                    }
                }
            }

            if ( generator.getXrefLocation() == null )
            {
                getLog().warn( "Unable to locate Source XRef to link to - DISABLED" );
            }
        }
        generator.generateReport( results );
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "checkstyle-report", locale, AbstractCheckstyleReport.class.getClassLoader() );
    }

    /** {@inheritDoc} */
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        super.setReportOutputDirectory( reportOutputDirectory );
        this.outputDirectory = reportOutputDirectory;
    }
}
