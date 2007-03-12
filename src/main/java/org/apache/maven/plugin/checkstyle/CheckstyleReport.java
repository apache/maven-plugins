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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageNamesLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.FilterSet;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import com.puppycrawl.tools.checkstyle.filters.SuppressionsLoader;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Perform checkstyle analysis, and generate report on violations.
 * 
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @goal checkstyle
 */
public class CheckstyleReport
    extends AbstractMavenReport
{
    private static final String PLUGIN_RESOURCES = "org/apache/maven/plugin/checkstyle";

    /**
     * @deprecated Remove with format parameter.
     */
    private static final Map FORMAT_TO_CONFIG_LOCATION;

    static
    {
        Map fmt2Cfg = new HashMap();

        fmt2Cfg.put( "sun", "config/sun_checks.xml" );
        fmt2Cfg.put( "turbine", "config/turbine_checks.xml" );
        fmt2Cfg.put( "avalon", "config/avalon_checks.xml" );
        fmt2Cfg.put( "maven", "config/maven_checks.xml" );

        FORMAT_TO_CONFIG_LOCATION = Collections.unmodifiableMap( fmt2Cfg );
    }

    /**
     * skip entire check
     * 
     * @parameter expression="${checkstyle.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Specifies the directory where the report will be generated
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
     * Specifies the names filter of the source files to be used for checkstyle
     * 
     * @parameter expression="${checkstyle.includes}" default-value="**\/*.java"
     * @required
     */
    private String includes;

    /**
     * Specifies the names filter of the source files to be excluded for
     * checkstyle
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
     * file before being passed to checkstyle as a configuration.
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
     * the Sun coding conventions), "turbine", and "avalon". Default is sun.
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
     * This parameter is resolved as URL, File, then resource. If successfully
     * resolved, the contents of the properties location is copied into the
     * <code>${project.build.directory}/checkstyle-checker.properties</code>
     * file before being passed to checkstyle for loading.
     * </p>
     * 
     * <p>
     * The contents of the <code>propertiesLocation</code> will be made
     * available to checkstyle for specifying values for parameters within the
     * xml configuration (specified in the <code>configLocation</code>
     * parameter).
     * </p>
     * 
     * @parameter expression="${checkstyle.properties.location}"
     * @since 2.0-beta-2
     */
    private String propertiesLocation;

    /**
     * Specifies the location of the checkstyle properties that will be used to
     * check the source.
     * 
     * @parameter
     * @deprecated Use propertiesLocation instead.
     */
    private File propertiesFile;

    /**
     * Specifies the URL of the checkstyle properties that will be used to check
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
     * copyright.
     * </p>
     * <p>
     * You need to use ${checkstyle.header.file} in your checkstyle xml
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
     * @parameter expression="${checkstyle.header.file}"
     *            default-value="LICENSE.txt"
     * @since 2.0-beta-2
     */
    private String headerLocation;

    /**
     * Specifies the location of the License file (a.k.a. the header file) that
     * is used by Checkstyle to verify that source code has the correct
     * copyright.
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
     * If null, the checkstyle task will display violations on stdout.
     * Otherwise, the text file will be created with the violations.
     * 
     * @parameter
     */
    private File useFile;

    /**
     * <p>
     * Specifies the location of the suppressions XML file to use.
     * </p>
     * 
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the suppressions XML is copied into the
     * <code>${project.build.directory}/checkstyle-supressions.xml</code> file
     * before being passed to checkstyle for loading.
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
     * Specifies the location of the supperssions XML file to use. The plugin
     * defines a Checkstyle property named
     * <code>checkstyle.suppressions.file</code> with the value of this
     * property. This allows using the Checkstyle property your own custom
     * checkstyle configuration file when specifying a suppressions file.
     * 
     * @parameter
     * @deprecated Use suppressionsLocation instead.
     */
    private String suppressionsFile;

    /**
     * Specifies the path and filename to save the checkstyle output. The format
     * of the output file is determined by the <code>outputFileFormat</code>
     * 
     * @parameter expression="${checkstyle.output.file}"
     *            default-value="${project.build.directory}/checkstyle-result.xml"
     */
    private File outputFile;

    /**
     * Specifies the format of the output to be used when writing to the output
     * file. Valid values are "plain" and "xml"
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
     * file before being passed to checkstyle for loading.
     * </p>
     * 
     * @parameter
     * @since 2.0-beta-2
     */
    private String packageNamesLocation;

    /**
     * Specifies the location of the package names XML to be used to configure
     * Checkstyle
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
     * Specifies the location of the source directory to be used for Checkstyle
     * 
     * @parameter default-value="${project.build.sourceDirectory}"
     * @required
     */
    private File sourceDirectory;

    /**
     * Specifies the location of the test source directory to be used for
     * Checkstyle
     * 
     * @parameter default-value="${project.build.testSourceDirectory}"
     */
    private File testSourceDirectory;

    /**
     * Include or not the test source directory to be used for Checkstyle
     * 
     * @parameter default-value="${false}"
     */
    private boolean includeTestSourceDirectory;

    /**
     * The Maven Project Object
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
     * Link the violation line numbers to the source xref. Defaults to true and
     * will link automatically if jxr plugin is being used.
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
    private SiteRenderer siteRenderer;

    /**
     * Velocity Component
     * 
     * @component role="org.codehaus.plexus.velocity.VelocityComponent"
     * @required
     */
    private VelocityComponent velocityComponent;

    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    private StringOutputStream stringOutputStream;

    /**
     * @component
     * @required
     * @readonly
     */
    private ResourceManager locator;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.checkstyle.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.checkstyle.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !skip )
        {
            mergeDeprecatedInfo();

            locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );

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
                // checkstyle will always use the context classloader in order
                // to load resources (dtds),
                // so we have to fix it
                ClassLoader checkstyleClassLoader = PackageNamesLoader.class.getClassLoader();
                Thread.currentThread().setContextClassLoader( checkstyleClassLoader );
                

                String configFile = getConfigFile();
                Properties overridingProperties = getOverridingProperties();
                ModuleFactory moduleFactory;
                Configuration config;
                CheckstyleResults results;

                moduleFactory = getModuleFactory();
                
                config = ConfigurationLoader.loadConfiguration( configFile,
                                                                new PropertiesExpander( overridingProperties ) );
                results = executeCheckstyle( config, moduleFactory );
                
                ResourceBundle bundle = getBundle( locale );
                generateReportStatics();
                generateMainReport( results, config, moduleFactory, bundle );
                if ( enableRSS )
                {
                    generateRSS( results );
                }

            }
            catch ( CheckstyleException e )
            {
                throw new MavenReportException( "Failed during checkstyle configuration", e );
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

    private void generateRSS( CheckstyleResults results )
        throws MavenReportException
    {
        VelocityTemplate vtemplate = new VelocityTemplate( velocityComponent, PLUGIN_RESOURCES );
        vtemplate.setLog( getLog() );

        Context context = new VelocityContext();
        context.put( "results", results );
        context.put( "project", project );
        context.put( "copyright", getCopyright() );
        context.put( "levelInfo", SeverityLevel.INFO );
        context.put( "levelWarning", SeverityLevel.WARNING );
        context.put( "levelError", SeverityLevel.ERROR );
        context.put( "stringutils", new StringUtils() );

        try
        {
            vtemplate.generate( outputDirectory.getPath() + "/checkstyle.rss", "checkstyle-rss.vm", context );
        }
        catch ( ResourceNotFoundException e )
        {
            throw new MavenReportException( "Unable to find checkstyle-rss.vm resource.", e );
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( "Unable to generate checkstyle.rss.", e );
        }
        catch ( VelocityException e )
        {
            throw new MavenReportException( "Unable to generate checkstyle.rss.", e );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to generate checkstyle.rss.", e );
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

    private void generateMainReport( CheckstyleResults results, Configuration config, ModuleFactory moduleFactory,
                                     ResourceBundle bundle )
    {
        CheckstyleReportGenerator generator = new CheckstyleReportGenerator( getSink(), bundle );

        generator.setLog( getLog() );
        generator.setEnableRulesSummary( enableRulesSummary );
        generator.setEnableSeveritySummary( enableSeveritySummary );
        generator.setEnableFilesSummary( enableFilesSummary );
        generator.setEnableRSS( enableRSS );
        generator.setCheckstyleConfig( config );
        generator.setCheckstyleModuleFactory( moduleFactory );
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
                for ( Iterator reports = getProject().getReportPlugins().iterator(); reports.hasNext(); )
                {
                    ReportPlugin report = (ReportPlugin) reports.next();

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

    private CheckstyleResults executeCheckstyle( Configuration config, ModuleFactory moduleFactory )
        throws MavenReportException, CheckstyleException
    {
        File[] files;
        try
        {
            files = getFilesToProcess( includes, excludes );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Error getting files to process", e );
        }

        FilterSet filterSet = getSuppressions();
        
        Checker checker = new Checker();

        // setup classloader, needed to avoid "Unable to get class information
        // for ..." errors
        List classPathStrings;
        try
        {
            classPathStrings = this.project.getCompileClasspathElements();

            if ( includeTestSourceDirectory && ( testSourceDirectory != null ) && ( testSourceDirectory.exists() )
                && ( testSourceDirectory.isDirectory() ) )
            {
                classPathStrings = this.project.getTestClasspathElements();
            }
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }

        List urls = new ArrayList( classPathStrings.size() );

        Iterator iter = classPathStrings.iterator();
        while ( iter.hasNext() )
        {
            try
            {
                urls.add( new File( ( (String) iter.next() ) ).toURL() );
            }
            catch ( MalformedURLException e )
            {
                throw new MavenReportException( e.getMessage(), e );
            }
        }

        URLClassLoader projectClassLoader = new URLClassLoader( (URL[]) urls.toArray( new URL[urls.size()] ), null );
        checker.setClassloader( projectClassLoader );

        if ( moduleFactory != null )
        {
            checker.setModuleFactory( moduleFactory );
        }

        if ( filterSet != null )
        {
            checker.addFilter( filterSet );
        }

        checker.configure( config );

        AuditListener listener = getListener();

        if ( listener != null )
        {
            checker.addListener( listener );
        }

        if ( consoleOutput )
        {
            checker.addListener( getConsoleListener() );
        }

        CheckstyleReportListener sinkListener = new CheckstyleReportListener( sourceDirectory );
        if ( includeTestSourceDirectory && ( testSourceDirectory != null ) && ( testSourceDirectory.exists() )
            && ( testSourceDirectory.isDirectory() ) )
        {
            sinkListener.addSourceDirectory( testSourceDirectory );
        }

        checker.addListener( sinkListener );

        int nbErrors = checker.process( files );

        checker.destroy();

        if ( stringOutputStream != null )
        {
            getLog().info( stringOutputStream.toString() );
        }

        if ( failsOnError && nbErrors > 0 )
        {
            // TODO: should be a failure, not an error. Report is not meant to
            // throw an exception here (so site would
            // work regardless of config), but should record this information
            throw new MavenReportException( "There are " + nbErrors + " checkstyle errors." );
        }
        else if ( nbErrors > 0 )
        {
            getLog().info( "There are " + nbErrors + " checkstyle errors." );
        }

        return sinkListener.getResults();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
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

    private File[] getFilesToProcess( String includes, String excludes )
        throws IOException
    {
        StringBuffer excludesStr = new StringBuffer();

        if ( StringUtils.isNotEmpty( excludes ) )
        {
            excludesStr.append( excludes );
        }

        String[] defaultExcludes = FileUtils.getDefaultExcludes();
        for ( int i = 0; i < defaultExcludes.length; i++ )
        {
            if ( excludesStr.length() > 0 )
            {
                excludesStr.append( "," );
            }

            excludesStr.append( defaultExcludes[i] );
        }

        List files = FileUtils.getFiles( sourceDirectory, includes, excludesStr.toString() );
        if ( includeTestSourceDirectory && ( testSourceDirectory != null ) && ( testSourceDirectory.exists() )
            && ( testSourceDirectory.isDirectory() ) )
        {
            files.addAll( FileUtils.getFiles( testSourceDirectory, includes, excludesStr.toString() ) );
        }

        return (File[]) files.toArray( EMPTY_FILE_ARRAY );
    }

    private Properties getOverridingProperties()
        throws MavenReportException
    {
        Properties p = new Properties();

        try
        {
            File propertiesFile = locator.resolveLocation( propertiesLocation, "checkstyle-checker.properties" );

            if ( propertiesFile != null )
            {
                p.load( new FileInputStream( propertiesFile ) );
            }

            if ( StringUtils.isNotEmpty( propertyExpansion ) )
            {
                p.load( new StringInputStream( propertyExpansion ) );
            }

            if ( StringUtils.isNotEmpty( headerLocation ) )
            {
                try
                {
                    File headerFile = locator.resolveLocation( headerLocation, "checkstyle-header.txt" );

                    if ( headerFile != null )
                    {
                        p.setProperty( "checkstyle.header.file", headerFile.getAbsolutePath() );
                    }
                }
                catch ( IOException e )
                {
                    throw new MavenReportException( "Unable to process header location: " + headerLocation, e );
                }
            }

            if ( cacheFile != null )
            {
                p.setProperty( "checkstyle.cache.file", cacheFile );
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Failed to get overriding properties", e );
        }

        if ( suppressionsFileExpression != null )
        {
            String suppresionFile = getSuppressionLocation();

            if ( suppresionFile != null )
            {
                p.setProperty( suppressionsFileExpression, suppresionFile );
            }
        }

        return p;
    }

    private String getConfigFile()
        throws MavenReportException
    {
        try
        {
            File configFile = locator.resolveLocation( configLocation, "checkstyle-checker.xml" );

            if ( configFile == null )
            {
                throw new MavenReportException( "Unable to process null config location." );
            }
            return configFile.getAbsolutePath();
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to find configuration file location.", e );
        }

    }

    private ModuleFactory getModuleFactory()
        throws CheckstyleException
    {
        // default to internal module factory.
        ModuleFactory moduleFactory = PackageNamesLoader.loadModuleFactory( Thread.currentThread()
            .getContextClassLoader() );

        try
        {
            // attempt to locate any specified package file.
            File packageNamesFile = locator.resolveLocation( packageNamesLocation, "checkstyle-packages.xml" );

            if ( packageNamesFile != null )
            {
                // load resolved location.
                moduleFactory = PackageNamesLoader.loadModuleFactory( packageNamesFile.getAbsolutePath() );
            }
        }
        catch ( IOException e )
        {
            getLog().error( "Unable to process package names location: " + packageNamesLocation, e );
        }
        return moduleFactory;
    }

    private String getSuppressionLocation()
        throws MavenReportException
    {
        try
        {
            File suppressionsFile = locator.resolveLocation( suppressionsLocation, "checkstyle-suppressions.xml" );

            if ( suppressionsFile == null )
            {
                return null;
            }

            return suppressionsFile.getAbsolutePath();
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Failed to process supressions location: " + suppressionsLocation, e );
        }
    }

    private FilterSet getSuppressions()
        throws MavenReportException
    {
        try
        {
            File suppressionsFile = locator.resolveLocation( suppressionsLocation, "checkstyle-suppressions.xml" );
            
            if ( suppressionsFile == null )
            {
                return null;
            }

            return SuppressionsLoader.loadSuppressions( suppressionsFile.getAbsolutePath() );
        }
        catch ( CheckstyleException ce )
        {
            throw new MavenReportException( "failed to load suppressions location: " + suppressionsLocation, ce );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Failed to process supressions location: " + suppressionsLocation, e );
        }
    }

    private DefaultLogger getConsoleListener()
        throws MavenReportException
    {
        DefaultLogger consoleListener;

        if ( useFile == null )
        {
            stringOutputStream = new StringOutputStream();
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

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        // TODO: would be good to scan the files here
        return sourceDirectory.exists();
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#setReportOutputDirectory(java.io.File)
     */
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        super.setReportOutputDirectory( reportOutputDirectory );
        this.outputDirectory = reportOutputDirectory;
    }
}
