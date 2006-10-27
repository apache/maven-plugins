package org.apache.maven.plugin.assembly.filter;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.util.List;

import junit.framework.TestCase;


public class AssemblyIncludesArtifactFilterTest
    extends TestCase
{
    private AssemblyArtifactFilterTCK tck = new AssemblyArtifactFilterTCK()
    {

        protected ArtifactFilter createFilter( List patterns )
        {
            return new AssemblyIncludesArtifactFilter( patterns );
        }

        protected ArtifactFilter createFilter( List patterns, boolean actTransitively )
        {
            return new AssemblyIncludesArtifactFilter( patterns, actTransitively );
        }
        
    };
    
    public void testShouldIncludeDirectlyMatchedArtifactByDependencyConflictId()
    {
        tck.testShouldIncludeDirectlyMatchedArtifactByDependencyConflictId( false );
    }

    public void testShouldIncludeDirectlyMatchedArtifactByGroupIdArtifactId()
    {
        tck.testShouldIncludeDirectlyMatchedArtifactByGroupIdArtifactId( false );
    }

    public void testShouldIncludeWhenPatternMatchesDependencyTrailAndTransitivityIsEnabled()
    {
        tck.testShouldIncludeWhenPatternMatchesDependencyTrailAndTransitivityIsEnabled( false );
    }

    public void testShouldNotIncludeWhenArtifactIdDiffers()
    {
        tck.testShouldNotIncludeWhenArtifactIdDiffers( false );
    }

    public void testShouldNotIncludeWhenBothIdElementsDiffer()
    {
        tck.testShouldNotIncludeWhenBothIdElementsDiffer( false );
    }

    public void testShouldNotIncludeWhenGroupIdDiffers()
    {
        tck.testShouldNotIncludeWhenGroupIdDiffers( false );
    }

    public void testShouldNotIncludeWhenNegativeMatch()
    {
        tck.testShouldNotIncludeWhenNegativeMatch( false );
    }

    public void testShouldIncludeWhenWildcardMatchesInSequence()
    {
        tck.testShouldIncludeWhenWildcardMatchesInSequence( false );
    }

}
