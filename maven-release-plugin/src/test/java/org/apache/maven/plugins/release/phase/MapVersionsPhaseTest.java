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
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.versions.VersionParseException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.jmock.Mock;
import org.jmock.core.constraint.IsAnything;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.constraint.IsNull;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.matcher.TestFailureMatcher;
import org.jmock.core.stub.ReturnStub;
import org.jmock.core.stub.ThrowStub;

import java.util.Collections;
import java.util.List;

/**
 * Test the version mapping phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MapVersionsPhaseTest
    extends PlexusTestCase
{
    public void testMapReleaseVersionsInteractive()
        throws Exception
    {
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).with( new IsAnything(),
                                                                                 new IsEqual( "1.0" ) ).will(
            new ReturnStub( "2.0" ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0-SNAPSHOT" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );

        releaseDescriptor = new ReleaseDescriptor();

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).with( new IsAnything(),
                                                                                 new IsEqual( "1.0" ) ).will(
            new ReturnStub( "2.0" ) );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0" ),
                      releaseDescriptor.getReleaseVersions() );
    }

    public void testMapReleaseVersionsNonInteractive()
        throws Exception
    {
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-release-versions" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0-SNAPSHOT" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getReleaseVersions() );

        mockPrompter.reset();
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );

        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.0" ),
                      releaseDescriptor.getReleaseVersions() );
    }

    public void testMapDevVersionsInteractive()
        throws Exception
    {
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).with( new IsAnything(),
                                                                                 new IsEqual( "1.1-SNAPSHOT" ) ).will(
            new ReturnStub( "2.0-SNAPSHOT" ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        releaseDescriptor = new ReleaseDescriptor();

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).with( new IsAnything(),
                                                                                 new IsEqual( "1.1-SNAPSHOT" ) ).will(
            new ReturnStub( "2.0-SNAPSHOT" ) );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testMapDevVersionsNonInteractive()
        throws Exception
    {
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        mockPrompter.reset();
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );

        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "1.1-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testPrompterException()
        throws Exception
    {
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will(
            new ThrowStub( new PrompterException( "..." ) ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", PrompterException.class, e.getCause().getClass() );
        }

        releaseDescriptor = new ReleaseDescriptor();

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will(
            new ThrowStub( new PrompterException( "..." ) ) );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", PrompterException.class, e.getCause().getClass() );
        }
    }

    public void testInvalidVersionInteractive()
        throws Exception
    {
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).with( new IsAnything(), new IsNull() ).will(
            new ReturnStub( "2.0-SNAPSHOT" ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "foo" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).with( new IsAnything(), new IsNull() ).will(
            new ReturnStub( "2.0-SNAPSHOT" ) );

        releaseDescriptor = new ReleaseDescriptor();

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check mapped versions", Collections.singletonMap( "groupId:artifactId", "2.0-SNAPSHOT" ),
                      releaseDescriptor.getDevelopmentVersions() );
    }

    public void testInvalidVersionNonInteractive()
        throws Exception
    {
        MapVersionsPhase phase = (MapVersionsPhase) lookup( ReleasePhase.ROLE, "test-map-development-versions" );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "foo" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", VersionParseException.class, e.getCause().getClass() );
        }

        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", VersionParseException.class, e.getCause().getClass() );
        }
    }

    private static MavenProject createProject( String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( "groupId" );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        return new MavenProject( model );
    }

}
