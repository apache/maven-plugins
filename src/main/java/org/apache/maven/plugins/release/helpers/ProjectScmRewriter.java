package org.apache.maven.plugins.release.helpers;

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
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.scm.provider.svn.SvnTagBranchUtils;

public class ProjectScmRewriter
{
    private ReleaseProgressTracker releaseProgress;

    public ProjectScmRewriter( ReleaseProgressTracker releaseProgress )
    {
        this.releaseProgress = releaseProgress;
    }

    public void rewriteScmInfo( Model model, String projectId, String tagLabel )
        throws MojoExecutionException
    {
        Scm scm = model.getScm();
        // If SCM is null in original model, it is inherited, no mods needed
        if ( scm != null )
        {
            releaseProgress.addOriginalScmInfo( projectId, scm );

            rewriteScmConnection( scm, tagLabel );
        }
    }

    public void restoreScmInfo( Model model )
        throws MojoExecutionException
    {
        Scm scm = model.getScm();
        if ( scm != null )
        {
            String groupId = model.getGroupId();
            if ( groupId == null && model.getParent() != null )
            {
                groupId = model.getParent().getGroupId();
            }

            if ( groupId == null )
            {
                throw new MojoExecutionException(
                    "Unable to determine groupId for artifact: " + model.getArtifactId() );
            }

            String projectId = ArtifactUtils.versionlessKey( groupId, model.getArtifactId() );

            releaseProgress.restoreScmInfo( projectId, scm );
        }
    }

    // TODO: Add other SCM types for rewriting, and allow other layouts
    private void rewriteScmConnection( Scm scm, String tag )
    {
        if ( scm != null )
        {
            String scmConnection = scm.getConnection();
            if ( scmConnection != null && scmConnection.startsWith( "scm:svn" ) )
            {
                scm.setConnection( SvnTagBranchUtils.resolveTagUrl( scmConnection, tag ) );

                String devConnection = scm.getDeveloperConnection();
                if ( devConnection != null )
                {
                    scm.setDeveloperConnection( SvnTagBranchUtils.resolveTagUrl( devConnection, tag ) );
                }

                String url = scm.getUrl();
                if ( url != null )
                {
                    scm.setUrl( SvnTagBranchUtils.resolveTagUrl( url, tag ) );
                }
            }
        }
    }

}
