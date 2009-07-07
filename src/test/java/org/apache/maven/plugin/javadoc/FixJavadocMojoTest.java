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
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class FixJavadocMojoTest
    extends AbstractMojoTestCase
{
    /** The vm line separator */
    private static final String EOL = System.getProperty( "line.separator" );

    /** The M2_HOME env variable */
    private static final File M2_HOME;

    static
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
            fail( "Cannot find Maven application "
                + "directory. Either specify \'maven.home\' system property, or M2_HOME environment variable." );
        }

        M2_HOME = new File( mavenHome );
    }

    /**
     * @throws Exception if any
     */
    public void testFix()
        throws Exception
    {
        File testPomBasedir = new File( getBasedir(), "target/test/unit/fix-test" );

        prepareTestProjects( testPomBasedir.getName() );

        File testPom = new File( testPomBasedir, "pom.xml" );
        assertTrue( testPom.getAbsolutePath() + " should exist", testPom.exists() );

        // compile the test project
        invokeCompileGoal( testPom );
        assertTrue( new File( testPomBasedir, "target/classes" ).exists() );

        FixJavadocMojo mojo = (FixJavadocMojo) lookupMojo( "fix", testPom );
        assertNotNull( mojo );
        mojo.execute();

        File packageDir = new File( testPomBasedir, "target/generated/fix/test" );
        assertTrue( packageDir.exists() );

        File clazzFile = new File( packageDir, "ClassWithJavadoc.java" );
        assertTrue( clazzFile.exists() );
        String content = readFile( clazzFile );
        assertTrue( content.indexOf( "" + EOL +
                "/**" + EOL +
                " * Some Javadoc." + EOL +
                " *" + EOL +
                " * @author " + System.getProperty( "user.name" ) + EOL+
                " * @version \u0024Id: \u0024" + EOL +
                " * @since 1.0" + EOL +
                " */" + EOL +
                "public class ClassWithJavadoc" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /** Constant <code>MY_STRING_CONSTANT=\"value\"</code> */" + EOL +
                "    public static final String MY_STRING_CONSTANT = \"value\";" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /** Constant <code>MY_INT_CONSTANT=1</code> */" + EOL +
                "    public static final int MY_INT_CONSTANT = 1;" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /** Constant <code>EOL=\"System.getProperty( line.separator )\"</code> */" + EOL +
                "    public static final String EOL = System.getProperty( \"line.separator\" );" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    private static final String MY_PRIVATE_CONSTANT = \"\";" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * <p>Constructor for ClassWithJavadoc.</p>" + EOL +
                "     */" + EOL +
                "    public ClassWithJavadoc()" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * The main method" + EOL +
                "     *" + EOL +
                "     * @param args      an array of strings that contains the arguments" + EOL +
                "     */" + EOL +
                "    public static void main( String[] args )" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * <p>methodWithMissingParameters</p>" + EOL +
                "     *" + EOL +
                "     * @param str a {@link java.lang.String} object." + EOL +
                "     * @param b a boolean." + EOL +
                "     * @param i a int." + EOL +
                "     * @return a {@link java.lang.String} object." + EOL +
                "     */" + EOL +
                "    public String methodWithMissingParameters( String str, boolean b, int i )" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * <p>methodWithMissingParameters2</p>" + EOL +
                "     *" + EOL +
                "     * @param str a {@link java.lang.String} object." + EOL +
                "     * @throws java.lang.UnsupportedOperationException if any" + EOL +
                "     * @param b a boolean." + EOL +
                "     * @param i a int." + EOL +
                "     * @return a {@link java.lang.String} object." + EOL +
                "     */" + EOL +
                "    public String methodWithMissingParameters2( String str, boolean b, int i )" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * <p>methodWithWrongJavadocParameters</p>" + EOL +
                "     *" + EOL +
                "     * @param aString a {@link java.lang.String} object." + EOL +
                "     */" + EOL +
                "    public void methodWithWrongJavadocParameters( String aString )" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * <p>methodWithMultiLinesJavadoc</p>" + EOL +
                "     *" + EOL +
                "     * @param aString" + EOL +
                "     *      a string" + EOL +
                "     * @param anotherString" + EOL +
                "     *      with" + EOL +
                "     *      multi" + EOL +
                "     *      line" + EOL +
                "     *      comments" + EOL +
                "     * @return a" + EOL +
                "     *      String" + EOL +
                "     * @throws java.lang.UnsupportedOperationException" + EOL +
                "     *      if any" + EOL +
                "     */" + EOL +
                "    public String methodWithMultiLinesJavadoc( String aString, String anotherString )" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * methodWithMultiLinesJavadoc2" + EOL +
                "     *" + EOL +
                "     * @return a {@link java.lang.String} object." + EOL +
                "     */" + EOL +
                "    protected String methodWithMultiLinesJavadoc2()" ) != -1 );

        clazzFile = new File( packageDir, "ClassWithNoJavadoc.java" );
        assertTrue( clazzFile.exists() );
        content = readFile( clazzFile );
        // QDOX-155
        assertTrue( content.indexOf( "" + EOL +
                "    /** Constant <code>SEPARATOR=','</code> */" + EOL +
                "    public static final char SEPARATOR = ',';" ) != -1 );
        // QDOX-156
        assertTrue( content.indexOf( "" + EOL +
                "    // TODO: blabla" + EOL +
                "    /** Constant <code>TEST1=\"test1\"</code> */" + EOL +
                "    public static final String TEST1 = \"test1\";" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "/**" + EOL +
                " * <p>ClassWithNoJavadoc class.</p>" + EOL +
                " *" + EOL +
                " * @author " + System.getProperty( "user.name" ) + EOL+
                " * @version \u0024Id: \u0024" + EOL +
                " * @since 1.0" + EOL +
                " */" + EOL +
                "public class ClassWithNoJavadoc" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * <p>main</p>" + EOL +
                "     *" + EOL +
                "     * @param args an array of {@link java.lang.String} objects." + EOL +
                "     */" + EOL +
                "    public static void main( String[] args )" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    private void sampleMethod( String str )" ) != -1 );

        clazzFile = new File( packageDir, "InterfaceWithJavadoc.java" );
        assertTrue( clazzFile.exists() );
        content = readFile( clazzFile );
        assertTrue( content.indexOf( "" + EOL +
                "/**" + EOL +
                " * Some Javadoc." + EOL +
                " *" + EOL +
                " * @author " + System.getProperty( "user.name" ) + EOL+
                " * @version \u0024Id: \u0024" + EOL +
                " * @since 1.0" + EOL +
                " */" + EOL +
                "public interface InterfaceWithJavadoc" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /** comment */" + EOL +
                "    String MY_STRING_CONSTANT = \"value\";" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * My method" + EOL +
                "     *" + EOL +
                "     * @param aString a {@link java.lang.String} object." + EOL +
                "     */" + EOL +
                "    public void method( String aString );" ) != -1 );

        clazzFile = new File( packageDir, "InterfaceWithNoJavadoc.java" );
        assertTrue( clazzFile.exists() );
        content = readFile( clazzFile );
        assertTrue( content.indexOf( "" + EOL +
                "/**" + EOL +
                " * <p>InterfaceWithNoJavadoc interface.</p>" + EOL +
                " *" + EOL +
                " * @author " + System.getProperty( "user.name" ) + EOL+
                " * @version \u0024Id: \u0024" + EOL +
                " * @since 1.0" + EOL +
                " */" + EOL +
                "public interface InterfaceWithNoJavadoc" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /** Constant <code>MY_STRING_CONSTANT=\"value\"</code> */" + EOL +
                "    String MY_STRING_CONSTANT = \"value\";" ) != -1 );
        assertTrue( content.indexOf( "" + EOL +
                "    /**" + EOL +
                "     * <p>method</p>" + EOL +
                "     *" + EOL +
                "     * @param aString a {@link java.lang.String} object." + EOL +
                "     */" + EOL +
                "    public void method( String aString );" ) != -1 );
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

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory( testPom.getParentFile() );
        request.setPomFile( testPom );

        ByteArrayOutputStream invokerLog = new ByteArrayOutputStream();
        InvocationOutputHandler outputHandler = new PrintStreamHandler( new PrintStream( invokerLog ), false );
        request.setOutputHandler( outputHandler );
        request.setDebug( true );
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

    /**
     * @param testDir not null
     * @throws IOException if any
     */
    private static void prepareTestProjects( String testDir )
        throws IOException
    {
        File testPomBasedir = new File( getBasedir(), "target/test/unit/" + testDir );

        // Using unit test dir
        FileUtils.copyDirectoryStructure( new File( getBasedir(), "src/test/resources/unit/" + testDir ),
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
