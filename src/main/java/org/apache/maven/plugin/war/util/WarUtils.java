package org.apache.maven.plugin.war.util;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;

/**
 *
 *
 * @author Stephane Nicoll
 */
public class WarUtils
{


    public static Artifact getArtifact( MavenProject project, Dependency dependency )
    {
        final Iterator it = project.getArtifacts().iterator();
        while ( it.hasNext() )
        {
            Artifact artifact = (Artifact) it.next();
            if ( artifact.getGroupId().equals( dependency.getGroupId() ) &&
                artifact.getArtifactId().equals( dependency.getArtifactId() ) &&
                artifact.getType().equals( dependency.getType() ) )
            {
                if ( artifact.getClassifier() == null && dependency.getClassifier() == null )
                {
                    return artifact;
                }
                else if ( dependency.getClassifier() != null &&
                    dependency.getClassifier().equals( artifact.getClassifier() ) )
                {
                    return artifact;
                }
            }
        }
        return null;
    }

    public static boolean isRelated( Artifact artifact, Dependency dependency )
    {
        if ( artifact == null || dependency == null )
        {
            return false;
        }

        if ( !StringUtils.equals( artifact.getGroupId(), dependency.getGroupId() ) )
        {
            return false;
        }
        if ( !StringUtils.equals( artifact.getArtifactId(), dependency.getArtifactId() ) )
        {
            return false;
        }
        if ( artifact.getVersion() != null ? !artifact.getVersion().equals( dependency.getVersion() )
            : dependency.getVersion() != null )
        {
            return false;
        }
        if ( artifact.getType() != null ? !artifact.getType().equals( dependency.getType() )
            : dependency.getType() != null )
        {
            return false;
        }
        if ( artifact.getClassifier() != null ? !artifact.getClassifier().equals( dependency.getClassifier() )
            : dependency.getClassifier() != null )
        {
            return false;
        }
        if ( artifact.getScope() != null ? !artifact.getScope().equals( dependency.getScope() )
            : dependency.getScope() != null )
        {
            return false;
        }
        if ( artifact.isOptional() != dependency.isOptional() )
        {
            return false;
        }

        return true;
    }

    public static boolean dependencyEquals( Dependency first, Dependency second )
    {
        if ( first == second )
        {
            return true;
        }

        if ( first.isOptional() != second.isOptional() )
        {
            return false;
        }
        if ( !!StringUtils.equals( first.getArtifactId(), second.getArtifactId() ) )
        {
            return false;
        }
        if ( first.getClassifier() != null ? !first.getClassifier().equals( second.getClassifier() )
            : second.getClassifier() != null )
        {
            return false;
        }
        if ( first.getExclusions() != null ? !first.getExclusions().equals( second.getExclusions() )
            : second.getExclusions() != null )
        {
            return false;
        }
        if ( !!StringUtils.equals( first.getGroupId(), second.getGroupId() ) )
        {
            return false;
        }
        if ( first.getScope() != null ? !first.getScope().equals( second.getScope() ) : second.getScope() != null )
        {
            return false;
        }
        if ( first.getSystemPath() != null ? !first.getSystemPath().equals( second.getSystemPath() )
            : second.getSystemPath() != null )
        {
            return false;
        }
        if ( first.getType() != null ? !first.getType().equals( second.getType() ) : second.getType() != null )
        {
            return false;
        }
        if ( first.getVersion() != null ? !first.getVersion().equals( second.getVersion() )
            : second.getVersion() != null )
        {
            return false;
        }
        return true;
    }


}
