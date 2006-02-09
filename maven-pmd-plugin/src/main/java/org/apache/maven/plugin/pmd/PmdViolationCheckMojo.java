package org.apache.maven.plugin.pmd;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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
 * Perform a violation check against the last pmd run to see if there are any violations.
 *
 * @goal check
 * @phase verify
 * @execute goal="pmd"
 */
public class PmdViolationCheckMojo
    extends AbstractMojo
{
    /**
     * Specifies the path and filename to save the PMD output.  The format of the output file is
     * determined by the <code>reportFormat</code>
     *
     * @parameter expression="${outputFile}" default-value="${project.build.directory}/site/pmd.xml"
     * @required
     */
    private File outputFile;

    /**
     * Specifies the format of the output to be used when writing to the output file. Valid values are
     * "plain" and "xml"
     *
     * @parameter expression="${format}" default-value="xml"
     */
    private String format;

    /**
     * Fail on violation?
     *
     * @parameter expression="${failOnViolation}" default-value="true"
     * @required
     */
    private boolean failOnViolation;

    /**
     * Number of violations
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !"xml".equals( format ) )
        {
            throw new MojoExecutionException(
                "Output format is '" + format + "', pmd:check requires format to be 'xml'." );
        }

        if ( outputFile.exists() )
        {
            try
            {
                XmlPullParser xpp = new MXParser();
                FileReader freader = new FileReader( outputFile );
                BufferedReader breader = new BufferedReader( freader );
                xpp.setInput( breader );

                int violations = countViolations( xpp );
                if ( violations > 0 && failOnViolation )
                {
                    throw new MojoFailureException(
                        "You have " + violations + " PMD violation" + ( violations > 1 ? "s" : "" ) + "." );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to read PMD results xml: " + outputFile.getAbsolutePath(),
                                                  e );
            }
            catch ( XmlPullParserException e )
            {
                throw new MojoExecutionException( "Unable to read PMD results xml: " + outputFile.getAbsolutePath(),
                                                  e );
            }
        }
        else
        {
            getLog().info( "Unable to perform pmd:check, " + "unable to find pmd:execute outputFile." );
        }
    }

    private int countViolations( XmlPullParser xpp )
        throws XmlPullParserException, IOException
    {
        int count = 0;

        int eventType = xpp.getEventType();
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG && "violation".equals( xpp.getName() ) )
            {
                count++;
            }
            eventType = xpp.next();
        }

        return count;
    }
}
