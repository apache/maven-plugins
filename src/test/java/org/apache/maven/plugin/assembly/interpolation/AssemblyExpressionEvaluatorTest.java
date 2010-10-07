package org.apache.maven.plugin.assembly.interpolation;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.testutils.ConfigSourceStub;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

public class AssemblyExpressionEvaluatorTest
    extends TestCase
{

    private AssemblyInterpolator interpolator;

    private final ConfigSourceStub configSourceStub = new ConfigSourceStub();

    @Override
    public void setUp()
        throws IOException
    {
        interpolator = new AssemblyInterpolator();

        interpolator.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
    }

    public void testShouldResolveModelGroupId()
        throws ExpressionEvaluationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        configSourceStub.setProject( new MavenProject( model ) );

        final Object result = new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${groupId}" );

        assertEquals( "assembly.group.id", result );
    }

    public void testShouldResolveModelPropertyBeforeModelGroupId()
        throws ExpressionEvaluationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        configSourceStub.setProject( new MavenProject( model ) );

        final Object result = new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${groupId}" );

        assertEquals( "assembly.other.id", result );
    }

    public void testShouldResolveContextValueBeforeModelPropertyOrModelGroupIdInAssemblyId()
        throws ExpressionEvaluationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        final MockManager mm = new MockManager();

        final MockControl sessionControl = MockClassControl.createControl( MavenSession.class );
        final MavenSession session = (MavenSession) sessionControl.getMock();

        mm.add( sessionControl );

        final Properties execProps = new Properties();
        execProps.setProperty( "groupId", "still.another.id" );

        session.getExecutionProperties();
        sessionControl.setReturnValue( execProps, MockControl.ZERO_OR_MORE );

        session.getUserProperties();
        sessionControl.setReturnValue( new Properties(), MockControl.ZERO_OR_MORE );

        final MockControl csControl = MockControl.createControl( AssemblerConfigurationSource.class );
        final AssemblerConfigurationSource cs = (AssemblerConfigurationSource) csControl.getMock();

        mm.add( csControl );

        cs.getMavenSession();
        csControl.setReturnValue( session, MockControl.ZERO_OR_MORE );

        cs.getProject();
        csControl.setReturnValue( new MavenProject( model ), MockControl.ZERO_OR_MORE );

        final MockControl lrCtl = MockControl.createControl( ArtifactRepository.class );
        final ArtifactRepository lr = (ArtifactRepository) lrCtl.getMock();
        mm.add( lrCtl );

        lr.getBasedir();
        lrCtl.setReturnValue( "/path/to/local/repo", MockControl.ZERO_OR_MORE );

        cs.getLocalRepository();
        csControl.setReturnValue( lr, MockControl.ZERO_OR_MORE );

        mm.replayAll();

        final Object result = new AssemblyExpressionEvaluator( cs ).evaluate( "assembly.${groupId}" );

        assertEquals( "assembly.still.another.id", result );

        mm.verifyAll();
        mm.clear();
    }

    public void testShouldReturnUnchangedInputForUnresolvedExpression()
        throws ExpressionEvaluationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        configSourceStub.setProject( new MavenProject( model ) );

        final Object result = new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${unresolved}" );

        assertEquals( "assembly.${unresolved}", result );
    }

    public void testShouldInterpolateMultiDotProjectExpression()
        throws ExpressionEvaluationException
    {
        final Build build = new Build();
        build.setFinalName( "final-name" );

        final Model model = new Model();
        model.setBuild( build );

        configSourceStub.setProject( new MavenProject( model ) );

        final Object result =
            new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${project.build.finalName}" );

        assertEquals( "assembly.final-name", result );
    }

}
