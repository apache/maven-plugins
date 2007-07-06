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

import junit.framework.TestCase;

import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleHelper;

/**
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestNoSnapshots
    extends TestCase
{

    public void testRule()
        throws IOException
    {
        ArtifactStubFactory factory = new ArtifactStubFactory();
        MockProject project = new MockProject();
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
        project.setArtifacts( factory.getMixedArtifacts() );
        project.setDependencyArtifacts( factory.getScopedArtifacts() );
        NoSnapshots rule = new NoSnapshots();

        rule.setSearchTransitive( false );

        execute( rule, helper, false );

        rule.setSearchTransitive( true );

        execute( rule, helper, true );

    } 

    /**
     * Simpler wrapper to execute and deal with the expected
     * result.
     * 
     * @param rule
     * @param helper
     * @param shouldFail
     */
    private void execute( NoSnapshots rule, EnforcerRuleHelper helper, boolean shouldFail )
    {
        try
        {
            rule.message = "Test Message";
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
            helper.getLog().debug(e.getMessage());
        }
    }
}
