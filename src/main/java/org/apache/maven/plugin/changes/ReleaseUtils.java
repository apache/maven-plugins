package org.apache.maven.plugin.changes;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.Release;

/**
 * A utility class for working with Release objects.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class ReleaseUtils
{
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    private Log log;

    public ReleaseUtils( Log log )
    {
        this.log = log;
    }

    /**
     * Get the latest release by matching the supplied releases
     * with the version from the pom.
     *
     * @param releases list of releases
     * @param pomVersion Version of the artifact
     * @return A <code>Release</code> that matches the next release of the current project
     * @throws org.apache.maven.plugin.MojoExecutionException If a release can't be found
     */
    public Release getLatestRelease( List<Release> releases, String pomVersion )
        throws MojoExecutionException
    {
        // Remove "-SNAPSHOT" from the end, if it's there
        if ( pomVersion != null && pomVersion.endsWith( SNAPSHOT_SUFFIX ) )
        {
            pomVersion = pomVersion.substring( 0, pomVersion.length() - SNAPSHOT_SUFFIX.length() );
        }
        getLog().debug( "Found " + releases.size() + " releases." );

        Release release = getRelease( releases, pomVersion );

        if ( release == null )
        {
            throw new MojoExecutionException( "Couldn't find the release '" + pomVersion
                + "' among the supplied releases." );
        }

        return release;
    }

    private Log getLog()
    {
        return log;
    }

    /**
     * Get a release with the specified version from the list of releases.
     *
     * @param releases A list of releases
     * @param version The version we want
     * @return A Release, or null if no release with the specified version can be found
     */
    protected Release getRelease( List<Release> releases, String version )
    {
        for ( Release release : releases )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "The release: " + release.getVersion()
                    + " has " + release.getActions().size() + " actions." );
            }

            if ( release.getVersion() != null && release.getVersion().equals( version ) )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Found the correct release: " + release.getVersion() );
                    logRelease( release );
                }
                return release;
            }
        }
        return null;
    }

    protected void logRelease( Release release )
    {
        Action action;
        for ( Iterator iterator = release.getActions().iterator(); iterator.hasNext(); )
        {
            action = (Action) iterator.next();
            getLog().debug( "o " + action.getType() );
            getLog().debug( "issue : " + action.getIssue() );
            getLog().debug( "action : " + action.getAction() );
            getLog().debug( "dueTo : " + action.getDueTo() );
        }
    }

    /**
     * Merge releases from one issue tracker with releases from another issue
     * tracker. If a release is found in both issue trackers, i.e. they have
     * the same version, their issues are merged into one release.
     *
     * @param firstReleases Releases from the first issue tracker
     * @param secondReleases Releases from the second issue tracker
     * @return A list containing the merged releases
     */
    public List<Release> mergeReleases( final List<Release> firstReleases, final List<Release> secondReleases )
    {
        if ( firstReleases == null && secondReleases == null )
        {
            return Collections.emptyList();
        }
        if ( firstReleases == null )
        {
            return secondReleases;
        }
        if ( secondReleases == null )
        {
            return firstReleases;
        }

        List<Release> mergedReleases = new ArrayList<Release>();

        // Loop through the releases from the first issue tracker, merging in
        // actions from releases with the same version from the second issue
        // tracker
        for ( Release firstRelease : firstReleases )
        {
            Release secondRelease = getRelease( secondReleases, firstRelease.getVersion() );
            if ( secondRelease != null )
            {
                if ( secondRelease.getActions() != null )
                {
                    firstRelease.getActions().addAll( secondRelease.getActions() );
                }
            }
            mergedReleases.add( firstRelease );
        }

        // Handle releases that are only in the second issue tracker
        for ( Release secondRelease : secondReleases )
        {
            Release mergedRelease = getRelease( mergedReleases, secondRelease.getVersion() );
            if ( mergedRelease == null )
            {
                mergedReleases.add( secondRelease );
            }
        }
        return mergedReleases;
    }

    /**
     * Convert an untyped List of Release objects that comes from changes.xml
     * into a typed List of Release objects.
     *
     * @param changesReleases An untyped List of Release objects
     * @return A type List of Release objects
     * @todo When Modello can generate typed collections this method is no longer needed
     */
    public List<Release> convertReleaseList( List changesReleases ) {
        List<Release> releases = new ArrayList<Release>();

        // Loop through the List of releases from changes.xml and casting each
        // release to a Release
        for ( Iterator iterator = changesReleases.iterator(); iterator.hasNext(); )
        {
            Release release = (Release) iterator.next();
            releases.add( release );
        }
        return releases;
    }
}
