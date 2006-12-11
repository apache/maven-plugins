package org.apache.maven.plugin.ear;

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


/**
 * The JBoss specific configuration, used to generate the jboss-app.xml
 * deployment descriptor file
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
class JbossConfiguration
{
    static final String VERSION_3_2 = "3.2";

    static final String VERSION_4 = "4";

    static final String VERSION = "version";

    static final String SECURITY_DOMAIN = "security-domain";

    static final String UNAUHTHENTICTED_PRINCIPAL = "unauthenticated-principal";

    static final String JMX_NAME = "jmx-name";


    private final String version;

    private boolean jbossThreeDot2;

    private boolean jbossFour;

    private final String securityDomain;

    private final String unauthenticatedPrincipal;

    private final String jmxName;


    public JbossConfiguration( String version, String securityDomain, String unauthenticatedPrincipal, String jmxName )
        throws EarPluginException
    {
        if ( version == null )
        {
            throw new EarPluginException( "jboss version could not be null." );
        }
        else
        {
            this.version = version;
            if ( version.equals( JbossConfiguration.VERSION_3_2 ) )
            {
                this.jbossThreeDot2 = true;
            }
            else if ( version.equals( JbossConfiguration.VERSION_4 ) )
            {
                this.jbossFour = true;
            }
            else
            {
                throw new EarPluginException(
                    "Invalid JBoss configuration, version[" + version + "] is not supported." );
            }
            this.securityDomain = securityDomain;
            this.unauthenticatedPrincipal = unauthenticatedPrincipal;
            this.jmxName = jmxName;
        }
    }

    /**
     * Returns the targeted version of JBoss.
     *
     * @return the jboss version
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Returns true if the targeted JBoss version is 3.2.
     *
     * @return if the targeted version is 3.2
     */
    public boolean isJbossThreeDot2()
    {
        return jbossThreeDot2;
    }

    /**
     * Returns true if the targeted JBoss version is 4.
     *
     * @return if the targeted version is 4
     */
    public boolean isJbossFour()
    {
        return jbossFour;
    }

    /**
     * The security-domain element specifies the JNDI name of the security
     * manager that implements the EJBSecurityManager and RealmMapping for
     * the domain. When specified at the jboss level it specifies the security
     * domain for all j2ee components in the deployment unit.
     * <p/>
     * One can override the global security-domain at the container
     * level using the security-domain element at the container-configuration
     * level.
     * <p/>
     * Only available as from JBoss 4.
     *
     * @return the JNDI name of the security manager
     */
    public String getSecurityDomain()
    {
        return securityDomain;
    }

    /**
     * The unauthenticated-principal element specifies the name of the principal
     * that will be returned by the EJBContext.getCallerPrincipal() method if there
     * is no authenticated user. This Principal has no roles or privaledges to call
     * any other beans.
     * <p/>
     * Only available as from JBoss 4.
     *
     * @return the unauthenticated principal
     */
    public String getUnauthenticatedPrincipal()
    {
        return unauthenticatedPrincipal;
    }

    /**
     * The jmx-name element allows one to specify the JMX ObjectName to use
     * for the MBean associated with the ear module. This must be a unique
     * name and valid JMX ObjectName string.
     *
     * @return the object name of the ear mbean
     */
    public String getJmxName()
    {
        return jmxName;
    }

}
