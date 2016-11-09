package org.apache.maven.plugins.assembly.interpolation;

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

import junit.framework.TestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.testutils.PojoConfigSource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.PropertiesBasedValueSource;
import org.easymock.classextension.EasyMockSupport;
import org.easymock.classextension.IMocksControl;

import java.util.Properties;

import static org.easymock.EasyMock.expect;

public class AssemblyExpressionEvaluatorTest
    extends TestCase
{

    private final PojoConfigSource configSourceStub = new PojoConfigSource();

    public void testShouldResolveModelGroupId()
        throws ExpressionEvaluationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        configSourceStub.setMavenProject( new MavenProject( model ) );
        setupInterpolation();

        final Object result = new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${groupId}" );

        assertEquals( "assembly.group.id", result );
    }

    private void setupInterpolation()
    {
        configSourceStub.setRootInterpolator( FixedStringSearchInterpolator.create() );
        configSourceStub.setEnvironmentInterpolator( FixedStringSearchInterpolator.create() );
        configSourceStub.setEnvInterpolator( FixedStringSearchInterpolator.create() );

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

        configSourceStub.setMavenProject( new MavenProject( model ) );
        setupInterpolation();

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

        final EasyMockSupport mm = new EasyMockSupport();

        MavenSession session = mm.createControl().createMock( MavenSession.class );

        final Properties execProps = new Properties();
        execProps.setProperty( "groupId", "still.another.id" );

        PropertiesBasedValueSource cliProps = new PropertiesBasedValueSource( execProps );
        expect( session.getExecutionProperties() ).andReturn( execProps ).anyTimes();
        expect( session.getUserProperties() ).andReturn( new Properties() ).anyTimes();

        AssemblerConfigurationSource cs = mm.createControl().createMock( AssemblerConfigurationSource.class );
        expect( cs.getCommandLinePropsInterpolator() ).andReturn(
            FixedStringSearchInterpolator.create( cliProps ) ).anyTimes();
        expect( cs.getRepositoryInterpolator() ).andReturn( FixedStringSearchInterpolator.create() ).anyTimes();
        expect( cs.getEnvInterpolator() ).andReturn( FixedStringSearchInterpolator.create() ).anyTimes();

        expect( cs.getMavenSession() ).andReturn( session ).anyTimes();
        expect( cs.getProject() ).andReturn( new MavenProject( model ) );

        final IMocksControl lrCtl = mm.createControl();
        final ArtifactRepository lr = lrCtl.createMock( ArtifactRepository.class );

        expect( lr.getBasedir() ).andReturn( "/path/to/local/repo" ).anyTimes();
        expect( cs.getLocalRepository() ).andReturn( lr ).anyTimes();

        mm.replayAll();

        final Object result = new AssemblyExpressionEvaluator( cs ).evaluate( "assembly.${groupId}" );

        assertEquals( "assembly.still.another.id", result );

        mm.verifyAll();
    }

    public void testShouldReturnUnchangedInputForUnresolvedExpression()
        throws ExpressionEvaluationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        configSourceStub.setMavenProject( new MavenProject( model ) );
        setupInterpolation();

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

        configSourceStub.setMavenProject( new MavenProject( model ) );
        setupInterpolation();

        final Object result =
            new AssemblyExpressionEvaluator( configSourceStub ).evaluate( "assembly.${project.build.finalName}" );

        assertEquals( "assembly.final-name", result );
    }

}
