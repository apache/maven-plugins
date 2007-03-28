package org.apache.maven.plugin.enforcer;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestMavenVersion
    extends TestCase
{
    public void testRule()
    throws EnforcerRuleException
{
    
        
    String thisVersion = RequireJavaVersion.fixJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED );

    RequireJavaVersion rule = new RequireJavaVersion();
    rule.setVersion( thisVersion );

    EnforcerRuleHelper helper = new DefaultEnforcementRuleHelper( EnforcerTestUtils.getMavenSession(),
                                                                  new SystemStreamLog() );

    // test the singular version
    rule.execute( helper );

    // exclude this version
    rule.setVersion( "(" + thisVersion );

    try
    {
        rule.execute( helper );
        fail( "Expected an exception." );
    }
    catch ( EnforcerRuleException e )
    {
        // expected to catch this.
    }
    
    //this shouldn't crash
    rule.setVersion( SystemUtils.JAVA_VERSION_TRIMMED );
    rule.execute( helper );

}
}
