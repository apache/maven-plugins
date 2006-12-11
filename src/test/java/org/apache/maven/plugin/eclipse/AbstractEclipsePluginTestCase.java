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
package org.apache.maven.plugin.eclipse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.AssertionFailedError;

import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.test.plugin.BuildTool;
import org.apache.maven.shared.test.plugin.PluginTestTool;
import org.apache.maven.shared.test.plugin.ProjectTool;
import org.apache.maven.shared.test.plugin.TestToolsException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.PlexusContainer;
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

    private BuildTool buildTool;

    private ProjectTool projectTool;

    /**
     * Test repository directory.
     */
    protected static File localRepositoryDirectory = getTestFile( "target/test-classes/m2repo" );

    /**
     * Group-Id for running test builds.
     */
    protected static final String GROUP_ID = "org.apache.maven.plugins";

    /**
     * Artifact-Id for running test builds.
     */
    protected static final String ARTIFACT_ID = "maven-eclipse-plugin";

    /**
     * Version under which the plugin was installed to the test-time local repository for running 
     * test builds.
     */
    protected static final String VERSION = "test";

    private static final String BUILD_OUTPUT_DIRECTORY = "target/surefire-reports/build-output";

    private static boolean installed = false;

    /**
     * @see org.codehaus.plexus.PlexusTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        if ( !installed )
        {
            System.out
                .println( "*** Running test builds; output will be directed to: " + BUILD_OUTPUT_DIRECTORY + "\n" );
        }

        super.setUp();

        buildTool = (BuildTool) lookup( BuildTool.ROLE, "default" );

        projectTool = (ProjectTool) lookup( ProjectTool.ROLE, "default" );

        String mavenHome = System.getProperty( "maven.home" );

        // maven.home is set by surefire when the test is run with maven, but better make the test run in IDEs without
        // the need of additional properties
        if ( mavenHome == null )
        {
            String path = System.getProperty( "java.library.path" );
            String[] paths = StringUtils.split( path, System.getProperty( "path.separator" ) );
            for ( int j = 0; j < paths.length; j++ )
            {
                String pt = paths[j];
                if ( new File( pt, "m2" ).exists() )
                {
                    System.setProperty( "maven.home", new File( pt ).getParent() );
                    break;
                }

            }
        }

        System.setProperty( "MAVEN_TERMINATE_CMD", "on" );

        synchronized ( AbstractEclipsePluginTestCase.class )
        {
            if ( !installed )
            {
                PluginTestTool pluginTestTool = (PluginTestTool) lookup( PluginTestTool.ROLE, "default" );

                localRepositoryDirectory = pluginTestTool
                    .preparePluginForUnitTestingWithMavenBuilds( "test", localRepositoryDirectory );

                System.out.println( "*** Installed test-version of the Eclipse plugin to: " + localRepositoryDirectory
                    + "\n" );

                installed = true;
            }
        }

    }

    /**
     * @see org.codehaus.plexus.PlexusTestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        List containers = new ArrayList();

        containers.add( getContainer() );

        for ( Iterator iter = containers.iterator(); iter.hasNext(); )
        {
            PlexusContainer container = (PlexusContainer) iter.next();

            if ( container != null )
            {
                container.dispose();

                ClassRealm realm = container.getContainerRealm();

                if ( realm != null )
                {
                    realm.getWorld().disposeRealm( realm.getId() );
                }
            }
        }
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     * @param projectName project directory
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName )
        throws Exception
    {
        testProject( projectName, new Properties(), "clean", "eclipse" );
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     * @param projectName project directory
     * @param properties additional properties
     * @throws Exception any exception generated during test
     * @deprecated Use {@link #testProject(String,Properties,String,String)} instead
     */
    protected void testProject( String projectName, Properties properties )
        throws Exception
    {
        testProject( projectName, properties, "clean", "eclipse" );
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     * @param projectName project directory
     * @param properties additional properties
     * @param cleanGoal TODO
     * @param genGoal TODO
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName, Properties properties, String cleanGoal, String genGoal )
        throws Exception
    {
        File basedir = getTestFile( "target/test-classes/projects/" + projectName );

        File pom = new File( basedir, "pom.xml" );

        String pluginSpec = getPluginCLISpecification();

        List goals = new ArrayList();

        goals.add( pluginSpec + cleanGoal );
        goals.add( pluginSpec + genGoal );

        executeMaven( pom, properties, goals );

        MavenProject project = readProject( pom );

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

        compareDirectoryContent( basedir, projectOutputDir, "" );
        compareDirectoryContent( basedir, projectOutputDir, ".settings/" );
        compareDirectoryContent( basedir, projectOutputDir, ".externalToolBuilders/" );
        compareDirectoryContent( basedir, projectOutputDir, "META-INF/" );

    }

    protected void executeMaven( File pom, Properties properties, List goals )
        throws TestToolsException, ExecutionFailedException
    {
        executeMaven( pom, properties, goals, true );
    }

    protected void executeMaven( File pom, Properties properties, List goals, boolean switchLocalRepo )
        throws TestToolsException, ExecutionFailedException
    {
        new File( BUILD_OUTPUT_DIRECTORY ).mkdirs();

        NullPointerException npe = new NullPointerException();
        StackTraceElement[] trace = npe.getStackTrace();

        File buildLog = null;

        for ( int i = 0; i < trace.length; i++ )
        {
            StackTraceElement element = trace[i];

            String methodName = element.getMethodName();

            if ( methodName.startsWith( "test" ) && !methodName.equals( "testProject" ) )
            {
                String classname = element.getClassName();

                buildLog = new File( BUILD_OUTPUT_DIRECTORY, classname + "_" + element.getMethodName() + ".build.log" );

                break;
            }
        }

        if ( buildLog == null )
        {
            buildLog = new File( BUILD_OUTPUT_DIRECTORY, "unknown.build.log" );
        }

        InvocationRequest request = buildTool.createBasicInvocationRequest( pom, properties, goals, buildLog );
        request.setUpdateSnapshots( false );
        request.setShowErrors( true );

        request.setDebug( true );

        if ( switchLocalRepo )
        {
            request.setLocalRepositoryDirectory( localRepositoryDirectory );
        }

        InvocationResult result = buildTool.executeMaven( request );

        if ( result.getExitCode() != 0 )
        {
            String buildLogUrl = buildLog.getAbsolutePath();

            try
            {
                buildLogUrl = buildLog.toURL().toExternalForm();
            }
            catch ( MalformedURLException e )
            {
            }

            throw new ExecutionFailedException( "Failed to execute build.\nPOM: " + pom + "\nGoals: "
                + StringUtils.join( goals.iterator(), ", " ) + "\nExit Code: " + result.getExitCode() + "\nError: "
                + result.getExecutionException() + "\nBuild Log: " + buildLogUrl + "\n", result );
        }
    }

    protected MavenProject readProject( File pom )
        throws TestToolsException
    {
        return projectTool.readProject( pom, localRepositoryDirectory );
    }

    protected String getPluginCLISpecification()
    {
        String pluginSpec = GROUP_ID + ":" + ARTIFACT_ID + ":";

        //        String pluginVersion = System.getProperty( "pluginVersion" );
        //        
        //        if ( pluginVersion != null )
        //        {
        //            pluginSpec += pluginVersion + ":";
        //        }
        //
        //        System.out.println( "\n\nUsing Eclipse plugin version: " + pluginVersion + "\n\n" );

        // try using the test-version installed during setUp()
        pluginSpec += VERSION + ":";

        return pluginSpec;
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
                File expectedFile = files[j];
                File actualFile = new File( projectOutputDir, additionalDir + expectedFile.getName() )
                    .getCanonicalFile();

                if ( !actualFile.exists() )
                {
                    throw new AssertionFailedError( "Expected file not found: " + actualFile.getAbsolutePath() );
                }

                assertFileEquals( localRepositoryDirectory.getCanonicalPath(), expectedFile, actualFile );

            }
        }
    }

    protected void assertFileEquals( String mavenRepo, File expectedFile, File actualFile )
        throws IOException
    {
        List expectedLines = getLines( mavenRepo, expectedFile );

        if ( !actualFile.exists() )
        {
            throw new AssertionFailedError( "Expected file not found: " + actualFile.getAbsolutePath() );
        }

        List actualLines = getLines( mavenRepo, actualFile );
        String filename = actualFile.getName();

        String basedir = new File( getBasedir() ).getCanonicalPath().replace( '\\', '/' );

        for ( int i = 0; i < expectedLines.size(); i++ )
        {
            String expected = expectedLines.get( i ).toString();

            // replace some vars in the expected line, to account
            // for absolute paths that are different on each installation.
            expected = StringUtils.replace( expected, "${basedir}", basedir );
            expected = StringUtils.replace( expected, "${M2_REPO}", localRepositoryDirectory.getCanonicalPath()
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
