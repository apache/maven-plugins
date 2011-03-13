package org.apache.maven.plugin.dependency.its;

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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *         Copied from the Eclipse
 *         AbstractEclipsePluginTestCase v2.4
 * @version $Id: AbstractDependencyPluginITCase.java 556442
 *          2007-07-15 20:20:23Z dantran $
 */
public abstract class AbstractDependencyPluginITCase
    extends PlexusTestCase
{

    private BuildTool buildTool;

    private ProjectTool projectTool;

    /**
     * Test repository directory.
     */
    protected static File localRepositoryDirectory = getTestFile( "target/test-classes/m2repo" );

    /**
     * Pom File
     */
    protected static File PomFile = new File( getBasedir(), "pom.xml" );

    /**
     * Group-Id for running test builds.
     */
    protected static final String GROUP_ID = "org.apache.maven.plugins";

    /**
     * Artifact-Id for running test builds.
     */
    protected static final String ARTIFACT_ID = "maven-dependency-plugin";

    /**
     * Version under which the plugin was installed to the
     * test-time local repository for running test builds.
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

        // maven.home is set by surefire when the test is
        // run with maven, but better make the test run in
        // IDEs without
        // the need of additional properties
        if ( mavenHome == null )
        {
            String path = System.getProperty( "java.library.path" );
            String[] paths = StringUtils.split( path, System.getProperty( "path.separator" ) );
            for ( int j = 0; j < paths.length; j++ )
            {
                String pt = paths[j];
                if ( new File( pt, "mvn" ).exists() )
                {
                    System.setProperty( "maven.home", new File( pt ).getAbsoluteFile().getParent() );
                    break;
                }

            }
        }

        System.setProperty( "MAVEN_TERMINATE_CMD", "on" );

        synchronized ( AbstractDependencyPluginITCase.class )
        {
            if ( !installed )
            {
                PluginTestTool pluginTestTool = (PluginTestTool) lookup( PluginTestTool.ROLE, "default" );

                localRepositoryDirectory = pluginTestTool
                    .preparePluginForUnitTestingWithMavenBuilds( PomFile, "test", localRepositoryDirectory );

                System.out.println( "*** Installed test-version of the Dependency plugin to: "
                    + localRepositoryDirectory + "\n" );

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

        List<PlexusContainer> containers = new ArrayList<PlexusContainer>();

        containers.add( getContainer() );

        for ( PlexusContainer container : containers )
        {
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
     * Execute the plugin with no properties
     * 
     * @param projectName project directory
     * @param goalList comma separated list of goals to
     *            execute
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName, String goalList )
        throws Exception
    {
        Properties props = new Properties();
        testProject( projectName, props, goalList );
    }

    /**
     * Execute the plugin.
     * 
     * @param projectName project directory
     * @param properties additional properties
     * @param goalList comma separated list of goals to
     *            execute
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName, Properties properties, String goalList )
        throws Exception
    {
        File theBasedir = getTestFile( "target/test-classes/its/" + projectName );

        File pom = new File( theBasedir, "pom.xml" );

        List<String> goals = Arrays.asList( goalList.split( "," ) );

        executeMaven( pom, properties, goals );

        // MavenProject project = readProject( pom );

        /*
         * String outputDirPath = IdeUtils.getPluginSetting(
         * project, "maven-dependency-plugin", "outputDir",
         * null ); File outputDir; File projectOutputDir =
         * basedir;
         * 
         * if ( outputDirPath == null ) { outputDir =
         * basedir; } else { outputDir = new File( basedir,
         * outputDirPath ); outputDir.mkdirs();
         * projectOutputDir = new File( outputDir,
         * project.getArtifactId() ); }
         */
    }

    protected File getOutputDirectory( String projectName )
    {
        return getTestFile( "target/test-classes/projects/" + projectName );
    }

    protected void executeMaven( File pom, Properties properties, List<String> goals )
        throws TestToolsException, ExecutionFailedException
    {
        executeMaven( pom, properties, goals, true );
    }

    protected void executeMaven( File pom, Properties properties, List<String> goals, boolean switchLocalRepo )
        throws TestToolsException, ExecutionFailedException
    {
        // insert the test property to activate the test
        // profile
        properties.setProperty( "test", "true" );
        new File( BUILD_OUTPUT_DIRECTORY ).mkdirs();

        File buildLog = null;

        for ( StackTraceElement element : new NullPointerException().getStackTrace() )
        {
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
                
                buildLogUrl = buildLog.toURI().toURL().toExternalForm();
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

        // String pluginVersion = System.getProperty(
        // "pluginVersion" );
        //        
        // if ( pluginVersion != null )
        // {
        // pluginSpec += pluginVersion + ":";
        // }
        //
        // System.out.println( "\n\nUsing Eclipse plugin
        // version: " + pluginVersion + "\n\n" );

        // try using the test-version installed during
        // setUp()
        pluginSpec += VERSION + ":";

        return pluginSpec;
    }
}
