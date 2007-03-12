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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Perform a violation check against the last Checkstyle run to see if there are
 * any violations. It reads the Checkstyle output file, counts the number of
 * violations found and displays it on the console.
 * 
 * @author <a href="mailto:joakim@erdfelt.net">Joakim Erdfelt</a>
 * @goal check
 * @phase verify
 * @execute goal="checkstyle"
 */
public class CheckstyleViolationCheckMojo
    extends AbstractMojo
{
    /**
     * Specifies the path and filename to save the Checkstyle output. The format
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
     * do we fail the build on a violation?
     * 
     * @parameter expression="${checkstyle.failOnViolation}"
     *            default-value="true"
     */
    private boolean failOnViolation;

    /**
     * skip entire check
     * 
     * @parameter expression="${checkstyle.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
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
                FileReader freader = new FileReader( outputFile );
                BufferedReader breader = new BufferedReader( freader );
                xpp.setInput( breader );

                int violations = countViolations( xpp );
                if ( violations > 0 )
                {
                    if ( failOnViolation )
                    {
                        throw new MojoFailureException( "You have " + violations + " Checkstyle violation"
                            + ( ( violations > 1 ) ? "s" : "" ) + "." );
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
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG && "error".equals( xpp.getName() )
                && "error".equals( xpp.getAttributeValue( "", "severity" ) ) )
            {
                count++;
            }
            eventType = xpp.next();
        }

        return count;
    }
}
