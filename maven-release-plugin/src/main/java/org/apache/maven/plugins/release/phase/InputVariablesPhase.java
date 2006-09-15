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
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import java.util.List;

/**
 * Input any variables that were not yet configured.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class InputVariablesPhase
    extends AbstractReleasePhase
{
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

        // get the root project
        MavenProject project = (MavenProject) reactorProjects.get( 0 );

        String tag = releaseDescriptor.getScmReleaseLabel();

        if ( tag == null )
        {
            // Must get default version from mapped versions, as the project will be the incorrect snapshot
            String key = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );
            String releaseVersion = (String) releaseDescriptor.getReleaseVersions().get( key );
            if ( releaseVersion == null )
            {
                throw new ReleaseExecutionException( "Project tag cannot be selected if version is not yet mapped" );
            }

            String defaultTag = project.getArtifactId() + "-" + releaseVersion;
            if ( releaseDescriptor.isInteractive() )
            {
                try
                {
                    tag = prompter.prompt( "What is SCM release tag or label for \"" + project.getName() + "\"? (" +
                        project.getGroupId() + ":" + project.getArtifactId() + ")", defaultTag );
                }
                catch ( PrompterException e )
                {
                    throw new ReleaseExecutionException( "Error reading version from input handler: " + e.getMessage(),
                                                         e );
                }
            }
            else
            {
                tag = defaultTag;
            }
            releaseDescriptor.setScmReleaseLabel( tag );
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
