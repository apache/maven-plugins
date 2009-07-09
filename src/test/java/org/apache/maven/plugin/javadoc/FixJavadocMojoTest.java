package org.apache.maven.plugin.javadoc;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class FixJavadocMojoTest
    extends AbstractMojoTestCase
{
    /** The M2_HOME env variable */
    private static final File M2_HOME;

    /** The M2_HOME env variable */
    private static final File JAVA_HOME;

    static
    {
        M2_HOME = getM2Home();

        JAVA_HOME = getJavaHome();
    }

    /**
     * @throws Exception if any
     */
    public void testFix()
        throws Exception
    {
        File testPomBasedir = new File( getBasedir(), "target/test/unit/fix-test" );

        executeMojoAndTest( testPomBasedir, new String[] { "ClassWithJavadoc.java", "ClassWithNoJavadoc.java",
            "InterfaceWithJavadoc.java", "InterfaceWithNoJavadoc.java" } );
    }

    /**
     * @throws Exception if any
     */
    public void testFixJdk5()
        throws Exception
    {
        if ( !SystemUtils.isJavaVersionAtLeast( 1.5f ) )
        {
            getContainer().getLogger().warn(
                                             "JDK 5.0 or more is required to run fix for '" + getClass().getName()
                                                 + "#" + getName() + "()'." );
            return;
        }

        File testPomBasedir = new File( getBasedir(), "target/test/unit/fix-jdk5-test" );
        executeMojoAndTest( testPomBasedir, new String[] { "ClassWithJavadoc.java", "ClassWithNoJavadoc.java",
            "InterfaceWithJavadoc.java", "InterfaceWithNoJavadoc.java", "SubClassWithJavadoc.java" } );
    }

    /**
     * @throws Exception if any
     */
    public void testFixJdk6()
        throws Exception
    {
        if ( !SystemUtils.isJavaVersionAtLeast( 1.6f ) )
        {
            getContainer().getLogger().warn(
                                             "JDK 6.0 or more is required to run fix for '" + getClass().getName()
                                                 + "#" + getName() + "()'." );
            return;
        }

        File testPomBasedir = new File( getBasedir(), "target/test/unit/fix-jdk6-test" );
        executeMojoAndTest( testPomBasedir, new String[] { "ClassWithJavadoc.java", "InterfaceWithJavadoc.java" } );
    }

    // ----------------------------------------------------------------------
    // private methods
    // ----------------------------------------------------------------------

    /**
     * @param testPomBasedir the basedir for the test project
     * @param clazzToCompare an array of the classes name to compare
     * @throws Exception if any
     */
    private void executeMojoAndTest( File testPomBasedir, String[] clazzToCompare )
        throws Exception
    {
        prepareTestProjects( testPomBasedir.getName() );

        File testPom = new File( testPomBasedir, "pom.xml" );
        assertTrue( testPom.getAbsolutePath() + " should exist", testPom.exists() );

        // compile the test project
        invokeCompileGoal( testPom );
        assertTrue( new File( testPomBasedir, "target/classes" ).exists() );

        FixJavadocMojo mojo = (FixJavadocMojo) lookupMojo( "fix", testPom );
        assertNotNull( mojo );
        mojo.execute();

        File expectedDir = new File( testPomBasedir, "expected/src/main/java/fix/test" );
        assertTrue( expectedDir.exists() );

        File generatedDir = new File( testPomBasedir, "target/generated/fix/test" );
        assertTrue( generatedDir.exists() );

        for ( int i = 0; i < clazzToCompare.length; i++ )
        {
            String className = clazzToCompare[i];
            assertEquals( new File( expectedDir, className ), new File( generatedDir, className ) );
        }
    }

    /**
     * Invoke the compilation on the given pom file.
     *
     * @param testPom
     */
    private void invokeCompileGoal( File testPom )
    {
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome( M2_HOME );

        ByteArrayOutputStream invokerLog = new ByteArrayOutputStream();
        InvocationOutputHandler outputHandler = new PrintStreamHandler( new PrintStream( invokerLog ), false );

        outputHandler.consumeLine( "Invoke Maven" );
        outputHandler.consumeLine( "M2_HOME=" + M2_HOME );
        outputHandler.consumeLine( "JAVA_HOME=" + JAVA_HOME );

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory( testPom.getParentFile() );
        request.setPomFile( testPom );
        request.setOutputHandler( outputHandler );
        request.setDebug( true );
        request.setJavaHome( JAVA_HOME );
        request.setMavenOpts( "-Xms256m -Xmx256m" );

        List goals = new ArrayList();
        goals.add( "clean" );
        goals.add( "compile" );

        request.setGoals( goals );

        try
        {
            InvocationResult result = invoker.execute( request );
            if ( result.getExitCode() != 0 )
            {
                StringBuffer msg = new StringBuffer();
                msg.append( "Ouput from invoker:" ).append( "\n\n" );
                msg.append( invokerLog ).append( "\n\n" );

                getContainer().getLogger().error( msg.toString() );

                fail( "Error when invoking Maven, see invoker log above" );
            }
        }
        catch ( MavenInvocationException e )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "Ouput from invoker:" ).append( "\n\n" );
            msg.append( invokerLog ).append( "\n\n" );

            getContainer().getLogger().error( msg.toString() );

            fail( "Error when invoking Maven, see invoker log above" );
        }
    }

    // ----------------------------------------------------------------------
    // static methods
    // ----------------------------------------------------------------------

    /**
     * Try to find the M2_HOME from System.getProperty( "maven.home" ) or M2_HOME env variable.
     *
     * @return the M2Home file
     */
    private static File getM2Home()
    {
        String mavenHome = System.getProperty( "maven.home" );

        if ( mavenHome == null )
        {
            try
            {
                mavenHome = CommandLineUtils.getSystemEnvVars().getProperty( "M2_HOME" );
            }
            catch ( IOException e )
            {
                // nop
            }
        }

        if ( mavenHome == null )
        {
            fail( "Cannot find Maven application directory. Either specify \'maven.home\' system property, or "
                + "M2_HOME environment variable." );
        }

        File m2Home = new File( mavenHome );
        if ( !m2Home.exists() )
        {
            fail( "Cannot find Maven application directory. Either specify \'maven.home\' system property, or "
                + "M2_HOME environment variable." );
        }

        return m2Home;
    }

    /**
     * Try to find the JAVA_HOME from System.getProperty( "java.home" )
     * By default, System.getProperty( "java.home" ) = JRE_HOME and JRE_HOME should be in the JDK_HOME
     *
     * @return the JavaHome file
     */
    private static File getJavaHome()
    {
        File javaHome;
        if ( SystemUtils.IS_OS_MAC_OSX )
        {
            javaHome = SystemUtils.getJavaHome();
        }
        else
        {
            javaHome = new File( SystemUtils.getJavaHome(), ".." );
        }

        if ( javaHome == null || !javaHome.exists() )
        {
            try
            {
                javaHome = new File( CommandLineUtils.getSystemEnvVars().getProperty( "JAVA_HOME" ) );
            }
            catch ( IOException e )
            {
                // nop
            }
        }

        if ( javaHome == null || !javaHome.exists() )
        {
            fail( "Cannot find Java application directory. Either specify \'java.home\' system property, or "
                + "JAVA_HOME environment variable." );
        }

        return javaHome;
    }

    /**
     * Asserts that files are equal. If they are not an AssertionFailedError is thrown.
     *
     * @throws IOException if any
     */
    private static void assertEquals( File expected, File actual )
        throws IOException
    {
        assertTrue( expected.exists() );
        String expectedContent = StringUtils.unifyLineSeparators( readFile( expected ) );

        assertTrue( actual.exists() );
        String actualContent = StringUtils.unifyLineSeparators( readFile( actual ) );

        assertEquals( "Expected file: " + expected.getAbsolutePath() + ", actual file: "
            + actual.getAbsolutePath(), expectedContent, actualContent );
    }

    /**
     * @param testProjectDirName not null
     * @throws IOException if any
     */
    private static void prepareTestProjects( String testProjectDirName )
        throws IOException
    {
        File testPomBasedir = new File( getBasedir(), "target/test/unit/" + testProjectDirName );

        // Using unit test dir
        FileUtils
                 .copyDirectoryStructure(
                                          new File( getBasedir(), "src/test/resources/unit/" + testProjectDirName ),
                                          testPomBasedir );
        List scmFiles = FileUtils.getDirectoryNames( testPomBasedir, "**/.svn", null, true );
        for ( Iterator it = scmFiles.iterator(); it.hasNext(); )
        {
            File dir = new File( it.next().toString() );

            if ( dir.isDirectory() )
            {
                FileUtils.deleteDirectory( dir );
            }
        }
    }

    /**
     * @param file not null
     * @return the content of the given file
     * @throws IOException if any
     */
    private static String readFile( File file )
        throws IOException
    {
        Reader fileReader = null;
        try
        {
            fileReader = ReaderFactory.newReader( file, "UTF-8" );
            return IOUtil.toString( fileReader );
        }
        finally
        {
            IOUtil.close( fileReader );
        }
    }
}
