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
import org.apache.maven.plugins.release.versions.DefaultVersionInfo;
import org.apache.maven.plugins.release.versions.VersionInfo;
import org.apache.maven.plugins.release.versions.VersionParseException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.Iterator;

/**
 * Map projects to their new versions after release / into the next development cycle.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MapVersionsPhase
    extends AbstractLogEnabled
    implements ReleasePhase
{
    /**
     * Whether to convert to a snapshot or a release.
     */
    private boolean convertToSnapshot;

    /**
     * Component used to prompt for input.
     */
    private Prompter prompter;

    void setPrompter( Prompter prompter )
    {
        this.prompter = prompter;
    }

    public void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        for ( Iterator i = releaseConfiguration.getReactorProjects().iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            VersionInfo version = null;
            try
            {
                // TODO [!]: make sure to test inherited version
                version = new DefaultVersionInfo( project.getVersion() );
            }
            catch ( VersionParseException e )
            {
                String msg = "Error parsing version, cannot determine next version: " + e.getMessage();
                if ( releaseConfiguration.isInteractive() )
                {
                    getLogger().warn( msg );
                    getLogger().debug( e.getMessage(), e );
                }
                else
                {
                    // cannot proceed without a next value in batch mode
                    throw new ReleaseExecutionException( msg, e );
                }
            }

            try
            {
                if ( convertToSnapshot )
                {
                    String nextVersion = null;
                    if ( version != null )
                    {
                        nextVersion = version.getNextVersion().getSnapshotVersionString();
                    }

                    if ( releaseConfiguration.isInteractive() )
                    {
                        nextVersion = prompter.prompt( "What is the new development version for \"" +
                            project.getName() + "\"? (" + projectId + ")", nextVersion );
                    }

                    releaseConfiguration.mapDevelopmentVersion( projectId, nextVersion );
                }
                else
                {
                    String nextVersion = null;
                    if ( version != null )
                    {
                        nextVersion = version.getReleaseVersionString();
                    }

                    if ( releaseConfiguration.isInteractive() )
                    {
                        nextVersion = prompter.prompt(
                            "What is the release version for \"" + project.getName() + "\"? (" + projectId + ")",
                            nextVersion );
                    }

                    releaseConfiguration.mapReleaseVersion( projectId, nextVersion );
                }
            }
            catch ( PrompterException e )
            {
                throw new ReleaseExecutionException( "Error reading version from input handler: " + e.getMessage(), e );
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
