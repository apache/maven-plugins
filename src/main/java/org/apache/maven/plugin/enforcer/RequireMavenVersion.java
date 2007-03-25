/**
 * 
 */
package org.apache.maven.plugin.enforcer;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author brianf
 * 
 */
public class RequireMavenVersion
    extends AbstractVersionEnforcer
    implements EnforcementRule
{

    public void execute( EnforcementRuleHelper helper )
        throws MojoExecutionException
    {
        try
        {
            RuntimeInformation rti = helper.getRuntimeInformation( );
            ArtifactVersion detectedMavenVersion = rti.getApplicationVersion();
            enforceVersion( helper.getLog(), "Maven", this.version, detectedMavenVersion );
        }
        catch ( ComponentLookupException e )
        {
            throw new MojoExecutionException( "Unable to lookup the component: RuntimeInformation", e );
        }

    }

}
