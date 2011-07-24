package org.apache.maven.plugins.site;

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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
@RunWith(JUnit4.class)
public class SiteDeployMojoTest
    extends PlexusTestCase
{
    private WagonManager wagonManager;

    private Repository repository;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        wagonManager = getContainer().lookup( WagonManager.class );
        repository = new Repository( "my-repository", "scp://repository-host/var/maven2" );
    }

    @Test
    public void testFoo()
    {
        // should not fail ;-)
        assertTrue( true );
    }

    /**
    FIXME find a way to restore those tests as wagonManager.addProxy is not anymore available in maven 3.
    public void testGetProxyInfoNoProxyForRepositoryProtocol()
    {
        wagonManager.addProxy( "http", "proxy-host", 8080, "my-user", "my-password", null );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNull( "ProxyInfo must be null because http != scp", proxyInfo );
    }

    public void testGetProxyInfoForRepositoryHostExactlyMatchesNonProxyHosts()
    {
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password",
                               "a-host,repository-host;another-host" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNull( "ProxyInfo must be null because 'repository-host' in nonProxyHosts list", proxyInfo );
    }

    public void testGetProxyInfoForRepositoryHostWildcardMatchNonProxyHosts1()
    {
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password", "a-host|repository*|another-host" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNull( "ProxyInfo must be null because 'repository-host' in nonProxyHosts list", proxyInfo );
    }

    public void testGetProxyInfoForRepositoryHostWildcardMatchNonProxyHosts2()
    {
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password", "*host" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNull( "ProxyInfo must be null because 'repository-host' in nonProxyHosts list", proxyInfo );
    }

    public void testGetProxyInfoForRepositoryHostWildcardMatchNonProxyHosts3()
    {
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password", "repository*host" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNull( "ProxyInfo must be null because 'repository-host' in nonProxyHosts list", proxyInfo );
    }

    public void testGetProxyInfoForRepositoryHostWildcardNoMatchNonProxyHosts1()
    {
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password", "mycompany*" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNotNull( "ProxyInfo must be found because 'repository-host' not in nonProxyHosts list", proxyInfo );
    }

    public void testGetProxyInfoForRepositoryHostWildcardNoMatchNonProxyHosts2()
    {
        wagonManager.
        
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password", "*mycompany" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNotNull( "ProxyInfo must be found because 'repository-host' not in nonProxyHosts list", proxyInfo );
    }

    public void testGetProxyInfoForRepositoryHostWildcardNoMatchNonProxyHosts3()
    {
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password", "repository*mycompany" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNotNull( "ProxyInfo must be found because 'repository-host' not in nonProxyHosts list", proxyInfo );
    }

    public void testGetProxyInfoForRepositoryHostWildcardNoMatchNonProxyHosts4()
    {
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password", "mycompany*host" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNotNull( "ProxyInfo must be found because 'repository-host' not in nonProxyHosts list", proxyInfo );
    }

    public void testGetProxyInfoForRepositoryHostWildcardNoMatchNonProxyHosts5()
    {
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password", "mycompany*mycompany" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNotNull( "ProxyInfo must be found because 'repository-host' not in nonProxyHosts list", proxyInfo );
    }

    public void testGetProxyInfoFound()
    {
        wagonManager.addProxy( "scp", "localhost", 8080, "my-user", "my-password", "an-host|another-host" );
        ProxyInfo proxyInfo = SiteDeployMojo.getProxyInfo( repository, wagonManager );
        assertNotNull( "ProxyInfo must be found because 'repository-host' not in nonProxyHosts list", proxyInfo );
    }
    **/
}
