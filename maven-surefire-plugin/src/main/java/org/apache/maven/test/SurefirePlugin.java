package org.apache.maven.test;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.surefire.SurefireBooter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @requiresDependencyResolution test
 * @goal test
 * @phase test
 * @description Run tests using surefire
 * @todo make version of junit and surefire configurable
 * @todo make report to be produced configurable
 */
public class SurefirePlugin
    extends AbstractMojo
{
    /**
     *  Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, but quite convenient on occasion.
     *
     * @parameter expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * @parameter expression="${maven.test.failure.ignore}"
     */
    private boolean testFailureIgnore;

    /**
     * @parameter expression="${basedir}"
     * @required
     */
    private String basedir;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File classesDirectory;

    /**
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     */
    private File testClassesDirectory;

    /**
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List classpathElements;

    /**
     * Base directory where all reports are written to.
     *
     * @parameter expression="${project.build.directory}/surefire-reports"
     */
    private String reportsDirectory;

    /**
     * Specify this parameter if you want to use the test regex notation to select tests to run.
     *
     * @parameter expression="${test}"
     */
    private String test;

    /**
     * @parameter
     */
    private List includes;

    /**
     * @parameter
     */
    private List excludes;

    /**
     * The project whose project files to create.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * List of System properties to pass to the JUnit tests.
     *
     * @parameter
     */
    private Properties systemProperties;

    /**
     * @parameter expression="${plugin.artifacts}"
     */
    private List pluginArtifacts;

    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Tests are skipped." );

            return;
        }

        // ----------------------------------------------------------------------
        // Setup the surefire booter
        // ----------------------------------------------------------------------

        SurefireBooter surefireBooter = new SurefireBooter();

        getLog().info( "Setting reports dir: " + reportsDirectory );

        surefireBooter.setReportsDirectory( reportsDirectory );

        // ----------------------------------------------------------------------
        // Check to see if we are running a single test. The raw parameter will
        // come through if it has not been set.
        // ----------------------------------------------------------------------

        if ( test != null )
        {
            // FooTest -> **/FooTest.java

            List includes = new ArrayList();

            List excludes = new ArrayList();

            String[] testRegexes = split( test, ",", -1 );

            for ( int i = 0; i < testRegexes.length; i++ )
            {
                includes.add( "**/" + testRegexes[i] + ".java" );
            }

            surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery",
                                       new Object[]{testClassesDirectory, includes, excludes} );
        }
        else
        {
            // defaults here, qdox doesn't like the end javadoc value
            if ( includes == null || includes.size() == 0 )
            {
                includes = new ArrayList( Collections.singletonList( "**/*Test.java" ) );
            }
            if ( excludes == null || excludes.size() == 0 )
            {
                excludes = new ArrayList( Collections.singletonList( "**/Abstract*Test.java" ) );
            }

            surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery",
                                       new Object[]{testClassesDirectory, includes, excludes} );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        System.setProperty( "basedir", basedir );

        // Add all system properties configured by the user
        if ( systemProperties != null )
        {
            Enumeration propertyKeys = systemProperties.propertyNames();
            while ( propertyKeys.hasMoreElements() )
            {
                String key = (String) propertyKeys.nextElement();
                System.setProperty( key, systemProperties.getProperty( key ) );
                getLog().debug( "Setting system property [" + key + "]=[" + systemProperties.getProperty( key ) + "]" );
            }
        }


        surefireBooter.addClassPathUrl( classesDirectory.getPath() );

        surefireBooter.addClassPathUrl( testClassesDirectory.getPath() );

        for ( Iterator i = pluginArtifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            surefireBooter.addClassPathUrl( artifact.getFile().getAbsolutePath() );
        }

        for ( Iterator i = classpathElements.iterator(); i.hasNext(); )
        {
            surefireBooter.addClassPathUrl( (String) i.next() );
        }

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReporter" );

        surefireBooter.addReport( "org.codehaus.surefire.report.FileReporter" );

        boolean success;
        try
        {
            success = surefireBooter.run();
        }
        catch ( Exception e )
        {
            // TODO: better handling
            throw new MojoExecutionException( "Error executing surefire", e );
        }

        if ( !success )
        {
            String msg = "There are some test failure.";

            if ( testFailureIgnore )
            {
                getLog().error( msg );
            }
            else
            {
                throw new MojoExecutionException( msg );
            }
        }
    }

    protected String[] split( String str, String separator, int max )
    {
        StringTokenizer tok;
        if ( separator == null )
        {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer( str );
        }
        else
        {
            tok = new StringTokenizer( str, separator );
        }

        int listSize = tok.countTokens();
        if ( max > 0 && listSize > max )
        {
            listSize = max;
        }

        String[] list = new String[listSize];
        int i = 0;
        int lastTokenBegin;
        int lastTokenEnd = 0;
        while ( tok.hasMoreTokens() )
        {
            if ( max > 0 && i == listSize - 1 )
            {
                // In the situation where we hit the max yet have
                // tokens left over in our input, the last list
                // element gets all remaining text.
                String endToken = tok.nextToken();
                lastTokenBegin = str.indexOf( endToken, lastTokenEnd );
                list[i] = str.substring( lastTokenBegin );
                break;
            }
            else
            {
                list[i] = tok.nextToken();
                lastTokenBegin = str.indexOf( list[i], lastTokenEnd );
                lastTokenEnd = lastTokenBegin + list[i].length();
            }
            i++;
        }
        return list;
    }
}
