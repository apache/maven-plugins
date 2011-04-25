package org.apache.maven.plugin.ear;

import java.util.List;

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

    static final String VERSION_5 = "5";

    static final String VERSION = "version";

    static final String SECURITY_DOMAIN = "security-domain";

    static final String UNAUHTHENTICTED_PRINCIPAL = "unauthenticated-principal";

    static final String JMX_NAME = "jmx-name";

    static final String LOADER_REPOSITORY = "loader-repository";

    static final String LOADER_REPOSITORY_CLASS_ATTRIBUTE = "loaderRepositoryClass";

    static final String LOADER_REPOSITORY_CONFIG = "loader-repository-config";

    static final String CONFIG_PARSER_CLASS_ATTRIBUTE = "configParserClass";

    static final String MODULE_ORDER = "module-order";

    static final String DATASOURCES = "data-sources";

    static final String DATASOURCE = "data-source";

    static final String LIBRARY_DIRECTORY = "library-directory";

    private final String version;

    private boolean jbossThreeDotTwo;

    private boolean jbossFour;

    private boolean jbossFourDotTwo;

    private boolean jbossFive;

    private final String securityDomain;

    private final String unauthenticatedPrincipal;

    private final String jmxName;

    private final String loaderRepository;

    private final String loaderRepositoryConfig;

    private final String loaderRepositoryClass;

    private final String configParserClass;

    private final String moduleOrder;

    private final List dataSources;

    private final String libraryDirectory;

    public JbossConfiguration( String version, String securityDomain, String unauthenticatedPrincipal, String jmxName,
                               String loaderRepository, String moduleOrder, List dataSources, String libraryDirectory,
                               String loaderRepositoryConfig, String loaderRepositoryClass, String configParserClass )
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
            else if ( version.equals( JbossConfiguration.VERSION_5 ) )
            {
                this.jbossFive = true;
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
            this.dataSources = dataSources;
            this.libraryDirectory = libraryDirectory;
            this.loaderRepositoryConfig = loaderRepositoryConfig;
            this.loaderRepositoryClass = loaderRepositoryClass;
            this.configParserClass = configParserClass;
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
     * Returns true if the targeted JBoss version if 4 or higher (that is
     * 4, 4.2 or 5).
     *
     * @return true if the targeted version is 4+
     */
    public boolean isJbossFourOrHigher()
    {
        return jbossFour || jbossFourDotTwo || jbossFive;
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
     * Returns true if the targeted JBoss version if 4.2 or higher (that is
     * 4.2 or 5).
     *
     * @return true if the targeted version is 4.2+
     */
    public boolean isJbossFourDotTwoOrHigher()
    {
        return jbossFourDotTwo || jbossFive;
    }


    /**
     * Returns true if the targeted JBoss version is 5.
     *
     * @return if the targeted version is 5
     */
    public boolean isJbossFive()
    {
        return jbossFive;
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
     * is no authenticated user. This Principal has no roles or privileges to call
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
     * &lt;loader-repository>jboss.test:loader=cts-cmp2v1-sar.ear&lt;/loader-repository>
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
     * Only available in JBoss 4.2 and 4.3. Has no effect in JBoss 5 and is
     * not added when mentioned version is used.
     *
     * @return the module order
     */
    public String getModuleOrder()
    {
        return moduleOrder;
    }

    /**
     * Returns the list of datasources to include in the <tt>jboss-app.xml</tt>
     * file as services. Each element of the list is the relative path to the
     * datasource file contained in the EAR archive.
     *
     * @return the list of datasources paths
     */
    public List getDataSources()
    {
        return dataSources;
    }

    /**
     * Returns the library directory to include in the <tt>jboss-app.xml</tt> file.
     * It tells JBoss where to find non-Java EE libraries included in the EAR.
     *
     * @return the library directory
     */
    public String getLibraryDirectory()
    {
        return libraryDirectory;
    }

    /**
     * Returns the class loader repository configuration to include in the <tt>jboss-app.xml</tt> file.
     * The content of this element is handed to the class loader, thereby altering it's default behaviour.
     * <p/>
     * This element is added as a child to the <tt>loader-repository</tt> element. If the element is not
     * present in the configuration, it will be added.
     * <p/>
     * Example: &lt;loader-repository-config>java2ParentDelegaton=true&lt;/loader-repository-config>
     *
     * @return the class loader repository configuration
     */
    public String getLoaderRepositoryConfig()
    {
        return loaderRepositoryConfig;
    }

    /**
     * Returns the class loader repository class to include in the <tt>jboss-app.xml</tt> file.
     * It tells JBoss which loader repository implementation to use.
     * <p/>
     * This element is added as an attribute to the <tt>loader-repository</tt> element, therefore it is
     * not added if no such element configuration is present.
     * <p/>
     * Example: &lt;loader-repository-class>org.mindbug.jboss.AlternateLoaderRepository&lt;/loader-repository-class>
     *
     * @return the class loader repository class
     */
    public String getLoaderRepositoryClass()
    {
        return loaderRepositoryClass;
    }

    /**
     * Returns the class loader's configuration parser class to include in the <tt>jboss-app.xml</tt> file.
     * It tells JBoss how to parse the configuration given in the <tt>loader-repository-config</tt> element.
     * <p/>
     * This element is added as an attribute to the <tt>loader-repository-config</tt> element, therefore it is
     * not added if no such element configuration is present.
     * <p/>
     * Example: &lt;config-parser-class>org.mindbug.jboss.AlternateLoaderRepositoryConfigParser&lt;/config-parser-class>
     *
     * @return the class loader's configuration parser class
     */
    public String getConfigParserClass()
    {
        return configParserClass;
    }
}
