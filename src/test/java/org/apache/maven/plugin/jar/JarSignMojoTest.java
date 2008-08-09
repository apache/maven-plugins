package org.apache.maven.plugin.jar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * These unit tests only check whether the generated command lines are correct.
 * Really running the command would mean checking the results, which is too painful and not really a unit test.
 * It would probably require to 'jarsigner -verify' the resulting signed jar and I believe it would make the code
 * too complex with very few benefits.
 *
 * @author Jerome Lacoste <jerome@coffeebreaks.org>
 * @version $Id$
 */
public class JarSignMojoTest
    extends TestCase
{
    private MockJarSignMojo mojo;

    static class MockJarSignMojo
        extends JarSignMojo
    {
        public int executeResult;

        public List commandLines = new ArrayList();

        public String failureMsg;

        public Map systemProperties = new HashMap();

        public JarSignVerifyMojo verifyMojo = new JarSignVerifyMojo();

        protected int executeCommandLine( Commandline commandLine, InputStream inputStream, StreamConsumer stream1,
                                          StreamConsumer stream2 )
            throws CommandLineException
        {
            commandLines.add( commandLine );
            if ( failureMsg != null )
            {
                throw new CommandLineException( failureMsg );
            }
            return executeResult;
        }

        protected JarSignVerifyMojo createJarSignVerifyMojo()
        {
            return verifyMojo;
        }

        protected String getSystemProperty( String key )
        {
            return (String) systemProperties.get( key );
        }
    }

    public void setUp()
        throws IOException
    {
        mojo = new MockJarSignMojo();
        mojo.executeResult = 0;
        // it doesn't really matter if the paths are not cross-platform, we don't execute the command lines anyway
        File basedir = new File( System.getProperty( "java.io.tmpdir" ) );
        mojo.setBasedir( basedir );
        mojo.setWorkingDir( basedir );
        mojo.setSignedJar( new File( getDummySignedJarPath() ) );
        mojo.setAlias( "alias" );
        mojo.setKeystore( "/tmp/keystore" );
        mojo.setKeypass( "secretpassword" );
        MavenProject project = new MavenProject( new Model() );
        MockArtifact mockArtifact = new MockArtifact();
        mockArtifact.setGroupId( "test" );
        mockArtifact.setArtifactId( "test" );
        mockArtifact.setVersion( "1.0" );
        mockArtifact.setType( "jar" );
        project.setArtifact( mockArtifact );
        mojo.setProject( project );

        new File( getDummyNonSignedJarPath() ).delete();
    }

    public void tearDown()
    {
        mojo = null;
    }

    /**
     */
    public void testRunOK()
        throws MojoExecutionException
    {
        JarSignVerifyMojoTest.MockJarSignVerifyMojo mockJarSignVerifyMojo =
            new JarSignVerifyMojoTest.MockJarSignVerifyMojo();
        mojo.verifyMojo = mockJarSignVerifyMojo;

        mojo.execute();

        String[] expectedArguments = {"-keystore", "/tmp/keystore", "-keypass", "secretpassword", "-signedjar",
            getDummySignedJarPath(), getDummyNonSignedJarPath(), "alias"};

        checkMojo( expectedArguments );

        assertEquals( "sign operation wasn't verified", 0, mockJarSignVerifyMojo.commandLines.size() );
    }

    /**
     */
    public void testVerifyJarGeneratedBySignOperation()
        throws MojoExecutionException
    {
        JarSignVerifyMojoTest.MockJarSignVerifyMojo mockJarSignVerifyMojo =
            new JarSignVerifyMojoTest.MockJarSignVerifyMojo();
        mojo.verifyMojo = mockJarSignVerifyMojo;
        mojo.setVerify( true );
        mockJarSignVerifyMojo.lastOutLine = "jar verified.";

        mojo.execute();

        String[] expectedArguments = {"-keystore", "/tmp/keystore", "-keypass", "secretpassword", "-signedjar",
            getDummySignedJarPath(), getDummyNonSignedJarPath(), "alias"};

        checkMojo( expectedArguments );

        String[] expectedVerifyArguments = {"-verify", getDummySignedJarPath()};

        JarSignVerifyMojoTest.checkMojo( mockJarSignVerifyMojo, expectedVerifyArguments );
    }

    /**
     */
    public void testVerifyInPlaceSignedJar()
        throws MojoExecutionException
    {
        JarSignVerifyMojoTest.MockJarSignVerifyMojo mockJarSignVerifyMojo =
            new JarSignVerifyMojoTest.MockJarSignVerifyMojo();
        mojo.verifyMojo = mockJarSignVerifyMojo;
        mojo.setSignedJar( null );
        mojo.setVerify( true );
        mockJarSignVerifyMojo.lastOutLine = "jar verified.";

        mojo.execute();

        String[] expectedArguments =
            {"-keystore", "/tmp/keystore", "-keypass", "secretpassword", getDummyNonSignedJarPath(), "alias"};

        checkMojo( expectedArguments );

        String[] expectedVerifyArguments = {"-verify", getDummyNonSignedJarPath()};

        JarSignVerifyMojoTest.checkMojo( mockJarSignVerifyMojo, expectedVerifyArguments );
    }

    /**
     * We shouldn't sign the jar twice.
     * On the second run, we simulated a created and signed jar.
     */
    public void testRunTwice()
        throws MojoExecutionException, IOException
    {
        mojo.execute();

        class MyJarSignVerifyMojo
            extends JarSignVerifyMojo
        {
            int nbExecutions;

            public void execute()
                throws MojoExecutionException
            {
                nbExecutions++;
            }

            public boolean isSigned()
            {
                return true;
            }
        }

        mojo.verifyMojo = new MyJarSignVerifyMojo();

        new File( getDummyNonSignedJarPath() ).createNewFile();

        mojo.execute();

        String[] expectedArguments = {"-keystore", "/tmp/keystore", "-keypass", "secretpassword", "-signedjar",
            getDummySignedJarPath(), getDummyNonSignedJarPath(), "alias"};

        checkMojo( expectedArguments );
    }

    /**
     */
    public void testRunFailure()
    {
        JarSignVerifyMojoTest.MockJarSignVerifyMojo mockJarSignVerifyMojo =
            new JarSignVerifyMojoTest.MockJarSignVerifyMojo();
        mojo.verifyMojo = mockJarSignVerifyMojo;

        mojo.executeResult = 1;

        // any missing argument should produce this. Let's simulate a missing alias
        mojo.setAlias( null );

        try
        {
            mojo.execute();
            fail( "expected failure" );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( e.getMessage().startsWith( "Result of " ) );
        }

        String[] expectedArguments = {"-keystore", "/tmp/keystore", "-keypass", "secretpassword", "-signedjar",
            getDummySignedJarPath(), getDummyNonSignedJarPath()};

        checkMojo( expectedArguments );

        assertEquals( "sign operation wasn't verified", 0, mockJarSignVerifyMojo.commandLines.size() );
    }

    private String getDummySignedJarPath()
    {
        return new File(System.getProperty( "java.io.tmpdir" ), "signed.jar").getAbsolutePath();
    }

    private String getDummyNonSignedJarPath()
    {
        return new File(System.getProperty( "java.io.tmpdir" ), "null.jar").getAbsolutePath();
    }

    /**
     */
    public void testRunError()
    {
        mojo.failureMsg = "simulated failure";

        try
        {
            mojo.execute();
            fail( "expected failure" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "command execution failed", e.getMessage() );
        }

        String[] expectedArguments = {"-keystore", "/tmp/keystore", "-keypass", "secretpassword", "-signedjar",
            getDummySignedJarPath(), getDummyNonSignedJarPath(), "alias"};

        checkMojo( expectedArguments );
    }

    private void checkMojo( String[] expectedCommandLineArguments )
    {
        assertEquals( 1, mojo.commandLines.size() );
        Commandline commandline = (Commandline) mojo.commandLines.get( 0 );
        String[] arguments = commandline.getArguments();

        assertEquals( "Differing number of arguments", expectedCommandLineArguments.length, arguments.length );
        for ( int i = 0; i < arguments.length; i++ )
        {
            assertEquals( expectedCommandLineArguments[i].replace( '\\', '/' ), arguments[i].replace( '\\', '/' ) );
        }
    }
}
