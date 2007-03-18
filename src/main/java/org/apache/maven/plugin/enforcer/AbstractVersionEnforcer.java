/**
 * 
 */
package org.apache.maven.plugin.enforcer;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
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

    /**
     * Flag to warn only if a version check fails.
     * 
     * @parameter expression="${enforcer.warn}" default-value="false"
     */
    private boolean warn = false;

    public boolean enforceVersion( String variableName, String requiredVersionRange, ArtifactVersion actualVersion )
        throws MojoExecutionException, MojoFailureException
    {
        boolean allowed = false;
        if ( StringUtils.isEmpty( requiredVersionRange ) )
        {
            throw new MojoExecutionException( variableName + " version can't be empty." );
        }
        else
        {

            VersionRange vr;
            Log log = this.getLog();
            String msg = "Detected " + variableName + " Version: " + actualVersion;
            if ( actualVersion.toString().equals( requiredVersionRange ) )
            {
                log.debug( msg + " is allowed." );
                allowed = true;
            }
            else
            {
                try
                {
                    vr = VersionRange.createFromVersionSpec( requiredVersionRange );

                    if ( vr.containsVersion( actualVersion ) || vr.toString().equals( requiredVersionRange ) )
                    {
                        log.debug( msg + " is allowed." );
                        allowed = true;
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
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new MojoExecutionException("The requested "+ variableName+" version "+ requiredVersionRange+" is invalid.",e);
                }
            }

            return allowed;
        }
    }
}
