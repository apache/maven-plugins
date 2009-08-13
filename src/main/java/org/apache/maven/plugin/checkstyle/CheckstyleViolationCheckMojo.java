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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * Perform a violation check against the last Checkstyle run to see if there are
 * any violations. It reads the Checkstyle output file, counts the number of
 * violations found and displays it on the console.
 *
 * @author <a href="mailto:joakim@erdfelt.net">Joakim Erdfelt</a>
 * @version $Id$
 * @goal check
 * @phase verify
 * @execute goal="checkstyle"
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
     * Output the detected violations to the console.
     *
     * @parameter expression="${checkstyle.console}" default-value="false"
     * @since 2.3
     */
    private boolean logViolationsToConsole;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !skip )
        {
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
}