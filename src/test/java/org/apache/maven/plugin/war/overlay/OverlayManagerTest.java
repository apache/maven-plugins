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

import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.plugin.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugin.war.stub.WarArtifactStub;
import org.codehaus.plexus.PlexusTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephane Nicoll
 * @version $Id$
 */
public class OverlayManagerTest
    extends PlexusTestCase
{

    public static final String DEFAULT_INCLUDES = "**/**";

    public static final String DEFAULT_EXCLUDES = "META-INF/MANIFEST.MF";


    public void testEmptyProject()
        throws Exception
    {
        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final List overlays = new ArrayList();
        try
        {
            final Overlay currentProjectOVerlay = Overlay.createInstance();
            OverlayManager manager = new OverlayManager( overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES,
                                                         currentProjectOVerlay );
            assertNotNull( manager.getOverlays() );
            assertEquals( 1, manager.getOverlays().size() );
            assertEquals( currentProjectOVerlay, manager.getOverlays().get( 0 ) );
        }
        catch ( InvalidOverlayConfigurationException e )
        {
            e.printStackTrace();
            fail( "Should not have failed to validate a valid overly config " + e.getMessage() );
        }
    }

    public void testAutodetectSimpleOverlay( Overlay currentProjectOverlay )
        throws Exception
    {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact( "test", "test-webapp" );
        project.addArtifact( first );

        final List overlays = new ArrayList();

        try
        {
            final Overlay overlay = currentProjectOverlay;
            OverlayManager manager = new OverlayManager( overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES,
                                                         overlay );
            assertNotNull( manager.getOverlays() );
            assertEquals( 2, manager.getOverlays().size() );
            assertEquals( overlay, manager.getOverlays().get( 0 ) );
            assertEquals( new DefaultOverlay( first ), manager.getOverlays().get( 1 ) );
        }
        catch ( InvalidOverlayConfigurationException e )
        {
            e.printStackTrace();
            fail( "Should not have failed to validate a valid overlay config " + e.getMessage() );
        }
    }

    public void testSimpleOverlay()
        throws Exception
    {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact( "test", "test-webapp" );
        project.addArtifact( first );

        final List overlays = new ArrayList();
        overlays.add( new DefaultOverlay( first ) );

        try
        {
            final Overlay currentProjectOverlay = Overlay.createInstance();
            OverlayManager manager = new OverlayManager( overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES,
                                                         currentProjectOverlay );
            assertNotNull( manager.getOverlays() );
            assertEquals( 2, manager.getOverlays().size() );
            assertEquals( Overlay.createInstance(), manager.getOverlays().get( 0 ) );
            assertEquals( overlays.get( 0 ), manager.getOverlays().get( 1 ) );
        }
        catch ( InvalidOverlayConfigurationException e )
        {
            e.printStackTrace();
            fail( "Should not have failed to validate a valid overlay config " + e.getMessage() );
        }
    }

    public void testUnknonwnOverlay()
        throws Exception
    {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact( "test", "test-webapp" );
        project.addArtifact( first );

        final List overlays = new ArrayList();
        overlays.add( new Overlay( "test", "test-webapp-2" ) );

        try
        {
            final Overlay currentProjectOVerlay = Overlay.createInstance();
            new OverlayManager( overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES, currentProjectOVerlay );
            fail( "Should have failed to validate an unknown overlay" );
        }
        catch ( InvalidOverlayConfigurationException e )
        {
            // OK
        }
    }

    public void testCustomCurrentProject()
        throws Exception
    {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact( "test", "test-webapp" );
        final ArtifactStub second = newWarArtifact( "test", "test-webapp-2" );
        project.addArtifact( first );
        project.addArtifact( second );

        final List overlays = new ArrayList();
        overlays.add( new DefaultOverlay( first ) );
        final Overlay currentProjectOverlay = Overlay.createInstance();
        overlays.add( currentProjectOverlay );

        try
        {
            OverlayManager manager = new OverlayManager( overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES,
                                                         currentProjectOverlay );
            assertNotNull( manager.getOverlays() );
            assertEquals( 3, manager.getOverlays().size() );
            assertEquals( overlays.get( 0 ), manager.getOverlays().get( 0 ) );
            assertEquals( currentProjectOverlay, manager.getOverlays().get( 1 ) );
            assertEquals( new DefaultOverlay( second ), manager.getOverlays().get( 2 ) );

        }
        catch ( InvalidOverlayConfigurationException e )
        {
            e.printStackTrace();
            fail( "Should not have failed to validate a valid overlay config " + e.getMessage() );
        }
    }

    public void testOverlaysWithSameArtifactAndGroupId()
        throws Exception
    {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact( "test", "test-webapp" );
        final ArtifactStub second = newWarArtifact( "test", "test-webapp", "my-classifier" );

        project.addArtifact( first );
        project.addArtifact( second );

        final List overlays = new ArrayList();
        overlays.add( new DefaultOverlay( first ) );
        overlays.add( new DefaultOverlay( second ) );

        try
        {
            final Overlay currentProjectOverlay = Overlay.createInstance();
            OverlayManager manager = new OverlayManager( overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES,
                                                         currentProjectOverlay );
            assertNotNull( manager.getOverlays() );
            assertEquals( 3, manager.getOverlays().size() );
            assertEquals( currentProjectOverlay, manager.getOverlays().get( 0 ) );
            assertEquals( overlays.get( 0 ), manager.getOverlays().get( 1 ) );
            assertEquals( overlays.get( 1 ), manager.getOverlays().get( 2 ) );

        }
        catch ( InvalidOverlayConfigurationException e )
        {
            e.printStackTrace();
            fail( "Should not have failed to validate a valid overlay config " + e.getMessage() );
        }
    }


    protected ArtifactStub newWarArtifact( String groupId, String artifactId, String classifier )
    {
        final WarArtifactStub a = new WarArtifactStub( getBasedir() );
        a.setGroupId( groupId );
        a.setArtifactId( artifactId );
        if ( classifier != null )
        {
            a.setClassifier( classifier );
        }
        return a;
    }

    protected ArtifactStub newWarArtifact( String groupId, String artifactId )
    {
        return newWarArtifact( groupId, artifactId, null );

    }
}
