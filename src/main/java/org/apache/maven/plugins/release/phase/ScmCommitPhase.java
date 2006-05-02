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
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Commit the project to the SCM.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmCommitPhase
    extends AbstractLogEnabled
    implements ReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * The format for the commit message.
     */
    private String messageFormat;

    public void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        validateConfiguration( releaseConfiguration );

        getLogger().info( "Checking in modified POMs..." );

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

        Collection pomFiles = createPomFiles( releaseConfiguration.getReactorProjects() );
        File[] files = (File[]) pomFiles.toArray( new File[pomFiles.size()] );

        CheckInScmResult result;
        try
        {
            ScmFileSet fileSet = new ScmFileSet( releaseConfiguration.getWorkingDirectory(), files );
            result = provider.checkIn( repository, fileSet, null, createMessage( releaseConfiguration ) );
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error is occurred in the checkin process: " + e.getMessage(), e );
        }
        if ( !result.isSuccess() )
        {
            throw new ReleaseScmCommandException( "Unable to commit files", result );
        }
    }

    public void simulate( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        validateConfiguration( releaseConfiguration );

        Collection pomFiles = createPomFiles( releaseConfiguration.getReactorProjects() );
        getLogger().info( "Full run would be checking in " + pomFiles.size() + " files with message: '" +
            createMessage( releaseConfiguration ) + "'" );
    }

    private static void validateConfiguration( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        if ( releaseConfiguration.getReleaseLabel() == null )
        {
            throw new ReleaseExecutionException( "A release label is required for committing" );
        }
    }

    private String createMessage( ReleaseConfiguration releaseConfiguration )
    {
        return MessageFormat.format( messageFormat, new Object[]{releaseConfiguration.getReleaseLabel()} );
    }

    private static Collection createPomFiles( List reactorProjects )
    {
        List pomFiles = new ArrayList( reactorProjects.size() );
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();
            pomFiles.add( project.getFile() );
        }
        return pomFiles;
    }
}
