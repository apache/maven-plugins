/**
 * 
 */
package org.apache.maven.plugin.enforcer;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author brianf
 * 
 */
public interface EnforcementRule
{
    public void execute( EnforcementRuleHelper helper)
        throws MojoExecutionException;

}
