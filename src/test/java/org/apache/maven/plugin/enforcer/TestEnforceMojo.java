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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRule;

import junit.framework.TestCase;

/**
 * Exhaustively check the enforcer mojo.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestEnforceMojo
    extends TestCase
{

    public void testEnforceMojo() throws MojoExecutionException
    {
        EnforceMojo mojo = new EnforceMojo();
        mojo.setFail( false );
        
        try
        {
            mojo.execute();
            fail("Expected a Mojo Execution Exception.");
        }
        catch ( MojoExecutionException e )
        {
            System.out.println("Caught Expected Exception:"+e.getLocalizedMessage());
        }
        
        EnforcerRule[] rules = new EnforcerRule[10];
        rules[0] = new MockEnforcerRule(true);
        mojo.setRules( rules );
        
        mojo.execute();
        
        try
        {
            mojo.setFail( true );
            mojo.execute();
            fail("Expected a Mojo Execution Exception.");
        }
        catch ( MojoExecutionException e )
        {
            System.out.println("Caught Expected Exception:"+e.getLocalizedMessage());
        }

        ((MockEnforcerRule) rules[0]).setFailRule(false);
        
        mojo.execute();
        
    }
}
