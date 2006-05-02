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
import org.apache.maven.scm.command.tag.TagScmResult;
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
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.matcher.TestFailureMatcher;
import org.jmock.core.stub.ReturnStub;
import org.jmock.core.stub.ThrowStub;

import java.util.Collections;

/**
 * Test the SCM tag phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmTagPhaseTest
    extends AbstractReleaseTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "scm-tag" );
    }

    public void testTag()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", false );
        config.setUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) config.getReactorProjects().get( 0 );
        config.setWorkingDirectory( rootProject.getFile().getParentFile() );
        config.setReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments =
            new Constraint[]{new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsEqual( "release-label" )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "tag" ).with( arguments ).will(
            new ReturnStub( new TagScmResult( "...", Collections.singletonList( rootProject.getFile() ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.execute( config );

        assertTrue( true );
    }

    public void testCommitMultiModule()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "multiple-poms", false );
        config.setUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) config.getReactorProjects().get( 0 );
        config.setWorkingDirectory( rootProject.getFile().getParentFile() );
        config.setReleaseLabel( "release-label" );

        ScmFileSet fileSet = new ScmFileSet( rootProject.getFile().getParentFile() );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments =
            new Constraint[]{new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsEqual( "release-label" )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "tag" ).with( arguments ).will(
            new ReturnStub( new TagScmResult( "...", Collections.singletonList( rootProject.getFile() ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.execute( config );

        assertTrue( true );
    }

    public void testTagNoReleaseLabel()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", false );

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

    public void testSimulateTag()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", false );
        config.setUrl( "scm-url" );
        MavenProject rootProject = (MavenProject) config.getReactorProjects().get( 0 );
        config.setWorkingDirectory( rootProject.getFile().getParentFile() );
        config.setReleaseLabel( "release-label" );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new TestFailureMatcher( "Shouldn't have called tag" ) ).method( "tag" );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.simulate( config );

        assertTrue( true );
    }

    public void testSimulateTagNoReleaseLabel()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", false );

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
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "tag" ).will(
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

    public void testScmResultFailure()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        ScmManager scmManager = (ScmManager) lookup( ScmManager.ROLE );
        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl( releaseConfiguration.getUrl() );

        providerStub.setTagScmResult( new TagScmResult( "", "", "", false ) );

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

    private ReleaseConfiguration createReleaseConfiguration()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "scm-commit/", "single-pom", false );
        config.setUrl( "scm-url" );
        config.setReleaseLabel( "release-label" );
        config.setWorkingDirectory( getTestFile( "target/test/checkout" ) );
        return config;
    }

}
