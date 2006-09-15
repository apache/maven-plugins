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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.ReleaseFailureException;
import org.apache.maven.plugins.release.ReleaseResult;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Check the dependencies of all projects being released to see if there are any unreleased snapshots.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo plugins with no version will be resolved to RELEASE which is not a snapshot, but remains unresolved to this point. This is a potential hole in the check, and should be revisited after the release pom writing is done and resolving versions to verify whether it is.
 * @todo plugins injected by the lifecycle are not tested here. They will be injected with a RELEASE version so are covered under the above point.
 */
public class CheckDependencySnapshotsPhase
    extends AbstractReleasePhase
{
    /**
     * Component used to prompt for input.
     */
    private Prompter prompter;

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        logInfo( result, "Checking dependencies and plugins for snapshots ..." );

        Map originalVersions = releaseDescriptor.getOriginalVersions( reactorProjects );

        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            checkProject( project, originalVersions, releaseDescriptor );
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void checkProject( MavenProject project, Map originalVersions, ReleaseDescriptor releaseDescriptor )
        throws ReleaseFailureException, ReleaseExecutionException
    {
        Set snapshotDependencies = new HashSet();

        if ( project.getParentArtifact() != null )
        {
            if ( checkArtifact( project.getParentArtifact(), originalVersions ) )
            {
                snapshotDependencies.add( project.getParentArtifact() );
            }
        }

        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, originalVersions ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        for ( Iterator i = project.getPluginArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, originalVersions ) )
            {
                boolean addToFailures = true;

                if ( "org.apache.maven.plugins".equals( artifact.getGroupId() ) &&
                    "maven-release-plugin".equals( artifact.getArtifactId() ) )
                {
                    // It's a snapshot of the release plugin. Maybe just testing - ask
                    // By default, we fail as for any ohter plugin
                    if ( releaseDescriptor.isInteractive() )
                    {
                        try
                        {
                            prompter.showMessage(
                                "This project relies on a SNAPSHOT of the release plugin. This may be necessary during testing." );
                            String result = prompter.prompt( "Do you want to continue with the release?",
                                                             Arrays.asList( new String[]{"yes", "no"} ), "no" );
                            if ( result.toLowerCase().startsWith( "y" ) )
                            {
                                addToFailures = false;
                            }
                        }
                        catch ( PrompterException e )
                        {
                            throw new ReleaseExecutionException( e.getMessage(), e );
                        }
                    }
                }

                if ( addToFailures )
                {
                    snapshotDependencies.add( artifact );
                }
            }
        }

        for ( Iterator i = project.getReportArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, originalVersions ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        for ( Iterator i = project.getExtensionArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            if ( checkArtifact( artifact, originalVersions ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        if ( !snapshotDependencies.isEmpty() )
        {
            List snapshotsList = new ArrayList( snapshotDependencies );

            Collections.sort( snapshotsList );

            StringBuffer message = new StringBuffer();

            for ( Iterator i = snapshotsList.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                message.append( "    " );

                message.append( artifact );

                message.append( "\n" );
            }

            message.append( "in project '" + project.getName() + "' (" + project.getId() + ")" );

            throw new ReleaseFailureException( "Can't release project due to non released dependencies :\n" + message );
        }
    }

    private static boolean checkArtifact( Artifact artifact, Map originalVersions )
    {
        String versionlessArtifactKey = ArtifactUtils.versionlessKey( artifact.getGroupId(), artifact.getArtifactId() );

        // We are only looking at dependencies external to the project - ignore anything found in the reactor as
        // it's version will be updated
        return artifact.isSnapshot() &&
            !artifact.getBaseVersion().equals( originalVersions.get( versionlessArtifactKey ) );
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        // It makes no modifications, so simulate is the same as execute
        return execute( releaseDescriptor, settings, reactorProjects );
    }

    public void setPrompter( Prompter prompter )
    {
        this.prompter = prompter;
    }
}
