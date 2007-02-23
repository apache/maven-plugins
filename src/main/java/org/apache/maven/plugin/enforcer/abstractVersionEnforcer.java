/**
 * 
 */
package org.apache.maven.plugin.enforcer;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author brianf
 * 
 */
public abstract class abstractVersionEnforcer
    extends AbstractMojo
{

    public void enforceVersion( String variableName, String requiredVersionRange, ArtifactVersion actualVersion,
                               boolean warn )
        throws MojoExecutionException, MojoFailureException
    {
        if ( StringUtils.isEmpty( requiredVersionRange ) )
        {
            throw new MojoExecutionException( variableName + " version can't be empty." );
        }

        VersionRange vr;

        vr = VersionRange.createFromVersion( requiredVersionRange );

        Log log = this.getLog();
        String msg = "Detected " + variableName + " Version: " + actualVersion;
        if ( vr.containsVersion( actualVersion ) )
        {
            log.debug( msg + " is allowed." );
        }
        else
        {
            String error = msg + " is not in the allowed range: " + vr;
            if ( warn )
            {
                log.warn( error );
            }
            else
            {
                throw new MojoExecutionException( error );
            }
        }
    }
}
