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

import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

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

public class AssemblyExpressionEvaluatorTest
    extends TestCase
{

    private AssemblyInterpolator interpolator;
    
    private ConfigSourceStub configSourceStub = new ConfigSourceStub();

    public void setUp()
        throws IOException
    {
        interpolator = new AssemblyInterpolator();

        interpolator.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
    }

    public void testShouldResolveModelGroupId()
        throws ExpressionEvaluationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );
        
        configSourceStub.setProject( new MavenProject( model ) );
        
        Object result = new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${groupId}" );

        assertEquals( "assembly.group.id", result );
    }

    public void testShouldResolveModelPropertyBeforeModelGroupId()
        throws ExpressionEvaluationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        configSourceStub.setProject( new MavenProject( model ) );
        
        Object result = new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${groupId}" );

        assertEquals( "assembly.other.id", result );
    }

    public void testShouldResolveContextValueBeforeModelPropertyOrModelGroupIdInAssemblyId()
        throws ExpressionEvaluationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        MockManager mm = new MockManager();
        
        MockControl sessionControl = MockClassControl.createControl( MavenSession.class );
        MavenSession session = (MavenSession) sessionControl.getMock();
        
        mm.add( sessionControl );
        
        Properties execProps = new Properties();
        execProps.setProperty( "groupId", "still.another.id" );
        
        session.getExecutionProperties();
        sessionControl.setReturnValue( execProps, MockControl.ZERO_OR_MORE );
        
        MockControl csControl = MockControl.createControl( AssemblerConfigurationSource.class );
        AssemblerConfigurationSource cs = (AssemblerConfigurationSource) csControl.getMock();
        
        mm.add( csControl );
        
        cs.getMavenSession();
        csControl.setReturnValue( session, MockControl.ZERO_OR_MORE );
        
        cs.getProject();
        csControl.setReturnValue( new MavenProject( model ), MockControl.ZERO_OR_MORE );
        
        mm.replayAll();

        Object result = new AssemblyExpressionEvaluator( cs ).evaluate( "assembly.${groupId}" );

        assertEquals( "assembly.still.another.id", result );
        
        mm.verifyAll();
        mm.clear();
    }

    public void testShouldReturnUnchangedInputForUnresolvedExpression()
        throws ExpressionEvaluationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );
        
        configSourceStub.setProject( new MavenProject( model ) );

        Object result = new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${unresolved}" );

        assertEquals( "assembly.${unresolved}", result );
    }

    public void testShouldInterpolateMultiDotProjectExpression()
        throws ExpressionEvaluationException
    {
        Build build = new Build();
        build.setFinalName( "final-name" );

        Model model = new Model();
        model.setBuild( build );

        configSourceStub.setProject( new MavenProject( model ) );

        Object result = new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${project.build.finalName}" );

        assertEquals( "assembly.final-name", result );
    }
    
}
