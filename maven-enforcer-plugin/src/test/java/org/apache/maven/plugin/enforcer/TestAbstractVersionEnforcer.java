package org.apache.maven.plugin.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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

        version = new DefaultArtifactVersion( "1.5.0-7" );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[1.5.0,)" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[1.5,1.6)" ), version ) );

        version = new DefaultArtifactVersion( RequireJavaVersion.normalizeJDKVersion( "1.5.0-07" ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[1.5.0,)" ), version ) );
        assertTrue( rule.containsVersion( VersionRange.createFromVersionSpec( "[1.5,1.6)" ), version ) );

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
