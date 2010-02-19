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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/**
 * Perform a violation check against the last Checkstyle run to see if there are
 * any violations. It reads the Checkstyle output file, counts the number of
 * violations found and displays it on the console.
 *
 * @author <a href="mailto:joakim@erdfelt.net">Joakim Erdfelt</a>
 * @version $Id$
 * @goal check
 * @phase verify
 * @requiresDependencyResolution compile
 */
public class CheckstyleViolationCheckMojo
    extends AbstractMojo
{
    /**
     * Specifies the path and filename to save the Checkstyle output. The format
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
     * Do we fail the build on a violation?
     *
     * @parameter expression="${checkstyle.failOnViolation}"
     *            default-value="true"
     */
    private boolean failOnViolation;

    /**
     * The maximum number of allowed violations. The execution fails only if the
     * number of violations is above this limit.
     *
     * @parameter expression="${checkstyle.maxAllowedViolations}" default-value="0"
     * @since 2.3
     */
    private int maxAllowedViolations = 0;

    /**
     * The lowest severity level that is considered a violation.
     * Valid values are "error", "warning" and "info".
     *
     * @parameter expression="${checkstyle.violationSeverity}" default-value="error"
     * @since 2.2
     */
    private String violationSeverity = "error";

    /**
     * Skip entire check.
     *
     * @parameter expression="${checkstyle.skip}" default-value="false"
     * @since 2.2
     */
    private boolean skip;
    
    /**
     * Skip checktyle execution will only scan the outputFile.
     *
     * @parameter expression="${checkstyle.skipExec}" default-value="false"
     * @since 2.5
     */
    private boolean skipExec;    

    /**
     * Output the detected violations to the console.
     *
     * @parameter expression="${checkstyle.console}" default-value="false"
     * @since 2.3
     */
    private boolean logViolationsToConsole;
    
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
     * @since 2.5
     * @parameter expression="${checkstyle.config.location}"
     *            default-value="config/sun_checks.xml"
     */
    private String configLocation;    

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
     * @since 2.5
     */
    private String propertiesLocation;
    
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
     * @parameter expression="${checkstyle.header.file}"
     *            default-value="LICENSE.txt"
     * @since 2.0-beta-2
     */
    private String headerLocation;    
    
    /**
     * Specifies the cache file used to speed up Checkstyle on successive runs.
     *
     * @parameter default-value="${project.build.directory}/checkstyle-cachefile"
     */
    private String cacheFile;    
    
    /**
     * The key to be used in the properties for the suppressions file.
     *
     * @parameter expression="${checkstyle.suppression.expression}"
     *            default-value="checkstyle.suppressions.file"
     * @since 2.1
     */
    private String suppressionsFileExpression;    
 
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
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used. <strong>Note:</strong> This parameter always overrides the
     * property <code>charset</code> from Checkstyle's <code>TreeWalker</code> module.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     * @since 2.2
     */
    private String encoding;    
    
    /**
     * @since 2.5
     * @component role="org.apache.maven.plugin.checkstyle.CheckstyleExecutor" role-hint="default"
     * @required
     * @readonly
     */
    protected CheckstyleExecutor checkstyleExecutor;    
    
    /**
     * Output errors to console.
     *
     * @parameter default-value="false"
     */
    private boolean consoleOutput;    
    
    /**
     * The Maven Project Object.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * If <code>null</code>, the Checkstyle plugin will display violations on stdout.
     * Otherwise, a text file will be created with the violations.
     *
     * @parameter
     */
    private File useFile;

    /**
     * Specifies the names filter of the source files to be excluded for
     * Checkstyle.
     *
     * @parameter expression="${checkstyle.excludes}"
     */
    private String excludes;    
    
    /**
     * Specifies the names filter of the source files to be used for Checkstyle.
     *
     * @parameter expression="${checkstyle.includes}" default-value="**\/*.java"
     * @required
     */
    private String includes;
    
    /**
     * Specifies if the build should fail upon a violation.
     *
     * @parameter default-value="false"
     */
    private boolean failsOnError;    
    
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
     * Specifies the location of the source directory to be used for Checkstyle.
     *
     * @parameter default-value="${project.build.sourceDirectory}"
     * @required
     */
    private File sourceDirectory;    
    
    private ByteArrayOutputStream stringOutputStream;
    
    
    /** {@inheritDoc} */
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
                    request.setConsoleListener( getConsoleListener() ).setConsoleOutput( consoleOutput )
                        .setExcludes( excludes ).setFailsOnError( failsOnError ).setIncludes( includes )
                        .setIncludeTestSourceDirectory( includeTestSourceDirectory ).setListener( getListener() )
                        .setLog( getLog() ).setProject( project ).setSourceDirectory( sourceDirectory )
                        .setStringOutputStream( stringOutputStream ).setSuppressionsLocation( suppressionsLocation )
                        .setTestSourceDirectory( testSourceDirectory ).setConfigLocation( configLocation )
                        .setPropertyExpansion( propertyExpansion ).setHeaderLocation( headerLocation )
                        .setCacheFile( cacheFile ).setSuppressionsFileExpression( suppressionsFileExpression )
                        .setEncoding( encoding ).setPropertiesLocation( propertiesLocation );

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
                throw new MojoExecutionException( "Output format is '" + outputFileFormat
                    + "', checkstyle:check requires format to be 'xml'." );
            }

            if ( !outputFile.exists() )
            {
                getLog().info(
                               "Unable to perform checkstyle:check, "
                                   + "unable to find checkstyle:checkstyle outputFile." );
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
                        String msg = "You have " + violations + " Checkstyle violation"
                            + ( ( violations > 1 ) ? "s" : "" ) + ".";
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
                throw new MojoExecutionException( "Unable to read Checkstyle results xml: "
                    + outputFile.getAbsolutePath(), e );
            }
            catch ( XmlPullParserException e )
            {
                throw new MojoExecutionException( "Unable to read Checkstyle results xml: "
                    + outputFile.getAbsolutePath(), e );
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

            if ( eventType == XmlPullParser.START_TAG && "error".equals( xpp.getName() )
                && isViolation( xpp.getAttributeValue( "", "severity" ) ) )
            {
                if ( logViolationsToConsole )
                {
                    StringBuffer stb = new StringBuffer();
                    stb.append( file );
                    stb.append( '[' );
                    stb.append( xpp.getAttributeValue( "", "line" ) );
                    stb.append( ':' );
                    stb.append( xpp.getAttributeValue( "", "column" ) );
                    stb.append( "] " );
                    stb.append( xpp.getAttributeValue( "", "message" ) );
                    getLog().error( stb.toString() );
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
            return "error".equals( violationSeverity ) || "warning".equals( violationSeverity )
                || "info".equals( violationSeverity );
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
                throw new MojoFailureException( "Invalid output file format: (" + outputFileFormat
                    + "). Must be 'plain' or 'xml'." );
            }
        }

        return listener;
    }
    
}