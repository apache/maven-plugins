package org.apache.maven.plugins.scmpublish;

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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;

/**
 * Base class for the site-scm-publish mojos.
 */
public abstract class AbstractScmPublishMojo
    extends AbstractMojo
{

    /**
     * Location of the inventory file.
     * 
     * @parameter expression="${scmpublish.inventoryFile}"
     *            default-value="${project.build.directory}/scmpublish-inventory.js"
     */
    protected File inventoryFile;

    /**
     * Location of the svn publication tree.
     * 
     * @parameter expression="${scmpublish.pubScmUrl}"
     * @required
     */
    protected String pubScmUrl;

    /**
     * Location of the svn publication tree.
     * 
     * @parameter expression="${scmpublish.checkoutDirectory}"
     *            default-value="${project.build.directory}/scmpublish-checkout"
     */
    protected File checkoutDirectory;

    /**
     * Patterns to exclude from the scm tree.
     * 
     * @parameter
     */
    protected String excludes;

    /**
     * Patterns to include in the scm tree.
     * 
     * @parameter
     */
    protected String includes;
    
    /**
     * List of provider implementations.
     *
     * @parameter
     */
    private Map<String, String> providerImplementations;
    
    /**
     * The SCM manager.
     *
     * @component
     */
    private ScmManager scmManager;

    /**
     * Tool that gets a configured SCM repository from release configuration.
     * 
     * @component
     */
    protected ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * The SCM username to use.
     * 
     * @parameter expression="${username}"
     */
    protected String username;

    /**
     * The SCM password to use.
     * 
     * @parameter expression="${password}"
     */
    protected String password;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    protected Settings settings;

    /**
     * Use a local checkout instead of doing a checkout from the upstream repository. ATTENTION: This will only work
     * with distributed SCMs which support the file:// protocol TODO: we should think about having the defaults for the
     * various SCM providers provided via modello!
     * 
     * @parameter expression="${localCheckout}" default-value="false"
     * @since 2.0
     */
    protected boolean localCheckout;

    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    protected File basedir;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    protected MavenSession session;

    /**
     * The outputEncoding parameter of the site plugin. This plugin will corrupt your site
     * if this does not match the value used by the site plugin.
     * 
     * @parameter expression="${outputEncoding}" default-value="${project.reporting.outputEncoding}"
     */
    protected String siteOutputEncoding;

    protected ScmProvider scmProvider;
    protected ScmRepository scmRepository;

    protected AbstractScmPublishMojo()
    {
        super();
    }

    protected void logInfo( String format, Object... params )
    {
        getLog().info( String.format( format, params ) );
    }

    protected void logError( String format, Object... params )
    {
        getLog().error( String.format( format, params ) );
    }

    protected ReleaseDescriptor setupScm()
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        String scmUrl;
        if ( localCheckout )
        {
            // in the release phase we have to change the checkout URL
            // to do a local checkout instead of going over the network.

            // the first step is a bit tricky, we need to know which provider! like e.g. "scm:jgit:http://"
            // the offset of 4 is because 'scm:' has 4 characters...
            String providerPart = pubScmUrl.substring( 0, pubScmUrl.indexOf( ':', 4 ) );

            // X TODO: also check the information from releaseDescriptor.getScmRelativePathProjectDirectory()
            // X TODO: in case our toplevel git directory has no pom.
            // X TODO: fix pathname once I understand this.
            scmUrl = providerPart + ":file://" + "target/localCheckout";
            logInfo( "Performing a LOCAL checkout from " + scmUrl );
        }

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( settings.isInteractiveMode() );

        releaseDescriptor.setScmPassword( password );
        releaseDescriptor.setScmUsername( username );

        releaseDescriptor.setWorkingDirectory( basedir.getAbsolutePath() );
        releaseDescriptor.setLocalCheckout( localCheckout );
        releaseDescriptor.setScmSourceUrl( pubScmUrl );

        if ( providerImplementations != null )
        {
            for ( Map.Entry<String, String> providerEntry : providerImplementations.entrySet() )
            {
                getLog().info( "Change the default '" + providerEntry.getKey() + "' provider implementation to '"
                    + providerEntry.getValue() + "'." );
                scmManager.setScmProviderImplementation( providerEntry.getKey(), providerEntry.getValue() );
            }
        }

        scmRepository = scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor, settings );

        scmProvider = scmRepositoryConfigurator.getRepositoryProvider( scmRepository );

        return releaseDescriptor;
    }

    protected void checkoutExisting()
        throws MojoExecutionException
    {
        logInfo( "Checking out the pub tree ..." );

        if ( checkoutDirectory.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( checkoutDirectory );
            }
            catch ( IOException e )
            {
                logError( e.getMessage() );

                throw new MojoExecutionException( "Unable to remove old checkout directory: " + e.getMessage(), e );
            }
        }

        checkoutDirectory.mkdirs();

        CheckOutScmResult scmResult;

        try
        {
            ScmFileSet fileSet = new ScmFileSet( checkoutDirectory, includes, excludes );
            scmResult = scmProvider.checkOut( scmRepository, fileSet );
        }
        catch ( ScmException e )
        {
            logError( e.getMessage() );

            throw new MojoExecutionException( "An error is occurred in the checkout process: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            logError( e.getMessage() );

            throw new MojoExecutionException( "An error is occurred in the checkout process: " + e.getMessage(), e );
        }

        if ( !scmResult.isSuccess() )
        {
            logError( scmResult.getProviderMessage() );

            throw new MojoExecutionException( "Unable to checkout from SCM" + "\nProvider message:\n"
                + scmResult.getProviderMessage() + "\nCommand output:\n" + scmResult.getCommandOutput() );
        }
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // setup the scm plugin with help from release plugin utilities
        try
        {
            setupScm();
        }
        catch ( ScmRepositoryException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        scmPublishExecute();
    }

    public abstract void scmPublishExecute()
        throws MojoExecutionException, MojoFailureException;
}