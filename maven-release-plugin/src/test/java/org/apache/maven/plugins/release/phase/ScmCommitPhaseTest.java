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
import org.apache.maven.plugins.release.ReleaseFailureException;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.scm.DefaultScmRepositoryConfigurator;
import org.apache.maven.plugins.release.scm.ReleaseScmCommandException;
import org.apache.maven.plugins.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.plugins.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.jmock.Mock;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsAnything;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.constraint.IsNull;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.matcher.TestFailureMatcher;
import org.jmock.core.stub.ReturnStub;
import org.jmock.core.stub.ThrowStub;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Test the SCM commit phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmCommitPhaseTest
    extends AbstractReleaseTestCase
{
    private static final String PREFIX = "[maven-release-plugin] prepare release ";

    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "scm-commit-release" );
    }

    public void testCommit()
        throws Exception
    {
        List reactorProjects = createReactorProjects();
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        descriptor.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) reactorProjects.get( 0 );
        descriptor.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), rootProject.getFile() );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments = new Constraint[]{new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsNull(),
            new IsEqual( PREFIX + "release-label" )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkIn" ).with( arguments ).will(
            new ReturnStub( new CheckInScmResult( "...", Collections.singletonList( rootProject.getFile() ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.execute( descriptor, null, reactorProjects );

        assertTrue( true );
    }

    public void testCommitMultiModule()
        throws Exception
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List reactorProjects = createReactorProjects( "scm-commit/", "multiple-poms", false );
        descriptor.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) reactorProjects.get( 0 );
        descriptor.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );

        List poms = new ArrayList();
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();
            poms.add( project.getFile() );
        }
        ScmFileSet fileSet =
            new ScmFileSet( rootProject.getFile().getParentFile(), (File[]) poms.toArray( EMPTY_FILE_ARRAY ) );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments = new Constraint[]{new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsNull(),
            new IsEqual( PREFIX + "release-label" )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkIn" ).with( arguments ).will(
            new ReturnStub( new CheckInScmResult( "...", Collections.singletonList( rootProject.getFile() ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.execute( descriptor, null, reactorProjects );

        assertTrue( true );
    }

    public void testCommitDevelopment()
        throws Exception
    {
        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "scm-commit-development" );

        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List reactorProjects = createReactorProjects();
        descriptor.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) reactorProjects.get( 0 );
        descriptor.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), rootProject.getFile() );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments = new Constraint[]{new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsNull(),
            new IsEqual( "[maven-release-plugin] prepare for next development iteration" )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkIn" ).with( arguments ).will(
            new ReturnStub( new CheckInScmResult( "...", Collections.singletonList( rootProject.getFile() ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.execute( descriptor, null, reactorProjects );

        assertTrue( true );
    }

    public void testCommitNoReleaseLabel()
        throws Exception
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List reactorProjects = createReactorProjects();

        try
        {
            phase.execute( descriptor, null, reactorProjects );
            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSimulateCommit()
        throws Exception
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List reactorProjects = createReactorProjects();
        descriptor.setScmSourceUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) reactorProjects.get( 0 );
        descriptor.setWorkingDirectory( rootProject.getFile().getParentFile().getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new TestFailureMatcher( "Shouldn't have called checkIn" ) ).method( "checkIn" );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.simulate( descriptor, null, reactorProjects );

        assertTrue( true );
    }

    public void testSimulateCommitNoReleaseLabel()
        throws Exception
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        List reactorProjects = createReactorProjects();

        try
        {
            phase.simulate( descriptor, null, reactorProjects );
            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testNoSuchScmProviderExceptionThrown()
        throws Exception
    {
        List reactorProjects = createReactorProjects();
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( "scm-url" ) ).will( new ThrowStub( new NoSuchScmProviderException( "..." ) ) );

        ScmManager scmManager = (ScmManager) scmManagerMock.proxy();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
    }

    public void testScmRepositoryExceptionThrown()
        throws Exception
    {
        List reactorProjects = createReactorProjects();
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( "scm-url" ) ).will( new ThrowStub( new ScmRepositoryException( "..." ) ) );

        ScmManager scmManager = (ScmManager) scmManagerMock.proxy();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }
    }

    public void testScmExceptionThrown()
        throws Exception
    {
        List reactorProjects = createReactorProjects();
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkIn" ).will(
            new ThrowStub( new ScmException( "..." ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", ScmException.class, e.getCause().getClass() );
        }
    }

    public void testScmResultFailure()
        throws Exception
    {
        List reactorProjects = createReactorProjects();
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        ScmManager scmManager = (ScmManager) lookup( ScmManager.ROLE );
        ScmProviderStub providerStub =
            (ScmProviderStub) scmManager.getProviderByUrl( releaseDescriptor.getScmSourceUrl() );

        providerStub.setCheckInScmResult( new CheckInScmResult( "", "", "", false ) );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Commit should have failed" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }
    }

    private List createReactorProjects()
        throws Exception
    {
        return createReactorProjects( "scm-commit/", "single-pom", false );
    }

    private static ReleaseDescriptor createReleaseDescriptor()
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        descriptor.setScmSourceUrl( "scm-url" );
        descriptor.setScmReleaseLabel( "release-label" );
        descriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );
        return descriptor;
    }

}
