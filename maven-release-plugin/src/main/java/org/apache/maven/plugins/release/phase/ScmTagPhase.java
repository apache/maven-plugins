package org.apache.maven.plugins.release.phase;

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

import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.apache.maven.plugins.release.scm.ReleaseScmCommandException;
import org.apache.maven.plugins.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.plugins.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Tag the SCM repository after committing the release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmTagPhase
    extends AbstractLogEnabled
    implements ReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    public void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        validateConfiguration( releaseConfiguration );

        getLogger().info( "Tagging release with the label " + releaseConfiguration.getReleaseLabel() + "..." );

        ScmRepository repository;
        ScmProvider provider;
        try
        {
            repository = scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

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

        TagScmResult result;
        try
        {
            // TODO: want includes/excludes?
            ScmFileSet fileSet = new ScmFileSet( releaseConfiguration.getWorkingDirectory() );
            result = provider.tag( repository, fileSet, releaseConfiguration.getReleaseLabel() );
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error is occurred in the tag process: " + e.getMessage(), e );
        }

        if ( !result.isSuccess() )
        {
            throw new ReleaseScmCommandException( "Unable to tag SCM", result );
        }
    }

    public void simulate( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        validateConfiguration( releaseConfiguration );

        getLogger().info( "Full run would be tagging " + releaseConfiguration.getWorkingDirectory() + " with label: '" +
            releaseConfiguration.getReleaseLabel() );
    }

    private static void validateConfiguration( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        if ( releaseConfiguration.getReleaseLabel() == null )
        {
            throw new ReleaseExecutionException( "A release label is required for committing" );
        }
    }
}
