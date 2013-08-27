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

import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;

/**
 * Perform a violation check against the last Checkstyle run to see if there are
 * any violations. It reads the Checkstyle output file, counts the number of
 * violations found and displays it on the console.
 *
 * @author <a href="mailto:joakim@erdfelt.net">Joakim Erdfelt</a>
 * @version $Id$
 */
@Mojo( name = "check", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST,
       threadSafe = true )
public class CheckstyleViolationCheckMojo
    extends AbstractMojo
{

    private static final String JAVA_FILES = "**\\/*.java";

    /**
     * Specifies the path and filename to save the Checkstyle output. The format
     * of the output file is determined by the <code>outputFileFormat</code>
     * parameter.
     */
    @Parameter( property = "checkstyle.output.file", defaultValue = "${project.build.directory}/checkstyle-result.xml" )
    private File outputFile;

    /**
     * Specifies the format of the output to be used when writing to the output
     * file. Valid values are "plain" and "xml".
     */
    @Parameter( property = "checkstyle.output.format", defaultValue = "xml" )
    private String outputFileFormat;

    /**
     * Do we fail the build on a violation?
     */
    @Parameter( property = "checkstyle.failOnViolation", defaultValue = "true" )
    private boolean failOnViolation;

    /**
     * The maximum number of allowed violations. The execution fails only if the
     * number of violations is above this limit.
     *
     * @since 2.3
     */
    @Parameter( property = "checkstyle.maxAllowedViolations", defaultValue = "0" )
    private int maxAllowedViolations = 0;

    /**
     * The lowest severity level that is considered a violation.
     * Valid values are "error", "warning" and "info".
     *
     * @since 2.2
     */
    @Parameter( property = "checkstyle.violationSeverity", defaultValue = "error" )
    private String violationSeverity = "error";

    /**
     * Skip entire check.
     *
     * @since 2.2
     */
    @Parameter( property = "checkstyle.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Skip checktyle execution will only scan the outputFile.
     *
     * @since 2.5
     */
    @Parameter( property = "checkstyle.skipExec", defaultValue = "false" )
    private boolean skipExec;

    /**
     * Output the detected violations to the console.
     *
     * @since 2.3
     */
    @Parameter( property = "checkstyle.console", defaultValue = "false" )
    private boolean logViolationsToConsole;

    /**
     * Specifies the location of the resources to be used for Checkstyle.
     *
     * @since 2.11
     */
    @Parameter( defaultValue = "${project.resources}", readonly = true )
    protected List<Resource> resources;

    /**
     * <p>
     * Specifies the location of the XML configuration to use.
     * </p>
     * <p/>
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * This parameter expects that the contents of the location conform to the
     * xml format (Checkstyle <a
     * href="http://checkstyle.sourceforge.net/config.html#Modules">Checker
     * module</a>) configuration of rulesets.
     * </p>
     * <p/>
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}/checkstyle-configuration.xml</code>
     * file before being passed to Checkstyle as a configuration.
     * </p>
     * <p/>
     * <p>
     * There are 4 predefined rulesets.
     * </p>
     * <p/>
     * <ul>
     * <li><code>config/sun_checks.xml</code>: Sun Checks.</li>
     * <li><code>config/turbine_checks.xml</code>: Turbine Checks.</li>
     * <li><code>config/avalon_checks.xml</code>: Avalon Checks.</li>
     * <li><code>config/maven_checks.xml</code>: Maven Source Checks.</li>
     * </ul>
     *
     * @since 2.5
     */
    @Parameter( property = "checkstyle.config.location", defaultValue = "config/sun_checks.xml" )
    private String configLocation;

    /**
     * <p>
     * Specifies the location of the properties file.
     * </p>
     * <p/>
     * <p>
     * This parameter is resolved as URL, File then resource. If successfully
     * resolved, the contents of the properties location is copied into the
     * <code>${project.build.directory}/checkstyle-checker.properties</code>
     * file before being passed to Checkstyle for loading.
     * </p>
     * <p/>
     * <p>
     * The contents of the <code>propertiesLocation</code> will be made
     * available to Checkstyle for specifying values for parameters within the
     * xml configuration (specified in the <code>configLocation</code>
     * parameter).
     * </p>
     *
     * @since 2.5
     */
    @Parameter( property = "checkstyle.properties.location" )
    private String propertiesLocation;

    /**
     * Allows for specifying raw property expansion information.
     */
    @Parameter
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
     * &lt;property name="headerFile" value="${checkstyle.header.file}"/>
     * &lt;/module>
     * </code>
     * </p>
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.header.file", defaultValue = "LICENSE.txt" )
    private String headerLocation;

    /**
     * Specifies the cache file used to speed up Checkstyle on successive runs.
     */
    @Parameter( defaultValue = "${project.build.directory}/checkstyle-cachefile" )
    private String cacheFile;

    /**
     * The key to be used in the properties for the suppressions file.
     *
     * @since 2.1
     */
    @Parameter( property = "checkstyle.suppression.expression", defaultValue = "checkstyle.suppressions.file" )
    private String suppressionsFileExpression;

    /**
     * <p>
     * Specifies the location of the suppressions XML file to use.
     * </p>
     * <p/>
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the suppressions XML is copied into the
     * <code>${project.build.directory}/checkstyle-supressions.xml</code> file
     * before being passed to Checkstyle for loading.
     * </p>
     * <p/>
     * <p>
     * See <code>suppressionsFileExpression</code> for the property that will
     * be made available to your checkstyle configuration.
     * </p>
     *
     * @since 2.0-beta-2
     */
    @Parameter( property = "checkstyle.suppressions.location" )
    private String suppressionsLocation;

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used. <strong>Note:</strong> This parameter always overrides the
     * property <code>charset</code> from Checkstyle's <code>TreeWalker</code> module.
     *
     * @since 2.2
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

    /**
     * @since 2.5
     */
    @Component( role = CheckstyleExecutor.class, hint = "default" )
    protected CheckstyleExecutor checkstyleExecutor;

    /**
     * Output errors to console.
     */
    @Parameter( defaultValue = "false" )
    private boolean consoleOutput;

    /**
     * The Maven Project Object.
     */
    @Component
    protected MavenProject project;

    /**
     * If <code>null</code>, the Checkstyle plugin will display violations on stdout.
     * Otherwise, a text file will be created with the violations.
     */
    @Parameter
    private File useFile;

    /**
     * Specifies the names filter of the source files to be excluded for
     * Checkstyle.
     */
    @Parameter( property = "checkstyle.excludes" )
    private String excludes;

    /**
     * Specifies the names filter of the source files to be used for Checkstyle.
     * <p/>
     * <strong>Note:</strong> default value is {@code **\/*.java}.
     */
    @Parameter( property = "checkstyle.includes", defaultValue = JAVA_FILES, required = true )
    private String includes;

    /**
     * Specifies if the build should fail upon a violation.
     */
    @Parameter( defaultValue = "false" )
    private boolean failsOnError;

    /**
     * Specifies the location of the test source directory to be used for
     * Checkstyle.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "${project.build.testSourceDirectory}" )
    private File testSourceDirectory;

    /**
     * Include or not the test source directory to be used for Checkstyle.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "false" )
    private boolean includeTestSourceDirectory;

    /**
     * Specifies the location of the source directory to be used for Checkstyle.
     */
    @Parameter( defaultValue = "${project.build.sourceDirectory}", required = true )
    private File sourceDirectory;

    private ByteArrayOutputStream stringOutputStream;


    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( !skip )
        {

            if ( !skipExec )
            {

                ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

                try
                {
                    CheckstyleExecutorRequest request = new CheckstyleExecutorRequest();
                    request.setConsoleListener( getConsoleListener() ).setConsoleOutput( consoleOutput ).setExcludes(
                        excludes ).setFailsOnError( failsOnError ).setIncludes(
                        includes ).setIncludeTestSourceDirectory( includeTestSourceDirectory ).setListener(
                        getListener() ).setLog( getLog() ).setProject( project ).setSourceDirectory(
                        sourceDirectory ).setResources( resources ).setStringOutputStream(
                        stringOutputStream ).setSuppressionsLocation( suppressionsLocation ).setTestSourceDirectory(
                        testSourceDirectory ).setConfigLocation( configLocation ).setPropertyExpansion(
                        propertyExpansion ).setHeaderLocation( headerLocation ).setCacheFile(
                        cacheFile ).setSuppressionsFileExpression( suppressionsFileExpression ).setEncoding(
                        encoding ).setPropertiesLocation( propertiesLocation );

                    checkstyleExecutor.executeCheckstyle( request );

                }
                catch ( CheckstyleException e )
                {
                    throw new MojoExecutionException( "Failed during checkstyle configuration", e );
                }
                catch ( CheckstyleExecutorException e )
                {
                    throw new MojoExecutionException( "Failed during checkstyle execution", e );
                }
                finally
                {
                    //be sure to restore original context classloader
                    Thread.currentThread().setContextClassLoader( currentClassLoader );
                }

            }
            if ( !"xml".equals( outputFileFormat ) )
            {
                throw new MojoExecutionException(
                    "Output format is '" + outputFileFormat + "', checkstyle:check requires format to be 'xml'." );
            }

            if ( !outputFile.exists() )
            {
                getLog().info(
                    "Unable to perform checkstyle:check, " + "unable to find checkstyle:checkstyle outputFile." );
                return;
            }

            try
            {
                XmlPullParser xpp = new MXParser();
                Reader freader = ReaderFactory.newXmlReader( outputFile );
                BufferedReader breader = new BufferedReader( freader );
                xpp.setInput( breader );

                int violations = countViolations( xpp );
                if ( violations > maxAllowedViolations )
                {
                    if ( failOnViolation )
                    {
                        String msg =
                            "You have " + violations + " Checkstyle violation" + ( ( violations > 1 ) ? "s" : "" )
                                + ".";
                        if ( maxAllowedViolations > 0 )
                        {
                            msg += " The maximum number of allowed violations is " + maxAllowedViolations + ".";
                        }
                        throw new MojoFailureException( msg );
                    }

                    getLog().warn( "checkstyle:check violations detected but failOnViolation set to false" );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                    "Unable to read Checkstyle results xml: " + outputFile.getAbsolutePath(), e );
            }
            catch ( XmlPullParserException e )
            {
                throw new MojoExecutionException(
                    "Unable to read Checkstyle results xml: " + outputFile.getAbsolutePath(), e );
            }
        }
    }

    private int countViolations( XmlPullParser xpp )
        throws XmlPullParserException, IOException
    {
        int count = 0;

        int eventType = xpp.getEventType();
        String file = "";
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG && "file".equals( xpp.getName() ) )
            {
                file = xpp.getAttributeValue( "", "name" );
                file = file.substring( file.lastIndexOf( File.separatorChar ) + 1 );
            }

            if ( eventType == XmlPullParser.START_TAG && "error".equals( xpp.getName() ) && isViolation(
                xpp.getAttributeValue( "", "severity" ) ) )
            {
                if ( logViolationsToConsole )
                {
                    getLog().error(
                        file + '[' + xpp.getAttributeValue( "", "line" ) + ':' + xpp.getAttributeValue( "", "column" )
                            + "] " + xpp.getAttributeValue( "", "message" ) );
                }
                count++;
            }
            eventType = xpp.next();
        }

        return count;
    }

    /**
     * Checks if the given severity is considered a violation.
     *
     * @param severity The severity to check
     * @return <code>true</code> if the given severity is a violation, otherwise <code>false</code>
     */
    private boolean isViolation( String severity )
    {
        if ( "error".equals( severity ) )
        {
            return "error".equals( violationSeverity ) || "warning".equals( violationSeverity ) || "info".equals(
                violationSeverity );
        }
        else if ( "warning".equals( severity ) )
        {
            return "warning".equals( violationSeverity ) || "info".equals( violationSeverity );
        }
        else if ( "info".equals( severity ) )
        {
            return "info".equals( violationSeverity );
        }
        else
        {
            return false;
        }
    }

    private DefaultLogger getConsoleListener()
        throws MojoExecutionException
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

    private OutputStream getOutputStream( File file )
        throws MojoExecutionException
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
            throw new MojoExecutionException( "Unable to create output stream: " + file, e );
        }
        return fileOutputStream;
    }

    private AuditListener getListener()
        throws MojoFailureException, MojoExecutionException
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
                throw new MojoFailureException(
                    "Invalid output file format: (" + outputFileFormat + "). Must be 'plain' or 'xml'." );
            }
        }

        return listener;
    }

}