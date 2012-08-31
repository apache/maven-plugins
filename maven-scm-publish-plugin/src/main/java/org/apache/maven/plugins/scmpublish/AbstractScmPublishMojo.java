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

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.scm.ScmBranch;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Base class for the scm-publish mojos.
 */
public abstract class AbstractScmPublishMojo
    extends AbstractMojo
{

    /**
     * Location of the inventory file.
     */
    @Parameter ( property = "scmpublish.inventoryFile",
                 defaultValue = "${project.build.directory}/scmpublish-inventory.js" )
    protected File inventoryFile;

    /**
     * Location of the scm publication tree.
     */
    @Parameter ( property = "scmpublish.pubScmUrl", defaultValue = "${project.distributionManagement.site.url}",
                 required = true )
    protected String pubScmUrl;

    /**
     * Location where the scm check-out is done.
     */
    @Parameter ( property = "scmpublish.checkoutDirectory",
                 defaultValue = "${project.build.directory}/scmpublish-checkout" )
    protected File checkoutDirectory;

    /**
     * Patterns to exclude from the scm tree.
     */
    @Parameter
    protected String excludes;

    /**
     * Patterns to include in the scm tree.
     */
    @Parameter
    protected String includes;

    /**
     * List of SCM provider implementations.
     */
    @Parameter
    private Map<String, String> providerImplementations;

    /**
     * The SCM manager.
     */
    @Component
    private ScmManager scmManager;

    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    @Component
    protected ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * The SCM username to use.
     */
    @Parameter ( property = "username" )
    protected String username;

    /**
     * The SCM password to use.
     */
    @Parameter ( property = "password" )
    protected String password;

    /**
     * Use a local checkout instead of doing a checkout from the upstream repository. ATTENTION: This will only work
     * with distributed SCMs which support the file:// protocol TODO: we should think about having the defaults for the
     * various SCM providers provided via modello!
     */
    @Parameter ( property = "localCheckout", defaultValue = "false" )
    protected boolean localCheckout;

    /**
     * The outputEncoding parameter of the site plugin. This plugin will corrupt your site
     * if this does not match the value used by the site plugin.
     */
    @Parameter ( property = "outputEncoding", defaultValue = "${project.reporting.outputEncoding}" )
    protected String siteOutputEncoding;

    /**
     * if the checkout directory exists and this flag is activated the plugin will try an update rather
     * than delete then checkout
     */
    @Parameter ( property = "scmpublish.tryUpdate", defaultValue = "false" )
    protected boolean tryUpdate;

    /**
     * Do not delete files to the scm
     */
    @Parameter ( property = "scmpublish.skipDeletedFiles", defaultValue = "false" )
    protected boolean skipDeletedFiles;

    /**
     */
    @Parameter ( defaultValue = "${basedir}", readonly = true )
    protected File basedir;

    /**
     */
    @Component
    protected Settings settings;

    /**
     */
    @Component
    protected MavenSession session;

    /**
     * Collections of paths to not delete when checking content to delete.
     * If your site has subdirectories published by an other mechanism/build
     */
    @Parameter
    protected String[] ignorePathsToDelete;

    /**
     * for github you must configure with gh-pages
     */
    @Parameter ( property = "scmpublish.scm.branch" )
    protected String scmBranch;

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

        //TODO use from settings with decrypt stuff

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
        logInfo( "%s the pub tree from  %s ...", ( tryUpdate ? "Updating" : "Checking out" ), pubScmUrl );

        if ( checkoutDirectory.exists() && !tryUpdate )
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

        boolean forceCheckout = false;

        if ( !checkoutDirectory.exists() )
        {
            if ( tryUpdate )
            {
                logInfo( "tryUpdate is configured but no local copy currently available so forcing checkout" );
            }
            checkoutDirectory.mkdirs();
            forceCheckout = true;
        }

        ScmResult scmResult;

        try
        {
            ScmFileSet fileSet = new ScmFileSet( checkoutDirectory, includes, excludes );
            if ( tryUpdate && !forceCheckout )
            {
                scmResult = scmProvider.update( scmRepository, fileSet );
            }
            else
            {
                if ( scmBranch == null )
                {
                    scmResult = scmProvider.checkOut( scmRepository, fileSet );
                }
                else
                {

                    ScmBranch scmBranch = new ScmBranch( this.scmBranch );
                    scmResult = scmProvider.checkOut( scmRepository, fileSet, scmBranch );
                }
            }
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

            throw new MojoExecutionException(
                "Unable to checkout from SCM" + "\nProvider message:\n" + scmResult.getProviderMessage()
                    + "\nCommand output:\n" + scmResult.getCommandOutput() );
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

        boolean tmpCheckout = false;

        if ( checkoutDirectory.getPath().contains( "${project." ) )
        {
            try
            {
                tmpCheckout = true;
                checkoutDirectory = File.createTempFile( "maven-scm-publish", ".checkout" );
                checkoutDirectory.delete();
                checkoutDirectory.mkdir();
            }
            catch ( IOException ioe )
            {
                throw new MojoExecutionException( ioe.getMessage(), ioe );
            }
        }

        try
        {
            scmPublishExecute();
        }
        finally
        {
            if ( tmpCheckout )
            {
                FileUtils.deleteQuietly( checkoutDirectory );
            }
        }
    }


    public abstract void scmPublishExecute()
        throws MojoExecutionException, MojoFailureException;
}