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
    
    public void testShouldNotIncludeDirectlyMatchedArtifactByDependencyConflictId()
    {
        tck.testShouldIncludeDirectlyMatchedArtifactByDependencyConflictId( true );
    }

    public void testShouldNotIncludeDirectlyMatchedArtifactByGroupIdArtifactId()
    {
        tck.testShouldIncludeDirectlyMatchedArtifactByGroupIdArtifactId( true );
    }

    public void testShouldNotIncludeWhenPatternMatchesDependencyTrailAndTransitivityIsEnabled()
    {
        tck.testShouldIncludeWhenPatternMatchesDependencyTrailAndTransitivityIsEnabled( true );
    }

    public void testShouldIncludeWhenArtifactIdDiffers()
    {
        tck.testShouldNotIncludeWhenArtifactIdDiffers( true );
    }

    public void testShouldIncludeWhenBothIdElementsDiffer()
    {
        tck.testShouldNotIncludeWhenBothIdElementsDiffer( true );
    }

    public void testShouldIncludeWhenGroupIdDiffers()
    {
        tck.testShouldNotIncludeWhenGroupIdDiffers( true );
    }

    public void testShouldIncludeWhenNegativeMatch()
    {
        tck.testShouldNotIncludeWhenNegativeMatch( true );
    }

    public void testShouldNotIncludeWhenWildcardMatchesInSequence()
    {
        tck.testShouldIncludeWhenWildcardMatchesInSequence( true );
    }

}
