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
 *  http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Manages the overlays.
 *
 * @author Stephane Nicoll
 */
public class OverlayManager
{
    private final List overlays;

    private final MavenProject project;

    private final List warArtifacts;

    /**
     * Creates a manager with the specified overlays.
     * <p/>
     * Note that the list is potentially updated by the
     * manager so a new list is created based on the overlays.
     *
     * @param overlays the overlays
     * @param project the maven project
     * @throws InvalidOverlayConfigurationException if the config is invalid
     */
    public OverlayManager( List overlays, MavenProject project )
        throws InvalidOverlayConfigurationException
    {
        this.overlays = new ArrayList();
        if ( overlays != null )
        {
            this.overlays.addAll( overlays );
        }
        this.project = project;

        this.warArtifacts = getWarOverlaysAsArtifacts();

        // Initialize
        initialize();

    }

    /**
     * Returns the war artifactst attachted to the project.
     *
     * @return a list of war Artifact
     */
    public List getWarArtifacts()
    {
        return warArtifacts;
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
     * Intializes the manager and validates the overlays configuration.
     *
     * @throws org.apache.maven.plugin.war.overlay.InvalidOverlayConfigurationException
     *          if the configuration is invalid
     */
    void initialize()
        throws InvalidOverlayConfigurationException
    {

        // Build the list of configured artifacts and makes sure that each overlay
        // refer to a valid artifact
        final List configuredWarArtifacts = new ArrayList();
        final Iterator it = overlays.iterator();
        while ( it.hasNext() )
        {
            Overlay overlay = (Overlay) it.next();
            if ( overlay == null )
            {
                throw new InvalidOverlayConfigurationException( "overlay could not be null." );
            }
            final Artifact artifact = getAssociatedArtifact( overlay );
            if ( artifact != null )
            {
                configuredWarArtifacts.add( artifact );
                overlay.setArtifact( artifact );
            }
        }

        // Build the list of missing overlays
        final Iterator it2 = warArtifacts.iterator();
        while ( it2.hasNext() )
        {
            Artifact artifact = (Artifact) it2.next();
            if ( !configuredWarArtifacts.contains( artifact ) )
            {
                // Add a default overlay for the given artifact which will be applied after
                // the ones that have been configured
                overlays.add( new DefaultOverlay( artifact ) );
            }
        }

        // Final validation, make sure that the current project is in there. Otherwise add it first
        final Iterator it3 = overlays.iterator();
        while ( it3.hasNext() )
        {
            Overlay overlay = (Overlay) it3.next();
            if ( overlay.equals( Overlay.currentProjectInstance() ) )
            {
                return;
            }
        }
        overlays.add( 0, Overlay.currentProjectInstance() );
    }


    void initializeOverlay( final Overlay overlay )
        throws InvalidOverlayConfigurationException
    {
        if ( overlay == null )
        {
            throw new InvalidOverlayConfigurationException( "overlay could not be null." );
        }
        final Artifact artifact = getAssociatedArtifact( overlay );
        overlay.setArtifact( artifact );


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

        final Iterator it = warArtifacts.iterator();
        while ( it.hasNext() )
        {
            Artifact artifact = (Artifact) it.next();
            if ( overlay.getGroupId().equals( artifact.getGroupId() ) &&
                overlay.getArtifactId().equals( artifact.getArtifactId() ) &&
                ( overlay.getClassifier() == null || ( overlay.getClassifier().equals( artifact.getClassifier() ) ) ) )
            {
                return artifact;
            }
        }
        throw new InvalidOverlayConfigurationException(
            "overlay[" + overlay + "] is not a dependency of the project." );

    }

    /**
     * Returns a list of war {@link org.apache.maven.artifact.Artifact} describing
     * the overlays of the current project.
     *
     * @return the overlays as artifacts objects
     */
    List getWarOverlaysAsArtifacts()
    {
        ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
        final Set artifacts = project.getArtifacts();
        final Iterator it = artifacts.iterator();

        final List result = new ArrayList();
        while ( it.hasNext() )
        {
            Artifact artifact = (Artifact) it.next();
            if ( !artifact.isOptional() && filter.include( artifact ) && "war".equals( artifact.getType() ) )
            {
                result.add( artifact );
            }
        }
        return result;
    }
}
