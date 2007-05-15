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
package org.apache.maven.plugin.ide;

import java.util.HashMap;
import java.util.Map;

public class JeeUtils
{
    private static final Map jeeMap = new HashMap();

    private static final Map ejbMap = new HashMap();

    private static final Map servletMap = new HashMap();

    private static final Map jspMap = new HashMap();

    static
    {
        addJEE( JeeDescriptor.J2EE_5_0, JeeDescriptor.EJB_3_0, JeeDescriptor.SERVLET_2_5, JeeDescriptor.JSP_2_1 );
        addJEE( JeeDescriptor.J2EE_1_4, JeeDescriptor.EJB_2_1, JeeDescriptor.SERVLET_2_4, JeeDescriptor.JSP_2_0 );
        addJEE( JeeDescriptor.J2EE_1_3, JeeDescriptor.EJB_2_0, JeeDescriptor.SERVLET_2_3, JeeDescriptor.JSP_1_2 );
        addJEE( JeeDescriptor.J2EE_1_2, JeeDescriptor.EJB_1_1, JeeDescriptor.SERVLET_2_2, JeeDescriptor.JSP_1_1 );

    }

    private static void addJEE( String jeeVersion, String ejbVersion, String servletVersion, String jspVersion )
    {
        JeeDescriptor descriptor = new JeeDescriptor( jeeVersion, ejbVersion, servletVersion, jspVersion );
        jeeMap.put( jeeVersion, descriptor );
        ejbMap.put( ejbVersion, descriptor );
        servletMap.put( servletVersion, descriptor );
        jspMap.put( jspVersion, descriptor );
    }

    public final static JeeDescriptor getJeeDescriptorFromJeeVersion( String jeeVersion )
    {
        if ( jeeMap.containsKey( jeeVersion ) )
            return (JeeDescriptor) jeeMap.get( jeeVersion );
        else
            return null;
    }

    public final static JeeDescriptor getJeeDescriptorFromEjbVersion( String ejbVersion )
    {
        if ( ejbMap.containsKey( ejbVersion ) )
            return (JeeDescriptor) ejbMap.get( ejbVersion );
        else
            return null;
    }

    public final static JeeDescriptor getJeeDescriptorFromServletVersion( String servletVersion )
    {
        if ( servletMap.containsKey( servletVersion ) )
            return (JeeDescriptor) servletMap.get( servletVersion );
        else
            return null;
    }

    public final static JeeDescriptor getJeeDescriptorFromJspVersion( String jspVersion )
    {
        if ( jspMap.containsKey( jspVersion ) )
            return (JeeDescriptor) jspMap.get( jspVersion );
        else
            return null;
    }

}
