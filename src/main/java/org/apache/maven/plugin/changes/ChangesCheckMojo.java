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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.changes.model.Release;

/**
 * Goal which checks that the changes.xml file has the necessary data to
 * generate an announcement or a report for the current release.
 * 
 * @goal changes-check
 * @author Justin Edelson
 * @author Dennis Lundberg
 * @since 2.4
 * @threadSafe
 */
public class ChangesCheckMojo extends AbstractMojo
{
    /**
     * The format that a correct release date should have. This value will be
     * used as a pattern to try to create a date.
     *
     * @parameter expression="${changes.releaseDateFormat}" default-value="yyyy-MM-dd"
     */
    private String releaseDateFormat;

    /**
     * Version of the artifact.
     *
     * @parameter expression="${changes.version}" default-value="${project.version}"
     * @required
     */
    private String version;

    /**
     * The path of the <code>changes.xml</code> file that will be checked.
     *
     * @parameter expression="${changes.xmlPath}" default-value="src/changes/changes.xml"
     */
    private File xmlPath;

    private ReleaseUtils releaseUtils = new ReleaseUtils( getLog() );

    /**
     * Check that the latest release contains a valid release date.
     *
     * @throws MojoExecutionException
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( xmlPath.exists() )
        {
            ChangesXML xml = new ChangesXML( xmlPath, getLog() );
            Release release = releaseUtils.getLatestRelease( xml.getReleaseList(), version );
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
