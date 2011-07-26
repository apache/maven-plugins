package org.apache.maven.plugin.eclipse;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;

public class WorkspaceConfigurationTest
    extends TestCase
{

    public void testGetWebsphereVersion() 
    {
        WorkspaceConfiguration wc = new WorkspaceConfiguration();
        // Websphere Application Servers
        final String was_express_v51 = "was.express.v51";
        wc.setDefaultDeployServerId( was_express_v51 );
        assertEquals( "5.1", wc.getWebsphereVersion() );

        final String was_base_v51 = "was.base.v51";
        wc.setDefaultDeployServerId( was_base_v51 );
        assertEquals( "5.1", wc.getWebsphereVersion() );
        
        final String was_base_v6 = "was.base.v6";
        wc.setDefaultDeployServerId( was_base_v6 );
        assertEquals( "6.0", wc.getWebsphereVersion() );
        
        final String was_base_v61 = "was.base.v61";
        wc.setDefaultDeployServerId( was_base_v61 );
        assertEquals( "6.1", wc.getWebsphereVersion() );
        
        final String was_base_v7 = "was.base.v7";
        wc.setDefaultDeployServerId( was_base_v7 );
        assertEquals( "7.0", wc.getWebsphereVersion() );
        
        // Websphere Portals
        //wps.base.v51
        //wps.base.v60
        //wps.base.v61
    }
}
