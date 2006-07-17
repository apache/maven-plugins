package org.apache.maven.plugin.assembly.interpolation;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class AssemblyInterpolatorTest
    extends TestCase
{

    public void testDependencyOutputFileNameMappingsAreNotInterpolated() 
        throws IOException, AssemblyInterpolationException
    {
        AssemblyInterpolator interpolator = new AssemblyInterpolator();

        Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );

        Assembly assembly = new Assembly();

        DependencySet set = new DependencySet();
        set.setOutputFileNameMapping( "${artifactId}.${extension}" );

        assembly.addDependencySet( set );

        Assembly outputAssembly = interpolator.interpolate( assembly, model, Collections.EMPTY_MAP );
        
        List outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );
        
        DependencySet outputSet = (DependencySet) outputDependencySets.get( 0 );
        
        assertEquals( set.getOutputFileNameMapping(), outputSet.getOutputFileNameMapping() );
    }

}
