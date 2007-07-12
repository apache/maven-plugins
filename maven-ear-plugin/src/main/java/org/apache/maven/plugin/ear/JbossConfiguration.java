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

    static final String VERSION_4_2 = "4.2";

    static final String VERSION = "version";

    static final String SECURITY_DOMAIN = "security-domain";

    static final String UNAUHTHENTICTED_PRINCIPAL = "unauthenticated-principal";

    static final String JMX_NAME = "jmx-name";

    static final String LOADER_REPOSITORY = "loader-repository";

    static final String MODULE_ORDER = "module-order";


    private final String version;

    private boolean jbossThreeDotTwo;

    private boolean jbossFour;

    private boolean jbossFourDotTwo;

    private final String securityDomain;

    private final String unauthenticatedPrincipal;

    private final String jmxName;

    private final String loaderRepository;

    private final String moduleOrder;


    public JbossConfiguration( String version, String securityDomain, String unauthenticatedPrincipal, String jmxName,
                               String loaderRepository, String moduleOrder )
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
                this.jbossThreeDotTwo = true;
            }
            else if ( version.equals( JbossConfiguration.VERSION_4 ) )
            {
                this.jbossFour = true;
            }
            else if ( version.equals( JbossConfiguration.VERSION_4_2 ) )
            {
                this.jbossFourDotTwo = true;
            }
            else
            {
                throw new EarPluginException(
                    "Invalid JBoss configuration, version[" + version + "] is not supported." );
            }
            this.securityDomain = securityDomain;
            this.unauthenticatedPrincipal = unauthenticatedPrincipal;
            this.jmxName = jmxName;
            this.loaderRepository = loaderRepository;
            this.moduleOrder = moduleOrder;
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
    public boolean isJbossThreeDotTwo()
    {
        return jbossThreeDotTwo;
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
     * Returns true if the targeted JBoss version is 4.2.
     *
     * @return if the targeted version is 4.2
     */
    public boolean isJbossFourDotTwo()
    {
        return jbossFourDotTwo;
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

    /**
     * The loader-repository specifies the name of the UnifiedLoaderRepository
     * MBean to use for the ear to provide ear level scoping of classes deployed
     * in the ear. It is a unique JMX ObjectName string.
     * <p/>
     * <P>Example:</P>
     * <loader-repository>jboss.test:loader=cts-cmp2v1-sar.ear</loader-repository>
     *
     * @return the object name of the ear mbean
     */
    public String getLoaderRepository()
    {
        return loaderRepository;
    }

    /**
     * The module-order specifies the order in which the modules specified
     * in the application.xml file gets loaded. Allowed values are:
     * <p/>
     * <module-order>strict</module-order>
     * The strict value indicates that the deployments of the modules will
     * be done in the order that would be specified in the application.xml
     * and jboss-app.xml file.
     * <p/>
     * <module-order>implicit</module-order>
     * The implicit value indicates the deployment would follow the order
     * which would be specified in the DeploymentSorter.
     * <p/>
     * Returns <tt>null</tt> if no module order is set.
     * <p/>
     * Only available as from JBoss 4.2.
     *
     * @return the module order
     */
    public String getModuleOrder()
    {
        return moduleOrder;
    }

}
