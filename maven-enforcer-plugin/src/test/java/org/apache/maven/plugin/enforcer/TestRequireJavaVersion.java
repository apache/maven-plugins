package org.apache.maven.plugin.enforcer;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestRequireJavaVersion
    extends TestCase
{
    public void testFixJDKVersion()
    {
        // test that we only take the first 3 versions for comparision
        assertEquals( "1.5.0", RequireJavaVersion.fixJDKVersion( "1.5.0-11" ) );
        assertEquals( "1.5.1", RequireJavaVersion.fixJDKVersion( "1.5.1" ) );
        assertEquals( "1.5.2", RequireJavaVersion.fixJDKVersion( "1.5.2-b11" ) );
        assertEquals( "1.5.3", RequireJavaVersion.fixJDKVersion( "1.5.3_11" ) );
        assertEquals( "1.5.4", RequireJavaVersion.fixJDKVersion( "1.5.4.5_11" ) );
        assertEquals( "1.5.5", RequireJavaVersion.fixJDKVersion( "1.5.5.6_11.2" ) );

        // test for non-standard versions
        assertEquals( "1.5.0", RequireJavaVersion.fixJDKVersion( "1-5-0-11" ) );
        assertEquals( "1.5.0", RequireJavaVersion.fixJDKVersion( "1-_5-_0-_11" ) );
        assertEquals( "1.5.0", RequireJavaVersion.fixJDKVersion( "1_5_0_11" ) );

    }
}
