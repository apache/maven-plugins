/**
 * 
 */
package org.apache.maven.plugin.enforcer;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author brianf
 * 
 */
public interface EnforcementRuleHelper
{
    public Log getLog();

    public MavenSession getSession();

    public Object getComponent( Class clazz )
        throws ComponentLookupException;

    public MavenProject getProject()
        throws ComponentLookupException;

    public RuntimeInformation getRuntimeInformation()
        throws ComponentLookupException;
}
