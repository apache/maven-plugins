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
package org.apache.maven.plugin.eclipse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public abstract class AbstractEclipsePluginTestCase
    extends PlexusTestCase
{

    /**
     * The embedder.
     */
    protected MavenEmbedder maven;

    /**
     * Test repository directory.
     */
    protected File localRepositoryDir = getTestFile( "target/test-classes/m2repo" );

    /**
     * @see org.codehaus.plexus.PlexusTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {

        this.maven = new MavenEmbedder();
        this.maven.setClassLoader( Thread.currentThread().getContextClassLoader() );
        this.maven.setLogger( new MavenEmbedderConsoleLogger() );
        this.maven.setLocalRepositoryDirectory( localRepositoryDir );
        this.maven.setOffline( true );
        this.maven.start();

        super.setUp();
    }

    /**
     * @see org.codehaus.plexus.PlexusTestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        maven.stop();
        super.tearDown();
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     * @param projectName project directory
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName )
        throws Exception
    {
        testProject( projectName, new Properties() );
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     * @param projectName project directory
     * @param properties additional properties
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName, Properties properties )
        throws Exception
    {

        File basedir = getTestFile( "target/test-classes/projects/" + projectName );

        MavenProject project = maven.readProjectWithDependencies( new File( basedir, "pom.xml" ) );

        EventMonitor eventMonitor = new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );

        String outputDirPath = IdeUtils.getPluginSetting( project, "maven-eclipse-plugin", "outputDir", null );
        File outputDir;
        File projectOutputDir = basedir;

        if ( outputDirPath == null )
        {
            outputDir = basedir;
        }
        else
        {
            outputDir = new File( basedir, outputDirPath );
            outputDir.mkdirs();
            projectOutputDir = new File( outputDir, project.getArtifactId() );
        }

        this.maven.execute( project, Arrays.asList( new String[] {
            "org.apache.maven.plugins:maven-eclipse-plugin:clean",
            "org.apache.maven.plugins:maven-eclipse-plugin:eclipse" } ), eventMonitor, new ConsoleDownloadMonitor(),
                            properties, basedir );

        compareDirectoryContent( basedir, projectOutputDir, "" );
        compareDirectoryContent( basedir, projectOutputDir, ".settings/" );
        compareDirectoryContent( basedir, projectOutputDir, "META-INF/" );

    }

    /**
     * @param basedir
     * @param projectOutputDir
     * @throws IOException
     */
    private void compareDirectoryContent( File basedir, File projectOutputDir, String additionalDir )
        throws IOException
    {
        File expectedConfigDir = new File( basedir, "expected/" + additionalDir );

        if ( expectedConfigDir.isDirectory() )
        {
            File[] files = expectedConfigDir.listFiles( new FileFilter()
            {
                public boolean accept( File file )
                {
                    return !file.isDirectory();
                }
            } );

            for ( int j = 0; j < files.length; j++ )
            {
                File file = files[j];

                assertFileEquals( localRepositoryDir.getCanonicalPath(), file,
                                  new File( projectOutputDir, additionalDir + file.getName() ) );

            }
        }
    }

    protected void assertFileEquals( String mavenRepo, File expectedFile, File actualFile )
        throws IOException
    {
        List expectedLines = getLines( mavenRepo, expectedFile );
        List actualLines = getLines( mavenRepo, actualFile );
        String filename = actualFile.getName();

        String basedir = new File( getBasedir() ).getCanonicalPath().replace( '\\', '/' );

        for ( int i = 0; i < expectedLines.size(); i++ )
        {
            String expected = expectedLines.get( i ).toString();

            // replace some vars in the expected line, to account
            // for absolute paths that are different on each installation.
            expected = StringUtils.replace( expected, "${basedir}", basedir );
            expected = StringUtils.replace( expected, "${M2_REPO}", localRepositoryDir.getCanonicalPath()
                .replace( '\\', '/' ) );

            if ( actualLines.size() <= i )
            {
                fail( "Too few lines in the actual file. Was " + actualLines.size() + ", expected: "
                    + expectedLines.size() );
            }

            String actual = actualLines.get( i ).toString();

            if ( expected.startsWith( "#" ) && actual.startsWith( "#" ) )
            {
                //ignore comments, for settings file
                continue;
            }

            assertEquals( "Checking " + filename + ", line #" + ( i + 1 ), expected, actual );
        }

        assertTrue( "Unequal number of lines.", expectedLines.size() == actualLines.size() );
    }

    protected void assertContains( String message, String full, String substring )
    {
        if ( full == null || full.indexOf( substring ) == -1 )
        {
            StringBuffer buf = new StringBuffer();
            if ( message != null )
            {
                buf.append( message );
            }
            buf.append( ". " );
            buf.append( "Expected \"" );
            buf.append( substring );
            buf.append( "\" not found" );
            fail( buf.toString() );
        }
    }

    protected void assertDoesNotContain( String message, String full, String substring )
    {
        if ( full == null || full.indexOf( substring ) != -1 )
        {
            StringBuffer buf = new StringBuffer();
            if ( message != null )
            {
                buf.append( message );
            }
            buf.append( ". " );
            buf.append( "Unexpected \"" );
            buf.append( substring );
            buf.append( "\" found" );
            fail( buf.toString() );
        }
    }

    private List getLines( String mavenRepo, File file )
        throws IOException
    {
        List lines = new ArrayList();

        BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( file ), "UTF-8" ) );

        String line;

        while ( ( line = reader.readLine() ) != null )
        {
            lines.add( line );
        }

        IOUtil.close( reader );

        return lines;
    }
}
