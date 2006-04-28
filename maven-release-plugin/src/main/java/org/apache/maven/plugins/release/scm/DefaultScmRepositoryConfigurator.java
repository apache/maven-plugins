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
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

/**
 * Tool that gets a configured SCM repository from release configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DefaultScmRepositoryConfigurator
    extends AbstractLogEnabled
    implements ScmRepositoryConfigurator
{
    /**
     * The SCM manager.
     */
    private ScmManager scmManager;

    public ScmRepository getConfiguredRepository( ReleaseConfiguration releaseConfiguration )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        String username = releaseConfiguration.getUsername();
        String password = releaseConfiguration.getPassword();
        String url = releaseConfiguration.getUrl();
        String privateKey = releaseConfiguration.getPrivateKey();
        String passphrase = releaseConfiguration.getPassphrase();

        ScmRepository repository = scmManager.makeScmRepository( url );

        ScmProviderRepository scmRepo = repository.getProviderRepository();

        if ( repository.getProviderRepository() instanceof ScmProviderRepositoryWithHost )
        {
            ScmProviderRepositoryWithHost repositoryWithHost =
                (ScmProviderRepositoryWithHost) repository.getProviderRepository();
            String host = repositoryWithHost.getHost();

            int port = repositoryWithHost.getPort();

            if ( port > 0 )
            {
                host += ":" + port;
            }

            Settings settings = releaseConfiguration.getSettings();
            if ( settings != null )
            {
                // TODO: this is a bit dodgy - id is not host, but since we don't have a <host> field we make an assumption
                Server server = settings.getServer( host );

                if ( server != null )
                {
                    if ( username == null )
                    {
                        username = server.getUsername();
                    }

                    if ( password == null )
                    {
                        password = server.getPassword();
                    }

                    if ( privateKey == null )
                    {
                        privateKey = server.getPrivateKey();
                    }

                    if ( passphrase == null )
                    {
                        passphrase = server.getPassphrase();
                    }
                }
            }
        }

        if ( !StringUtils.isEmpty( username ) )
        {
            scmRepo.setUser( username );
        }
        if ( !StringUtils.isEmpty( password ) )
        {
            scmRepo.setPassword( password );
        }

        if ( scmRepo instanceof ScmProviderRepositoryWithHost )
        {
            ScmProviderRepositoryWithHost repositoryWithHost = (ScmProviderRepositoryWithHost) scmRepo;
            if ( !StringUtils.isEmpty( privateKey ) )
            {
                repositoryWithHost.setPrivateKey( privateKey );
            }

            if ( !StringUtils.isEmpty( passphrase ) )
            {
                repositoryWithHost.setPassphrase( passphrase );
            }
        }

        if ( "svn".equals( repository.getProvider() ) )
        {
            SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) repository.getProviderRepository();

            String tagBase = releaseConfiguration.getTagBase();
            if ( !StringUtils.isEmpty( tagBase ) )
            {
                svnRepo.setTagBase( tagBase );
            }
        }

        return repository;
    }

    public ScmProvider getRepositoryProvider( ScmRepository repository )
        throws NoSuchScmProviderException
    {
        return scmManager.getProviderByRepository( repository );
    }

    public void setScmManager( ScmManager scmManager )
    {
        this.scmManager = scmManager;
    }
}
