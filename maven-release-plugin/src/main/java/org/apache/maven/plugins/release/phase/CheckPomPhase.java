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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.ReleaseFailureException;
import org.apache.maven.plugins.release.ReleaseResult;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.plugins.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;

/**
 * Phase that checks the validity of the POM before release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckPomPhase
    extends AbstractReleasePhase
{
    /**
     * Retrieve an SCM repository, useful for validating an URL.
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        // Currently, we don't deal with multiple SCM locations in a multiproject
        if ( StringUtils.isEmpty( releaseDescriptor.getScmSourceUrl() ) )
        {
            MavenProject rootProject = (MavenProject) reactorProjects.get( 0 );
            if ( rootProject != null && rootProject.getScm() != null )
            {
                if ( rootProject.getScm().getDeveloperConnection() != null )
                {
                    releaseDescriptor.setScmSourceUrl( rootProject.getScm().getDeveloperConnection() );
                }
                else if ( rootProject.getScm().getConnection() != null )
                {
                    releaseDescriptor.setScmSourceUrl( rootProject.getScm().getConnection() );
                }
            }

            if ( StringUtils.isEmpty( releaseDescriptor.getScmSourceUrl() ) )
            {
                throw new ReleaseFailureException(
                    "Missing required setting: scm connection or developerConnection must be specified." );
            }

            try
            {
                scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor, settings );
            }
            catch ( ScmRepositoryException e )
            {
                throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
            }
            catch ( NoSuchScmProviderException e )
            {
                throw new ReleaseFailureException(
                    "The provider given in the SCM URL could not be found: " + e.getMessage() );
            }
        }

        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            if ( !ArtifactUtils.isSnapshot( project.getVersion() ) )
            {
                throw new ReleaseFailureException(
                    "The project " + projectId + " isn't a snapshot (" + project.getVersion() + ")." );
            }
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        // It makes no modifications, so simulate is the same as execute
        return execute( releaseDescriptor, settings, reactorProjects );
    }
}
