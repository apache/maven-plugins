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
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;

/**
 * Phase that checks the validity of the POM before release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckPomPhase
    implements ReleasePhase
{
    public void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        // TODO [!]: SCM URL is coming from the store but it probably needs to be read from every project instead
        // TODO [!]: prepare release mojo needs to set this correct to either connection or developerConnection as appropriate - new version doesn't handle it
        if ( StringUtils.isEmpty( releaseConfiguration.getUrl() ) )
        {
            throw new ReleaseExecutionException(
                "Missing required setting: scm connection or developerConnection must be specified." );
        }

        List reactorProjects = releaseConfiguration.getReactorProjects();
        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            if ( !ArtifactUtils.isSnapshot( project.getVersion() ) )
            {
                throw new ReleaseExecutionException(
                    "The project " + projectId + " isn't a snapshot (" + project.getVersion() + ")." );
            }
        }
    }

    public void simulate( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        // It makes no modifications, so simulate is the same as execute
        execute( releaseConfiguration );
    }
}
