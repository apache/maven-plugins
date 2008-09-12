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
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.testutils.ConfigSourceStub;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

public class AssemblyInterpolatorTest
    extends TestCase
{

    private AssemblyInterpolator interpolator;
    
    private AssemblerConfigurationSource configSourceStub = new ConfigSourceStub();

    public void setUp()
        throws IOException
    {
        interpolator = new AssemblyInterpolator();

        interpolator.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
    }

    public void testDependencySetOutputFileNameMappingsAreNotInterpolated()
        throws IOException, AssemblyInterpolationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        MavenProject project = new MavenProject( model );

        Assembly assembly = new Assembly();

        // artifactId is blacklisted, but packaging is not.
        String outputFileNameMapping = "${artifactId}.${packaging}";

        DependencySet set = new DependencySet();
        set.setOutputFileNameMapping( outputFileNameMapping );

        assembly.addDependencySet( set );

        Assembly outputAssembly = interpolator.interpolate( assembly, project, configSourceStub );

        List outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );

        DependencySet outputSet = (DependencySet) outputDependencySets.get( 0 );

        assertEquals( "${artifactId}.${packaging}", outputSet.getOutputFileNameMapping() );
    }

    public void testDependencySetOutputDirectoryIsNotInterpolated()
        throws IOException, AssemblyInterpolationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        Assembly assembly = new Assembly();

        String outputDirectory = "${artifactId}.${packaging}";

        DependencySet set = new DependencySet();
        set.setOutputDirectory( outputDirectory );

        assembly.addDependencySet( set );

        Assembly outputAssembly = interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        List outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );

        DependencySet outputSet = (DependencySet) outputDependencySets.get( 0 );

        assertEquals( "${artifactId}.${packaging}", outputSet.getOutputDirectory() );
    }

    public void testShouldResolveModelGroupIdInAssemblyId()
        throws AssemblyInterpolationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        Assembly assembly = new Assembly();

        assembly.setId( "assembly.${groupId}" );

        Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        assertEquals( "assembly.group.id", result.getId() );
    }

    public void testShouldResolveModelPropertyBeforeModelGroupIdInAssemblyId()
        throws AssemblyInterpolationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        Assembly assembly = new Assembly();

        assembly.setId( "assembly.${groupId}" );

        Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        assertEquals( "assembly.other.id", result.getId() );
    }

    public void testShouldResolveContextValueBeforeModelPropertyOrModelGroupIdInAssemblyId()
        throws AssemblyInterpolationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        Assembly assembly = new Assembly();

        assembly.setId( "assembly.${groupId}" );
        
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
        
        mm.replayAll();

        Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), cs );

        assertEquals( "assembly.still.another.id", result.getId() );
        
        mm.verifyAll();
        mm.clear();
    }

    public void testShouldNotTouchUnresolvedExpression()
        throws AssemblyInterpolationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        Assembly assembly = new Assembly();

        assembly.setId( "assembly.${unresolved}" );

        Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        assertEquals( "assembly.${unresolved}", result.getId() );
    }

    public void testShouldInterpolateMultiDotProjectExpression()
        throws AssemblyInterpolationException
    {
        Build build = new Build();
        build.setFinalName( "final-name" );

        Model model = new Model();
        model.setBuild( build );

        Assembly assembly = new Assembly();

        assembly.setId( "assembly.${project.build.finalName}" );

        Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), configSourceStub );

        assertEquals( "assembly.final-name", result.getId() );
    }
    

}
