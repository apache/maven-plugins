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

import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.plugins.release.ReleaseFailureException;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

import java.util.Collections;

/**
 * Test the POM verification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckPomPhaseTest
    extends PlexusTestCase
{
    private ReleasePhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "check-poms" );
    }

    public void testCorrectlyConfigured()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );

        phase.execute( releaseDescriptor, null, Collections.singletonList( createProject( "1.0-SNAPSHOT" ) ) );

        phase.simulate( releaseDescriptor, null, Collections.singletonList( createProject( "1.0-SNAPSHOT" ) ) );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testGetUrlFromProjectConnection()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        MavenProject project = createProject( "1.0-SNAPSHOT" );
        Scm scm = new Scm();
        scm.setConnection( "scm:svn:file://localhost/tmp/repo" );
        project.setScm( scm );

        phase.execute( releaseDescriptor, null, Collections.singletonList( project ) );

        assertEquals( "Check URL", "scm:svn:file://localhost/tmp/repo", releaseDescriptor.getScmSourceUrl() );
    }

    public void testGetUrlFromProjectConnectionSimulate()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        MavenProject project = createProject( "1.0-SNAPSHOT" );
        Scm scm = new Scm();
        scm.setConnection( "scm:svn:file://localhost/tmp/repo" );
        project.setScm( scm );

        phase.simulate( releaseDescriptor, null, Collections.singletonList( project ) );

        assertEquals( "Check URL", "scm:svn:file://localhost/tmp/repo", releaseDescriptor.getScmSourceUrl() );
    }

    public void testGetUrlFromProjectDevConnection()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        MavenProject project = createProject( "1.0-SNAPSHOT" );
        Scm scm = new Scm();
        scm.setConnection( "scm:svn:file://localhost/tmp/repo" );
        scm.setDeveloperConnection( "scm:svn:https://localhost/tmp/repo" );
        project.setScm( scm );

        phase.execute( releaseDescriptor, null, Collections.singletonList( project ) );

        assertEquals( "Check URL", "scm:svn:https://localhost/tmp/repo", releaseDescriptor.getScmSourceUrl() );
    }

    public void testGetUrlFromProjectDevConnectionSimulate()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        MavenProject project = createProject( "1.0-SNAPSHOT" );
        Scm scm = new Scm();
        scm.setConnection( "scm:svn:file://localhost/tmp/repo" );
        scm.setDeveloperConnection( "scm:svn:https://localhost/tmp/repo" );
        project.setScm( scm );

        phase.simulate( releaseDescriptor, null, Collections.singletonList( project ) );

        assertEquals( "Check URL", "scm:svn:https://localhost/tmp/repo", releaseDescriptor.getScmSourceUrl() );
    }

    public void testGetInvalidUrl()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        MavenProject project = createProject( "1.0-SNAPSHOT" );
        Scm scm = new Scm();
        scm.setConnection( "scm:cvs:" );
        project.setScm( scm );

        try
        {
            phase.execute( releaseDescriptor, null, Collections.singletonList( project ) );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertTrue( true );
        }
    }

    public void testGetInvalidProvider()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        MavenProject project = createProject( "1.0-SNAPSHOT" );
        Scm scm = new Scm();
        scm.setConnection( "scm:foo:" );
        project.setScm( scm );

        try
        {
            phase.execute( releaseDescriptor, null, Collections.singletonList( project ) );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testMissingUrl()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        try
        {
            phase.execute( releaseDescriptor, null, Collections.singletonList( createProject( "1.0-SNAPSHOT" ) ) );

            fail( "Should have failed to execute" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, Collections.singletonList( createProject( "1.0-SNAPSHOT" ) ) );

            fail( "Should have failed to simulate" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testReleasingNonSnapshot()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );

        try
        {
            phase.execute( releaseDescriptor, null, Collections.singletonList( createProject( "1.0" ) ) );

            fail( "Should have failed to execute" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, Collections.singletonList( createProject( "1.0" ) ) );

            fail( "Should have failed to simulate" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    private static MavenProject createProject( String version )
    {
        Model model = new Model();

        model.setArtifactId( "artifactId" );
        model.setGroupId( "groupId" );
        model.setVersion( version );

        return new MavenProject( model );
    }
}
