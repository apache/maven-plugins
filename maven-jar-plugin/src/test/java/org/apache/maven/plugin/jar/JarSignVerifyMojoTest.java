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

import org.apache.maven.plugin.MojoExecutionException;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.StreamConsumer;

import junit.framework.TestCase;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * These unit tests only check whether the generated command lines are correct.
 * Really running the command would mean checking the results, which is too painful and not really a unit test.
 * It would probably require to 'jarsigner -verify' the resulting signed jar and I believe it would make the code
 * too complex with very few benefits.
 *
 * @author Jerome Lacoste <jerome@coffeebreaks.org>
 * @version $Id$
 */
public class JarSignVerifyMojoTest
    extends TestCase
{
    private MockJarSignVerifyMojo mojo;

    static class MockJarSignVerifyMojo
        extends JarSignVerifyMojo
    {
        public int executeResult;

        public List commandLines = new ArrayList();

        public String failureMsg;

        public Map systemProperties = new HashMap();

        public String lastOutLine;

        protected int executeCommandLine( Commandline commandLine, InputStream inputStream, StreamConsumer systemOut,
                                          StreamConsumer systemErr )
            throws CommandLineException
        {
            commandLines.add( commandLine );
            if ( failureMsg != null )
            {
                throw new CommandLineException( failureMsg );
            }
            if ( lastOutLine != null )
            {
                systemOut.consumeLine( lastOutLine );
            }
            return executeResult;
        }

        protected String getSystemProperty( String key )
        {
            return (String) systemProperties.get( key );
        }
    }


    public void setUp()
        throws IOException
    {
        mojo = new MockJarSignVerifyMojo();
        mojo.executeResult = 0;
        // it doesn't really matter if the paths are not cross-platform, we don't execute the command lines anyway
        File basedir = new File( System.getProperty( "java.io.tmpdir" ) );
        mojo.setBasedir( basedir );
        mojo.setWorkingDir( basedir );
        mojo.setJarPath( new File( "/tmp/signed/file-version.jar" ) );
    }

    public void tearDown()
    {
        mojo = null;
    }

    public void testPleaseMaven()
    {
        assertTrue( true );
    }

    /**
     */
    public void testRunOK()
        throws MojoExecutionException
    {
        mojo.lastOutLine = "jar verified.";

        mojo.execute();

        String[] expectedArguments = {"-verify", "/tmp/signed/file-version.jar"};

        checkMojo( expectedArguments );
    }

    /**
     */
    public void testRunOKAllArguments()
        throws MojoExecutionException
    {
        mojo.lastOutLine = "jar verified.";

        mojo.setVerbose( true );
        mojo.setCheckCerts( true );

        mojo.execute();

        String[] expectedArguments = {"-verify", "-verbose", "-certs", "/tmp/signed/file-version.jar"};

        checkMojo( expectedArguments );
    }

    /**
     */
    public void testRunFailureNeverHappens()
    {
        mojo.executeResult = 1;

        try
        {
            mojo.execute();
            fail( "expected failure" );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( e.getMessage().startsWith( "Result of " ) );
        }

        String[] expectedArguments = {"-verify", "/tmp/signed/file-version.jar"};

        checkMojo( expectedArguments );
    }

    /**
     */
    public void testRunFailureVerifyFailed()
    {
        mojo.lastOutLine = "jar is unsigned.";

        try
        {
            mojo.execute();
            fail( "expected failure" );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( e.getMessage().startsWith( "Verify failed: jar is unsigned." ) );
        }

        String[] expectedArguments = {"-verify", "/tmp/signed/file-version.jar"};

        checkMojo( expectedArguments );
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

        String[] expectedArguments = {"-verify", "/tmp/signed/file-version.jar"};

        checkMojo( expectedArguments );
    }

    private void checkMojo( String[] expectedCommandLineArguments )
    {
        checkMojo( mojo, expectedCommandLineArguments );
    }

    static void checkMojo( MockJarSignVerifyMojo mojo, String[] expectedCommandLineArguments )
    {
        assertEquals( 1, mojo.commandLines.size() );
        Commandline commandline = (Commandline) mojo.commandLines.get( 0 );
        String[] arguments = commandline.getArguments();
        // isn't there an assertEquals for arrays?
        /*
        for (int i = 0; i < arguments.length; i++ ) {
            System.out.println( arguments[ i ] );
        }
        */
        assertEquals( "Differing number of arguments", expectedCommandLineArguments.length, arguments.length );
        for ( int i = 0; i < arguments.length; i++ )
        {
            expectedCommandLineArguments[i] =
                StringUtils.replace( expectedCommandLineArguments[i], "/", File.separator );
            assertEquals( expectedCommandLineArguments[i], expectedCommandLineArguments[i], arguments[i] );
        }
    }
}
