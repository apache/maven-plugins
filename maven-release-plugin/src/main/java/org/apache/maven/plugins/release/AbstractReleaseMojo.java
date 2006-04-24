package org.apache.maven.plugins.release;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.release.helpers.ReleaseProgressTracker;
import org.apache.maven.plugins.release.helpers.ScmHelper;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.InputHandler;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public abstract class AbstractReleaseMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    protected File basedir;

    /**
     * @component
     */
    private ScmManager scmManager;

    /**
     * @component
     */
    private InputHandler inputHandler;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;


    private ScmHelper scmHelper;

    protected abstract ReleaseProgressTracker getReleaseProgress()
        throws MojoExecutionException;

    protected InputHandler getInputHandler()
    {
        return inputHandler;
    }

    protected Settings getSettings()
    {
        return settings;
    }

    protected ScmHelper getScm( String directory )
        throws MojoExecutionException
    {
        if ( scmHelper == null )
        {
            scmHelper = new ScmHelper();

            scmHelper.setScmManager( scmManager );

            ReleaseProgressTracker releaseProgress = getReleaseProgress();

            scmHelper.setUrl( releaseProgress.getScmUrl() );

            scmHelper.setTag( releaseProgress.getScmTag() );

            scmHelper.setTagBase( releaseProgress.getScmTagBase() );

            scmHelper.setUsername( releaseProgress.getUsername() );

            scmHelper.setPassword( releaseProgress.getPassword() );
        }

        scmHelper.setWorkingDirectory( directory );

        loadUserInfos( scmHelper );

        return scmHelper;
    }

    /**
     * Set the SCM Helper
     *
     * @param scmHelper
     */
    protected void setScmHelper( ScmHelper scmHelper )
    {
        this.scmHelper = scmHelper;
    }

    /**
     * Get the SCM Manager
     *
     * @return
     */
    private ScmManager getScmManager()
    {
        return this.scmManager;
    }

    /**
     * Set the SCM Manager
     *
     * @param scmManager
     */
    public void setScmManager( ScmManager scmManager )
    {
        this.scmManager = scmManager;
    }

    /**
     * Load starteam username/password from settings if needed
     *
     * @param scmHelper
     * @throws MojoExecutionException
     */
    private void loadUserInfos( ScmHelper scmHelper )
        throws MojoExecutionException
    {
        if ( scmHelper.getUsername() == null || scmHelper.getPassword() == null )
        {
            ScmRepository repository = null;

            try
            {
                repository = scmManager.makeScmRepository( scmHelper.getUrl() );
            }
            catch ( ScmRepositoryException e )
            {
                List messages = e.getValidationMessages();

                Iterator iter = messages.iterator();

                while ( iter.hasNext() )
                {
                    getLog().error( iter.next().toString() );
                }

                getLog().error( "The invalid scm url connection: '" + scmHelper.getUrl() + "'." );

                throw new MojoExecutionException( "Command failed. Bad Scm URL." );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Can't load the scm provider.", e );
            }

            if ( repository.getProviderRepository() instanceof ScmProviderRepositoryWithHost )
            {
                loadInfosFromSettings( (ScmProviderRepositoryWithHost) repository.getProviderRepository(), scmHelper );
            }
        }
    }

    /**
     * Load username password from settings if user has not set them in JVM properties
     *
     * @param repo
     */
    private void loadInfosFromSettings( ScmProviderRepositoryWithHost repo, ScmHelper scmHelper )
    {
        if ( scmHelper.getUsername() == null || scmHelper.getPassword() == null )
        {
            String host = repo.getHost();

            int port = repo.getPort();

            if ( port > 0 )
            {
                host += ":" + port;
            }

            Server server = this.settings.getServer( host );

            if ( server != null )
            {
                if ( scmHelper.getUsername() == null )
                {
                    scmHelper.setUsername( this.settings.getServer( host ).getUsername() );
                }

                if ( scmHelper.getPassword() == null )
                {
                    scmHelper.setPassword( this.settings.getServer( host ).getPassword() );
                }

                if ( scmHelper.getPrivateKey() == null )
                {
                    scmHelper.setPrivateKey( this.settings.getServer( host ).getPrivateKey() );
                }

                if ( scmHelper.getPassphrase() == null )
                {
                    scmHelper.setPassphrase( this.settings.getServer( host ).getPassphrase() );
                }
            }
        }
    }
    // ----------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------

    protected void removeReleaseProperties()
    {
        File releaseProperties = new File( basedir, ReleaseProgressTracker.RELEASE_PROPERTIES );

        if ( releaseProperties.exists() )
        {
            releaseProperties.delete();
        }
    }

}
