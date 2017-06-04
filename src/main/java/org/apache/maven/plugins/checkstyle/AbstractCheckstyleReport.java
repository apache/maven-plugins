package org.apache.maven.plugins.checkstyle;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutor;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutorException;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleExecutorRequest;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleResults;
import org.apache.maven.plugins.checkstyle.rss.CheckstyleRssGenerator;
import org.apache.maven.plugins.checkstyle.rss.CheckstyleRssGeneratorRequest;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringUtils;

import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/**
 * Base abstract class for Checkstyle reports.
 *
 * @version $Id$
 */
public abstract class AbstractCheckstyleReport
    extends AbstractMavenReport
{
    public static final String PLUGIN_RESOURCES = "org/apache/maven/plugins/checkstyle";

    protected static final String JAVA_FILES = "**\\/*.java";

    /**
     * Specifies the cache file used to speed up Checkstyle on successive runs.
     */
    @Parameter( defaultValue = "${project.build.directory}/checkstyle-cachefile" )
    protected String cacheFile;

    /**
     * <p>
     * Specifies the location of the XML configuration to use.
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * This parameter expects that the contents of the location conform to the
     * xml format (Checkstyle <a
     * href="http://checkstyle.sourceforge.net/config.html#Modules">Checker
     * module</a>) configuration of rulesets.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}/checkstyle-configuration.xml</code>
     * file before being passed to Checkstyle as a configuration.
     * <p>
     * There are 2 predefined rulesets included in Maven Checkstyle Plugin:
     * <ul>
     * <li><code>sun_checks.xml</code>: Sun Checks.</li>
     * <li><code>google_checks.xml</code>: Google Checks.</li>
     * </ul>
     */
    @Parameter( property = "checkstyle.config.location", defaultValue = "sun_checks.xml" )
    protected String configLocation;

    /**
     * Output errors to console.
     */
    @Parameter( property = "checkstyle.consoleOutput", defaultValue = "false" )
    protected boolean consoleOutput;

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used. <strong>Note:</strong> This parameter always overrides the
     * property <code>charset</code> from Checkstyle's <code>TreeWalker</code> module.
     *
     * @since 2.2
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    protected String encoding;

    /**
     * Specifies if the build should fail upon a violation.
     */
    @Parameter( defaultValue = "false" )
    protected boolean failsOnError;

    /**
     * <p>
     * Specifies the location of the License file (a.k.a. the header file) that
     * can be used by Checkstyle to verify that source code has the correct
     * license header.
     * <p>
     * You need to use <code>${checkstyle.header.file}</code> in your Checkstyle xml
     * configuration to reference the name of this header file.
     * <p>
     * For instance:
     * <pre>
     * &lt;module name="RegexpHeader"&gt;
     *   &lt;property name="headerFile" value="${checkstyle.header.file}"/&gt;
     * &lt;/module&gt;
     * </pre>
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.header.file", defaultValue = "LICENSE.txt" )
    protected String headerLocation;

    /**
     * Skip entire check.
     *
     * @since 2.2
     */
    @Parameter( property = "checkstyle.skip", defaultValue = "false" )
    protected boolean skip;

    /**
     * Specifies the path and filename to save the Checkstyle output. The format
     * of the output file is determined by the <code>outputFileFormat</code>
     * parameter.
     */
    @Parameter( property = "checkstyle.output.file", defaultValue = "${project.build.directory}/checkstyle-result.xml" )
    private File outputFile;

    /**
     * <p>
     * Specifies the location of the properties file.
     * <p>
     * This parameter is resolved as URL, File then resource. If successfully
     * resolved, the contents of the properties location is copied into the
     * <code>${project.build.directory}/checkstyle-checker.properties</code>
     * file before being passed to Checkstyle for loading.
     * <p>
     * The contents of the <code>propertiesLocation</code> will be made
     * available to Checkstyle for specifying values for parameters within the
     * xml configuration (specified in the <code>configLocation</code>
     * parameter).
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.properties.location" )
    protected String propertiesLocation;

    /**
     * Allows for specifying raw property expansion information.
     */
    @Parameter
    protected String propertyExpansion;

    /**
     * Specifies the location of the resources to be used for Checkstyle.
     *
     * @since 2.10
     */
    @Parameter( defaultValue = "${project.resources}", readonly = true )
    protected List<Resource> resources;

    /**
     * Specifies the location of the test resources to be used for Checkstyle.
     *
     * @since 2.11
     */
    @Parameter( defaultValue = "${project.testResources}", readonly = true )
    protected List<Resource> testResources;

    /**
     * Specifies the names filter of the source files to be used for Checkstyle.
     */
    @Parameter( property = "checkstyle.includes", defaultValue = JAVA_FILES, required = true )
    protected String includes;

    /**
     * Specifies the names filter of the source files to be excluded for
     * Checkstyle.
     */
    @Parameter( property = "checkstyle.excludes" )
    protected String excludes;

    /**
     * Specifies the names filter of the resource files to be used for Checkstyle.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.resourceIncludes", defaultValue = "**/*.properties", required = true )
    protected String resourceIncludes;

    /**
     * Specifies the names filter of the resource files to be excluded for
     * Checkstyle.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.resourceExcludes" )
    protected String resourceExcludes;

    /**
     * Specifies whether to include the resource directories in the check.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.includeResources", defaultValue = "true", required = true )
    protected boolean includeResources;

    /**
     * Specifies whether to include the test resource directories in the check.
     * @since 2.11
     */
    @Parameter( property = "checkstyle.includeTestResources", defaultValue = "true", required = true )
    protected boolean includeTestResources;

    /**
     * Specifies the location of the source directory to be used for Checkstyle.
     * 
     * @deprecated instead use {@link #sourceDirectories}. For version 3.0.0, this parameter is only defined to break
     *             the build if you use it!
     */
    @Deprecated
    @Parameter
    private File sourceDirectory;

    /**
     * Specifies the location of the source directories to be used for Checkstyle.
     * Default value is <code>${project.compileSourceRoots}</code>.
     * @since 2.13
     */
    // Compatibility with all Maven 3: default of 'project.compileSourceRoots' is done manually because of MNG-5440
    @Parameter
    private List<String> sourceDirectories;
    
    /**
     * Specifies the location of the test source directory to be used for Checkstyle.
     *
     * @since 2.2
     * @deprecated instead use {@link #testSourceDirectories}. For version 3.0.0, this parameter is only defined to
     *             break the build if you use it!
     */
    @Parameter
    @Deprecated
    private File testSourceDirectory;
    
    /**
     * Specifies the location of the test source directories to be used for Checkstyle.
     * Default value is <code>${project.testCompileSourceRoots}</code>.
     * @since 2.13
     */
    // Compatibility with all Maven 3: default of 'project.testCompileSourceRoots' is done manually because of MNG-5440
    @Parameter
    private List<String> testSourceDirectories;

    /**
     * Include or not the test source directory/directories to be used for Checkstyle.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "false" )
    protected boolean includeTestSourceDirectory;

    /**
     * The key to be used in the properties for the suppressions file.
     *
     * @since 2.1
     */
    @Parameter( property = "checkstyle.suppression.expression", defaultValue = "checkstyle.suppressions.file" )
    protected String suppressionsFileExpression;

    /**
     * <p>
     * Specifies the location of the suppressions XML file to use.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the suppressions XML is copied into the
     * <code>${project.build.directory}/checkstyle-supressions.xml</code> file
     * before being passed to Checkstyle for loading.
     * <p>
     * See <code>suppressionsFileExpression</code> for the property that will
     * be made available to your Checkstyle configuration.
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.suppressions.location" )
    protected String suppressionsLocation;

    /**
     * If <code>null</code>, the Checkstyle plugin will display violations on stdout.
     * Otherwise, a text file will be created with the violations.
     */
    @Parameter
    private File useFile;

    /**
     * Specifies the format of the output to be used when writing to the output
     * file. Valid values are "<code>plain</code>" and "<code>xml</code>".
     */
    @Parameter( property = "checkstyle.output.format", defaultValue = "xml" )
    private String outputFileFormat;

    /**
     * Specifies if the Rules summary should be enabled or not.
     */
    @Parameter( property = "checkstyle.enable.rules.summary", defaultValue = "true" )
    private boolean enableRulesSummary;

    /**
     * Specifies if the Severity summary should be enabled or not.
     */
    @Parameter( property = "checkstyle.enable.severity.summary", defaultValue = "true" )
    private boolean enableSeveritySummary;

    /**
     * Specifies if the Files summary should be enabled or not.
     */
    @Parameter( property = "checkstyle.enable.files.summary", defaultValue = "true" )
    private boolean enableFilesSummary;

    /**
     * Specifies if the RSS should be enabled or not.
     */
    @Parameter( property = "checkstyle.enable.rss", defaultValue = "true" )
    private boolean enableRSS;

    /**
     * SiteTool.
     *
     * @since 2.2
     */
    @Component( role = SiteTool.class )
    protected SiteTool siteTool;

    /**
     * The Plugin Descriptor
     */
    @Parameter( defaultValue = "${plugin}", readonly = true, required = true )
    private PluginDescriptor plugin;

    /**
     * Link the violation line numbers to the source xref. Will link
     * automatically if Maven JXR plugin is being used.
     *
     * @since 2.1
     */
    @Parameter( property = "linkXRef", defaultValue = "true" )
    private boolean linkXRef;

    /**
     * Location of the Xrefs to link to.
     */
    @Parameter( defaultValue = "${project.reporting.outputDirectory}/xref" )
    private File xrefLocation;

    /**
     * When using custom treeWalkers, specify their names here so the checks
     * inside the treeWalker end up the the rule-summary.
     * 
     * @since 2.11
     */
    @Parameter
    private List<String> treeWalkerNames;

    /**
     * Specifies whether modules with a configured severity of <code>ignore</code> should be omitted during Checkstyle
     * invocation.
     * 
     * @since 3.0.0
     */
    @Parameter( defaultValue = "false" )
    private boolean omitIgnoredModules;

    /**
     */
    @Component
    protected ResourceManager locator;

    /**
     * CheckstyleRssGenerator.
     *
     * @since 2.4
     */
    @Component( role = CheckstyleRssGenerator.class, hint = "default" )
    protected CheckstyleRssGenerator checkstyleRssGenerator;

    /**
     * @since 2.5
     */
    @Component( role = CheckstyleExecutor.class, hint = "default" )
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
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        checkDeprecatedParameterUsage( sourceDirectory, "sourceDirectory", "sourceDirectories" );
        checkDeprecatedParameterUsage( testSourceDirectory, "testSourceDirectory", "testSourceDirectories" );

        locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );
        locator.addSearchPath( "url", "" );

        locator.setOutputDirectory( new File( project.getBuild().getDirectory() ) );

        // for when we start using maven-shared-io and maven-shared-monitor...
        // locator = new Locator( new MojoLogMonitorAdaptor( getLog() ) );

        // locator = new Locator( getLog(), new File( project.getBuild().getDirectory() ) );

        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            CheckstyleExecutorRequest request = createRequest().setLicenseArtifacts( collectArtifacts( "license" ) )
                            .setConfigurationArtifacts( collectArtifacts( "configuration" ) )
                            .setOmitIgnoredModules( omitIgnoredModules );

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

    private void checkDeprecatedParameterUsage( Object parameter, String name, String replacement )
        throws MavenReportException
    {
        if ( parameter != null )
        {
            throw new MavenReportException( "You are using '" + name + "' which has been removed"
                + " from the maven-checkstyle-plugin. " + "Please use '" + replacement
                + "' and refer to the >>Major Version Upgrade to version 3.0.0<< " + "on the plugin site." );
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

    private List<Artifact> collectArtifacts( String hint )
    {
        List<Artifact> artifacts = new ArrayList<>();

        PluginManagement pluginManagement = project.getBuild().getPluginManagement();
        if ( pluginManagement != null )
        {
            artifacts.addAll( getCheckstylePluginDependenciesAsArtifacts( pluginManagement.getPluginsAsMap(), hint ) );
        }

        artifacts.addAll( getCheckstylePluginDependenciesAsArtifacts( project.getBuild().getPluginsAsMap(), hint ) );

        return artifacts;
    }

    private List<Artifact> getCheckstylePluginDependenciesAsArtifacts( Map<String, Plugin> plugins, String hint )
    {
        List<Artifact> artifacts = new ArrayList<>();
        
        Plugin checkstylePlugin = plugins.get( plugin.getGroupId() + ":" + plugin.getArtifactId() );
        if ( checkstylePlugin != null )
        {
            for ( Dependency dep : checkstylePlugin.getDependencies() )
            {
             // @todo if we can filter on hints, it should be done here...
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                artifacts.add( (Artifact) plugin.getArtifactMap().get( depKey ) );
            }
        }
        return artifacts;
    }

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
            new CheckstyleReportGenerator( getSink(), bundle, project.getBasedir(), siteTool, configLocation );

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
                for ( ReportPlugin report : (Iterable<ReportPlugin>) getProject().getReportPlugins() )
                {
                    String artifactId = report.getArtifactId();
                    if ( "maven-jxr-plugin".equals( artifactId ) || "jxr-maven-plugin".equals( artifactId ) )
                    {
                        generator.setXrefLocation( relativePath );
                    }
                }
            }

            if ( generator.getXrefLocation() == null && results.getFileCount() > 0 )
            {
                getLog().warn( "Unable to locate Source XRef to link to - DISABLED" );
            }
        }
        if ( treeWalkerNames != null )
        {
            generator.setTreeWalkerNames( treeWalkerNames );
        }
        generator.generateReport( results );
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "checkstyle-report", locale, AbstractCheckstyleReport.class.getClassLoader() );
    }

    protected List<File> getSourceDirectories()
    {
        if ( sourceDirectories == null )
        {
            sourceDirectories = project.getCompileSourceRoots();
        }
        List<File> sourceDirs = new ArrayList<>( sourceDirectories.size() );
        for ( String sourceDir : sourceDirectories )
        {
            sourceDirs.add( FileUtils.resolveFile( project.getBasedir(), sourceDir ) );
        }
        return sourceDirs;
    }

    protected List<File> getTestSourceDirectories()
    {
        if ( testSourceDirectories == null )
        {
            testSourceDirectories = project.getTestCompileSourceRoots();
        }
        List<File> testSourceDirs = new ArrayList<>( testSourceDirectories.size() );
        for ( String testSourceDir : testSourceDirectories )
        {
            testSourceDirs.add( FileUtils.resolveFile( project.getBasedir(), testSourceDir ) );
        }
        return testSourceDirs;
    }
}
