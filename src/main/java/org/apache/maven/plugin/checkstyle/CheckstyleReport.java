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
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
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
 * Perform a Checkstyle analysis, and generate a report on violations.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @goal checkstyle
 * @requiresDependencyResolution compile
 * @threadSafe
 */
public class CheckstyleReport
    extends AbstractMavenReport
{
    public static final String PLUGIN_RESOURCES = "org/apache/maven/plugin/checkstyle";

    /**
     * @deprecated Remove with format parameter.
     */
    private static final Map<String, String> FORMAT_TO_CONFIG_LOCATION;

    static
    {
        Map<String, String> fmt2Cfg = new HashMap<String, String>();

        fmt2Cfg.put( "sun", "config/sun_checks.xml" );
        fmt2Cfg.put( "turbine", "config/turbine_checks.xml" );
        fmt2Cfg.put( "avalon", "config/avalon_checks.xml" );
        fmt2Cfg.put( "maven", "config/maven_checks.xml" );

        FORMAT_TO_CONFIG_LOCATION = Collections.unmodifiableMap( fmt2Cfg );
    }

    /**
     * Skip entire check.
     *
     * @parameter expression="${checkstyle.skip}" default-value="false"
     * @since 2.2
     */
    private boolean skip;

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
     * Specifies the names filter of the source files to be used for Checkstyle.
     *
     * @parameter expression="${checkstyle.includes}" default-value="**\/*.java"
     * @required
     */
    private String includes;

    /**
     * Specifies the names filter of the source files to be excluded for
     * Checkstyle.
     *
     * @parameter expression="${checkstyle.excludes}"
     */
    private String excludes;

    /**
     * <p>
     * Specifies the location of the XML configuration to use.
     * </p>
     *
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * This parameter expects that the contents of the location conform to the
     * xml format (Checkstyle <a
     * href="http://checkstyle.sourceforge.net/config.html#Modules">Checker
     * module</a>) configuration of rulesets.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}/checkstyle-configuration.xml</code>
     * file before being passed to Checkstyle as a configuration.
     * </p>
     *
     * <p>
     * There are 4 predefined rulesets.
     * </p>
     *
     * <ul>
     * <li><code>config/sun_checks.xml</code>: Sun Checks.</li>
     * <li><code>config/turbine_checks.xml</code>: Turbine Checks.</li>
     * <li><code>config/avalon_checks.xml</code>: Avalon Checks.</li>
     * <li><code>config/maven_checks.xml</code>: Maven Source Checks.</li>
     * </ul>
     *
     * @parameter expression="${checkstyle.config.location}"
     *            default-value="config/sun_checks.xml"
     */
    private String configLocation;

    /**
     * Specifies what predefined check set to use. Available sets are "sun" (for
     * the Sun coding conventions), "turbine", and "avalon".
     *
     * @parameter default-value="sun"
     * @deprecated Use configLocation instead.
     */
    private String format;

    /**
     * <p>
     * Specifies the location of the properties file.
     * </p>
     *
     * <p>
     * This parameter is resolved as URL, File then resource. If successfully
     * resolved, the contents of the properties location is copied into the
     * <code>${project.build.directory}/checkstyle-checker.properties</code>
     * file before being passed to Checkstyle for loading.
     * </p>
     *
     * <p>
     * The contents of the <code>propertiesLocation</code> will be made
     * available to Checkstyle for specifying values for parameters within the
     * xml configuration (specified in the <code>configLocation</code>
     * parameter).
     * </p>
     *
     * @parameter expression="${checkstyle.properties.location}"
     * @since 2.0-beta-2
     */
    private String propertiesLocation;

    /**
     * Specifies the location of the Checkstyle properties file that will be used to
     * check the source.
     *
     * @parameter
     * @deprecated Use propertiesLocation instead.
     */
    private File propertiesFile;

    /**
     * Specifies the URL of the Checkstyle properties that will be used to check
     * the source.
     *
     * @parameter
     * @deprecated Use propertiesLocation instead.
     */
    private URL propertiesURL;

    /**
     * Allows for specifying raw property expansion information.
     *
     * @parameter
     */
    private String propertyExpansion;

    /**
     * <p>
     * Specifies the location of the License file (a.k.a. the header file) that
     * can be used by Checkstyle to verify that source code has the correct
     * license header.
     * </p>
     * <p>
     * You need to use ${checkstyle.header.file} in your Checkstyle xml
     * configuration to reference the name of this header file.
     * </p>
     * <p>
     * For instance:
     * </p>
     * <p>
     * <code>
     * &lt;module name="RegexpHeader">
     *   &lt;property name="headerFile" value="${checkstyle.header.file}"/>
     * &lt;/module>
     * </code>
     * </p>
     *
     * @parameter expression="${checkstyle.header.file}" default-value="LICENSE.txt"
     * @since 2.0-beta-2
     */
    private String headerLocation;

    /**
     * Specifies the location of the License file (a.k.a. the header file) that
     * is used by Checkstyle to verify that source code has the correct
     * license header.
     *
     * @parameter expression="${basedir}/LICENSE.txt"
     * @deprecated Use headerLocation instead.
     */
    private File headerFile;

    /**
     * Specifies the cache file used to speed up Checkstyle on successive runs.
     *
     * @parameter default-value="${project.build.directory}/checkstyle-cachefile"
     */
    private String cacheFile;

    /**
     * If <code>null</code>, the Checkstyle plugin will display violations on stdout.
     * Otherwise, a text file will be created with the violations.
     *
     * @parameter
     */
    private File useFile;

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
     * <p>
     * Specifies the location of the suppressions XML file to use.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the suppressions XML is copied into the
     * <code>${project.build.directory}/checkstyle-supressions.xml</code> file
     * before being passed to Checkstyle for loading.
     * </p>
     *
     * <p>
     * See <code>suppressionsFileExpression</code> for the property that will
     * be made available to your checkstyle configuration.
     * </p>
     *
     * @parameter expression="${checkstyle.suppressions.location}"
     * @since 2.0-beta-2
     */
    private String suppressionsLocation;

    /**
     * The key to be used in the properties for the suppressions file.
     *
     * @parameter expression="${checkstyle.suppression.expression}"
     *            default-value="checkstyle.suppressions.file"
     * @since 2.1
     */
    private String suppressionsFileExpression;

    /**
     * Specifies the location of the suppressions XML file to use. The plugin
     * defines a Checkstyle property named
     * <code>checkstyle.suppressions.file</code> with the value of this
     * property. This allows using the Checkstyle property in your own custom
     * checkstyle configuration file when specifying a suppressions file.
     *
     * @parameter
     * @deprecated Use suppressionsLocation instead.
     */
    private String suppressionsFile;

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
     * Specifies the format of the output to be used when writing to the output
     * file. Valid values are "plain" and "xml".
     *
     * @parameter expression="${checkstyle.output.format}" default-value="xml"
     */
    private String outputFileFormat;

    /**
     * <p>
     * Specifies the location of the package names XML to be used to configure
     * the Checkstyle <a
     * href="http://checkstyle.sourceforge.net/config.html#Packages">Packages</a>.
     * </p>
     *
     * <p>
     * This parameter is resolved as resource, URL, then file. If resolved to a
     * resource, or a URL, the contents of the package names XML is copied into
     * the <code>${project.build.directory}/checkstyle-packagenames.xml</code>
     * file before being passed to Checkstyle for loading.
     * </p>
     *
     * @parameter
     * @since 2.0-beta-2
     */
    private String packageNamesLocation;

    /**
     * Specifies the location of the package names XML to be used to configure
     * Checkstyle.
     *
     * @parameter
     * @deprecated Use packageNamesLocation instead.
     */
    private String packageNamesFile;

    /**
     * Specifies if the build should fail upon a violation.
     *
     * @parameter default-value="false"
     */
    private boolean failsOnError;

    /**
     * Specifies the location of the source directory to be used for Checkstyle.
     *
     * @parameter default-value="${project.build.sourceDirectory}"
     * @required
     */
    private File sourceDirectory;

    /**
     * Specifies the location of the test source directory to be used for
     * Checkstyle.
     *
     * @parameter default-value="${project.build.testSourceDirectory}"
     * @since 2.2
     */
    private File testSourceDirectory;

    /**
     * Include or not the test source directory to be used for Checkstyle.
     *
     * @parameter default-value="${false}"
     * @since 2.2
     */
    private boolean includeTestSourceDirectory;

    /**
     * The Maven Project Object.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Output errors to console.
     *
     * @parameter default-value="false"
     */
    private boolean consoleOutput;

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
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used. <strong>Note:</strong> This parameter always overrides the
     * property <code>charset</code> from Checkstyle's <code>TreeWalker</code> module.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     * @since 2.2
     */
    private String encoding;

    /**
     * @component
     * @required
     * @readonly
     */
    private Renderer siteRenderer;

    private ByteArrayOutputStream stringOutputStream;

    /**
     * @component
     * @required
     * @readonly
     */
    private ResourceManager locator;

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
        if ( !skip )
        {
            mergeDeprecatedInfo();

            locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );
            locator.addSearchPath( "url", "" );

            locator.setOutputDirectory( new File( project.getBuild().getDirectory() ) );

            if ( !canGenerateReport() )
            {
                getLog().info( "Source directory does not exist - skipping report." );
                return;
            }

            // for when we start using maven-shared-io and
            // maven-shared-monitor...
            // locator = new Locator( new MojoLogMonitorAdaptor( getLog() ) );

            // locator = new Locator( getLog(), new File(
            // project.getBuild().getDirectory() ) );

            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

            try
            {
                CheckstyleExecutorRequest request = new CheckstyleExecutorRequest();
                request.setConsoleListener( getConsoleListener() ).setConsoleOutput( consoleOutput )
                    .setExcludes( excludes ).setFailsOnError( failsOnError ).setIncludes( includes )
                    .setIncludeTestSourceDirectory( includeTestSourceDirectory ).setListener( getListener() )
                    .setLog( getLog() ).setProject( project ).setSourceDirectory( sourceDirectory )
                    .setStringOutputStream( stringOutputStream ).setSuppressionsLocation( suppressionsLocation )
                    .setTestSourceDirectory( testSourceDirectory ).setConfigLocation( configLocation )
                    .setPropertyExpansion( propertyExpansion ).setHeaderLocation( headerLocation )
                    .setCacheFile( cacheFile ).setSuppressionsFileExpression( suppressionsFileExpression )
                    .setEncoding( encoding ).setPropertiesLocation( propertiesLocation );

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

    /**
     * Merge in the deprecated parameters to the new ones, unless the new
     * parameters have values.
     *
     * @deprecated Remove when deprecated params are removed.
     */
    private void mergeDeprecatedInfo()
    {
        if ( "config/sun_checks.xml".equals( configLocation ) && !"sun".equals( format ) )
        {
            configLocation = (String) FORMAT_TO_CONFIG_LOCATION.get( format );
        }

        if ( StringUtils.isEmpty( propertiesLocation ) )
        {
            if ( propertiesFile != null )
            {
                propertiesLocation = propertiesFile.getPath();
            }
            else if ( propertiesURL != null )
            {
                propertiesLocation = propertiesURL.toExternalForm();
            }
        }

        if ( "LICENSE.txt".equals( headerLocation ) )
        {
            File defaultHeaderFile = new File( project.getBasedir(), "LICENSE.txt" );
            if ( !defaultHeaderFile.equals( headerFile ) )
            {
                headerLocation = headerFile.getPath();
            }
        }

        if ( StringUtils.isEmpty( suppressionsLocation ) )
        {
            suppressionsLocation = suppressionsFile;
        }

        if ( StringUtils.isEmpty( packageNamesLocation ) )
        {
            packageNamesLocation = packageNamesFile;
        }
    }


    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "checkstyle";
    }

    private AuditListener getListener()
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

    private DefaultLogger getConsoleListener()
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

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "checkstyle-report", locale, CheckstyleReport.class.getClassLoader() );
    }

    /** {@inheritDoc} */
    public boolean canGenerateReport()
    {
        // TODO: would be good to scan the files here
        return sourceDirectory.exists() || ( includeTestSourceDirectory && testSourceDirectory.exists() );
    }
    
    /** {@inheritDoc} */
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        super.setReportOutputDirectory( reportOutputDirectory );
        this.outputDirectory = reportOutputDirectory;
    }
}
