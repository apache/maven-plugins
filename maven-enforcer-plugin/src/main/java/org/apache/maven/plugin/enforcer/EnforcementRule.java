/**
 * 
 */
package org.apache.maven.plugin.enforcer;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 * 
 */
public interface EnforcementRule
{
    public void execute(MavenSession session, Log log)
        throws MojoExecutionException;

}
