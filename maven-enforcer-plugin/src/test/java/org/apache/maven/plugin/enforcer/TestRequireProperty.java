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

import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;

/**
 * @author Paul Gier
 * 
 */
public class TestRequireProperty extends TestCase
{
    public void testRule() throws EnforcerRuleException
    {
    	MockProject project = new MockProject();
    	project.setProperty("testProp", "This is a test.");
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper(project);

        RequireProperty rule = new RequireProperty();
        // this property should not be set
        rule.property = "testPropJunk";

        try
        {
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            // expected to catch this.
        }

        // this property should be set by the surefire plugin
        rule.property = "testProp";
        try 
        {
        	rule.execute( helper );
        } 
        catch ( EnforcerRuleException e )
        {
        	fail("This should not throw an exception");
        }
    }
    
    public void testRuleWithRegex() throws EnforcerRuleException
    {
    	MockProject project = new MockProject();
    	project.setProperty("testProp", "This is a test.");
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper(project);

        RequireProperty rule = new RequireProperty();
        rule.property = "testProp";
        // This expression should not match the property value
        rule.regex = "[^abc]";
        
        try
        {
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            // expected to catch this.
        }

        // this expr should match the property
        rule.regex = "[This].*[.]";
        try 
        {
        	rule.execute( helper );
        } 
        catch ( EnforcerRuleException e )
        {
        	fail("This should not throw an exception");
        }
    }
}
