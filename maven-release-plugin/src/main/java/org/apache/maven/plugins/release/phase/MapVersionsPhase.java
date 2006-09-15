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
import org.apache.maven.plugins.release.ReleaseResult;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.versions.DefaultVersionInfo;
import org.apache.maven.plugins.release.versions.VersionInfo;
import org.apache.maven.plugins.release.versions.VersionParseException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Map projects to their new versions after release / into the next development cycle.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MapVersionsPhase
    extends AbstractReleasePhase
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

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            VersionInfo version = null;
            try
            {
                version = new DefaultVersionInfo( project.getVersion() );
            }
            catch ( VersionParseException e )
            {
                String msg = "Error parsing version, cannot determine next version: " + e.getMessage();
                if ( releaseDescriptor.isInteractive() )
                {
                    logWarn( result, msg );
                    logDebug( result, e.getMessage(), e );
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

                    if ( releaseDescriptor.isInteractive() )
                    {
                        nextVersion = prompter.prompt( "What is the new development version for \"" +
                            project.getName() + "\"? (" + projectId + ")", nextVersion );
                    }
                    else
                    {
                        Map devVersions = releaseDescriptor.getDevelopmentVersions();
                        if ( devVersions.containsKey( projectId ) )
                        {
                            nextVersion = devVersions.remove( projectId ).toString();
                        }
                    }

                    releaseDescriptor.mapDevelopmentVersion( projectId, nextVersion );
                }
                else
                {
                    String nextVersion = null;
                    if ( version != null )
                    {
                        nextVersion = version.getReleaseVersionString();
                    }

                    if ( releaseDescriptor.isInteractive() )
                    {
                        nextVersion = prompter.prompt(
                            "What is the release version for \"" + project.getName() + "\"? (" + projectId + ")",
                            nextVersion );
                    }
                    else
                    {
                        Map relVersions = releaseDescriptor.getReleaseVersions();
                        if ( relVersions.containsKey( projectId ) )
                        {
                            nextVersion = relVersions.remove( projectId ).toString();
                        }
                    }

                    releaseDescriptor.mapReleaseVersion( projectId, nextVersion );
                }
            }
            catch ( PrompterException e )
            {
                throw new ReleaseExecutionException( "Error reading version from input handler: " + e.getMessage(), e );
            }
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        // It makes no modifications, so simulate is the same as execute
        execute( releaseDescriptor, settings, reactorProjects );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

}
