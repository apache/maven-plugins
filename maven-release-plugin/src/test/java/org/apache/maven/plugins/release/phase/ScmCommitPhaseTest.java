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
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
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
import org.jmock.cglib.Mock;
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
import java.util.Arrays;
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
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", true );
        config.setUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) config.getReactorProjects().get( 0 );
        config.setWorkingDirectory( rootProject.getFile().getParentFile() );
        config.setReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), rootProject.getFile() );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments = new Constraint[]{new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsNull(),
            new IsEqual( PREFIX + "release-label" )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkIn" ).with( arguments ).will(
            new ReturnStub( new CheckInScmResult( "...", Collections.singletonList( rootProject.getFile() ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.execute( config );

        assertTrue( true );
    }

    public void testCommitMultiModule()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "multiple-poms", true );
        config.setUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) config.getReactorProjects().get( 0 );
        config.setWorkingDirectory( rootProject.getFile().getParentFile() );
        config.setReleaseLabel( "release-label" );

        List poms = new ArrayList();
        for ( Iterator i = config.getReactorProjects().iterator(); i.hasNext(); )
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

        phase.execute( config );

        assertTrue( true );
    }

    public void testCommitDevelopment()
        throws Exception
    {
        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "scm-commit-development" );

        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", true );
        config.setUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) config.getReactorProjects().get( 0 );
        config.setWorkingDirectory( rootProject.getFile().getParentFile() );
        config.setReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile(), rootProject.getFile() );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments = new Constraint[]{new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsNull(),
            new IsEqual( "[maven-release-plugin] prepare for next development iteration" )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkIn" ).with( arguments ).will(
            new ReturnStub( new CheckInScmResult( "...", Collections.singletonList( rootProject.getFile() ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.execute( config );

        assertTrue( true );
    }

    public void testCommitNoReleaseLabel()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", true );

        try
        {
            phase.execute( config );
            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testSimulateCommit()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", true );
        config.setUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) config.getReactorProjects().get( 0 );
        config.setWorkingDirectory( rootProject.getFile().getParentFile() );
        config.setReleaseLabel( "release-label" );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new TestFailureMatcher( "Shouldn't have called checkIn" ) ).method( "checkIn" );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.simulate( config );

        assertTrue( true );
    }

    public void testSimulateCommitNoReleaseLabel()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", true );

        try
        {
            phase.simulate( config );
            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testNoSuchScmProviderExceptionThrown()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( "scm-url" ) ).will( new ThrowStub( new NoSuchScmProviderException( "..." ) ) );

        ScmManager scmManager = (ScmManager) scmManagerMock.proxy();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );

        try
        {
            phase.execute( releaseConfiguration );

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
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( "scm-url" ) ).will( new ThrowStub( new ScmRepositoryException( "..." ) ) );

        ScmManager scmManager = (ScmManager) scmManagerMock.proxy();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );

        try
        {
            phase.execute( releaseConfiguration );

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
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkIn" ).will(
            new ThrowStub( new ScmException( "..." ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", ScmException.class, e.getCause().getClass() );
        }
    }

    private ReleaseConfiguration createReleaseConfiguration()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", false );
        config.setUrl( "scm-url" );
        config.setReleaseLabel( "release-label" );
        config.setWorkingDirectory( getTestFile( "target/test/checkout" ) );
        return config;
    }

    public void testScmResultFailure()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        ScmManager scmManager = (ScmManager) lookup( ScmManager.ROLE );
        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl( releaseConfiguration.getUrl() );

        providerStub.setCheckInScmResult( new CheckInScmResult( "", "", "", false ) );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Commit should have failed" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }
    }

    private static class IsScmFileSetEquals
        implements Constraint
    {
        private final ScmFileSet fileSet;

        IsScmFileSetEquals( ScmFileSet fileSet )
        {
            this.fileSet = fileSet;
        }

        public boolean eval( Object object )
        {
            ScmFileSet fs = (ScmFileSet) object;

            return fs.getBasedir().equals( fileSet.getBasedir() ) &&
                Arrays.asList( fs.getFiles() ).equals( Arrays.asList( fileSet.getFiles() ) );
        }

        public StringBuffer describeTo( StringBuffer stringBuffer )
        {
            return stringBuffer.append( fileSet.toString() );
        }
    }
}
