package org.apache.maven.plugin.enforcer;

import org.apache.maven.shared.enforcer.rule.api.EnforcerRule;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MockEnforcerRule
    implements EnforcerRule
{

    public boolean failRule = false;
    
    public MockEnforcerRule (boolean fail)
    {
        this.failRule = fail;
    }
    
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        if (isFailRule())
        {
            throw new EnforcerRuleException(" this condition is not allowed.");
        }
    }

    /**
     * @return the failRule
     */
    public boolean isFailRule()
    {
        return this.failRule;
    }

    /**
     * @param theFailRule the failRule to set
     */
    public void setFailRule( boolean theFailRule )
    {
        this.failRule = theFailRule;
    }

}
