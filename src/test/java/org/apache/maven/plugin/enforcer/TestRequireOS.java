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

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.codehaus.plexus.util.Os;

/**
 * Exhaustively check the OS mojo.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestRequireOS
    extends TestCase
{
    public void testOS()
    {
        Log log = new SystemStreamLog();
      
        RequireOS rule = new RequireOS();
        rule.displayOSInfo( log, true );

        Iterator iter = rule.getValidFamilies().iterator();
        String validFamily = null;
        String invalidFamily = null;
        while ( iter.hasNext() && ( validFamily == null || invalidFamily == null ) )
        {
            String fam = (String) iter.next();
            if ( Os.isFamily( fam ) )
            {
                validFamily = fam;
            }
            else
            {
                invalidFamily = fam;
            }
        }

        log.info( "Testing Mojo Using Valid Family: " + validFamily + " Invalid Family: " + invalidFamily );

        rule.setFamily( validFamily );
        assertTrue( rule.isAllowed() );

        rule.setFamily( invalidFamily );
        assertFalse( rule.isAllowed() );

        rule.setFamily( "!" + invalidFamily );
        assertTrue( rule.isAllowed() );

        rule.setFamily( "junk" );
        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
            fail( "Expected MojoExecution Exception becuase of invalid family type" );
        }
        catch ( EnforcerRuleException e )
        {
            log.info( "Caught Expected Exception:" + e.getLocalizedMessage() );
        }

        rule.setFamily( null );
        rule.setArch( RequireOS.OS_ARCH );
        assertTrue( rule.isAllowed() );

        rule.setArch( "somecrazyarch" );
        assertFalse( rule.isAllowed() );

        rule.setArch( "!somecrazyarch" );
        assertTrue( rule.isAllowed() );

        rule.setArch( null );

        rule.setName( RequireOS.OS_NAME );
        assertTrue( rule.isAllowed() );

        rule.setName( "somecrazyname" );
        assertFalse( rule.isAllowed() );

        rule.setName( "!somecrazyname" );
        assertTrue( rule.isAllowed() );

        rule.setName( null );

        rule.setVersion( RequireOS.OS_VERSION );
        assertTrue( rule.isAllowed() );

        rule.setVersion( "somecrazyversion" );
        assertFalse( rule.isAllowed() );

        rule.setVersion( "!somecrazyversion" );
        assertTrue( rule.isAllowed() );
    }

}
