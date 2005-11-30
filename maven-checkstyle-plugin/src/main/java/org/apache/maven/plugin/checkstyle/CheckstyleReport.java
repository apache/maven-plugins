package org.apache.maven.plugin.checkstyle;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.puppycrawl.tools.checkstyle.filters.SuppressionsLoader;

import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * TODO: add report generation for ruleset/configuration. 
 * 
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @goal checkstyle
 */
public class CheckstyleReport
    extends AbstractMavenReport
{
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
     * Specifies the directory where the report will be generated
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Specifies the names filter of the source files to be used for checkstyle
     *
     * @parameter default-value="**\/*.java"
     * @required
     */
    private String includes;

    /**
     * Specifies the names filter of the source files to be excluded for checkstyle
     *
     * @parameter
     */
    private String excludes;

    /**
     * <p>
     * Specifies the location of the XML configuration to use.
     * </p>
     * 
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath
     * resource.  This parameter expects that the contents of the location
     * conform to the xml format (Checkstyle 
     * <a href="http://checkstyle.sourceforge.net/config.html#Modules">Checker 
     * module</a>) configuration of rulesets.
     * </p>
     * 
     * <p>
     * This parameter is resolved as resource, URL, then file.  
     * If resolved to a resource, or a URL, the contents of the configuration
     * is copied into the 
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
     * @parameter default-value="config/sun_checks.xml"
     */
    private String configLocation;
    
    /**
     * Specifies what predefined check set to use. Available sets are
     * "sun" (for the Sun coding conventions), "turbine", and "avalon".
     * Default is sun.
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
     * This parameter is resolved as URL, File, then resource.  
     * If successfully resolved, the contents of the properties location is 
     * copied into the 
     * <code>${project.build.directory}/checkstyle-checker.properties</code>
     * file before being passed to checkstyle for loading.
     * </p>
     * 
     * <p>
     * The contents of the <code>propertiesLocation</code> will be made 
     * available to checkstyle for specifying values for parameters within 
     * the xml configuration (specified in the <code>configLocation</code> 
     * parameter).
     * </p> 
     * 
     * @parameter 
     * @since 2.0-beta-2
     */
    private String propertiesLocation;
    
    /**
     * Specifies the location of the checkstyle properties that will be used to check the source.
     *
     * @parameter
     * @deprecated Use propertiesLocation instead.
     */
    private File propertiesFile;

    /**
     * Specifies the URL of the checkstyle properties that will be used to check the source.
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
     * Specifies the location of the License file (a.k.a. the header file) 
     * that is used by Checkstyle to verify that source code has the 
     * correct copyright.
     *
     * @parameter default-value="LICENSE.txt"
     * @since 2.0-beta-2
     */
    private String headerLocation;

    /**
     * Specifies the location of the License file (a.k.a. the header file) that is used by Checkstyle
     * to verify that source code has the correct copyright.
     *
     * @parameter expression="${basedir}/LICENSE.txt"
     * @deprecated Use headerLocation instead.
     */
    private File headerFile;
    
    /**
     * Specifies the DEFAULT location of the License file (a.k.a. the header file) in order to check whether
     * the headerFile parameter is defaulted.
     *
     * @parameter expression="${basedir}/LICENSE.txt"
     * @readonly
     * @required
     * @deprecated Remove with headerFile.
     */
    private File defaultHeaderFile;

    /**
     * Specifies the cache file used to speed up Checkstyle on successive runs.
     *
     * @parameter default-value="${project.build.directory}/checkstyle-cachefile"
     */
    private String cacheFile;

    /**
     * If null, the checkstyle task will display violations on stdout. Otherwise, the text file will be
     * created with the violations.
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
     * This parameter is resolved as resource, URL, then file.  
     * If resolved to a resource, or a URL, the contents of the suppressions
     * XML is copied into the 
     * <code>${project.build.directory}/checkstyle-supressions.xml</code>
     * file before being passed to checkstyle for loading.
     * </p>
     *
     * @parameter
     * @since 2.0-beta-2
     */
    private String suppressionsLocation;

    /**
     * Specifies the location of the supperssions XML file to use. The plugin defines a Checkstyle
     * property named <code>checkstyle.suppressions.file</code> with the value of this
     * property. This allows using the Checkstyle property your own custom checkstyle
     * configuration file when specifying a suppressions file.
     *
     * @parameter
     * @deprecated Use suppressionsLocation instead.
     */
    private String suppressionsFile;

    /**
     * Specifies the path and filename to save the checkstyle output.  The format of the output file is
     * determined by the <code>outputFileFormat</code>
     *
     * @parameter default-value="${project.build.directory}/checkstyle-result.txt"
     */
    private String outputFile;

    /**
     * Specifies the format of the output to be used when writing to the output file. Valid values are
     * "plain" and "xml"
     *
     * @parameter default-value="plain"
     */
    private String outputFileFormat;

    /**
     * <p>
     * Specifies the location of the package names XML to be used to configure 
     * the Checkstyle <a href="http://checkstyle.sourceforge.net/config.html#Packages">Packages</a>.
     * </p>
     * 
     * <p>
     * This parameter is resolved as resource, URL, then file.  
     * If resolved to a resource, or a URL, the contents of the package names
     * XML is copied into the 
     * <code>${project.build.directory}/checkstyle-packagenames.xml</code>
     * file before being passed to checkstyle for loading.
     * </p>
     *
     * @parameter
     * @since 2.0-beta-2
     */
    private String packageNamesLocation;

    /**
     * Specifies the location of the package names XML to be used to configure Checkstyle
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
     * Specifies the location of the source files to be used for Checkstyle
     *
     * @parameter default-value="${project.build.sourceDirectory}"
     * @required
     */
    private File sourceDirectory;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    private StringOutputStream stringOutputStream;
    private Locator locator;

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
        mergeDeprecatedInfo();
        
        if ( !canGenerateReport() )
        {
            // TODO: failure if not a report
            throw new MavenReportException( "No source directory to process for checkstyle" );
        }
        
//        for when we start using maven-shared-io and maven-shared-monitor...
//        locator = new Locator( new MojoLogMonitorAdaptor( getLog() ) );
        
        locator = new Locator( getLog() );

        Map files = executeCheckstyle();

        CheckstyleReportGenerator generator = new CheckstyleReportGenerator( getSink(), getBundle( locale ) );

        generator.generateReport( files );
    }

    /**
     * Merge in the deprecated parameters to the new ones, unless the new parameters have values.
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
        
        if ( "LICENSE.txt".equals( headerLocation ) && !defaultHeaderFile.equals( headerFile ) )
        {
            headerLocation = headerFile.getPath();
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

    private Map executeCheckstyle()
        throws MavenReportException
    {
        File[] files = new File[0];
        try
        {
            files = getFilesToProcess( includes, excludes );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Error getting files to process", e );
        }

        String configFile = getConfigFile();
        
        Properties overridingProperties = getOverridingProperties();

        Checker checker;

        try
        {
            ModuleFactory moduleFactory = getModuleFactory();

            FilterSet filterSet = getSuppressions();

            Configuration config =
                ConfigurationLoader.loadConfiguration( configFile, new PropertiesExpander( overridingProperties ) );

            checker = new Checker();

            if ( moduleFactory != null )
            {
                checker.setModuleFactory( moduleFactory );
            }

            if ( filterSet != null )
            {
                checker.addFilter( filterSet );
            }

            checker.configure( config );
        }
        catch ( CheckstyleException ce )
        {
            throw new MavenReportException( "Failed during checkstyle configuration", ce );
        }

        AuditListener listener = getListener();

        if ( listener != null )
        {
            checker.addListener( listener );
        }

        checker.addListener( getConsoleListener() );

        CheckstyleReportListener sinkListener = new CheckstyleReportListener( sourceDirectory );

        checker.addListener( sinkListener );

        int nbErrors = checker.process( files );

        checker.destroy();

        if ( stringOutputStream != null )
        {
            getLog().info( stringOutputStream.toString() );
        }

        if ( failsOnError && nbErrors > 0 )
        {
            // TODO: should be a failure, not an error. Report is not meant to throw an exception here (so site would
            // work regardless of config), but should record this information
            throw new MavenReportException( "There are " + nbErrors + " formatting errors." );
        }

        return sinkListener.getFiles();
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
            File resultFile = new File( outputFile );

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
                throw new MavenReportException(
                    "Invalid output file format: (" + outputFileFormat + "). Must be 'plain' or 'xml'." );
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

        FileOutputStream fileOutputStream = null;
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
        throws MavenReportException, IOException
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

        return (File[]) files.toArray( EMPTY_FILE_ARRAY );
    }
    
    private String getLocationTemp(String name) {
        return project.getBuild().getDirectory() + File.separator + name;
    }

    private Properties getOverridingProperties()
        throws MavenReportException
    {
        Properties p = new Properties();

        try
        {
            File propertiesFile = locator.resolveLocation( propertiesLocation,
                                                           getLocationTemp( "checkstyle-checker.properties" ) );
            
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
                    File headerFile = locator.resolveLocation( headerLocation,
                                                               getLocationTemp( "checkstyle-header.txt" ) );
                    if ( headerFile != null )
                    {
                        p.setProperty( "checkstyle.header.file", headerFile.getAbsolutePath() );
                    }
                }
                catch ( IOException e )
                {
                    throw new MavenReportException( "Unable to process header location.", e );
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

        return p;
    }
    
    private String getConfigFile()
        throws MavenReportException
    {
        try
        {
            File configFile = locator.resolveLocation( configLocation, getLocationTemp( "checkstyle-checker.xml" ) );
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
        try
        {
            File packageNamesFile = locator.resolveLocation( packageNamesLocation,
                                                             getLocationTemp( "checkstyle-packages.xml" ) );

            if ( packageNamesFile == null )
            {
                return null;
            }

            return PackageNamesLoader.loadModuleFactory( packageNamesFile.getAbsolutePath() );
        }
        catch ( IOException e )
        {
            getLog().error( "Unable to process package names location: " + packageNamesLocation, e );
            return null;
        }
    }

    private FilterSet getSuppressions()
        throws MavenReportException
    {
        try
        {
            File suppressionsFile = locator.resolveLocation( suppressionsLocation,
                                                             getLocationTemp( "checkstyle-suppressions.xml" ) );

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

    public boolean canGenerateReport()
    {
        // TODO: would be good to scan the files here
        return super.canGenerateReport() && sourceDirectory.exists();
    }
}
