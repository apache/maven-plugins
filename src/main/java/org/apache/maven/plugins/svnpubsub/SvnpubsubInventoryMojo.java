package org.apache.maven.plugins.svnpubsub;

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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.plexus.util.StringUtils;

/**
 * Prepare a directory for version-managed site generation. This checks out the specified directory from the SCM and
 * then takes inventory of all the resulting files. This inventory then allows the 'checkin' target to tee up deletions
 * as well as modifications and additions. There's an assumption here that an entire directory in svn is dedicated to
 * the publication process for this project. In the aggregate case, this is going to take some doing. TODO: we want
 * multiple includes/excludes, but the scm API doesn't go there.
 * 
 * @goal prepare
 * @phase pre-site
 */
public class SvnpubsubInventoryMojo
    extends AbstractMojo
{
    /**
     * Location of the inventory file.
     * 
     * @parameter expression="${svnpubsub.inventoryFile}"
     *            default-value="${project.build.directory}/svnpubsub-inventory.js"
     */
    private File inventoryFile;

    /**
     * Location of the svn publication tree.
     * 
     * @parameter expression="${svnpubsub.pubScmUrl}"
     * @required
     */
    private String pubScmUrl;

    /**
     * Location of the svn publication tree.
     * 
     * @parameter expression="${svnpubsub.checkoutDirectory}"
     *            default-value="${project.build.directory}/svnpubsub-checkout"
     */
    private File checkoutDirectory;

    /**
     * Patterns to exclude from the scm tree.
     * 
     * @parameter
     */
    private String excludes;

    /**
     * Patterns to include in the scm tree.
     * 
     * @parameter
     */
    private String includes;

    /**
     * Tool that gets a configured SCM repository from release configuration.
     * 
     * @plexus.requirement
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * The SCM username to use.
     * 
     * @parameter expression="${username}"
     */
    private String username;

    /**
     * The SCM password to use.
     * 
     * @parameter expression="${password}"
     */
    private String password;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    protected Settings settings;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List<MavenProject> reactorProjects;

    /**
     * Use a local checkout instead of doing a checkout from the upstream repository. ATTENTION: This will only work
     * with distributed SCMs which support the file:// protocol TODO: we should think about having the defaults for the
     * various SCM providers provided via modello!
     * 
     * @parameter expression="${localCheckout}" default-value="false"
     * @since 2.0
     */
    private boolean localCheckout;

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

    private ScmProvider scmProvider;

    private ScmRepository scmRepository;

    private Collection<File> inventory;

    private void logInfo( String format, Object... params )
    {
        getLog().info( String.format( format, params ) );
    }

    private void logError( String format, Object... params )
    {
        getLog().error( String.format( format, params ) );
    }

    private void checkoutExisting()
        throws ReleaseExecutionException, ReleaseFailureException
    {

        logInfo( "Checking out the pub tree ..." );

        String scmUrl = pubScmUrl;

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

        try
        {
            scmRepository = scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor, settings );

            scmProvider = scmRepositoryConfigurator.getRepositoryProvider( scmRepository );
        }
        catch ( ScmRepositoryException e )
        {
            // TODO: rethink this error pattern.
            logError( e.getMessage() );

            throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException e )
        {
            logError( e.getMessage() );

            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
        }

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        if ( checkoutDirectory.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( checkoutDirectory );
            }
            catch ( IOException e )
            {
                logError( e.getMessage() );

                throw new ReleaseExecutionException( "Unable to remove old checkout directory: " + e.getMessage(), e );
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

            throw new ReleaseExecutionException( "An error is occurred in the checkout process: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            logError( e.getMessage() );

            throw new ReleaseExecutionException( "An error is occurred in the checkout process: " + e.getMessage(), e );
        }

        String scmRelativePathProjectDirectory = scmResult.getRelativePathProjectDirectory();
        if ( StringUtils.isEmpty( scmRelativePathProjectDirectory ) )
        {
            String basedir;
            try
            {
                basedir = ReleaseUtil.getCommonBasedir( reactorProjects );
            }
            catch ( IOException e )
            {
                throw new ReleaseExecutionException( "Exception occurred while calculating common basedir: "
                    + e.getMessage(), e );
            }

            String rootProjectBasedir = rootProject.getBasedir().getAbsolutePath();
            try
            {
                if ( ReleaseUtil.isSymlink( rootProject.getBasedir() ) )
                {
                    rootProjectBasedir = rootProject.getBasedir().getCanonicalPath();
                }
            }
            catch ( IOException e )
            {
                throw new ReleaseExecutionException( e.getMessage(), e );
            }
            if ( rootProjectBasedir.length() > basedir.length() )
            {
                scmRelativePathProjectDirectory = rootProjectBasedir.substring( basedir.length() + 1 );
            }
        }
        releaseDescriptor.setScmRelativePathProjectDirectory( scmRelativePathProjectDirectory );

        if ( !scmResult.isSuccess() )
        {
            logError( scmResult.getProviderMessage() );

            throw new ReleaseScmCommandException( "Unable to checkout from SCM", scmResult );
        }
    }
    
    private static class DotFilter implements IOFileFilter {

        public boolean accept( File file )
        {
            return !file.getName().startsWith( "." );        
            }

        public boolean accept( File dir, String name )
        {                
            return !name.startsWith( "." );
        }
        
    }

    /**
     * Create a list of all the files in the checkout (which we will presently remove). For now, duck anything that
     * starts with a ., since the site plugin won't make any and it will dodge metadata I'm familiar with. None if this
     * is really good enough for safe usage with exotics like clearcase. Perhaps protest if anything other than svn or
     * git?
     * @throws MojoFailureException 
     */
    private void writeInventory() throws MojoFailureException
    {
        inventory = FileUtils.listFiles( checkoutDirectory, new DotFilter(), new DotFilter());
        SvnpubsubInventory initialInventory = new SvnpubsubInventory();
        Set<String> paths = new HashSet<String>();
        /*
         * It might be cleverer to store paths relative to the checkoutDirectory, but this really should work.
         */
        for ( File f : inventory )
        {
            // See below. We only bother about files.
            if ( f.isFile() )
            {
                paths.add( f.getAbsolutePath() );
            }
        }
        initialInventory.setPaths( paths );
        try
        {
            MappingJsonFactory factory = new MappingJsonFactory();
            JsonGenerator gen = factory.createJsonGenerator( inventoryFile, JsonEncoding.UTF8 );
            gen.writeObject( initialInventory );
            gen.close();
        }
        catch ( JsonProcessingException e )
        {
            throw new MojoFailureException( "Failed to write inventory to " + inventoryFile.getAbsolutePath(), e );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to write inventory to " + inventoryFile.getAbsolutePath(), e );
        }
    }
    
    /**
     * Clear out the data, so we can tell what's left after the run of the site plugin.
     * For now, don't bother with deleting empty directories. They are fairly harmless,
     * and leaving them around allows this to work with pre-1.7 svn.
     */
    private void deleteInventory() 
    {
        for ( File f : inventory )
        {
            if ( f.isFile() )
            {
                FileUtils.deleteQuietly( f );
            }
        }
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            checkoutExisting();
            writeInventory();
            deleteInventory();
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( ReleaseFailureException e )
        {
            throw new MojoFailureException( e.getMessage() );

        }
    }
}
