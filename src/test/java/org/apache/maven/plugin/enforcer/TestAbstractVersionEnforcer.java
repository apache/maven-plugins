package org.apache.maven.plugin.enforcer;

import junit.framework.TestCase;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestAbstractVersionEnforcer
    extends TestCase
{
    public void testContainsVersion()
        throws InvalidVersionSpecificationException
    {
        RequireMavenVersion rule = new RequireMavenVersion();
        ArtifactVersion version = new DefaultArtifactVersion( "2.0.5" );
        // test ranges
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[2.0.5,)" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[2.0.4,)" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[2.0.4,2.0.5]" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[2.0.4,2.0.6]" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[2.0.4,2.0.6)" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[2.0,)" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[2.0.0,)" ), version ) );
        // not matching versions
        assertFalse( rule.containsVersion( VersionRange.createFromVersionSpec( "[2.0.4,2.0.5)" ), version ) );
        assertFalse( rule.containsVersion( VersionRange.createFromVersionSpec( "[2.0.6,)" ), version ) );
        assertFalse( rule.containsVersion( VersionRange.createFromVersionSpec( "(2.0.5,)" ), version ) );

        // test singular versions -> 2.0.5 == [2.0.5,) or x >= 2.0.5
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "2.0" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "2.0.4" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "2.0.5" ), version ) );

        assertFalse( rule.containsVersion( VersionRange.createFromVersionSpec( "2.0.6" ), version ) );
    }

    private void enforceFalse( AbstractVersionEnforcer rule, Log log, String var, String range, ArtifactVersion version )
    {
        try
        {
            rule.enforceVersion( log, var, range, version );
            fail( "Expected to receive EnforcerRuleException because:" + version + " is not contained by " + range );
        }
        catch ( Exception e )
        {
            if ( e instanceof EnforcerRuleException )
            {
                // log.info( "Caught Expected Exception: " +
                // e.getLocalizedMessage() );
            }
            else
            {
                fail( "Received wrong exception. Expected EnforcerRuleExeption. Received:" + e.toString() );
            }
        }
    }

    public void testEnforceVersion()
    {
        RequireMavenVersion rule = new RequireMavenVersion();
        ArtifactVersion version = new DefaultArtifactVersion( "2.0.5" );
        SystemStreamLog log = new SystemStreamLog();
        // test ranges

        // not matching versions
        try
        {
            rule.enforceVersion( log, "test", "[2.0.5,)", version );
            rule.enforceVersion( log, "test", "[2.0.4,)", version );
            rule.enforceVersion( log, "test", "[2.0.4,2.0.5]", version );
            rule.enforceVersion( log, "test", "[2.0.4,2.0.6]", version );
            rule.enforceVersion( log, "test", "[2.0.4,2.0.6)", version );
            rule.enforceVersion( log, "test", "[2.0,)", version );
            rule.enforceVersion( log, "test", "[2.0.0,)", version );

            // test singular versions -> 2.0.5 == [2.0.5,) or x >= 2.0.5
            rule.enforceVersion( log, "test", "2.0", version );
            rule.enforceVersion( log, "test", "2.0.4", version );
            rule.enforceVersion( log, "test", "2.0.5", version );
        }
        catch ( Exception e )
        {
            fail( "No Exception expected. Caught:" + e.getLocalizedMessage() );

        }

        enforceFalse( rule, log, "test", "[2.0.6,)", version );
        enforceFalse( rule, log, "test", "(2.0.5,)", version );
        enforceFalse( rule, log, "test", "2.0.6", version );

        enforceFalse( rule, log, "test", "[2.0.4,2.0.5)", version );

        // make sure to handle the invalid range specification
        enforceFalse( rule, log, "test", "[[2.0.4,2.0.5)", version );

    }
}
