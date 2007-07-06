package org.apache.maven.plugin.enforcer.util;

import java.util.Iterator;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class EnforcerUtils
{
    public static synchronized boolean containsVersion( VersionRange allowedRange, ArtifactVersion theVersion )
    {
        boolean matched = false;
        ArtifactVersion recommendedVersion = allowedRange.getRecommendedVersion();
        if ( recommendedVersion == null )
        {

            for ( Iterator i = allowedRange.getRestrictions().iterator(); i.hasNext() && !matched; )
            {
                Restriction restriction = (Restriction) i.next();
                if ( restriction.containsVersion( theVersion ) )
                {
                    matched = true;
                }
            }
        }
        else
        {
            // only singular versions ever have a recommendedVersion
            int compareTo = recommendedVersion.compareTo( theVersion );
            matched = ( compareTo <= 0 );
        }
        return matched;
    }
}
