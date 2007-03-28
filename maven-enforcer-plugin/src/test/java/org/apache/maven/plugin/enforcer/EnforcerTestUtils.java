package org.apache.maven.plugin.enforcer;

import java.util.Date;

import org.apache.maven.execution.MavenSession;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class EnforcerTestUtils
{
    public static MavenSession getMavenSession()
    {
        return new MavenSession( new MockPlexusContainer(), null, null, null, null, null, null, null, new Date() );
    }
}
