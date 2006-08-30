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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.jmock.Mock;
import org.jmock.core.constraint.IsAnything;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.matcher.TestFailureMatcher;
import org.jmock.core.stub.ReturnStub;
import org.jmock.core.stub.ThrowStub;

import java.util.Collections;
import java.util.List;

/**
 * Test the variable input phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class InputVariablesPhaseTest
    extends PlexusTestCase
{
    private InputVariablesPhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        phase = (InputVariablesPhase) lookup( ReleasePhase.ROLE, "input-variables" );
    }

    public void testInputVariablesInteractive()
        throws Exception
    {
        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).with( new IsAnything(),
                                                                                 new IsEqual( "artifactId-1.0" ) ).will(
            new ReturnStub( "tag-value" ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check tag", "tag-value", releaseDescriptor.getScmReleaseLabel() );

        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).with( new IsAnything(),
                                                                                 new IsEqual( "artifactId-1.0" ) ).will(
            new ReturnStub( "simulated-tag-value" ) );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check tag", "simulated-tag-value", releaseDescriptor.getScmReleaseLabel() );
    }

    public void testUnmappedVersion()
        throws Exception
    {
        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "check no cause", e.getCause() );
        }

        releaseDescriptor = new ReleaseDescriptor();

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Expected an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertNull( "check no cause", e.getCause() );
        }
    }

    public void testInputVariablesNonInteractive()
        throws Exception
    {
        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check tag", "artifactId-1.0", releaseDescriptor.getScmReleaseLabel() );

        mockPrompter.reset();
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );

        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check tag", "artifactId-1.0", releaseDescriptor.getScmReleaseLabel() );
    }

    public void testInputVariablesNonInteractiveConfigured()
        throws Exception
    {
        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setScmReleaseLabel( "tag-value" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check tag", "tag-value", releaseDescriptor.getScmReleaseLabel() );

        mockPrompter.reset();
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );

        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setScmReleaseLabel( "simulated-tag-value" );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check tag", "simulated-tag-value", releaseDescriptor.getScmReleaseLabel() );
    }

    public void testInputVariablesInteractiveConfigured()
        throws Exception
    {
        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmReleaseLabel( "tag-value" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check tag", "tag-value", releaseDescriptor.getScmReleaseLabel() );

        mockPrompter.reset();
        mockPrompter.expects( new TestFailureMatcher( "prompter should not be called" ) ).method( "prompt" );

        releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmReleaseLabel( "simulated-tag-value" );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertEquals( "Check tag", "simulated-tag-value", releaseDescriptor.getScmReleaseLabel() );
    }

    public void testPrompterException()
        throws Exception
    {
        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will(
            new ThrowStub( new PrompterException( "..." ) ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        List reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );

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
        releaseDescriptor.mapReleaseVersion( "groupId:artifactId", "1.0" );

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

    private static MavenProject createProject( String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( "groupId" );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        return new MavenProject( model );
    }

}
