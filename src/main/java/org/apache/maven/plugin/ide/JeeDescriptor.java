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

public class JeeDescriptor
{
    private String jeeVersion;

    private String ejbVersion;

    private String servletVersion;

    private String jspVersion;

    public static final String J2EE_1_2 = "1.2";

    public static final String J2EE_1_3 = "1.3";

    public static final String J2EE_1_4 = "1.4";

    public static final String J2EE_5_0 = "5.0";

    public static final String EJB_1_1 = "1.1";

    public static final String EJB_2_0 = "2.0";

    public static final String EJB_2_1 = "2.1";

    public static final String EJB_3_0 = "3.0";

    public static final String SERVLET_2_2 = "2.2";

    public static final String SERVLET_2_3 = "2.3";

    public static final String SERVLET_2_4 = "2.4";

    public static final String SERVLET_2_5 = "2.5";

    public static final String JSP_1_1 = "1.1";

    public static final String JSP_1_2 = "1.2";

    public static final String JSP_2_0 = "2.0";

    public static final String JSP_2_1 = "2.1";

    /**
     * @param jeeVersion
     * @param ejbVersion
     * @param servletVersion
     * @param jspVersion
     */
    public JeeDescriptor( String jeeVersion, String ejbVersion, String servletVersion, String jspVersion )
    {
        super();
        this.jeeVersion = jeeVersion;
        this.ejbVersion = ejbVersion;
        this.servletVersion = servletVersion;
        this.jspVersion = jspVersion;
    }

    /**
     * @return the ejbVersion
     */
    public String getEjbVersion()
    {
        return ejbVersion;
    }

    /**
     * @return the jeeVersion
     */
    public String getJeeVersion()
    {
        return jeeVersion;
    }

    /**
     * @return the jspVersion
     */
    public String getJspVersion()
    {
        return jspVersion;
    }

    /**
     * @return the servletVersion
     */
    public String getServletVersion()
    {
        return servletVersion;
    }
}
