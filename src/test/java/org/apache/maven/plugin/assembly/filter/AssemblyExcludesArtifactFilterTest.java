package org.apache.maven.plugin.assembly.filter;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.util.List;

import junit.framework.TestCase;


public class AssemblyExcludesArtifactFilterTest
    extends TestCase
{
    
    private AssemblyArtifactFilterTCK tck = new AssemblyArtifactFilterTCK()
    {

        protected ArtifactFilter createFilter( List patterns )
        {
            return new AssemblyExcludesArtifactFilter( patterns );
        }

        protected ArtifactFilter createFilter( List patterns, boolean actTransitively )
        {
            return new AssemblyExcludesArtifactFilter( patterns, actTransitively );
        }
        
    };
    
    public void testShouldIncludeDirectlyMatchedArtifactByDependencyConflictId()
    {
        tck.testShouldIncludeDirectlyMatchedArtifactByDependencyConflictId( true );
    }

    public void testShouldIncludeDirectlyMatchedArtifactByGroupIdArtifactId()
    {
        tck.testShouldIncludeDirectlyMatchedArtifactByGroupIdArtifactId( true );
    }

    public void testShouldIncludeWhenPatternMatchesDependencyTrailAndTransitivityIsEnabled()
    {
        tck.testShouldIncludeWhenPatternMatchesDependencyTrailAndTransitivityIsEnabled( true );
    }

    public void testShouldNotIncludeWhenArtifactIdDiffers()
    {
        tck.testShouldNotIncludeWhenArtifactIdDiffers( true );
    }

    public void testShouldNotIncludeWhenBothIdElementsDiffer()
    {
        tck.testShouldNotIncludeWhenBothIdElementsDiffer( true );
    }

    public void testShouldNotIncludeWhenGroupIdDiffers()
    {
        tck.testShouldNotIncludeWhenGroupIdDiffers( true );
    }

}
