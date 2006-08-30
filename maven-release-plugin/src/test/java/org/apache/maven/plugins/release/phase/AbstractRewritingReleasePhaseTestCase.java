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
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewritePomWithParent()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "pom-with-parent" );
        ReleaseDescriptor config = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewritePomWithUnmappedParent()
        throws Exception
    {

        List reactorProjects = createReactorProjects( "pom-with-parent" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        // Process the child first
        reactorProjects = new ArrayList( reactorProjects );
        Collections.reverse( reactorProjects );

        mapAlternateNextVersion( config, "groupId:subproject1" );

        try
        {
            phase.execute( config, null, reactorProjects );

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

        List reactorProjects = createReactorProjects( "pom-with-released-parent" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        mapAlternateNextVersion( config, "groupId:subproject1" );
        config.mapReleaseVersion( "groupId:artifactId", "1" );
        config.mapDevelopmentVersion( "groupId:artifactId", "1" );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    protected abstract void mapAlternateNextVersion( ReleaseDescriptor config, String projectId );

    public void testRewritePomWithInheritedVersion()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "pom-with-inherited-version" );
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewritePomWithChangedInheritedVersion()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "pom-with-inherited-version" );
        ReleaseDescriptor config = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects );

        phase.execute( config, null, reactorProjects );

        MavenProject project = (MavenProject) getProjectsAsMap( reactorProjects ).get( "groupId:subproject1" );

        String actual = FileUtils.fileRead( project.getFile() );
        String expected =
            FileUtils.fileRead( new File( project.getFile().getParentFile(), "expected-pom-version-changed.xml" ) );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    protected abstract ReleaseDescriptor createConfigurationForPomWithParentAlternateNextVersion( List reactorProjects )
        throws Exception;

    public void testRewritePomDependencies()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-snapshot-dependencies" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );
        mapNextVersion( config, "groupId:subsubproject" );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedDependencies()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-snapshot-dependencies" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, null, reactorProjects );

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
        List reactorProjects = createReactorProjects( "internal-differing-snapshot-dependencies" );
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewriteManagedPomDependencies()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-managed-snapshot-dependency" );
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewriteManagedPomUnmappedDependencies()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-managed-snapshot-dependency" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, null, reactorProjects );

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
        List reactorProjects = createReactorProjects( "internal-snapshot-plugins" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedPlugins()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-snapshot-plugins" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, null, reactorProjects );

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
        List reactorProjects = createReactorProjects( "internal-differing-snapshot-plugins" );
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewriteManagedPomPlugins()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-managed-snapshot-plugin" );
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewriteManagedPomUnmappedPlugins()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-managed-snapshot-plugin" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, null, reactorProjects );

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
        List reactorProjects = createReactorProjects( "internal-snapshot-report-plugins" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedReportPlugins()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-snapshot-report-plugins" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, null, reactorProjects );

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
        List reactorProjects = createReactorProjects( "internal-differing-snapshot-report-plugins" );
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewritePomExtension()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-snapshot-extension" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewritePomUnmappedExtension()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "internal-snapshot-extension" );
        ReleaseDescriptor config = createUnmappedConfiguration( reactorProjects );

        try
        {
            phase.execute( config, null, reactorProjects );

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
        List reactorProjects = createReactorProjects( "internal-differing-snapshot-extension" );
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithEditMode()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithEditModeFailure()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
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
            phase.execute( config, null, reactorProjects );

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
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
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
            phase.execute( config, null, reactorProjects );

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
            List reactorProjects = createReactorProjects( "basic-pom", copyFiles );
            ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
            mapNextVersion( config, "groupId:artifactId" );
            config.setAddSchema( true );

            phase.execute( config, null, reactorProjects );

            String expected = readTestProjectFile( "basic-pom/expected-pom-with-schema.xml" );
            String actual = readTestProjectFile( "basic-pom/pom.xml" );
            assertEquals( "Check the transformed POM", expected, actual );

            copyFiles = false;
        }
    }

    public void testSimulateRewriteEditModeSkipped()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new TestFailureMatcher( "edit should not be called" ) ).method( "edit" );

        ScmManagerStub scmManager = new ScmManagerStub();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );
        scmManager.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.simulate( config, null, reactorProjects );

        // Getting past mock is success
        assertTrue( true );
    }

    public void testRewriteUnmappedPom()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );

        try
        {
            phase.execute( config, null, reactorProjects );

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
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeAtLeastOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( config.getScmSourceUrl() ) ).will( new ThrowStub( new ScmRepositoryException( "..." ) ) );

        setMockScmManager( scmManagerMock );

        try
        {
            phase.execute( config, null, reactorProjects );

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
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.setScmUseEditMode( true );
        mapNextVersion( config, "groupId:artifactId" );

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeAtLeastOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( config.getScmSourceUrl() ) ).will( new ThrowStub( new NoSuchScmProviderException( "..." ) ) );

        setMockScmManager( scmManagerMock );

        try
        {
            phase.execute( config, null, reactorProjects );

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

        phase.clean( null );

        assertTrue( true );
    }

    private ReleaseDescriptor createUnmappedConfiguration( List reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        unmapNextVersion( config, "groupId:subproject1" );
        mapNextVersion( config, "groupId:subproject2" );
        mapNextVersion( config, "groupId:subproject3" );
        mapNextVersion( config, "groupId:artifactId" );
        return config;
    }

    protected List createReactorProjects( String path )
        throws Exception
    {
        return createReactorProjects( path, true );
    }

    protected ReleaseDescriptor createDefaultConfiguration( List reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        mapNextVersion( config, "groupId:subproject4" );
        return config;
    }

    protected ReleaseDescriptor createMappedConfiguration( List reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDifferingVersionConfiguration( reactorProjects );

        mapNextVersion( config, "groupId:subproject3" );
        return config;
    }

    private ReleaseDescriptor createDifferingVersionConfiguration( List reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( reactorProjects );

        mapNextVersion( config, "groupId:subproject2" );
        return config;
    }

    protected List createReactorProjectsFromBasicPom()
        throws Exception
    {
        return createReactorProjects( "basic-pom" );
    }

    protected abstract ReleaseDescriptor createConfigurationForWithParentNextVersion( List reactorProjects )
        throws Exception;

    protected abstract void unmapNextVersion( ReleaseDescriptor config, String projectId );

    protected abstract void mapNextVersion( ReleaseDescriptor config, String projectId );

    protected ReleaseDescriptor createDescriptorFromBasicPom( List reactorProjects )
        throws Exception
    {
        return createDescriptorFromProjects( reactorProjects );
    }

    protected abstract String readTestProjectFile( String fileName )
        throws IOException;

    public void testRewritePomDependenciesWithNamespace()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "pom-with-namespace" );
        ReleaseDescriptor config = createDefaultConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    protected abstract List createReactorProjects( String path, boolean copyFiles )
        throws Exception;

    protected ReleaseDescriptor createDescriptorFromProjects( List reactorProjects )
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        MavenProject rootProject = (MavenProject) reactorProjects.get( 0 );
        if ( rootProject.getScm() == null )
        {
            descriptor.setScmSourceUrl( "scm:svn:file://localhost/tmp/scm-repo/trunk" );
        }
        else
        {
            descriptor.setScmSourceUrl( rootProject.getScm().getConnection() );
        }

        descriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        return descriptor;
    }
}
