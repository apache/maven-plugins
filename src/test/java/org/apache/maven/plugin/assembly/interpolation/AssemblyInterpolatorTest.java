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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

public class AssemblyInterpolatorTest
    extends TestCase
{

    private AssemblyInterpolator interpolator;

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

        Assembly outputAssembly = interpolator.interpolate( assembly, project, Collections.EMPTY_MAP );

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

        Assembly outputAssembly = interpolator.interpolate( assembly, new MavenProject( model ), Collections.EMPTY_MAP );

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

        Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), Collections.EMPTY_MAP );

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

        Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), Collections.EMPTY_MAP );

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

        Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), Collections.singletonMap( "groupId",
            "still.another.id" ) );

        assertEquals( "assembly.still.another.id", result.getId() );
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

        Assembly result = interpolator.interpolate( assembly, new MavenProject( model ), Collections.EMPTY_MAP );

        assertEquals( "assembly.${unresolved}", result.getId() );
    }

}
