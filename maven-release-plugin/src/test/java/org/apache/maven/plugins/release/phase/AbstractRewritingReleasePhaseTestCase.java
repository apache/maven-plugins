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
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.util.FileUtils;
import org.jmock.Mock;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeAtLeastOnceMatcher;
import org.jmock.core.matcher.TestFailureMatcher;
import org.jmock.core.stub.ThrowStub;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class with tests for rewriting POMs.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractRewritingReleasePhaseTestCase
    extends AbstractReleaseTestCase
{
    public void testRewriteBasicPom()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromBasicPom();
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewritePomWithParent()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationForPomWithParentAlternateNextVersion( "pom-with-parent" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewritePomWithUnmappedParent()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromProjects( "pom-with-parent" );

        // Process the child first
        List reactorProjects = new ArrayList( getReactorProjects() );
        Collections.reverse( reactorProjects );

        mapAlternateNextVersion( config, "groupId:subproject1" );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomWithReleasedParent()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromProjects( "pom-with-released-parent" );

        mapAlternateNextVersion( config, "groupId:subproject1" );
        config.mapReleaseVersion( "groupId:artifactId", "1" );
        config.mapDevelopmentVersion( "groupId:artifactId", "1" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    protected abstract void mapAlternateNextVersion( ReleaseDescriptor config, String projectId );

    public void testRewritePomWithInheritedVersion()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( "pom-with-inherited-version" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewritePomWithChangedInheritedVersion()
        throws Exception
    {
        ReleaseDescriptor config =
            createConfigurationForPomWithParentAlternateNextVersion( "pom-with-inherited-version" );

        phase.execute( config, null, getReactorProjects() );

        MavenProject project = (MavenProject) getProjectsAsMap( getReactorProjects() ).get( "groupId:subproject1" );

        String actual = FileUtils.fileRead( project.getFile() );
        String expected =
            FileUtils.fileRead( new File( project.getFile().getParentFile(), "expected-pom-version-changed.xml" ) );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    protected abstract ReleaseDescriptor createConfigurationForPomWithParentAlternateNextVersion( String path )
        throws Exception;

    public void testRewritePomDependencies()
        throws Exception
    {
        ReleaseDescriptor config = createDefaultConfiguration( "internal-snapshot-dependencies" );
        mapNextVersion( config, "groupId:subsubproject" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewritePomUnmappedDependencies()
        throws Exception
    {
        ReleaseDescriptor config = createUnmappedConfiguration( "internal-snapshot-dependencies" );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomDependenciesDifferentVersion()
        throws Exception
    {
        ReleaseDescriptor config = createDifferingVersionConfiguration( "internal-differing-snapshot-dependencies" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewriteManagedPomDependencies()
        throws Exception
    {
        ReleaseDescriptor config = createMappedConfiguration( "internal-managed-snapshot-dependency" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewriteManagedPomUnmappedDependencies()
        throws Exception
    {
        ReleaseDescriptor config = createUnmappedConfiguration( "internal-managed-snapshot-dependency" );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomPlugins()
        throws Exception
    {
        ReleaseDescriptor config = createDefaultConfiguration( "internal-snapshot-plugins" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewritePomUnmappedPlugins()
        throws Exception
    {
        ReleaseDescriptor config = createUnmappedConfiguration( "internal-snapshot-plugins" );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomPluginsDifferentVersion()
        throws Exception
    {
        ReleaseDescriptor config = createDifferingVersionConfiguration( "internal-differing-snapshot-plugins" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewriteManagedPomPlugins()
        throws Exception
    {
        ReleaseDescriptor config = createMappedConfiguration( "internal-managed-snapshot-plugin" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewriteManagedPomUnmappedPlugins()
        throws Exception
    {
        ReleaseDescriptor config = createUnmappedConfiguration( "internal-managed-snapshot-plugin" );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomReportPlugins()
        throws Exception
    {
        ReleaseDescriptor config = createDefaultConfiguration( "internal-snapshot-report-plugins" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewritePomUnmappedReportPlugins()
        throws Exception
    {
        ReleaseDescriptor config = createUnmappedConfiguration( "internal-snapshot-report-plugins" );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomReportPluginsDifferentVersion()
        throws Exception
    {
        ReleaseDescriptor config = createDifferingVersionConfiguration( "internal-differing-snapshot-report-plugins" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewritePomExtension()
        throws Exception
    {
        ReleaseDescriptor config = createDefaultConfiguration( "internal-snapshot-extension" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewritePomUnmappedExtension()
        throws Exception
    {
        ReleaseDescriptor config = createUnmappedConfiguration( "internal-snapshot-extension" );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewritePomExtensionDifferentVersion()
        throws Exception
    {
        ReleaseDescriptor config = createDifferingVersionConfiguration( "internal-differing-snapshot-extension" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewriteBasicPomWithEditMode()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromBasicPom();
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }

    public void testRewriteBasicPomWithEditModeFailure()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromBasicPom();
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );

        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl( config.getScmSourceUrl() );
        providerStub.setEditScmResult( new EditScmResult( "", "", "", false ) );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "Check no other cause", e.getCause() );
        }
    }

    public void testRewriteBasicPomWithEditModeException()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromBasicPom();
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new InvokeAtLeastOnceMatcher() ).method( "edit" ).will(
            new ThrowStub( new ScmException( "..." ) ) );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );
        scmManager.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", ScmException.class, e.getCause().getClass() );
        }
    }

    public void testRewriteAddSchema()
        throws Exception
    {
        boolean copyFiles = true;

        // Run a second time to check they are not duplicated
        for ( int i = 0; i < 2; i++ )
        {
            ReleaseDescriptor config = createConfigurationFromBasicPom( copyFiles );
            mapNextVersion( config, "groupId:artifactId" );
            config.setAddSchema( true );

            phase.execute( config, null, getReactorProjects() );

            String expected = readTestProjectFile( "basic-pom/expected-pom-with-schema.xml" );
            String actual = readTestProjectFile( "basic-pom/pom.xml" );
            assertEquals( "Check the transformed POM", expected, actual );

            copyFiles = false;
        }
    }

    public void testSimulateRewriteEditModeSkipped()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromBasicPom();
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new TestFailureMatcher( "edit should not be called" ) ).method( "edit" );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );
        scmManager.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.simulate( config, null, getReactorProjects() );

        // Getting past mock is success
        assertTrue( true );
    }

    public void testRewriteUnmappedPom()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromBasicPom();

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testRewriteBasicPomWithScmRepoException()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromBasicPom();
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeAtLeastOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( config.getScmSourceUrl() ) ).will( new ThrowStub( new ScmRepositoryException( "..." ) ) );

        setMockScmManager( scmManagerMock );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }
    }

    public void testRewriteBasicPomWithNoSuchProviderException()
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromBasicPom();
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeAtLeastOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( config.getScmSourceUrl() ) ).will( new ThrowStub( new NoSuchScmProviderException( "..." ) ) );

        setMockScmManager( scmManagerMock );

        try
        {
            phase.execute( config, null, getReactorProjects() );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
    }

    public void testCleanNoProjects()
        throws Exception
    {
        // This occurs when it is release:perform run standalone. Just check there are no errors.
        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo" );
        config.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        phase.clean( reactorProjects );

        assertTrue( true );
    }

    private ReleaseDescriptor createUnmappedConfiguration( String path )
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationFromProjects( path );

        unmapNextVersion( config, "groupId:subproject1" );
        mapNextVersion( config, "groupId:subproject2" );
        mapNextVersion( config, "groupId:subproject3" );
        mapNextVersion( config, "groupId:artifactId" );
        return config;
    }

    protected ReleaseDescriptor createConfigurationFromProjects( String path )
        throws Exception
    {
        return createConfigurationFromProjects( path, true );
    }

    protected ReleaseDescriptor createDefaultConfiguration( String path )
        throws Exception
    {
        ReleaseDescriptor config = createMappedConfiguration( path );

        mapNextVersion( config, "groupId:subproject4" );
        return config;
    }

    protected ReleaseDescriptor createMappedConfiguration( String path )
        throws Exception
    {
        ReleaseDescriptor config = createDifferingVersionConfiguration( path );

        mapNextVersion( config, "groupId:subproject3" );
        return config;
    }

    private ReleaseDescriptor createDifferingVersionConfiguration( String path )
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( path );

        mapNextVersion( config, "groupId:subproject2" );
        return config;
    }

    protected ReleaseDescriptor createConfigurationFromBasicPom()
        throws Exception
    {
        return createConfigurationFromBasicPom( true );
    }

    protected abstract ReleaseDescriptor createConfigurationForWithParentNextVersion( String path )
        throws Exception;

    protected abstract void unmapNextVersion( ReleaseDescriptor config, String projectId );

    protected abstract void mapNextVersion( ReleaseDescriptor config, String projectId );

    protected abstract ReleaseDescriptor createConfigurationFromProjects( String path, boolean copyFiles )
        throws Exception;

    protected abstract ReleaseDescriptor createConfigurationFromBasicPom( boolean copyFiles )
        throws Exception;

    protected abstract String readTestProjectFile( String fileName )
        throws IOException;

    public void testRewritePomDependenciesWithNamespace()
        throws Exception
    {
        ReleaseDescriptor config = createDefaultConfiguration( "pom-with-namespace" );

        phase.execute( config, null, getReactorProjects() );

        assertTrue( compareFiles( getReactorProjects() ) );
    }
}
