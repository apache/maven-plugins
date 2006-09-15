package org.apache.maven.plugins.release.phase;

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

import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.exec.MavenExecutor;
import org.apache.maven.plugins.release.exec.MavenExecutorException;
import org.codehaus.plexus.PlexusTestCase;
import org.jmock.Mock;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.constraint.IsNull;
import org.jmock.core.constraint.IsAnything;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.matcher.TestFailureMatcher;
import org.jmock.core.stub.ThrowStub;
import org.jmock.core.Constraint;

import java.io.File;

/**
 * Test the simple test running phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RunGoalsPhaseTest
    extends PlexusTestCase
{
    private RunGoalsPhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (RunGoalsPhase) lookup( ReleasePhase.ROLE, "run-preparation-goals" );
    }

    public void testExecute()
        throws ReleaseExecutionException
    {
        File testFile = getTestFile( "target/working-directory" );

        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setPreparationGoals( "clean integration-test" );
        config.setWorkingDirectory( testFile.getAbsolutePath() );

        Mock mock = new Mock( MavenExecutor.class );
        Constraint[] constraints = new Constraint[]{ new IsEqual( testFile ),
                                                     new IsEqual( "clean integration-test" ),
                                                     new IsEqual( Boolean.TRUE ),
                                                     new IsNull(),
                                                     new IsAnything() };

        mock.expects( new InvokeOnceMatcher() ).method( "executeGoals" ).with( constraints );

        phase.setMavenExecutor( (MavenExecutor) mock.proxy() );

        phase.execute( config, null, null );

        // just needs to survive the mock
        assertTrue( true );
    }

    public void testSimulate()
        throws ReleaseExecutionException
    {
        File testFile = getTestFile( "target/working-directory" );

        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setPreparationGoals( "clean integration-test" );
        config.setWorkingDirectory( testFile.getAbsolutePath() );

        Mock mock = new Mock( MavenExecutor.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( testFile ),
                                                    new IsEqual( "clean integration-test" ),
                                                    new IsEqual( Boolean.TRUE ),
                                                    new IsNull(),
                                                    new IsAnything() };
        mock.expects( new InvokeOnceMatcher() ).method( "executeGoals" ).with( constraints );

        phase.setMavenExecutor( (MavenExecutor) mock.proxy() );

        phase.simulate( config, null, null );

        // just needs to survive the mock
        assertTrue( true );
    }

    public void testExecuteException()
    {
        File testFile = getTestFile( "target/working-directory" );

        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setPreparationGoals( "clean integration-test" );
        config.setWorkingDirectory( testFile.getAbsolutePath() );

        Mock mock = new Mock( MavenExecutor.class );
        Constraint[] constraints = new Constraint[]{ new IsEqual( testFile ),
                                                       new IsEqual( "clean integration-test" ),
                                                       new IsEqual( Boolean.TRUE ),
                                                       new IsNull(),
                                                       new IsAnything() };
        mock.expects( new InvokeOnceMatcher() ).method( "executeGoals" ).with( constraints ).will(
            new ThrowStub( new MavenExecutorException( "...", new Exception() ) ) );

        phase.setMavenExecutor( (MavenExecutor) mock.proxy() );

        try
        {
            phase.execute( config, null, null );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", MavenExecutorException.class, e.getCause().getClass() );
        }
    }

    public void testSimulateException()
    {
        File testFile = getTestFile( "target/working-directory" );

        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setPreparationGoals( "clean integration-test" );
        config.setWorkingDirectory( testFile.getAbsolutePath() );

        Mock mock = new Mock( MavenExecutor.class );
        Constraint[] constraints = new Constraint[]{ new IsEqual( testFile ),
                                                     new IsEqual( "clean integration-test" ),
                                                     new IsEqual( Boolean.TRUE ),
                                                     new IsNull(),
                                                     new IsAnything() };
        mock.expects( new InvokeOnceMatcher() ).method( "executeGoals" ).with( constraints ).will(
            new ThrowStub( new MavenExecutorException( "...", new Exception() ) ) );

        phase.setMavenExecutor( (MavenExecutor) mock.proxy() );

        try
        {
            phase.simulate( config, null, null );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", MavenExecutorException.class, e.getCause().getClass() );
        }
    }

    public void testEmptyGoals()
        throws Exception
    {
        File testFile = getTestFile( "target/working-directory" );

        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setPreparationGoals( "" );
        config.setWorkingDirectory( testFile.getAbsolutePath() );

        Mock mock = new Mock( MavenExecutor.class );
        mock.expects( new TestFailureMatcher( "Shouldn't invoke executeGoals" ) ).method( "executeGoals" );

        phase.setMavenExecutor( (MavenExecutor) mock.proxy() );

        phase.execute( config, null, null );

        // just needs to survive the mock
        assertTrue( true );
    }
}
