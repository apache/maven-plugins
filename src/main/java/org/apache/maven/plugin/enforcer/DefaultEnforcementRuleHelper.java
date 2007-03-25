package org.apache.maven.plugin.enforcer;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class DefaultEnforcementRuleHelper
    implements EnforcementRuleHelper
{
    Log log;

    MavenSession session;

    public DefaultEnforcementRuleHelper( MavenSession session, Log log )
    {
        this.log = log;
        this.session = session;
    }

    public Log getLog()
    {
        return log;
    }

    public MavenSession getSession()
    {
        return session;
    }

    public Object getComponent( Class clazz )
        throws ComponentLookupException
    {
        return session.lookup( clazz.getName() );
    }

    public MavenProject getProject()
        throws ComponentLookupException
    {
        return (MavenProject) getComponent( MavenProject.class );
    }

    public RuntimeInformation getRuntimeInformation()
        throws ComponentLookupException
    {
        return (RuntimeInformation) getComponent( RuntimeInformation.class );
    }
}
