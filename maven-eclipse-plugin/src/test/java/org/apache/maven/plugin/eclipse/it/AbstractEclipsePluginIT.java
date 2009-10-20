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
package org.apache.maven.plugin.eclipse.it;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import junit.framework.AssertionFailedError;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.ExecutionFailedException;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.test.plugin.BuildTool;
import org.apache.maven.shared.test.plugin.PluginTestTool;
import org.apache.maven.shared.test.plugin.ProjectTool;
import org.apache.maven.shared.test.plugin.RepositoryTool;
import org.apache.maven.shared.test.plugin.TestToolsException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public abstract class AbstractEclipsePluginIT
    extends AbstractMojoTestCase
{

    private BuildTool buildTool;

    private ProjectTool projectTool;

    private RepositoryTool repositoryTool;

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
    protected static final String ARTIFACT_ID = "maven-eclipse-plugin";

    /**
     * Version under which the plugin was installed to the test-time local repository for running test builds.
     */
    protected static final String VERSION = "test";

    private static final String BUILD_OUTPUT_DIRECTORY = "target/surefire-reports/build-output";

    private static boolean installed = false;

    /**
     * The name of the directory used for comparison of expected output.
     */
    private static final String EXPECTED_DIRECTORY_NAME = "expected";

    /**
     * The XML Header used to check if the file contains XML content.
     */
    private static final String XML_HEADER = "<?xml";

    /**
     * @see org.codehaus.plexus.PlexusTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        if ( !installed )
        {
            System.out.println( "*** Running test builds; output will be directed to: " + BUILD_OUTPUT_DIRECTORY + "\n" );
        }

        super.setUp();

        buildTool = (BuildTool) lookup( BuildTool.ROLE, "default" );

        projectTool = (ProjectTool) lookup( ProjectTool.ROLE, "default" );

        repositoryTool = (RepositoryTool) lookup( RepositoryTool.ROLE, "default" );

        String mavenHome = System.getProperty( "maven.home" );

        // maven.home is set by surefire when the test is run with maven, but better make the test
        // run in IDEs without the need of additional properties
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

        synchronized ( AbstractEclipsePluginIT.class )
        {
            if ( !installed )
            {
                PluginTestTool pluginTestTool = (PluginTestTool) lookup( PluginTestTool.ROLE, "default" );

                localRepositoryDirectory =
                    pluginTestTool.preparePluginForUnitTestingWithMavenBuilds( PomFile, "test",
                                                                               localRepositoryDirectory );

                System.out.println( "*** Installed test-version of the Eclipse plugin to: " + localRepositoryDirectory
                    + "\n" );

                // Hack: to work around proxys and DTDs retrievals.
                EntityResolver ignoreDtds = new EntityResolver()
                {

                    public InputSource resolveEntity( String publicId, String systemId )
                        throws SAXException, IOException
                    {
                        return new InputSource( new StringReader( "<!ELEMENT ignored (#PCDATA)>" ) );
                    }

                };
                XMLUnit.setTestEntityResolver( ignoreDtds );
                XMLUnit.setControlEntityResolver( ignoreDtds );

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
     *
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
     *
     * @param basedir basedir of mvn execution
     * @throws Exception any exception generated during test
     */
    protected void testProject( File basedir )
        throws Exception
    {
        testProject( basedir, new Properties(), "clean", "eclipse" );
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     *
     * @param projectName project directory
     * @param properties additional properties
     * @param cleanGoal TODO
     * @param genGoal TODO
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName, Properties properties, String cleanGoal, String genGoal )
        throws Exception
    {
        testProject( projectName, properties, cleanGoal, genGoal, false );
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     *
     * @param projectName project directory
     * @param properties additional properties
     * @param cleanGoal TODO
     * @param genGoal TODO
     * @param withInstall true to include the install goal, false to exclude it.
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName, Properties properties, String cleanGoal, String genGoal,
                                boolean withInstall )
        throws Exception
    {
        File basedir = getTestFile( "target/test-classes/projects/" + projectName );
        testProject( basedir, properties, cleanGoal, genGoal, withInstall );
    }

    /**
     * @param basedir Execute the eclipse:eclipse goal on a test project and verify generated files.
     * @param properties additional properties
     * @param cleanGoal TODO
     * @param genGoal TODO
     * @throws Exception any exception generated during test
     */
    protected void testProject( File basedir, Properties properties, String cleanGoal, String genGoal )
        throws Exception
    {
        testProject( basedir, properties, cleanGoal, genGoal, false );
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     *
     * @param basedir basedir of mvn execution
     * @param properties additional properties
     * @param cleanGoal TODO
     * @param genGoal TODO
     * @param withInstall true to include the install goal, false to exclude it.
     * @throws Exception any exception generated during test
     */
    protected void testProject( File basedir, Properties properties, String cleanGoal, String genGoal,
                                boolean withInstall )
        throws Exception
    {
        File pom = new File( basedir, "pom.xml" );

        String pluginSpec = getPluginCLISpecification();

        List goals = new ArrayList();

        goals.add( pluginSpec + cleanGoal );
        goals.add( pluginSpec + genGoal );
        if ( withInstall )
        {
            goals.add( "install" );
        }

        executeMaven( pom, properties, goals );

        MavenProject project = readProject( pom );

        String outputDirPath =
            IdeUtils.getPluginSetting( project, "org.apache.maven.plugins:maven-eclipse-plugin", "outputDir", null );
        File projectOutputDir = basedir;

        if ( outputDirPath != null )
        {
            File outputDir = new File( basedir, outputDirPath );
            outputDir.mkdirs();
            projectOutputDir = new File( outputDir, project.getArtifactId() );
        }

        compareDirectoryContent( basedir, projectOutputDir );
    }

    /**
     * Execute the eclipse:configure-workspace goal on a test project and verify generated files.
     *
     * @param projectName project directory
     * @throws Exception any exception generated during test
     */
    protected void testWorkspace( String projectName )
        throws Exception
    {
        testWorkspace( projectName, new Properties(), "configure-workspace" );
    }

    /**
     * Execute the eclipse:configure-workspace goal on a test project and verify generated files.
     *
     * @param projectName project directory
     * @throws Exception any exception generated during test
     */
    protected void testWorkspace( String projectName, String goal )
        throws Exception
    {
        testWorkspace( projectName, new Properties(), goal );
    }

    /**
     * Execute the eclipse:configure-workspace goal on a test project and verify generated files.
     *
     * @param projectName project directory
     * @param properties additional properties
     * @param cleanGoal TODO
     * @param genGoal TODO
     * @throws Exception any exception generated during test
     */
    protected void testWorkspace( String projectName, Properties properties, String genGoal )
        throws Exception
    {
        File basedir = getOutputDirectory( projectName );

        File pom = new File( basedir, "pom.xml" );

        String pluginSpec = getPluginCLISpecification();

        List goals = new ArrayList();

        goals.add( pluginSpec + genGoal );

        executeMaven( pom, properties, goals );

        MavenProject project = readProject( pom );

        String outputDirPath =
            IdeUtils.getPluginSetting( project, "org.apache.maven.plugins:maven-eclipse-plugin", "outputDir", null );
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

        compareDirectoryContent( basedir, projectOutputDir );

    }

    protected File getOutputDirectory( String projectName )
    {
        return getTestFile( "target/test-classes/projects/" + projectName );
    }

    protected File getTestWorkspaceWorkDirectory( String projectName )
    {
        return new File( this.getOutputDirectory( projectName ), ".metadata" );
    }

    protected void executeMaven( File pom, Properties properties, List goals )
        throws TestToolsException, ExecutionFailedException
    {
        executeMaven( pom, properties, goals, true );
    }

    protected void executeMaven( File pom, Properties properties, List goals, boolean switchLocalRepo )
        throws TestToolsException, ExecutionFailedException
    {
        System.out.println( "  Building " + pom.getParentFile().getName() );

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

        if (properties == null) properties = new Properties();
        InvocationRequest request = buildTool.createBasicInvocationRequest( pom, properties, goals, buildLog );
        request.setUpdateSnapshots( false );
        request.setShowErrors( true );
        request.getProperties().setProperty( "downloadSources", "false" );
        request.getProperties().setProperty( "downloadJavadocs", "false" );

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

        // String pluginVersion = System.getProperty( "pluginVersion" );
        //
        // if ( pluginVersion != null )
        // {
        // pluginSpec += pluginVersion + ":";
        // }
        //
        // System.out.println( "\n\nUsing Eclipse plugin version: " + pluginVersion + "\n\n" );

        // try using the test-version installed during setUp()
        pluginSpec += VERSION + ":";

        return pluginSpec;
    }

    /**
     * @param basedir the base directory of the project
     * @param projectOutputDir the directory where the eclipse plugin will write the output files.
     * @throws MojoExecutionException
     */
    protected void compareDirectoryContent( File basedir, File projectOutputDir )
        throws MojoExecutionException
    {
        File[] expectedDirectories = getExpectedDirectories( basedir );

        for ( int i = 0; i < expectedDirectories.length; i++ )
        {
            File expectedDirectory = expectedDirectories[i];
            File[] expectedFilesToCompare = getExpectedFilesToCompare( expectedDirectory );

            for ( int j = 0; j < expectedFilesToCompare.length; j++ )
            {
                File expectedFile = expectedFilesToCompare[j];
                File actualFile = getActualFile( projectOutputDir, basedir, expectedFile );

                if ( !actualFile.exists() )
                {
                    throw new AssertionFailedError( "Expected file not found: " + actualFile.getAbsolutePath() );
                }

                assertFileEquals( expectedFile, actualFile );

            }
        }
    }

    protected void assertFileEquals( File expectedFile, File actualFile )
        throws MojoExecutionException
    {
        if ( !actualFile.exists() )
        {
            throw new AssertionFailedError( "Expected file not found: " + actualFile.getAbsolutePath() );
        }

        HashMap variableReplacement = new HashMap();
        variableReplacement.put( "${basedir}",
                                 IdeUtils.fixSeparator( IdeUtils.getCanonicalPath( new File( getBasedir() ) ) ) );
        variableReplacement.put( "${M2_REPO}",
                                 IdeUtils.fixSeparator( IdeUtils.getCanonicalPath( localRepositoryDirectory ) ) );

        String expectedFileContents = preprocess( expectedFile, variableReplacement );
        String actualFileContents = preprocess( actualFile, null );

        if ( isXml( expectedFile ) )
        {
            assertXmlFileEquals( expectedFile, expectedFileContents, actualFile, actualFileContents );
        }
        else
        {
            assertTextFileEquals( expectedFile, expectedFileContents, actualFile, actualFileContents );
        }
    }

    /**
     * Assert that two XML files are equal.
     *
     * @param expectedFile the expected file - only used for path information
     * @param expectedFileContents the contents of the expected file
     * @param actualFile the actual file - only used for path information
     * @param actualFileContents the contents of the actual file
     * @throws MojoExecutionException failures.
     */
    private void assertXmlFileEquals( File expectedFile, String expectedFileContents, File actualFile,
                                      String actualFileContents )
        throws MojoExecutionException
    {
        try
        {
            XMLAssert.assertXMLEqual( "Comparing '" + IdeUtils.getCanonicalPath( actualFile ) + "' against '"
                + IdeUtils.getCanonicalPath( expectedFile ), expectedFileContents, actualFileContents );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( IdeUtils.getCanonicalPath( expectedFile )
                + "assertXmlFileEquals failure: IO " + e.getMessage(), e );
        }
        catch ( SAXException e )
        {
            throw new MojoExecutionException( "assertXmlFileEquals failure: SAX " + e.getMessage(), e );
        }
    }

    /**
     * Assert that two text files are equals. Lines that start with # are comments and ignored.
     *
     * @param expectedFile the expected file - only used for path information
     * @param expectedFileContents the contents of the expected file
     * @param actualFile the actual file - only used for path information
     * @param actualFileContents the contents of the actual fiel
     * @throws MojoExecutionException failures.
     */
    private void assertTextFileEquals( File expectedFile, String expectedFileContents, File actualFile,
                                       String actualFileContents )
        throws MojoExecutionException
    {
        List expectedLines = getLines( expectedFileContents );
        List actualLines = getLines( actualFileContents );
        for ( int i = 0; i < expectedLines.size(); i++ )
        {
            String expected = expectedLines.get( i ).toString();
            if ( actualLines.size() <= i )
            {
                fail( "Too few lines in the actual file. Was " + actualLines.size() + ", expected: "
                    + expectedLines.size() );
            }
            String actual = actualLines.get( i ).toString();
            if ( expected.startsWith( "#" ) && actual.startsWith( "#" ) )
            {
                // ignore comments, for settings file
                continue;
            }
            assertEquals( "Comparing '" + IdeUtils.getCanonicalPath( actualFile ) + "' against '"
                + IdeUtils.getCanonicalPath( expectedFile ) + "' at line #" + ( i + 1 ), expected, actual );
        }
        assertTrue( "Unequal number of lines.", expectedLines.size() == actualLines.size() );
    }

    /**
     * Preprocess the file so that equals comparison can be done. Preprocessing may vary based on filename.
     *
     * @param file the file being processed
     * @param variables if not null, then replace all keys with the corresponding values in the expected string.
     * @return processed input
     */
    private String preprocess( File file, Map variables )
        throws MojoExecutionException
    {
        String result = null;
        try
        {
            result = FileUtils.fileRead( file, "UTF-8" );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( "Unable to read file", ex );
        }
        result = replaceVariables( result, variables );
        result = IdeUtils.fixWindowsDriveURI( result );

        /*
         * NOTE: This is another hack to compensate for some metadata files that contain a complete XML file as the
         * value for a key like "org.eclipse.jdt.ui.formatterprofiles" from "org.eclipse.jdt.ui.prefs". Line terminators
         * in this value are platform-dependent.
         */
        if ( file.getName().endsWith( ".prefs" ) )
        {
            result = normalizeNewlineTerminators( result );
        }

        /*
         * NOTE: This is a hack to compensate for files that contain generated values like dependent-object in
         * org.eclipse.wst.common.component. Regex would be a better solution.
         */
        if ( file.getName().equals( "org.eclipse.wst.common.component" ) || file.getName().equals( ".modulemaps" )
            || file.getName().equals( "application.xml" ) )
        {
            result = result.replaceAll( "_\\d+", "" );
        }
        return result;
    }

    /**
     * Normalize line terminators into \n. \r\n, \r, \n all get changed into \n.
     *
     * @param input the string to normalize
     * @return string with line terminators normalized
     */
    private String normalizeNewlineTerminators( String input )
    {
        return input.replaceAll( "(\\\\r\\\\n)|(\\\\n)|(\\\\r)", "\\n" );
    }

    /**
     * @param str input string
     * @param variables map of variables (keys) and replacement value (values)
     * @return the string with all variable values replaced.
     */
    private String replaceVariables( String str, Map variables )
    {
        String result = str;
        if ( variables != null && !variables.isEmpty() )
        {
            Iterator iter = variables.entrySet().iterator();
            while ( iter.hasNext() )
            {
                Map.Entry entry = (Entry) iter.next();
                String variable = (String) entry.getKey();
                String replacement = (String) entry.getValue();
                result = StringUtils.replace( result, variable, replacement );
            }
        }

        return result;
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

    private List getLines( String input )
        throws MojoExecutionException
    {
        try

        {
            List lines = new ArrayList();

            BufferedReader reader = new BufferedReader( new StringReader( input ) );

            String line;

            while ( ( line = reader.readLine() ) != null )
            {
                lines.add( line );
            }

            IOUtil.close( reader );

            return lines;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "failed to getLines", e );
        }
    }

    /**
     * @param basedir base directory to search for directories named "expected"
     * @return an array of directories that match "expected"
     */
    private File[] getExpectedDirectories( File basedir )
    {
        List expectedDirectories = new ArrayList();
        List subdirectories = new ArrayList();

        File[] allFiles = basedir.listFiles();
        if ( allFiles != null )
        {
            for ( int i = 0; i < allFiles.length; i++ )
            {
                File currentFile = allFiles[i];
                if ( currentFile.isDirectory() )
                {
                    if ( currentFile.getName().equals( EXPECTED_DIRECTORY_NAME ) )
                    {
                        expectedDirectories.add( currentFile );
                    }
                    else
                    {
                        subdirectories.add( currentFile );
                    }
                }
            }
        }
        if ( !subdirectories.isEmpty() )
        {
            for ( Iterator iter = subdirectories.iterator(); iter.hasNext(); )
            {
                File subdirectory = (File) iter.next();
                File[] subdirectoryFiles = getExpectedDirectories( subdirectory );
                expectedDirectories.addAll( Arrays.asList( subdirectoryFiles ) );
            }
        }
        return (File[]) expectedDirectories.toArray( new File[expectedDirectories.size()] );
    }

    /**
     * @param expectedDirectory the expected directory to locate expected Files
     * @return an array of Files found under the expectedDirectory - will recurse through the directory structure.
     */
    private File[] getExpectedFilesToCompare( File expectedDirectory )
    {
        List expectedFiles = new ArrayList();
        List subdirectories = new ArrayList();

        File[] allFiles = expectedDirectory.listFiles();
        if ( allFiles != null )
        {
            for ( int i = 0; i < allFiles.length; i++ )
            {
                File currentFile = allFiles[i];
                if ( currentFile.isDirectory() )
                {
                    subdirectories.add( currentFile );
                }
                else
                {
                    expectedFiles.add( currentFile );
                }
            }
        }
        if ( !subdirectories.isEmpty() )
        {
            for ( Iterator iter = subdirectories.iterator(); iter.hasNext(); )
            {
                File subdirectory = (File) iter.next();
                File[] subdirectoryFiles = getExpectedFilesToCompare( subdirectory );
                expectedFiles.addAll( Arrays.asList( subdirectoryFiles ) );
            }
        }

        return (File[]) expectedFiles.toArray( new File[expectedFiles.size()] );
    }

    /**
     * Locate the actual file needed for comparison. The expectedFile has the baseDir prefix removed and the resulting
     * relative path used to locate the file within the projectOutputDir.
     *
     * @param projectOutputDir the directory where the eclipse plugin writes files to
     * @param basedir the base dir of the project being tested
     * @param expectedFile the expected file used to compare to the actual file
     * @return the actual file needed for comparison against the expectedFile
     * @throws MojoExecutionException failures for obtaining actual file.
     */
    private File getActualFile( File projectOutputDir, File basedir, File expectedFile )
        throws MojoExecutionException
    {
        String relativePath = IdeUtils.toRelativeAndFixSeparator( basedir, expectedFile, false );
        relativePath = relativePath.replaceFirst( EXPECTED_DIRECTORY_NAME, "" );
        File actualFile = new File( projectOutputDir, relativePath );
        try
        {
            return actualFile.getCanonicalFile();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException(
                                              Messages.getString(
                                                                  "EclipsePlugin.cantcanonicalize", actualFile.getAbsolutePath() ), e ); //$NON-NLS-1$
        }
    }

    /**
     * Test if the file contains xml content.
     *
     * @param f the file to test
     * @return true if the file contains xml content, false otherwise.
     */
    private boolean isXml( File f )
    {
        FileReader reader = null;
        try
        {
            reader = new FileReader( f );
            char[] header = new char[XML_HEADER.length()];
            reader.read( header );
            return XML_HEADER.equals( new String( header ) );
        }
        catch ( Exception e )
        {
            return false;
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    /**
     * Return the not available marker file for the specified artifact details.
     *
     * @param groupId group id of artifact
     * @param artifactId artifact id of artifact
     * @param version version of artifact
     * @param classifier the classifier of the artifact
     * @param inClassifier the sources/javadocs to be attached
     * @return the not available marker file
     * @throws Exception failures.
     * @see IdeUtils#createArtifactWithClassifier(String, String, String, String, String, ArtifactFactory)
     */
    protected File getNotAvailableMarkerFile( String groupId, String artifactId, String version, String classifier,
                                              String inClassifier )
        throws Exception
    {
        // HACK: START
        // TODO: Work out how to use Plexus to obtain these values
        String url = "file://" + localRepositoryDirectory;
        ArtifactRepository localRepository =
            new DefaultArtifactRepository( "local", url, new DefaultRepositoryLayout() );

        ArtifactFactory artifactFactory = new DefaultArtifactFactory();

        DefaultArtifactHandler javaSourceArtifactHandler = new DefaultArtifactHandler( "java-source" );
        setVariableValueToObject( javaSourceArtifactHandler, "extension", "jar" );

        DefaultArtifactHandler javadocArtifactHandler = new DefaultArtifactHandler( "javadoc" );
        setVariableValueToObject( javadocArtifactHandler, "extension", "jar" );

        Map artifactHandlers = new HashMap();
        artifactHandlers.put( "java-source", javaSourceArtifactHandler );
        artifactHandlers.put( "javadoc", javadocArtifactHandler );

        ArtifactHandlerManager artifactHandlerManager = new DefaultArtifactHandlerManager();
        setVariableValueToObject( artifactHandlerManager, "artifactHandlers", artifactHandlers );
        setVariableValueToObject( artifactFactory, "artifactHandlerManager", artifactHandlerManager );
        // HACK: END

        Artifact artifact =
            IdeUtils.createArtifactWithClassifier( groupId, artifactId, version, classifier, inClassifier,
                                                   artifactFactory );
        return IdeUtils.getNotAvailableMarkerFile( localRepository, artifact );
    }

    /**
     * Assert that the not available marker file exists for the specified artifact details.
     *
     * @param groupId group id of artifact
     * @param artifactId artifact id of artifact
     * @param version version of artifact
     * @param classifier the classifier of the artifact
     * @param inClassifier the sources/javadocs to be attached
     * @throws Exception failures
     */
    protected void assertNotAvailableMarkerFileExists( String groupId, String artifactId, String version,
                                                       String classifier, String inClassifier )
        throws Exception
    {
        File markerFile = getNotAvailableMarkerFile( groupId, artifactId, version, classifier, inClassifier );
        assertTrue( "The \"Not Available\" marker file does not exist: " + markerFile, markerFile.exists() );
    }

    /**
     * Assert that the not available marker file does not exist for the specified artifact details.
     *
     * @param groupId group id of artifact
     * @param artifactId artifact id of artifact
     * @param version version of artifact
     * @param classifier the classifier of the artifact
     * @param inClassifier the sources/javadocs to be attached
     * @throws Exception failures
     */
    protected void assertNotAvailableMarkerFileDoesNotExist( String groupId, String artifactId, String version,
                                                             String classifier, String inClassifier )
        throws Exception
    {
        File markerFile = getNotAvailableMarkerFile( groupId, artifactId, version, classifier, inClassifier );
        assertTrue( "The \"Not Available\" marker file incorrectly exists: " + markerFile, !markerFile.exists() );
    }

}
