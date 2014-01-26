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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.scm.CommandParameter;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmBranch;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.svn.AbstractSvnScmProvider;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base class for the scm-publish mojos.
 */
public abstract class AbstractScmPublishMojo
    extends AbstractMojo
{
    /**
     * Location of the scm publication tree:
     * <code>scm:&lt;scm_provider&gt;&lt;delimiter&gt;&lt;provider_specific_part&gt;</code>.
     * Example: <<<scm:svn:https://svn.apache.org/repos/infra/websites/production/maven/content/plugins/maven-scm-publish-plugin-LATEST/>>>
     */
    @Parameter ( property = "scmpublish.pubScmUrl", defaultValue = "${project.distributionManagement.site.url}",
                 required = true )
    protected String pubScmUrl;

    /**
     * If the checkout directory exists and this flag is activated, the plugin will try an SCM-update instead
     * of delete then checkout.
     */
    @Parameter ( property = "scmpublish.tryUpdate", defaultValue = "false" )
    protected boolean tryUpdate;

   /**
     * Location where the scm check-out is done. By default, scm checkout is done in build (target) directory,
     * which is deleted on every <code>mvn clean</code>. To avoid this and get better performance, configure
     * this location outside build structure and set <code>tryUpdate</code> to <code>true</code>.
     * See <a href="http://maven.apache.org/plugins/maven-scm-publish-plugin/various-tips.html#Improving_SCM_Checkout_Performance">
     * Improving SCM Checkout Performance</a> for more information.
     */
    @Parameter ( property = "scmpublish.checkoutDirectory",
                 defaultValue = "${project.build.directory}/scmpublish-checkout" )
    protected File checkoutDirectory;

    /**
     * Display list of added, deleted, and changed files, but do not do any actual SCM operations.
     */
    @Parameter ( property = "scmpublish.dryRun" )
    private boolean dryRun;

    /**
     * Run add and delete commands, but leave the actually checkin for the user to run manually.
     */
    @Parameter ( property = "scmpublish.skipCheckin" )
    private boolean skipCheckin;

    /**
     * SCM log/checkin comment for this publication.
     */
    @Parameter ( property = "scmpublish.checkinComment", defaultValue = "Site checkin for project ${project.name}" )
    private String checkinComment;

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
     * Key is the provider type, eg. <code>cvs</code>.
     * Value is the provider implementation (the role-hint of the provider), eg. <code>cvs</code> or <code>cvs_native</code>.
     * @see ScmManager.setScmProviderImplementation
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
     * Use a local checkout instead of doing a checkout from the upstream repository. <b>WARNING</b>: This will only work
     * with distributed SCMs which support the file:// protocol
     * TODO: we should think about having the defaults for the various SCM providers provided via Modello!
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
     * Collections of paths not to delete when checking content to delete.
     * If your site has subdirectories published by an other mechanism/build
     */
    @Parameter
    protected String[] ignorePathsToDelete;

    /**
     * SCM branch to use. For github, you must configure with <code>gh-pages</code>.
     */
    @Parameter ( property = "scmpublish.scm.branch" )
    protected String scmBranch;

    /**
     * Configure svn automatic remote url creation.
     */
    @Parameter ( property = "scmpublish.automaticRemotePathCreation", defaultValue = "true" )
    protected boolean automaticRemotePathCreation;

    /**
     * Filename extensions of files which need new line normalization.
     */
    private final static String[] NORMALIZE_EXTENSIONS = { "html", "css", "js" };

    /**
     * Extra file extensions to normalize line ending (will be added to default
     * <code>html</code>,<code>css</code>,<code>js</code> list)
     */
    @Parameter
    protected String[] extraNormalizeExtensions;

    private Set<String> normalizeExtensions;

    protected ScmProvider scmProvider;

    protected ScmRepository scmRepository;

    protected void logInfo( String format, Object... params )
    {
        getLog().info( String.format( format, params ) );
    }

    protected void logWarn( String format, Object... params )
    {
        getLog().warn( String.format( format, params ) );
    }

    protected void logError( String format, Object... params )
    {
        getLog().error( String.format( format, params ) );
    }

    private File relativize( File base, File file )
    {
        return new File( base.toURI().relativize( file.toURI() ).getPath() );
    }

    protected boolean requireNormalizeNewlines( File f )
        throws IOException
    {
        if ( normalizeExtensions == null )
        {
            normalizeExtensions = new HashSet<String>( Arrays.asList( NORMALIZE_EXTENSIONS ) );
            if ( extraNormalizeExtensions != null )
            {
                normalizeExtensions.addAll( Arrays.asList( extraNormalizeExtensions ) );
            }
        }

        return FilenameUtils.isExtension( f.getName(), normalizeExtensions );
    }

    private ReleaseDescriptor setupScm()
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
                logInfo( "Changing the default '%s' provider implementation to '%s'.", providerEntry.getKey(),
                         providerEntry.getValue() );
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

        if ( scmProvider instanceof AbstractSvnScmProvider )
        {
            checkCreateRemoteSvnPath();
        }

        logInfo( "%s the pub tree from %s into %s", ( tryUpdate ? "Updating" : "Checking out" ), pubScmUrl, checkoutDirectory );

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
                logInfo( "TryUpdate is configured but no local copy currently available: forcing checkout." );
            }
            checkoutDirectory.mkdirs();
            forceCheckout = true;
        }

        try
        {
            ScmFileSet fileSet = new ScmFileSet( checkoutDirectory, includes, excludes );

            ScmResult scmResult;
            if ( tryUpdate && !forceCheckout )
            {
                scmResult = scmProvider.update( scmRepository, fileSet );
            }
            else if ( scmBranch == null )
            {
                scmResult = scmProvider.checkOut( scmRepository, fileSet );
            }
            else
            {
                ScmBranch scmBranch = new ScmBranch( this.scmBranch );
                scmResult = scmProvider.checkOut( scmRepository, fileSet, scmBranch );
            }

            checkScmResult( scmResult, "check out from SCM" );
        }
        catch ( ScmException e )
        {
            logError( e.getMessage() );

            throw new MojoExecutionException( "An error occurred during the checkout process: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            logError( e.getMessage() );

            throw new MojoExecutionException( "An error occurred during the checkout process: " + e.getMessage(), e );
        }
    }

    private void checkCreateRemoteSvnPath()
        throws MojoExecutionException
    {
        getLog().debug( "AbstractSvnScmProvider used, so we can check if remote url exists and eventually create it." );
        AbstractSvnScmProvider svnScmProvider = (AbstractSvnScmProvider) scmProvider;

        try
        {
            boolean remoteExists = svnScmProvider.remoteUrlExist( scmRepository.getProviderRepository(), null );

            if ( remoteExists )
            {
                return;
            }
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        String remoteUrl = ( (SvnScmProviderRepository) scmRepository.getProviderRepository() ).getUrl();

        if ( !automaticRemotePathCreation )
        {
            // olamy: return ?? that will fail during checkout IMHO :-)
            logWarn( "Remote svn url %s does not exist and automatic remote path creation disabled.",
                     remoteUrl );
            return;
        }

        logInfo( "Remote svn url %s does not exist: creating.", remoteUrl );

        File baseDir = null;
        try
        {

            // create a temporary directory for svnexec
            baseDir = File.createTempFile( "scm", "tmp" );
            baseDir.delete();
            baseDir.mkdirs();
            // to prevent fileSet cannot be empty
            ScmFileSet scmFileSet = new ScmFileSet( baseDir, new File( "" ) );

            CommandParameters commandParameters = new CommandParameters();
            commandParameters.setString( CommandParameter.SCM_MKDIR_CREATE_IN_LOCAL, Boolean.FALSE.toString() );
            commandParameters.setString( CommandParameter.MESSAGE, "Automatic svn path creation: " + remoteUrl );
            svnScmProvider.mkdir( scmRepository.getProviderRepository(), scmFileSet, commandParameters );

            // new remote url so force checkout!
            if ( checkoutDirectory.exists() )
            {
                FileUtils.deleteDirectory( checkoutDirectory );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            if ( baseDir != null )
            {
                try
                {
                    FileUtils.forceDeleteOnExit( baseDir );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( e.getMessage(), e );
                }
            }
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

    /**
     * Check-in content from scm checkout.
     *
     * @throws MojoExecutionException
     */
    protected void checkinFiles()
        throws MojoExecutionException
    {
        if ( skipCheckin )
        {
            return;
        }

        ScmFileSet updatedFileSet = new ScmFileSet( checkoutDirectory );
        try
        {
            long start = System.currentTimeMillis();

            CheckInScmResult checkinResult =
                checkScmResult( scmProvider.checkIn( scmRepository, updatedFileSet, new ScmBranch( scmBranch ),
                                                     checkinComment ), "check-in files to SCM" );

            logInfo( "Checked in %d file(s) to revision %s in %s", checkinResult.getCheckedInFiles().size(),
                     checkinResult.getScmRevision(),
                     DurationFormatUtils.formatPeriod( start, System.currentTimeMillis(), "H'h'm'm's's'" ) );
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "Failed to perform SCM checkin", e );
        }
    }

    protected void deleteFiles( Collection<File> deleted )
        throws MojoExecutionException
    {
        if ( skipDeletedFiles )
        {
            logInfo( "Deleting files is skipped." );
            return;
        }
        List<File> deletedList = new ArrayList<File>();
        for ( File f : deleted )
        {
            deletedList.add( relativize( checkoutDirectory, f ) );
        }
        ScmFileSet deletedFileSet = new ScmFileSet( checkoutDirectory, deletedList );
        try
        {
            getLog().debug( "Deleting files: " + deletedList );

            checkScmResult( scmProvider.remove( scmRepository, deletedFileSet, "Deleting obsolete site files." ),
                            "delete files from SCM" );
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "Failed to delete removed files to SCM", e );
        }
    }

    /**
     * Add files to scm.
     *
     * @param added files to be added
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    protected void addFiles( Collection<File> added )
        throws MojoFailureException, MojoExecutionException
    {
        List<File> addedList = new ArrayList<File>();
        Set<File> createdDirs = new HashSet<File>();
        Set<File> dirsToAdd = new TreeSet<File>();

        createdDirs.add( relativize( checkoutDirectory, checkoutDirectory ) );

        for ( File f : added )
        {
            for ( File dir = f.getParentFile(); !dir.equals( checkoutDirectory ); dir = dir.getParentFile() )
            {
                File relativized = relativize( checkoutDirectory, dir );
                //  we do the best we can with the directories
                if ( createdDirs.add( relativized ) )
                {
                    dirsToAdd.add( relativized );
                }
                else
                {
                    break;
                }
            }
            addedList.add( relativize( checkoutDirectory, f ) );
        }

        for ( File relativized : dirsToAdd )
        {
            try
            {
                ScmFileSet fileSet = new ScmFileSet( checkoutDirectory, relativized );
                getLog().debug( "scm add directory: " + relativized );
                AddScmResult addDirResult = scmProvider.add( scmRepository, fileSet, "Adding directory" );
                if ( !addDirResult.isSuccess() )
                {
                    getLog().debug( " Error adding directory " + relativized + ": " + addDirResult.getCommandOutput() );
                }
            }
            catch ( ScmException e )
            {
                //
            }
        }

        // remove directories already added !
        addedList.removeAll( dirsToAdd );

        ScmFileSet addedFileSet = new ScmFileSet( checkoutDirectory, addedList );
        getLog().debug( "scm add files: " + addedList );
        try
        {

                CommandParameters commandParameters = new CommandParameters();
                commandParameters.setString( CommandParameter.MESSAGE, "Adding new site files." );
                commandParameters.setString( CommandParameter.FORCE_ADD, Boolean.TRUE.toString() );
                checkScmResult( scmProvider.add( scmRepository, addedFileSet, commandParameters ),
                                "add new files to SCM" );

        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "Failed to add new files to SCM", e );
        }
    }

    private<T extends ScmResult> T checkScmResult( T result, String failure )
        throws MojoExecutionException
    {
        if ( !result.isSuccess() )
        {
            String msg = "Failed to " + failure + ": " + result.getProviderMessage() + " " + result.getCommandOutput();
            logError( msg );
            throw new MojoExecutionException( msg );
        }
        return result;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public abstract void scmPublishExecute()
        throws MojoExecutionException, MojoFailureException;

    public void setPubScmUrl( String pubScmUrl )
    {
        // Fix required for Windows, which fit other OS as well
        if ( pubScmUrl.startsWith( "scm:svn:" ) )
        {
            this.pubScmUrl = pubScmUrl.replaceFirst( "file:/[/]*", "file:///" );
        }
    }

}