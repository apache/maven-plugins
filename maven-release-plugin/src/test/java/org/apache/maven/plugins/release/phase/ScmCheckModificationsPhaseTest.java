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
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.PlexusTestCase;
import org.jmock.cglib.Mock;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeAtLeastOnceMatcher;
import org.jmock.core.stub.ThrowStub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmCheckModificationsPhaseTest
    extends PlexusTestCase
{
    private ReleasePhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "scm-check-modifications" );
    }

    public void testNoSuchScmProviderExceptionThrown()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm-url" );
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test/checkout" ) );

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeAtLeastOnceMatcher() ).method( "makeScmRepository" ).with(
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

        try
        {
            phase.simulate( releaseConfiguration );

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
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm-url" );
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test/checkout" ) );

        Mock scmManagerMock = new Mock( ScmManager.class );
        scmManagerMock.expects( new InvokeAtLeastOnceMatcher() ).method( "makeScmRepository" ).with(
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

        try
        {
            phase.simulate( releaseConfiguration );

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
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm-url" );
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test/checkout" ) );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new InvokeAtLeastOnceMatcher() ).method( "status" ).will(
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

        try
        {
            phase.simulate( releaseConfiguration );

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

        providerStub.setStatusScmResult( new StatusScmResult( "", "", "", false ) );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }
    }

    public void testNoModifications()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        setChangedFiles( releaseConfiguration, Collections.EMPTY_LIST );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testModificationsToExcludedFilesOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        setChangedFiles( releaseConfiguration, Arrays.asList( new String[]{"release.properties", "pom.xml",
            "pom.xml.backup", "module/pom.xml", "pom.xml.tag", "pom.xml.release"} ) );

        phase.execute( releaseConfiguration );

        phase.simulate( releaseConfiguration );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testModificationsToIncludedFilesOnly()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        setChangedFiles( releaseConfiguration, Collections.singletonList( "something.txt" ) );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            // verify no cause as an extra check that it is the right exception
            assertNull( "check no cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            // verify no cause as an extra check that it is the right exception
            assertNull( "check no cause", e.getCause() );
        }
    }

    public void testModificationsToIncludedAndExcludedFiles()
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        setChangedFiles( releaseConfiguration, Arrays.asList( new String[]{"release.properties", "pom.xml",
            "pom.xml.backup", "module/pom.xml", "pom.xml.tag", "pom.xml.release", "something.txt"} ) );

        try
        {
            phase.execute( releaseConfiguration );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            // verify no cause as an extra check that it is the right exception
            assertNull( "check no cause", e.getCause() );
        }

        try
        {
            phase.simulate( releaseConfiguration );

            fail( "Status check should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            // verify no cause as an extra check that it is the right exception
            assertNull( "check no cause", e.getCause() );
        }
    }

    private void setChangedFiles( ReleaseConfiguration releaseConfiguration, List changedFiles )
        throws Exception
    {
        ScmManager scmManager = (ScmManager) lookup( ScmManager.ROLE );
        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl( releaseConfiguration.getUrl() );

        providerStub.setStatusScmResult( new StatusScmResult( "", createScmFiles( changedFiles ) ) );
    }

    private static List createScmFiles( List changedFiles )
    {
        List files = new ArrayList( changedFiles.size() );
        for ( Iterator i = changedFiles.iterator(); i.hasNext(); )
        {
            String fileName = (String) i.next();
            files.add( new ScmFile( fileName, ScmFileStatus.MODIFIED ) );
        }
        return files;
    }

    private static ReleaseConfiguration createReleaseConfiguration()
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm:svn:file://localhost/tmp/scm-repo" );
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test/checkout" ) );
        return releaseConfiguration;
    }
}
