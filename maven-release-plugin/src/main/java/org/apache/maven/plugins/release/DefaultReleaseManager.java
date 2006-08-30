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

import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.config.ReleaseDescriptorStore;
import org.apache.maven.plugins.release.config.ReleaseDescriptorStoreException;
import org.apache.maven.plugins.release.exec.MavenExecutor;
import org.apache.maven.plugins.release.exec.MavenExecutorException;
import org.apache.maven.plugins.release.phase.ReleasePhase;
import org.apache.maven.plugins.release.scm.ReleaseScmCommandException;
import org.apache.maven.plugins.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.plugins.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the release manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DefaultReleaseManager
    extends AbstractLogEnabled
    implements ReleaseManager
{
    /**
     * The phases of release to run, and in what order.
     */
    private List phases;

    /**
     * The available phases.
     */
    private Map releasePhases;

    /**
     * The configuration storage.
     */
    private ReleaseDescriptorStore configStore;

    /**
     * Tool for configuring SCM repositories from release configuration.
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * Tool to execute Maven.
     */
    private MavenExecutor mavenExecutor;

    public void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        prepare( releaseDescriptor, settings, reactorProjects, true, false );
    }

    public void prepare( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects, boolean resume,
                         boolean dryRun )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseDescriptor config;
        if ( resume )
        {
            try
            {
                config = configStore.read( releaseDescriptor );
            }
            catch ( ReleaseDescriptorStoreException e )
            {
                throw new ReleaseExecutionException( "Error reading stored configuration: " + e.getMessage(), e );
            }
        }
        else
        {
            config = releaseDescriptor;
        }

        // Later, it would be a good idea to introduce a proper workflow tool so that the release can be made up of a
        // more flexible set of steps.

        String completedPhase = config.getCompletedPhase();
        int index = phases.indexOf( completedPhase );

        if ( index == phases.size() - 1 )
        {
            getLogger().info(
                "Release preparation already completed. You can now continue with release:perform, or start again using the -Dresume=false flag" );
        }
        else if ( index >= 0 )
        {
            getLogger().info( "Resuming release from phase '" + phases.get( index + 1 ) + "'" );
        }

        // start from next phase
        for ( int i = index + 1; i < phases.size(); i++ )
        {
            String name = (String) phases.get( i );

            ReleasePhase phase = (ReleasePhase) releasePhases.get( name );

            if ( phase == null )
            {
                throw new ReleaseExecutionException( "Unable to find phase '" + name + "' to execute" );
            }

            if ( dryRun )
            {
                phase.simulate( config, settings, reactorProjects );
            }
            else
            {
                phase.execute( config, settings, reactorProjects );
            }

            config.setCompletedPhase( name );
            try
            {
                configStore.write( config );
            }
            catch ( ReleaseDescriptorStoreException e )
            {
                // TODO: rollback?
                throw new ReleaseExecutionException( "Error writing release properties after completing phase", e );
            }
        }
    }

    public void perform( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects,
                         File checkoutDirectory, String goals, boolean useReleaseProfile )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        getLogger().info( "Checking out the project to perform the release ..." );

        ReleaseDescriptor config;
        try
        {
            config = configStore.read( releaseDescriptor );
        }
        catch ( ReleaseDescriptorStoreException e )
        {
            throw new ReleaseExecutionException( "Error reading stored configuration: " + e.getMessage(), e );
        }

        // if we stopped mid-way through preparation - don't perform
        if ( config.getCompletedPhase() != null && !"end-release".equals( config.getCompletedPhase() ) )
        {
            throw new ReleaseFailureException(
                "Cannot perform release - the preparation step was stopped mid-way. Please re-run release:prepare to continue, or perform the release from an SCM tag." );
        }

        if ( config.getScmSourceUrl() == null )
        {
            throw new ReleaseFailureException( "No SCM URL was provided to perform the release from" );
        }

        ScmRepository repository;
        ScmProvider provider;
        try
        {
            repository = scmRepositoryConfigurator.getConfiguredRepository( config, settings );

            provider = scmRepositoryConfigurator.getRepositoryProvider( repository );
        }
        catch ( ScmRepositoryException e )
        {
            throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
        }

        // TODO: sanity check that it is not . or .. or lower

        if ( checkoutDirectory.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( checkoutDirectory );
            }
            catch ( IOException e )
            {
                throw new ReleaseExecutionException( "Unable to remove old checkout directory: " + e.getMessage(), e );
            }
        }
        checkoutDirectory.mkdirs();

        CheckOutScmResult result;
        try
        {
            result = provider.checkOut( repository, new ScmFileSet( checkoutDirectory ), config.getScmReleaseLabel() );
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error is occurred in the checkout process: " + e.getMessage(), e );
        }
        if ( !result.isSuccess() )
        {
            throw new ReleaseScmCommandException( "Unable to checkout from SCM", result );
        }

        String additionalArguments = config.getAdditionalArguments();

        if ( useReleaseProfile )
        {
            if ( !StringUtils.isEmpty( additionalArguments ) )
            {
                additionalArguments = additionalArguments + " -DperformRelease=true";
            }
            else
            {
                additionalArguments = "-DperformRelease=true";
            }
        }

        try
        {
            mavenExecutor.executeGoals( checkoutDirectory, goals, config.isInteractive(), additionalArguments,
                                        config.getPomFileName() );
        }
        catch ( MavenExecutorException e )
        {
            throw new ReleaseExecutionException( "Error executing Maven: " + e.getMessage(), e );
        }

        clean( config, reactorProjects );
    }

    public void clean( ReleaseDescriptor releaseDescriptor, List reactorProjects )
    {
        getLogger().info( "Cleaning up after release..." );

        configStore.delete( releaseDescriptor );

        for ( Iterator i = phases.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();

            ReleasePhase phase = (ReleasePhase) releasePhases.get( name );

            phase.clean( reactorProjects );
        }
    }

    void setConfigStore( ReleaseDescriptorStore configStore )
    {
        this.configStore = configStore;
    }

    void setMavenExecutor( MavenExecutor mavenExecutor )
    {
        this.mavenExecutor = mavenExecutor;
    }
}
