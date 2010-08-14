package org.apache.maven.plugin.war.overlay;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Manages the overlays.
 *
 * @author Stephane Nicoll
 * 
 * @version $Id$
 */
public class OverlayManager
{
    private final List overlays;

    private final MavenProject project;

    private final List artifactsOverlays;

    /**
     * Creates a manager with the specified overlays.
     * <p/>
     * Note that the list is potentially updated by the
     * manager so a new list is created based on the overlays.
     *
     * @param overlays        the overlays
     * @param project         the maven project
     * @param defaultIncludes the default includes to use
     * @param defaultExcludes the default excludes to use
     * @param currentProjectOverlay the overlay for the current project
     * @throws InvalidOverlayConfigurationException
     *          if the config is invalid
     */
    public OverlayManager( List overlays, MavenProject project, String defaultIncludes, String defaultExcludes,
                           Overlay currentProjectOverlay )
        throws InvalidOverlayConfigurationException
    {
        this.overlays = new ArrayList();
        if ( overlays != null )
        {
            this.overlays.addAll( overlays );
        }
        this.project = project;

        this.artifactsOverlays = getOverlaysAsArtifacts();

        // Initialize
        initialize( defaultIncludes, defaultExcludes, currentProjectOverlay );

    }


    /**
     * Returns the resolved overlays.
     *
     * @return the overlays
     */
    public List getOverlays()
    {
        return overlays;
    }

    /**
     * Returns the id of the resolved overlays.
     *
     * @return the overlay ids
     */
    public List getOverlayIds()
    {
        final Iterator it = overlays.iterator();
        final List result = new ArrayList();
        while ( it.hasNext() )
        {
            Overlay overlay = (Overlay) it.next();
            result.add( overlay.getId() );
        }
        return result;

    }

    /**
     * Intializes the manager and validates the overlays configuration.
     *
     * @param defaultIncludes the default includes to use
     * @param defaultExcludes the default excludes to use
     * @param currentProjectOverlay  the overlay for the current project
     * @throws InvalidOverlayConfigurationException
     *          if the configuration is invalid
     */
    void initialize( String defaultIncludes, String defaultExcludes, Overlay currentProjectOverlay )
        throws InvalidOverlayConfigurationException
    {

        // Build the list of configured artifacts and makes sure that each overlay
        // refer to a valid artifact
        final List configuredWarArtifacts = new ArrayList();
        final ListIterator it = overlays.listIterator();
        while ( it.hasNext() )
        {
            Overlay overlay = (Overlay) it.next();
            if ( overlay == null )
            {
                throw new InvalidOverlayConfigurationException( "overlay could not be null." );
            }
            // If it's the current project, return the project instance
            if ( overlay.isCurrentProject() )
            {
                overlay = currentProjectOverlay;
                it.set( overlay );
            }
            // default includes/excludes - only if the overlay uses the default settings
            if ( Arrays.equals( Overlay.DEFAULT_INCLUDES, overlay.getIncludes() )
                && Arrays.equals( Overlay.DEFAULT_EXCLUDES, overlay.getExcludes() ) )
            {
                overlay.setIncludes( defaultIncludes );
                overlay.setExcludes( defaultExcludes );
            }

            final Artifact artifact = getAssociatedArtifact( overlay );
            if ( artifact != null )
            {
                configuredWarArtifacts.add( artifact );
                overlay.setArtifact( artifact );
            }
        }

        // Build the list of missing overlays
        final Iterator it2 = artifactsOverlays.iterator();
        while ( it2.hasNext() )
        {
            Artifact artifact = (Artifact) it2.next();
            if ( !configuredWarArtifacts.contains( artifact ) )
            {
                // Add a default overlay for the given artifact which will be applied after
                // the ones that have been configured
                overlays.add( new DefaultOverlay( artifact, defaultIncludes, defaultExcludes ) );
            }
        }

        // Final validation, make sure that the current project is in there. Otherwise add it first
        final Iterator it3 = overlays.iterator();
        while ( it3.hasNext() )
        {
            Overlay overlay = (Overlay) it3.next();
            if ( overlay.equals( currentProjectOverlay ) )
            {
                return;
            }
        }
        overlays.add( 0, currentProjectOverlay );
    }

    /**
     * Returns the Artifact associated to the specified overlay.
     * <p/>
     * If the overlay defines the current project, <tt>null</tt> is
     * returned. If no artifact could not be found for the overlay
     * a InvalidOverlayConfigurationException is thrown.
     *
     * @param overlay an overlay
     * @return the artifact associated to the overlay
     * @throws org.apache.maven.plugin.war.overlay.InvalidOverlayConfigurationException
     *          if the overlay does not have an associated artifact
     */
    Artifact getAssociatedArtifact( final Overlay overlay )
        throws InvalidOverlayConfigurationException
    {
        if ( overlay.isCurrentProject() )
        {
            return null;
        }

        for ( Iterator iterator = artifactsOverlays.iterator(); iterator.hasNext(); )
        {
            // Handle classifier dependencies properly (clash management)
            Artifact artifact = (Artifact) iterator.next();
            if ( compareOverlayWithArtifact( overlay, artifact ) )
            {
                return artifact;
            }
        }

        // maybe its a project dependencies zip or an other type
        Set projectArtifacts = this.project.getDependencyArtifacts();
        if ( projectArtifacts != null )
        {
            for ( Iterator iterator = projectArtifacts.iterator(); iterator.hasNext(); )
            {
                Artifact artifact = (Artifact) iterator.next();
                if ( compareOverlayWithArtifact( overlay, artifact ) )
                {
                    return artifact;
                }
            }
        }
        throw new InvalidOverlayConfigurationException(
            "overlay [" + overlay + "] is not a dependency of the project." );

    }

    /**
     * compare groupId && artifactId && Type && classifier
     * @param overlay the overlay
     * @param artifact the artifact
     * @return boolean true if equals
     */
    private boolean compareOverlayWithArtifact( Overlay overlay, Artifact artifact )
    {
        return ( StringUtils.equals( overlay.getGroupId(), artifact.getGroupId() )
            && StringUtils.equals( overlay.getArtifactId(), artifact.getArtifactId() )
            && StringUtils.equals( overlay.getType(), artifact.getType() )
            && StringUtils.equals( overlay.getClassifier(), artifact.getClassifier() ) );
    }

    /**
     * Returns a list of WAR {@link org.apache.maven.artifact.Artifact} describing
     * the overlays of the current project.
     *
     * @return the overlays as artifacts objects
     */
    private List getOverlaysAsArtifacts()
    {
        ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
        final Set artifacts = project.getArtifacts();
        final Iterator it = artifacts.iterator();

        final List result = new ArrayList();
        while ( it.hasNext() )
        {
            Artifact artifact = (Artifact) it.next();
            if ( !artifact.isOptional() && filter.include( artifact ) && ( "war".equals( artifact.getType() ) ) )
            {
                result.add( artifact );
            }
        }
        return result;
    }
}
