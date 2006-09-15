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
import org.apache.maven.plugins.release.ReleaseFailureException;
import org.apache.maven.plugins.release.ReleaseResult;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.scm.ReleaseScmCommandException;
import org.apache.maven.plugins.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.plugins.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * See if there are any local modifications to the files before proceeding with SCM operations and the release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmCheckModificationsPhase
    extends AbstractReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * The files to exclude from the status check.
     *
     * @todo proper construction of filenames, especially release properties
     */
    private Set excludedFiles = new HashSet( Arrays.asList(
        new String[]{"pom.xml", "pom.xml.backup", "pom.xml.tag", "pom.xml.next", "release.properties"} ) );

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult relResult = new ReleaseResult();

        logInfo( relResult, "Verifying that there are no local modifications..." );

        ScmRepository repository;
        ScmProvider provider;
        try
        {
            repository = scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor, settings );

            provider = scmRepositoryConfigurator.getRepositoryProvider( repository );
        }
        catch ( ScmRepositoryException e )
        {
            throw new ReleaseScmRepositoryException(
                e.getMessage() + " for URL: " + releaseDescriptor.getScmSourceUrl(), e.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
        }

        StatusScmResult result;
        try
        {
            result =
                provider.status( repository, new ScmFileSet( new File( releaseDescriptor.getWorkingDirectory() ) ) );
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error occurred during the status check process: " + e.getMessage(),
                                                 e );
        }

        if ( !result.isSuccess() )
        {
            throw new ReleaseScmCommandException( "Unable to check for local modifications", result );
        }

        List changedFiles = result.getChangedFiles();

        // TODO: would be nice for SCM status command to do this for me.
        for ( Iterator i = changedFiles.iterator(); i.hasNext(); )
        {
            ScmFile f = (ScmFile) i.next();

            String fileName = f.getPath().replace( '\\', '/' );
            fileName = fileName.substring( fileName.lastIndexOf( '/' ) + 1, fileName.length() );

            if ( excludedFiles.contains( fileName ) )
            {
                i.remove();
            }
        }

        if ( !changedFiles.isEmpty() )
        {
            StringBuffer message = new StringBuffer();

            for ( Iterator i = changedFiles.iterator(); i.hasNext(); )
            {
                ScmFile file = (ScmFile) i.next();

                message.append( file.toString() );

                message.append( "\n" );
            }

            throw new ReleaseFailureException(
                "Cannot prepare the release because you have local modifications : \n" + message );
        }

        relResult.setResultCode( ReleaseResult.SUCCESS );

        return relResult;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        // It makes no modifications, so simulate is the same as execute
        return execute( releaseDescriptor, settings, reactorProjects );
    }
}
