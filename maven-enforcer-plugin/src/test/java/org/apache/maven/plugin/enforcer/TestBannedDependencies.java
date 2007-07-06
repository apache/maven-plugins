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

import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRule;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;

/**
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestBannedDependencies
    extends TestCase
{

    public void testRule()
        throws IOException
    {
        ArtifactStubFactory factory = new ArtifactStubFactory( null, false );
        MockProject project = new MockProject();
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
        project.setArtifacts( factory.getMixedArtifacts() );
        project.setDependencyArtifacts( factory.getScopedArtifacts() );
        BannedDependencies rule = new BannedDependencies();

        ArrayList excludes = new ArrayList();
        rule.setSearchTransitive( false );

        // test whole name
        excludes.add( "testGroupId:release:1.0" );
        rule.setExcludes( excludes );

        execute( rule, helper, false );

        // test group:artifact
        excludes.clear();
        excludes.add( "testGroupId:release" );
        execute( rule, helper, false );

        // test group
        excludes.clear();
        excludes.add( "testGroupId" );
        execute( rule, helper, false );

        // now check one that should be found in direct
        // dependencies
        excludes.clear();
        excludes.add( "g:compile:1.0" );
        execute( rule, helper, true );
        rule.setSearchTransitive( true );

        // whole name
        excludes.clear();
        excludes.add( "testGroupId:release:1.0" );
        execute( rule, helper, true );

        // group:artifact
        excludes.clear();
        excludes.add( "testGroupId:release" );
        execute( rule, helper, true );

        // group
        excludes.clear();
        excludes.add( "testGroupId" );
        execute( rule, helper, true );

        // now check wildcards
        excludes.clear();
        excludes.add( "*:release" );
        execute( rule, helper, true );

        // now check wildcards
        excludes.clear();
        excludes.add( "*:*:1.0" );
        execute( rule, helper, true );

        // now check wildcards
        excludes.clear();
        excludes.add( "*:release:*" );
        execute( rule, helper, true );

        // now check wildcards
        excludes.clear();
        excludes.add( "*:release:1.2" );
        execute( rule, helper, false );
        
        //now check multiple excludes
        excludes.add( "*:release:*");
        execute( rule, helper, true );
        
        //now check space trimming
        excludes.clear();
        excludes.add( "  testGroupId  :  release   :   1.0    " );
        execute( rule, helper, true );
        
        //now check weirdness
        excludes.clear();
        excludes.add( ":::" ); //null entry, won't match anything
        execute( rule, helper, false );
    }

    /**
     * Simpler wrapper to execute and deal with the expected
     * result.
     * 
     * @param rule
     * @param helper
     * @param shouldFail
     */
    private void execute( BannedDependencies rule, EnforcerRuleHelper helper, boolean shouldFail )
    {
        try
        {
            rule.message = null;
            rule.execute( helper );
            if ( shouldFail )
            {
                fail( "Exception expected." );
            }
        }
        catch ( EnforcerRuleException e )
        {
            if ( !shouldFail )
            {
                fail( "No Exception expected:" + e.getLocalizedMessage() );
            }
            //helper.getLog().debug(e.getMessage());
        }
    }
}
