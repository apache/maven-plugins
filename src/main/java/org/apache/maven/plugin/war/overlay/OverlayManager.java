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
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
     * @throws InvalidOverlayConfigurationException
     *          if the config is invalid
     */
    public OverlayManager( List overlays, MavenProject project, String defaultIncludes, String defaultExcludes )
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
        initialize( defaultIncludes, defaultExcludes );

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
     * @throws InvalidOverlayConfigurationException
     *          if the configuration is invalid
     */
    void initialize( String defaultIncludes, String defaultExcludes )
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
                overlay = Overlay.currentProjectInstance();
                it.set( overlay );
            }
            // default includes/excludes - only if the overlay uses the default settings
            if ( Overlay.DEFAULT_INCLUDES.equals( overlay.getIncludes() ) &&
                Overlay.DEFAULT_EXCLUDES.equals( overlay.getExcludes() ) )
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
            if ( overlay.equals( Overlay.currentProjectInstance() ) )
            {
                return;
            }
        }
        overlays.add( 0, Overlay.currentProjectInstance() );
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

        for (Iterator iterator = artifactsOverlays.iterator();iterator.hasNext();)
        {
            // TODO Handle ZIP artifact ; Handle classifier dependencies properly (clash management)
            Artifact artifact = (Artifact) iterator.next();
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
            // zip overlay is disabled by default except if user want it in the mojo's overlays
            if ( !artifact.isOptional() && filter.include( artifact ) && ( "zip".equals( artifact.getType() ) ) )
            {
                Overlay overlay = getAssociatedOverlay( artifact );
                // if the overlay doesn't exists we create a new with skip by default
                if ( overlay != null )
                {
                    Overlay zipOverlay = new DefaultOverlay(artifact);
                    zipOverlay.setSkip( true );
                    this.overlays.add( zipOverlay );
                }
                result.add( artifact );
            }
        }
        return result;
    }
    
    private Overlay getAssociatedOverlay( Artifact artifact )
    {
        if ( this.overlays == null )
        {
            return null;
        }
        for ( Iterator iterator = this.overlays.iterator(); iterator.hasNext(); )
        {
            Overlay overlay = (Overlay) iterator.next();
            if ( StringUtils.equals( artifact.getGroupId(), overlay.getGroupId() )
                && StringUtils.equals( artifact.getArtifactId(), overlay.getArtifactId() )
                && StringUtils.equals( artifact.getClassifier(), overlay.getClassifier() ))
            {
                return overlay;
            }
        }
        return null;
    }
}
