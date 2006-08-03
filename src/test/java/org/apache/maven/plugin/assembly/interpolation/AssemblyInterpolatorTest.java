package org.apache.maven.plugin.assembly.interpolation;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.cli.CommandLineUtils;

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

    public void testBlacklistedExpressionsInDependencyOutputFileNameMappingsAreNotInterpolated()
        throws IOException, AssemblyInterpolationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        Assembly assembly = new Assembly();

        // artifactId is blacklisted, but packaging is not.
        String outputFileNameMapping = "${artifactId}.${packaging}";

        DependencySet set = new DependencySet();
        set.setOutputFileNameMapping( outputFileNameMapping );

        assembly.addDependencySet( set );

        Assembly outputAssembly = interpolator.interpolate( assembly, model, Collections.EMPTY_MAP );

        List outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );

        DependencySet outputSet = (DependencySet) outputDependencySets.get( 0 );

        assertEquals( "${artifactId}.jar", outputSet.getOutputFileNameMapping() );
    }

    public void testBlacklistedExpressionsInDependencySetOutputDirectoryIsNotInterpolated()
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

        Assembly outputAssembly = interpolator.interpolate( assembly, model, Collections.EMPTY_MAP );

        List outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );

        DependencySet outputSet = (DependencySet) outputDependencySets.get( 0 );

        assertEquals( "${artifactId}.jar", outputSet.getOutputDirectory() );
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

        Assembly result = interpolator.interpolate( assembly, model, Collections.EMPTY_MAP );

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

        Assembly result = interpolator.interpolate( assembly, model, Collections.EMPTY_MAP );

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

        Assembly result = interpolator.interpolate( assembly, model, Collections.singletonMap( "groupId",
            "still.another.id" ) );

        assertEquals( "assembly.still.another.id", result.getId() );
    }

    public void testShouldResolveEnvarHOMEValueInDependencySetOutputDirectory()
        throws IOException, AssemblyInterpolationException
    {
        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        Assembly assembly = new Assembly();

        Properties envars = CommandLineUtils.getSystemEnvVars();

        String homeValue = envars.getProperty( "PATH" );

        String outputDirectory = "${env.PATH}";

        DependencySet set = new DependencySet();
        set.setOutputDirectory( outputDirectory );

        assembly.addDependencySet( set );

        Assembly outputAssembly = interpolator.interpolate( assembly, model, Collections.EMPTY_MAP );

        List outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );

        DependencySet outputSet = (DependencySet) outputDependencySets.get( 0 );

        assertEquals( homeValue, outputSet.getOutputDirectory() );
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

        Assembly result = interpolator.interpolate( assembly, model, Collections.EMPTY_MAP );

        assertEquals( "assembly.${unresolved}", result.getId() );
    }

}
