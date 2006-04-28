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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
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
import org.jmock.cglib.Mock;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeAtLeastOnceMatcher;
import org.jmock.core.stub.ThrowStub;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForReleasePhaseTest
    extends AbstractReleaseTestCase
{
    private ReleasePhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "rewrite-poms-for-release" );
    }

    public void testRewriteBasicPom()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "basic-pom" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewritePomWithParent()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "pom-with-parent" );

        config.mapReleaseVersion( "groupId:artifactId", "1.0" );
        config.mapReleaseVersion( "groupId:subproject1", "2.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewritePomWithUnmappedParent()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "pom-with-parent" );

        // remove parent from processing so it fails when looking at the parent of the child instead
        MavenProject project =
            (MavenProject) getProjectsAsMap( config.getReactorProjects() ).get( "groupId:subproject1" );
        config.setReactorProjects( Collections.singletonList( project ) );
        config.mapReleaseVersion( "groupId:subproject1", "2.0" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewritePomWithReleasedParent()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "pom-with-released-parent" );

        config.mapReleaseVersion( "groupId:subproject1", "2.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewritePomWithInheritedVersion()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "pom-with-inherited-version" );

        config.mapReleaseVersion( "groupId:artifactId", "1.0" );
        config.mapReleaseVersion( "groupId:subproject1", "1.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewritePomWithChangedInheritedVersion()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "pom-with-inherited-version" );

        config.mapReleaseVersion( "groupId:artifactId", "1.0" );
        config.mapReleaseVersion( "groupId:subproject1", "2.0" );

        phase.execute( config );

        MavenProject project =
            (MavenProject) getProjectsAsMap( config.getReactorProjects() ).get( "groupId:subproject1" );

        String actual = FileUtils.fileRead( project.getFile() );
        String expected =
            FileUtils.fileRead( new File( project.getFile().getParentFile(), "expected-pom-version-changed.xml" ) );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    public void testRewritePomDependencies()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-snapshot-dependencies" );

        config.mapReleaseVersion( "groupId:subproject1", "1.0" );
        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:subproject4", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewritePomUnmappedDependencies()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-snapshot-dependencies" );

        MavenProject project =
            (MavenProject) getProjectsAsMap( config.getReactorProjects() ).get( "groupId:subproject2" );
        config.setReactorProjects( Collections.singletonList( project ) );

        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewritePomDependenciesDifferentVersion()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-differing-snapshot-dependencies" );

        config.mapReleaseVersion( "groupId:subproject1", "1.0" );
        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewriteManagedPomDependencies()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-managed-snapshot-dependency" );

        config.mapReleaseVersion( "groupId:subproject1", "1.0" );
        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewriteManagedPomUnmappedDependencies()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-managed-snapshot-dependency" );

        MavenProject project =
            (MavenProject) getProjectsAsMap( config.getReactorProjects() ).get( "groupId:subproject2" );
        config.setReactorProjects( Collections.singletonList( project ) );

        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewritePomPlugins()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-snapshot-plugins" );

        config.mapReleaseVersion( "groupId:subproject1", "1.0" );
        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:subproject4", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewritePomUnmappedPlugins()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-snapshot-plugins" );

        MavenProject project =
            (MavenProject) getProjectsAsMap( config.getReactorProjects() ).get( "groupId:subproject2" );
        config.setReactorProjects( Collections.singletonList( project ) );

        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewritePomPluginsDifferentVersion()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-differing-snapshot-plugins" );

        config.mapReleaseVersion( "groupId:subproject1", "1.0" );
        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewriteManagedPomPlugins()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-managed-snapshot-plugin" );

        config.mapReleaseVersion( "groupId:subproject1", "1.0" );
        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewriteManagedPomUnmappedPlugins()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-managed-snapshot-plugin" );

        MavenProject project =
            (MavenProject) getProjectsAsMap( config.getReactorProjects() ).get( "groupId:subproject2" );
        config.setReactorProjects( Collections.singletonList( project ) );

        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewritePomReportPlugins()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-snapshot-report-plugins" );

        config.mapReleaseVersion( "groupId:subproject1", "1.0" );
        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:subproject4", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewritePomUnmappedReportPlugins()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-snapshot-report-plugins" );

        MavenProject project =
            (MavenProject) getProjectsAsMap( config.getReactorProjects() ).get( "groupId:subproject2" );
        config.setReactorProjects( Collections.singletonList( project ) );

        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:subproject3", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewritePomReportPluginsDifferentVersion()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "internal-differing-snapshot-report-plugins" );

        config.mapReleaseVersion( "groupId:subproject1", "1.0" );
        config.mapReleaseVersion( "groupId:subproject2", "1.0" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewriteBasicPomWithEditMode()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "basic-pom" );
        config.setUseEditMode( true );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.execute( config );

        assertTrue( compareFiles( config.getReactorProjects() ) );
    }

    public void testRewriteBasicPomWithEditModeFailure()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "basic-pom" );
        config.setUseEditMode( true );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        ScmManager scmManager = (ScmManager) lookup( ScmManager.ROLE );
        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl( config.getUrl() );

        providerStub.setEditScmResult( new EditScmResult( "", "", "", false ) );

        try
        {
            phase.execute( config );

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
        ReleaseConfiguration config = createConfigurationFromProjects( "basic-pom" );
        config.setUseEditMode( true );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new InvokeAtLeastOnceMatcher() ).method( "edit" ).will(
            new ThrowStub( new ScmException( "..." ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        try
        {
            phase.execute( config );

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
        ReleaseConfiguration config = createConfigurationFromProjects( "basic-pom" );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );
        config.setAddSchema( true );

        // Run a second time to check they are not duplicated
        for ( int i = 0; i < 2; i++ )
        {
            phase.execute( config );

            String expected = readTestProjectFile( "basic-pom/expected-pom-with-schema.xml" );
            String actual = readTestProjectFile( "basic-pom/pom.xml" );
            assertEquals( "Check the transformed POM", expected, actual );
        }
    }

    public void testRewriteUnmappedPom()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "basic-pom" );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

    public void testRewriteBasicPomWithScmRepoException()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( "basic-pom" );
        config.setUseEditMode( true );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeAtLeastOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( config.getUrl() ) ).will( new ThrowStub( new ScmRepositoryException( "..." ) ) );

        ScmManager scmManager = (ScmManager) scmManagerMock.proxy();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );

        try
        {
            phase.execute( config );

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
        ReleaseConfiguration config = createConfigurationFromProjects( "basic-pom" );
        config.setUseEditMode( true );
        config.mapReleaseVersion( "groupId:artifactId", "1.0" );

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeAtLeastOnceMatcher() ).method( "makeScmRepository" ).with(
            new IsEqual( config.getUrl() ) ).will( new ThrowStub( new NoSuchScmProviderException( "..." ) ) );

        ScmManager scmManager = (ScmManager) scmManagerMock.proxy();
        DefaultScmRepositoryConfigurator configurator =
            (DefaultScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
        configurator.setScmManager( scmManager );

        try
        {
            phase.execute( config );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
    }

    private static String readTestProjectFile( String fileName )
        throws IOException
    {
        return FileUtils.fileRead( getTestFile( "target/test-classes/projects/rewrite-for-release/" + fileName ) );
    }

    private ReleaseConfiguration createConfigurationFromProjects( String path )
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createConfigurationFromProjects( "rewrite-for-release/", path );
        releaseConfiguration.setUrl( "scm:svn:file://localhost/tmp/scm-repo" );
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test/checkout" ) );

        return releaseConfiguration;
    }

    private boolean compareFiles( List reactorProjects )
        throws IOException
    {
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            File actualFile = project.getFile();
            String actual = FileUtils.fileRead( actualFile );
            File expectedFile = new File( actualFile.getParentFile(), "expected-pom.xml" );
            String expected = FileUtils.fileRead( expectedFile );
            assertEquals( "Check the transformed POM", expected, actual );
        }
        return true;
    }

    private static Map getProjectsAsMap( List reactorProjects )
    {
        Map map = new HashMap();
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            map.put( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ), project );
        }
        return map;
    }
}
