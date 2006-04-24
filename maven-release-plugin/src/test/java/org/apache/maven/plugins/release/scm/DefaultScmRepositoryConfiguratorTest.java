package org.apache.maven.plugins.release.scm;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the default SCM repository configurator.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DefaultScmRepositoryConfiguratorTest
    extends PlexusTestCase
{
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    private static final int CVS_PORT = 2401;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        scmRepositoryConfigurator = (ScmRepositoryConfigurator) lookup( ScmRepositoryConfigurator.ROLE );
    }

    public void testGetConfiguredRepository()
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        ScmRepository repository = scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

        assertEquals( "check provider", "cvs", repository.getProvider() );
        assertEquals( "check username", "anoncvs", repository.getProviderRepository().getUser() );
        assertNull( "check password", repository.getProviderRepository().getPassword() );
    }

    public void testGetConfiguredRepositoryWithUsernameAndPassword()
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration( "username", "password" );

        ScmRepository repository = scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

        assertEquals( "check username", "username", repository.getProviderRepository().getUser() );
        assertEquals( "check password", "password", repository.getProviderRepository().getPassword() );
    }

    public void testGetConfiguredRepositoryWithTagBase()
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm:svn:http://localhost/home/svn/module/trunk" );
        releaseConfiguration.setTagBase( "http://localhost/home/svn/module/tags" );

        ScmRepository repository = scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

        SvnScmProviderRepository providerRepository = (SvnScmProviderRepository) repository.getProviderRepository();
        assertEquals( "check tag base", "http://localhost/home/svn/module/tags", providerRepository.getTagBase() );
    }

    public void testGetConfiguredRepositoryWithHost()
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        Settings settings = new Settings();
        Server server = new Server();
        server.setId( "localhost:" + CVS_PORT );
        server.setUsername( "settings-username" );
        server.setPassword( "settings-password" );
        server.setPrivateKey( "settings-private-key" );
        server.setPassphrase( "settings-passphrase" );
        settings.addServer( server );

        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm:cvs:pserver:anoncvs@localhost:/home/cvs:module" );
        releaseConfiguration.setSettings( settings );

        ScmRepository repository = scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

        ScmProviderRepositoryWithHost providerRepository =
            (ScmProviderRepositoryWithHost) repository.getProviderRepository();
        assertEquals( "check host", "localhost", providerRepository.getHost() );
        assertEquals( "check port", CVS_PORT, providerRepository.getPort() );
        assertEquals( "check username", "settings-username", providerRepository.getUser() );
        assertEquals( "check password", "settings-password", providerRepository.getPassword() );
        assertEquals( "check private key", "settings-private-key", providerRepository.getPrivateKey() );
        assertEquals( "check passphrase", "settings-passphrase", providerRepository.getPassphrase() );
    }

    public void testGetConfiguredRepositoryInvalidScmUrl()
        throws NoSuchScmProviderException
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm-url" );

        try
        {
            scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

            fail( "Expected failure to get a repository with an invalid SCM URL" );
        }
        catch ( ScmRepositoryException e )
        {
            // expected
        }
    }

    public void testGetConfiguredRepositoryInvalidScmProvider()
        throws ScmRepositoryException
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm:url:" );

        try
        {
            scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

            fail( "Expected failure to get a repository with an invalid SCM URL" );
        }
        catch ( NoSuchScmProviderException e )
        {
            // expected
        }
    }

    public void testGetConfiguredRepositoryInvalidScmUrlParameters()
        throws NoSuchScmProviderException
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm:cvs:" );

        try
        {
            scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

            fail( "Expected failure to get a repository with an invalid SCM URL" );
        }
        catch ( ScmRepositoryException e )
        {
            // expected
        }
    }

    public void testGetRepositoryProvider()
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        ScmRepository repository = scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

        ScmProvider provider = scmRepositoryConfigurator.getRepositoryProvider( repository );
        assertEquals( "Check SCM provider", "cvs", provider.getScmType() );
    }

    private static ReleaseConfiguration createReleaseConfiguration()
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm:cvs:pserver:anoncvs@localhost:/home/cvs:module" );
        return releaseConfiguration;
    }

    private static ReleaseConfiguration createReleaseConfiguration( String username, String password )
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();
        releaseConfiguration.setUsername( username );
        releaseConfiguration.setPassword( password );
        return releaseConfiguration;
    }
}
