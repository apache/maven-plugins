/**
 * 
 */
package org.apache.maven.plugin.enforcer;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.RuntimeInformation;

/**
 * @author brianf
 *
 */
public class MockRuntimeInformation
    implements RuntimeInformation
{

    public ArtifactVersion getApplicationVersion()
    {
        return new DefaultArtifactVersion("2.0.5");
    }

}
