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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Manages the overlays.
 *
 * @author Stephane Nicoll
 * 
 * @version $Id$
 */
public class OverlayManager
{
    private final List<Overlay> overlays;

    private final MavenProject project;

    private final List<Artifact> artifactsOverlays;

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
    public OverlayManager( List<Overlay> overlays, MavenProject project, String defaultIncludes, String defaultExcludes,
                           Overlay currentProjectOverlay )
        throws InvalidOverlayConfigurationException
    {
        this.overlays = new ArrayList<Overlay>();
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
    public List<Overlay> getOverlays()
    {
        return overlays;
    }

    /**
     * Returns the id of the resolved overlays.
     *
     * @return the overlay ids
     */
    public List<String> getOverlayIds()
    {
        final List<String> result = new ArrayList<String>();
        for ( Overlay overlay : overlays )
        {
            result.add( overlay.getId() );
        }
        return result;

    }

    /**
     * Initializes the manager and validates the overlays configuration.
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
        final List<Artifact> configuredWarArtifacts = new ArrayList<Artifact>();
        final ListIterator<Overlay> it = overlays.listIterator();
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
        for ( Artifact artifact : artifactsOverlays )
        {
            if ( !configuredWarArtifacts.contains( artifact ) )
            {
                // Add a default overlay for the given artifact which will be applied after
                // the ones that have been configured
                overlays.add( new DefaultOverlay( artifact, defaultIncludes, defaultExcludes ) );
            }
        }

        // Final validation, make sure that the current project is in there. Otherwise add it first
        for ( Overlay overlay : overlays )
        {
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

        for ( Artifact artifact : artifactsOverlays )
        {
            // Handle classifier dependencies properly (clash management)
            if ( compareOverlayWithArtifact( overlay, artifact ) )
            {
                return artifact;
            }
        }

        // maybe its a project dependencies zip or an other type
        @SuppressWarnings( "unchecked" )
        Set<Artifact> projectArtifacts = this.project.getDependencyArtifacts();
        if ( projectArtifacts != null )
        {
            for ( Artifact artifact : projectArtifacts )
            {
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
     * Compare groupId && artifactId && type && classifier.
     *
     * @param overlay the overlay
     * @param artifact the artifact
     * @return boolean true if equals
     */
    private boolean compareOverlayWithArtifact( Overlay overlay, Artifact artifact )
    {
        return ( StringUtils.equals( overlay.getGroupId(), artifact.getGroupId() )
            && StringUtils.equals( overlay.getArtifactId(), artifact.getArtifactId() )
            && StringUtils.equals( overlay.getType(), artifact.getType() )
            // MWAR-241 Make sure to treat null and "" as equal when comparing the classifier
            && StringUtils.equals( StringUtils.defaultString( overlay.getClassifier() ), StringUtils.defaultString( artifact.getClassifier() ) ) );
    }

    /**
     * Returns a list of WAR {@link org.apache.maven.artifact.Artifact} describing
     * the overlays of the current project.
     *
     * @return the overlays as artifacts objects
     */
    private List<Artifact> getOverlaysAsArtifacts()
    {
        ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
        @SuppressWarnings( "unchecked" )
        final Set<Artifact> artifacts = project.getArtifacts();

        final List<Artifact> result = new ArrayList<Artifact>();
        for ( Artifact artifact : artifacts )
        {
            if ( !artifact.isOptional() && filter.include( artifact ) && ( "war".equals( artifact.getType() ) ) )
            {
                result.add( artifact );
            }
        }
        return result;
    }
}
