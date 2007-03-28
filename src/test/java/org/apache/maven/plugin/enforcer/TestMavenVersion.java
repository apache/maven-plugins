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
