package org.apache.maven.plugin.enforcer;

import junit.framework.TestCase;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class TestDefaultEnforcementRuleHelper extends TestCase
{
    public void testHelper() throws ComponentLookupException
    {
        Log log = new SystemStreamLog();
        DefaultEnforcementRuleHelper helper = new DefaultEnforcementRuleHelper(EnforcerTestUtils.getMavenSession(),log);
        
        assertSame( log, helper.getLog() );
        
        assertNotNull( helper.getSession());
        assertNotNull(helper.getProject());
        assertNotNull( helper.getRuntimeInformation() );
        
    }
}
