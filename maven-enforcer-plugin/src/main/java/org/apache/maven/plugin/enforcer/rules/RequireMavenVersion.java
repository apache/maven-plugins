/**
 * 
 */
package org.apache.maven.plugin.enforcer.rules;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author brianf
 * 
 */
public class RequireMavenVersion
    extends AbstractVersionEnforcer
    implements EnforcementRule
{

    public void execute( MavenSession session, Log log )
        throws MojoExecutionException
    {
        try
        {
            RuntimeInformation rti = (RuntimeInformation) session
                .lookup( "org.apache.maven.execution.RuntimeInformation" );
            ArtifactVersion detectedMavenVersion = rti.getApplicationVersion();
            enforceVersion( log, "Maven", this.version, detectedMavenVersion );
        }
        catch ( ComponentLookupException e )
        {
            throw new MojoExecutionException( "Unable to lookup the component: RuntimeInformation", e );
        }

    }

}
