package org.apache.maven.plugins.release.helpers;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.release.versions.DefaultVersionInfo;
import org.apache.maven.plugins.release.versions.VersionInfo;
import org.apache.maven.plugins.release.versions.VersionParseException;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProjectVersionResolver
{
    private Map resolvedVersions = new HashMap();

    private final Log log;

    private final InputHandler inputHandler;

    private final boolean interactive;

    public ProjectVersionResolver( Log log, InputHandler inputHandler, boolean interactive )
    {
        this.log = log;
        this.inputHandler = inputHandler;
        this.interactive = interactive;
    }

    public void resolveVersion( Model model, String projectId )
        throws MojoExecutionException
    {
        if ( resolvedVersions.containsKey( projectId ) )
        {
            throw new IllegalArgumentException(
                "Project: " + projectId + " is already resolved. Each project should only be resolved once." );
        }

        //Rewrite project version
        VersionInfo version = getVersionInfo( model.getVersion() );

        String projectVersion = ( version != null ) ? version.getReleaseVersionString() : null;

        if ( interactive )
        {
            projectVersion =
                getVersionFromUser( "What is the release version for \"" + projectId + "\"?", projectVersion );
        }
        else if ( StringUtils.isEmpty( projectVersion ) )
        {
            throw new MojoExecutionException( "Unable to determine release project version" );
        }

        model.setVersion( projectVersion );

        resolvedVersions.put( projectId, projectVersion );
    }

    private String getVersionFromUser( String promptText, String defaultVersionStr )
        throws MojoExecutionException
    {
        if ( defaultVersionStr != null )
        {
            promptText = promptText + " [" + defaultVersionStr + "]";
        }

        try
        {
            log.info( promptText );

            String inputVersion = inputHandler.readLine();

            return ( StringUtils.isEmpty( inputVersion ) ) ? defaultVersionStr : inputVersion;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Can't read version from user input.", e );
        }
    }

    public String getResolvedVersion( String groupId, String artifactId )
    {
        String projectId = ArtifactUtils.versionlessKey( groupId, artifactId );

        return (String) resolvedVersions.get( projectId );
    }

    public VersionInfo getVersionInfo( String version )
    {
        // TODO: Provide a way to override the implementation of VersionInfo
        try
        {
            return new DefaultVersionInfo( version );
        }
        catch ( VersionParseException e )
        {
            return null;
        }

    }

    public void incrementVersion( Model model, String projectId )
        throws MojoExecutionException
    {
        VersionInfo version = getVersionInfo( model.getVersion() );

        if ( version != null && version.isSnapshot() )
        {
            throw new MojoExecutionException( "The project " + projectId + " is a snapshot (" + model.getVersion() +
                "). It appears that the release version has not been committed." );
        }

        VersionInfo nextVersionInfo = version != null ? version.getNextVersion() : null;

        String nextVersion = nextVersionInfo != null ? nextVersionInfo.getSnapshotVersionString() : null;

        if ( interactive )
        {
            nextVersion =
                getVersionFromUser( "What is the new development version for \"" + projectId + "\"?", nextVersion );
        }
        else if ( nextVersion == null )
        {
            throw new MojoExecutionException( "Cannot determine incremented development version for: " + projectId );
        }

        model.setVersion( nextVersion );

        resolvedVersions.put( projectId, nextVersion );
    }
}
