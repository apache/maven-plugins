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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;

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
        assertEquals( "1.5.0-11", RequireJavaVersion.normalizeJDKVersion( "1.5.0_11" ) );
        assertEquals( "1.5.1", RequireJavaVersion.normalizeJDKVersion( "1.5.1" ) );
        assertEquals( "1.5.2-1", RequireJavaVersion.normalizeJDKVersion( "1.5.2-1.b11" ) );
        assertEquals( "1.5.3-11", RequireJavaVersion.normalizeJDKVersion( "1.5.3_11" ) );
        assertEquals( "1.5.4-5", RequireJavaVersion.normalizeJDKVersion( "1.5.4.5_11" ) );
        assertEquals( "1.5.5-6", RequireJavaVersion.normalizeJDKVersion( "1.5.5.6_11.2" ) );

        // test for non-standard versions
        assertEquals( "1.5.0-11", RequireJavaVersion.normalizeJDKVersion( "1-5-0-11" ) );
        assertEquals( "1.5.0-11", RequireJavaVersion.normalizeJDKVersion( "1-_5-_0-_11" ) );
        assertEquals( "1.5.0-11", RequireJavaVersion.normalizeJDKVersion( "1_5_0_11" ) );
        assertEquals( "1.5.0-7", RequireJavaVersion.normalizeJDKVersion( "1.5.0-07" ) );
        assertEquals( "1.5.0-7", RequireJavaVersion.normalizeJDKVersion( "1.5.0-b7" ) );
        assertEquals( "1.5.0-7", RequireJavaVersion.normalizeJDKVersion( "1.5.0-;7" ) );
    }

    public void testRule()
        throws EnforcerRuleException
    {
        String thisVersion = RequireJavaVersion.normalizeJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED );

        RequireJavaVersion rule = new RequireJavaVersion();
        rule.setVersion( thisVersion );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();

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
