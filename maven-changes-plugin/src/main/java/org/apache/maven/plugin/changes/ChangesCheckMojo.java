package org.apache.maven.plugin.changes;

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

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.changes.model.Release;

/**
 * Goal which checks that the changes.xml file has the necessary data to
 * generate an announcement or a report for the current release.
 *
 * @author Justin Edelson
 * @author Dennis Lundberg
 * @since 2.4
 */
@Mojo( name = "changes-check", threadSafe = true )
public class ChangesCheckMojo
    extends AbstractChangesMojo
{
    /**
     * The format that a correct release date should have. This value will be
     * used as a pattern to try to create a date.
     */
    @Parameter( property = "changes.releaseDateFormat", defaultValue = "yyyy-MM-dd" )
    private String releaseDateFormat;

    /**
     * Version of the artifact.
     */
    @Parameter( property = "changes.version", defaultValue = "${project.version}", required = true )
    private String version;

    /**
     * The path of the <code>changes.xml</code> file that will be checked.
     */
    @Parameter( property = "changes.xmlPath", defaultValue = "src/changes/changes.xml" )
    private File xmlPath;

    /**
     * Flag controlling snapshot processing. If set, versions ending with <code>-SNAPSHOT</code> won't be checked.
     *
     * @since 2.7
     */
    @Parameter( property = "changes.skipSnapshots", defaultValue = "false" )
    private boolean skipSnapshots;

    private ReleaseUtils releaseUtils = new ReleaseUtils( getLog() );

    /**
     * Check that the latest release contains a valid release date.
     *
     * @throws MojoExecutionException
     */
    public void execute()
        throws MojoExecutionException
    {
        // Run only at the execution root
        if ( runOnlyAtExecutionRoot && !isThisTheExecutionRoot() )
        {
            getLog().info( "Skipping the changes check in this project because it's not the Execution Root" );
        }
        else
        {
            if ( this.version.endsWith( "-SNAPSHOT" ) && this.skipSnapshots )
            {
                getLog().info( "Skipping snapshot version '" + this.version + "'." );
            }
            else if ( xmlPath.exists() )
            {
                ChangesXML xml = new ChangesXML( xmlPath, getLog() );
                ReleaseUtils releaseUtils = new ReleaseUtils( getLog() );
                Release release =
                    releaseUtils.getLatestRelease( releaseUtils.convertReleaseList( xml.getReleaseList() ), version );

                if ( !isValidDate( release.getDateRelease(), releaseDateFormat ) )
                {
                    throw new MojoExecutionException(
                        "The file " + xmlPath.getAbsolutePath() + " has an invalid release date." );

                }
            }
            else
            {
                getLog().warn( "The file " + xmlPath.getAbsolutePath() + " does not exist." );
            }
        }
    }

    /**
     * Use the pattern to try to parse a Date from the given string.
     *
     * @param string A date as text
     * @param pattern A pattern that can be used by {@link SimpleDateFormat}
     * @return <code>true</code> if the string can be parsed as a date using the pattern, otherwise <code>false</code>
     */
    protected static boolean isValidDate( String string, String pattern )
    {
        if ( StringUtils.isEmpty( string ) )
        {
            return false;
        }

        if ( StringUtils.isEmpty( pattern ) )
        {
            return false;
        }

        try
        {
            SimpleDateFormat df = new SimpleDateFormat( pattern );
            df.parse( string );
            return true;
        }
        catch ( ParseException e )
        {
            return false;
        }
    }
}
