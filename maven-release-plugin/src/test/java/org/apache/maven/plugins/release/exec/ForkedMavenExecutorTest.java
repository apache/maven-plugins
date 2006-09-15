package org.apache.maven.plugins.release.exec;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.jmock.cglib.Mock;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ReturnStub;
import org.jmock.core.stub.ThrowStub;
import org.apache.maven.plugins.release.ReleaseResult;

import java.io.File;

/**
 * Test the forked Maven executor.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ForkedMavenExecutorTest
    extends PlexusTestCase
{
    private ForkedMavenExecutor executor;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        executor = (ForkedMavenExecutor) lookup( MavenExecutor.ROLE );
    }

    public void testExecution()
        throws MavenExecutorException
    {
        File workingDirectory = getTestFile( "target/working-directory" );

        Process process = createMockProcess( 0 );

        Mock commandLineMock = createMockCommandLine( workingDirectory, process );
        expectDefaultArguments( commandLineMock );

        Mock mock = new Mock( CommandLineFactory.class );

        mock.expects( new InvokeOnceMatcher() ).method( "createCommandLine" ).with( new IsEqual( "mvn" ) ).will(
            new ReturnStub( commandLineMock.proxy() ) );

        executor.setCommandLineFactory( (CommandLineFactory) mock.proxy() );

        executor.executeGoals( workingDirectory, "clean integration-test", false, null, new ReleaseResult() );

        assertTrue( true );
    }

    public void testExecutionWithCustomPomFile()
        throws MavenExecutorException
    {
        File workingDirectory = getTestFile( "target/working-directory" );

        Process process = createMockProcess( 0 );

        Mock commandLineMock = createMockCommandLine( workingDirectory, process );
        expectDefaultArguments( commandLineMock );

        String arguments = "-f my-pom.xml";
        commandLineMock.expects( new InvokeOnceMatcher() ).method( "createArgument" ).will(
            new ReturnStub( createArgumentLineMock( arguments ) ) );

        Mock mock = new Mock( CommandLineFactory.class );

        mock.expects( new InvokeOnceMatcher() ).method( "createCommandLine" ).with( new IsEqual( "mvn" ) ).will(
            new ReturnStub( commandLineMock.proxy() ) );

        executor.setCommandLineFactory( (CommandLineFactory) mock.proxy() );

        executor.executeGoals( workingDirectory, "clean integration-test", false, null, "my-pom.xml", new ReleaseResult() );

        assertTrue( true );
    }

    public void testExecutionWithArguments()
        throws MavenExecutorException
    {
        File workingDirectory = getTestFile( "target/working-directory" );

        Process process = createMockProcess( 0 );

        Mock commandLineMock = createMockCommandLine( workingDirectory, process );
        String arguments = "-DperformRelease=true -Dmaven.test.skip=true";
        commandLineMock.expects( new InvokeOnceMatcher() ).method( "createArgument" ).will(
            new ReturnStub( createArgumentLineMock( arguments ) ) );

        expectDefaultArguments( commandLineMock );

        Mock mock = new Mock( CommandLineFactory.class );

        mock.expects( new InvokeOnceMatcher() ).method( "createCommandLine" ).with( new IsEqual( "mvn" ) ).will(
            new ReturnStub( commandLineMock.proxy() ) );

        executor.setCommandLineFactory( (CommandLineFactory) mock.proxy() );

        executor.executeGoals( workingDirectory, "clean integration-test", false, arguments, null );

        assertTrue( true );
    }

    public void testExecutionWithNonZeroExitCode()
        throws MavenExecutorException
    {
        File workingDirectory = getTestFile( "target/working-directory" );

        Process process = createMockProcess( 1 );

        Mock commandLineMock = createMockCommandLine( workingDirectory, process );

        expectDefaultArguments( commandLineMock );

        Mock mock = new Mock( CommandLineFactory.class );

        mock.expects( new InvokeOnceMatcher() ).method( "createCommandLine" ).with( new IsEqual( "mvn" ) ).will(
            new ReturnStub( commandLineMock.proxy() ) );

        executor.setCommandLineFactory( (CommandLineFactory) mock.proxy() );

        try
        {
            executor.executeGoals( workingDirectory, "clean integration-test", false, null, new ReleaseResult() );

            fail( "Should have thrown an exception" );
        }
        catch ( MavenExecutorException e )
        {
            assertEquals( "Check exit code", 1, e.getExitCode() );
        }
    }

    public void testExecutionWithCommandLineException()
        throws MavenExecutorException
    {
        File workingDirectory = getTestFile( "target/working-directory" );

        Mock commandLineMock = new Mock( Commandline.class );
        commandLineMock.expects( new InvokeOnceMatcher() ).method( "setWorkingDirectory" ).with(
            new IsEqual( workingDirectory.getAbsolutePath() ) );
        commandLineMock.expects( new InvokeOnceMatcher() ).method( "addEnvironment" ).with(
            new IsEqual( "MAVEN_TERMINATE_CMD" ), new IsEqual( "on" ) );
        commandLineMock.expects( new InvokeOnceMatcher() ).method( "execute" ).will(
            new ThrowStub( new CommandLineException( "..." ) ) );

        expectDefaultArguments( commandLineMock );

        Mock mock = new Mock( CommandLineFactory.class );

        mock.expects( new InvokeOnceMatcher() ).method( "createCommandLine" ).with( new IsEqual( "mvn" ) ).will(
            new ReturnStub( commandLineMock.proxy() ) );

        executor.setCommandLineFactory( (CommandLineFactory) mock.proxy() );

        try
        {
            executor.executeGoals( workingDirectory, "clean integration-test", false, null, new ReleaseResult() );

            fail( "Should have thrown an exception" );
        }
        catch ( MavenExecutorException e )
        {
            assertEquals( "Check cause", CommandLineException.class, e.getCause().getClass() );
        }
    }

    private static void expectDefaultArguments( Mock commandLineMock )
    {
        String[] args = new String[]{"clean", "integration-test", "--no-plugin-updates", "--batch-mode"};
        for ( int i = args.length - 1; i >= 0; i-- )
        {
            commandLineMock.expects( new InvokeOnceMatcher() ).method( "createArgument" ).will(
                new ReturnStub( createArgumentValueMock( args[i] ) ) );
        }
    }

    private static Mock createMockCommandLine( File workingDirectory, Process process )
    {
        Mock commandLineMock = new Mock( Commandline.class );
        commandLineMock.expects( new InvokeOnceMatcher() ).method( "setWorkingDirectory" ).with(
            new IsEqual( workingDirectory.getAbsolutePath() ) );
        commandLineMock.expects( new InvokeOnceMatcher() ).method( "addEnvironment" ).with(
            new IsEqual( "MAVEN_TERMINATE_CMD" ), new IsEqual( "on" ) );
        commandLineMock.expects( new InvokeOnceMatcher() ).method( "execute" ).will( new ReturnStub( process ) );

        return commandLineMock;
    }

    private static Commandline.Argument createArgumentValueMock( String value )
    {
        Mock mock = new Mock( Commandline.Argument.class );
        mock.expects( new InvokeOnceMatcher() ).method( "setValue" ).with( new IsEqual( value ) );
        return (Commandline.Argument) mock.proxy();
    }

    private static Commandline.Argument createArgumentLineMock( String value )
    {
        Mock mock = new Mock( Commandline.Argument.class );
        mock.expects( new InvokeOnceMatcher() ).method( "setLine" ).with( new IsEqual( value ) );
        return (Commandline.Argument) mock.proxy();
    }

    private static Process createMockProcess( int exitCode )
    {
        Mock mockProcess = new Mock( Process.class );
        mockProcess.expects( new InvokeOnceMatcher() ).method( "getInputStream" ).will(
            new ReturnStub( new StringInputStream( "" ) ) );
        mockProcess.expects( new InvokeOnceMatcher() ).method( "getErrorStream" ).will(
            new ReturnStub( new StringInputStream( "" ) ) );
        mockProcess.expects( new InvokeOnceMatcher() ).method( "waitFor" ).will(
            new ReturnStub( new Integer( exitCode ) ) );
        if ( exitCode != 0 )
        {
            mockProcess.expects( new InvokeOnceMatcher() ).method( "exitValue" ).will(
                new ReturnStub( new Integer( exitCode ) ) );
        }
        return (Process) mockProcess.proxy();
    }
}
