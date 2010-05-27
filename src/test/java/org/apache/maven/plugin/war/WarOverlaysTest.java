package org.apache.maven.plugin.war;

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
import org.apache.maven.plugin.war.overlay.DefaultOverlay;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Stephane Nicoll
 */
public class WarOverlaysTest
    extends AbstractWarExplodedMojoTest
{

    private static File pomFile = new File( getBasedir(), "target/test-classes/unit/waroverlays/default.xml" );


    public void setUp()
        throws Exception
    {
        super.setUp();
        generateFullOverlayWar( "overlay-full-1" );
        generateFullOverlayWar( "overlay-full-2" );
        generateFullOverlayWar( "overlay-full-3" );
    }

    protected File getPomFile()
    {
        return pomFile;
    }

    protected File getTestDirectory()
    {
        return new File( getBasedir(), "target/test-classes/unit/waroverlays" );
    }

    public void testEnvironment()
        throws Exception
    {
        // see setup
    }

    public void testNoOverlay()
        throws Exception
    {
        // setup test data
        final String testId = "no-overlay";
        final File xmlSource = createXMLConfigDir( testId, new String[]{"web.xml"} );

        final File webAppDirectory = setUpMojo( testId, null );
        try
        {
            mojo.setWebXml( new File( xmlSource, "web.xml" ) );
            mojo.execute();

            // Validate content of the webapp
            assertDefaultContent( webAppDirectory );
            assertWebXml( webAppDirectory );
        }
        finally
        {
            cleanDirectory( webAppDirectory );
        }
    }

    public void testDefaultOverlay()
        throws Exception
    {
        // setup test data
        final String testId = "default-overlay";

        // Add an overlay
        final ArtifactStub overlay = buildWarOverlayStub( "overlay-one" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay} );
        final List assertedFiles = new ArrayList();
        try
        {
            mojo.execute();
            assertedFiles.addAll( assertDefaultContent( webAppDirectory ) );
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory, new String[]{"index.jsp", "login.jsp"},
                                                       "overlay file not found" ) );

            // index and login come from overlay1
            assertOverlayedFile( webAppDirectory, "overlay-one", "index.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-one", "login.jsp" );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{MANIFEST_PATH} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );
        }
        finally
        {
            cleanDirectory( webAppDirectory );
        }
    }

    public void testDefaultOverlays()
        throws Exception
    {
        // setup test data
        final String testId = "default-overlays";

        // Add an overlay
        final ArtifactStub overlay = buildWarOverlayStub( "overlay-one" );
        final ArtifactStub overlay2 = buildWarOverlayStub( "overlay-two" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay, overlay2} );
        final List assertedFiles = new ArrayList();
        try
        {
            mojo.execute();
            assertedFiles.addAll( assertDefaultContent( webAppDirectory ) );
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory,
                                                       new String[]{"index.jsp", "login.jsp", "admin.jsp"},
                                                       "overlay file not found" ) );

            // index and login come from overlay1
            assertOverlayedFile( webAppDirectory, "overlay-one", "index.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-one", "login.jsp" );

            //admin comes from overlay2
            // index and login comes from overlay1
            assertOverlayedFile( webAppDirectory, "overlay-two", "admin.jsp" );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{MANIFEST_PATH} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );
        }
        finally
        {
            cleanDirectory( webAppDirectory );
        }
    }


    /**
     * Merge a dependent WAR when a file in the war source directory
     * overrides one found in the WAR.
     * <p/>
     * It also tests completeness of the resulting war as well as the proper
     * order of dependencies.
     *
     * @throws Exception if any error occurs
     */
    public void testScenarioOneWithDefaulSettings()
        throws Exception
    {
        // setup test data
        final String testId = "scenario-one-default-settings";

        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub( "overlay-full-1" );
        final ArtifactStub overlay2 = buildWarOverlayStub( "overlay-full-2" );
        final ArtifactStub overlay3 = buildWarOverlayStub( "overlay-full-3" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay1, overlay2, overlay3},
                                                new String[]{"org/sample/company/test.jsp", "jsp/b.jsp"} );

        assertScenariOne( testId, webAppDirectory );
    }


    /**
     * Tests that specifying the overlay explicitely has the same behavior as
     * the default (i.e. order, etc).
     * <p/>
     * The default project is not specified in this case so it is processed
     * first by default
     *
     * @throws Exception if an error occurs
     */
    public void testScenarioOneWithOverlaySettings()
        throws Exception
    {
        // setup test data
        final String testId = "scenario-one-overlay-settings";

        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub( "overlay-full-1" );
        final ArtifactStub overlay2 = buildWarOverlayStub( "overlay-full-2" );
        final ArtifactStub overlay3 = buildWarOverlayStub( "overlay-full-3" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay1, overlay2, overlay3},
                                                new String[]{"org/sample/company/test.jsp", "jsp/b.jsp"} );

        // Add the tags
        final List overlays = new ArrayList();
        overlays.add( new DefaultOverlay( overlay1 ) );
        overlays.add( new DefaultOverlay( overlay2 ) );
        overlays.add( new DefaultOverlay( overlay3 ) );
        mojo.setOverlays( overlays );

        // current project ignored. Should be on top of the list
        assertScenariOne( testId, webAppDirectory );
    }

    /**
     * Tests that specifying the overlay explicitely has the same behavior as
     * the default (i.e. order, etc).
     * <p/>
     * The default project is explicitely specified so this should match the
     * default.
     *
     * @throws Exception if an error occurs
     */
    public void testScenarioOneWithFullSettings()
        throws Exception
    {
        // setup test data
        final String testId = "scenario-one-full-settings";

        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub( "overlay-full-1" );
        final ArtifactStub overlay2 = buildWarOverlayStub( "overlay-full-2" );
        final ArtifactStub overlay3 = buildWarOverlayStub( "overlay-full-3" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay1, overlay2, overlay3},
                                                new String[]{"org/sample/company/test.jsp", "jsp/b.jsp"} );

        // Add the tags
        final List overlays = new ArrayList();

        // Add the default project explicitely
        overlays.add( mojo.getCurrentProjectOverlay() );

        // Other overlays
        overlays.add( new DefaultOverlay( overlay1 ) );
        overlays.add( new DefaultOverlay( overlay2 ) );
        overlays.add( new DefaultOverlay( overlay3 ) );
        mojo.setOverlays( overlays );

        // current project ignored. Should be on top of the list
        assertScenariOne( testId, webAppDirectory );
    }


    /**
     * Runs the mojo and asserts a scenerio with 3 overlays and no
     * includes/excludes settings.
     *
     * @param testId          thie id of the test
     * @param webAppDirectory the webapp directory
     * @throws Exception if an exception occurs
     */
    private void assertScenariOne( String testId, File webAppDirectory )
        throws Exception
    {
        final List assertedFiles = new ArrayList();
        try
        {
            mojo.execute();
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory, new String[]{"jsp/a.jsp", "jsp/b.jsp",
                "jsp/c.jsp", "jsp/d/a.jsp", "jsp/d/b.jsp", "jsp/d/c.jsp", "org/sample/company/test.jsp",
                "WEB-INF/classes/a.class", "WEB-INF/classes/b.class", "WEB-INF/classes/c.class", "WEB-INF/lib/a.jar",
                "WEB-INF/lib/b.jar", "WEB-INF/lib/c.jar"}, "overlay file not found" ) );

            // Those files should come from the source webapp without any config
            assertDefaultFileContent( testId, webAppDirectory, "jsp/b.jsp" );
            assertDefaultFileContent( testId, webAppDirectory, "org/sample/company/test.jsp" );

            // Everything else comes from overlay1 (order of addition in the dependencies)
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "jsp/a.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "jsp/c.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "jsp/d/a.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "jsp/d/b.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "jsp/d/c.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "WEB-INF/web.xml" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "WEB-INF/classes/a.class" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "WEB-INF/classes/b.class" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "WEB-INF/classes/c.class" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "WEB-INF/lib/a.jar" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "WEB-INF/lib/b.jar" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "WEB-INF/lib/c.jar" );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{MANIFEST_PATH} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );
        }
        finally
        {
            cleanDirectory( webAppDirectory );
        }
    }

    public void testOverlaysIncludesExcludesWithMultipleDefinitions()
        throws Exception
    {
        // setup test data
        final String testId = "overlays-includes-excludes-multiple-defs";

        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub( "overlay-full-1" );
        final ArtifactStub overlay2 = buildWarOverlayStub( "overlay-full-2" );
        final ArtifactStub overlay3 = buildWarOverlayStub( "overlay-full-3" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay1, overlay2, overlay3},
                                                new String[]{"org/sample/company/test.jsp", "jsp/b.jsp"} );

        Overlay over1 = new DefaultOverlay( overlay3 );
        over1.setExcludes( "**/a.*,**/c.*,**/*.xml" );

        Overlay over2 = new DefaultOverlay( overlay1 );
        over2.setIncludes( "jsp/d/*" );
        over2.setExcludes( "jsp/d/a.jsp" );

        Overlay over3 = new DefaultOverlay( overlay3 );
        over3.setIncludes( "**/*.jsp" );

        Overlay over4 = new DefaultOverlay( overlay2 );

        mojo.setOverlays( new LinkedList() );
        mojo.addOverlay( over1 );
        mojo.addOverlay( over2 );
        mojo.addOverlay( over3 );
        mojo.addOverlay( mojo.getCurrentProjectOverlay());
        mojo.addOverlay( over4 );

        final List assertedFiles = new ArrayList();
        try
        {
            mojo.execute();
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory, new String[]{"jsp/a.jsp", "jsp/b.jsp",
                "jsp/c.jsp", "jsp/d/a.jsp", "jsp/d/b.jsp", "jsp/d/c.jsp", "org/sample/company/test.jsp",
                "WEB-INF/classes/a.class", "WEB-INF/classes/b.class", "WEB-INF/classes/c.class", "WEB-INF/lib/a.jar",
                "WEB-INF/lib/b.jar", "WEB-INF/lib/c.jar"}, "overlay file not found" ) );

            assertOverlayedFile( webAppDirectory, "overlay-full-3", "jsp/a.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "jsp/b.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "jsp/c.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "jsp/d/a.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "jsp/d/b.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "jsp/d/c.jsp" );
            assertDefaultFileContent( testId, webAppDirectory, "org/sample/company/test.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/web.xml" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/classes/a.class" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "WEB-INF/classes/b.class" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/classes/c.class" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/lib/a.jar" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "WEB-INF/lib/b.jar" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/lib/c.jar" );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{MANIFEST_PATH} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );
        }
        finally
        {
            cleanDirectory( webAppDirectory );
        }
    }


    public void testOverlaysIncludesExcludesWithMultipleDefinitions2()
        throws Exception
    {
        // setup test data
        final String testId = "overlays-includes-excludes-multiple-defs2";

        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub( "overlay-full-1" );
        final ArtifactStub overlay2 = buildWarOverlayStub( "overlay-full-2" );
        final ArtifactStub overlay3 = buildWarOverlayStub( "overlay-full-3" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay1, overlay2, overlay3},
                                                new String[]{"org/sample/company/test.jsp", "jsp/b.jsp"} );

        Overlay over1 = new DefaultOverlay( overlay3 );
        over1.setExcludes( "**/a.*,**/c.*,**/*.xml,jsp/b.jsp" );

        Overlay over2 = new DefaultOverlay( overlay1 );
        over2.setIncludes( "jsp/d/*" );
        over2.setExcludes( "jsp/d/a.jsp" );

        Overlay over3 = new DefaultOverlay( overlay3 );
        over3.setIncludes( "**/*.jsp" );
        over3.setExcludes( "jsp/b.jsp" );

        Overlay over4 = new DefaultOverlay( overlay2 );

        mojo.setOverlays( new LinkedList() );
        mojo.addOverlay( over1 );
        mojo.addOverlay( over2 );
        mojo.addOverlay( over3 );
        mojo.addOverlay( mojo.getCurrentProjectOverlay() );
        mojo.addOverlay( over4 );

        final List assertedFiles = new ArrayList();
        try
        {
            mojo.execute();
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory, new String[]{"jsp/a.jsp", "jsp/b.jsp",
                "jsp/c.jsp", "jsp/d/a.jsp", "jsp/d/b.jsp", "jsp/d/c.jsp", "org/sample/company/test.jsp",
                "WEB-INF/classes/a.class", "WEB-INF/classes/b.class", "WEB-INF/classes/c.class", "WEB-INF/lib/a.jar",
                "WEB-INF/lib/b.jar", "WEB-INF/lib/c.jar"}, "overlay file not found" ) );

            assertOverlayedFile( webAppDirectory, "overlay-full-3", "jsp/a.jsp" );
            assertDefaultFileContent( testId, webAppDirectory, "jsp/b.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "jsp/c.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "jsp/d/a.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "jsp/d/b.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-1", "jsp/d/c.jsp" );
            assertDefaultFileContent( testId, webAppDirectory, "org/sample/company/test.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/web.xml" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/classes/a.class" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "WEB-INF/classes/b.class" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/classes/c.class" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/lib/a.jar" );
            assertOverlayedFile( webAppDirectory, "overlay-full-3", "WEB-INF/lib/b.jar" );
            assertOverlayedFile( webAppDirectory, "overlay-full-2", "WEB-INF/lib/c.jar" );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{MANIFEST_PATH} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );
        }
        finally
        {
            cleanDirectory( webAppDirectory );
        }

    }

    public void testCacheWithUpdatedOverlay()
        throws Exception
    {
        // setup test data
        final String testId = "cache-updated-overlay";

        // Add an overlay
        final ArtifactStub overlay = buildWarOverlayStub( "overlay-one" );
        final ArtifactStub overlay2 = buildWarOverlayStub( "overlay-two" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay, overlay2} );
        final List assertedFiles = new ArrayList();
        try
        {
            // Use the cache
            setVariableValueToObject( mojo, "useCache", Boolean.TRUE );
            setVariableValueToObject( mojo, "cacheFile", new File( mojo.getWorkDirectory(), "cache.xml" ) );

            final LinkedList overlays = new LinkedList();
            overlays.add( new DefaultOverlay( overlay ) );
            overlays.add( new DefaultOverlay( overlay2 ) );
            mojo.setOverlays( overlays );

            mojo.execute();

            // Now change the overlay order and make sure the right file is overwritten
            final LinkedList updatedOverlays = new LinkedList();
            updatedOverlays.add( new DefaultOverlay( overlay2 ) );
            updatedOverlays.add( new DefaultOverlay( overlay ) );
            mojo.setOverlays( updatedOverlays );

            mojo.execute();

            assertedFiles.addAll( assertDefaultContent( webAppDirectory ) );
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory,
                                                       new String[]{"index.jsp", "login.jsp", "admin.jsp"},
                                                       "overlay file not found" ) );

            // index and login come from overlay2 now
            assertOverlayedFile( webAppDirectory, "overlay-two", "index.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-one", "login.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-two", "admin.jsp" );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{MANIFEST_PATH} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );
        }
        finally
        {
            cleanDirectory( webAppDirectory );
        }
    }

    public void testCacheWithRemovedOverlay()
        throws Exception
    {
        // setup test data
        final String testId = "cache-removed-overlay";

        // Add an overlay
        final ArtifactStub overlay = buildWarOverlayStub( "overlay-one" );
        final ArtifactStub overlay2 = buildWarOverlayStub( "overlay-two" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay, overlay2} );
        final List assertedFiles = new ArrayList();
        try
        {
            // Use the cache
            setVariableValueToObject( mojo, "useCache", Boolean.TRUE );
            setVariableValueToObject( mojo, "cacheFile", new File( mojo.getWorkDirectory(), "cache.xml" ) );

            final LinkedList overlays = new LinkedList();
            overlays.add( new DefaultOverlay( overlay ) );
            overlays.add( new DefaultOverlay( overlay2 ) );
            mojo.setOverlays( overlays );

            mojo.execute();

            // Now remove overlay one the right file is overwritten
            final LinkedList updatedOverlays = new LinkedList();
            updatedOverlays.add( new DefaultOverlay( overlay2 ) );
            mojo.setOverlays( updatedOverlays );

            // Remove overlay one as a dep
            mojo.getProject().getArtifacts().remove( overlay );

            mojo.execute();

            assertedFiles.addAll( assertDefaultContent( webAppDirectory ) );
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory,
                                                       new String[]{"index.jsp", "login.jsp", "admin.jsp"},
                                                       "overlay file not found" ) );

            // index and login come from overlay2 now
            assertOverlayedFile( webAppDirectory, "overlay-two", "index.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-one", "login.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-two", "admin.jsp" );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{MANIFEST_PATH} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );
        }
        finally
        {
            cleanDirectory( webAppDirectory );
        }
    }

    // Helpers


    /**
     * Asserts that the content of an overlayed file is correct.
     * <p/>
     * Note that the <tt>filePath</tt> is relative to both the webapp
     * directory and the overlayed directory, defined by the <tt>overlayId</tt>.
     *
     * @param webAppDirectory the webapp directory
     * @param overlayId       the id of the overlay
     * @param filePath        the relative path
     * @throws IOException if an error occurred while reading the files
     */
    protected void assertOverlayedFile( File webAppDirectory, String overlayId, String filePath )
        throws IOException
    {
        final File webAppFile = new File( webAppDirectory, filePath );
        final File overlayFile = getOverlayFile( overlayId, filePath );
        assertEquals( "Wrong content for overlayed file " + filePath, FileUtils.fileRead( overlayFile ),
                      FileUtils.fileRead( webAppFile ) );

    }


    /**
     * Asserts that the content of an overlayed file is correct.
     * <p/>
     * Note that the <tt>filePath</tt> is relative to both the webapp
     * directory and the overlayed directory, defined by the <tt>overlayId</tt>.
     *
     * @param testId          te id of the test
     * @param webAppDirectory the webapp directory
     * @param filePath        the relative path
     * @throws IOException if an error occurred while reading the files
     */
    protected void assertDefaultFileContent( String testId, File webAppDirectory, String filePath )
        throws Exception
    {
        final File webAppFile = new File( webAppDirectory, filePath );
        final File sourceFile = new File( getWebAppSource( testId ), filePath );
        final String expectedContent = sourceFile.toString();
        assertEquals( "Wrong content for file " + filePath, expectedContent, FileUtils.fileRead( webAppFile ) );

    }

    protected ArtifactStub generateSimpleWarArtifactStub( String id )
        throws Exception
    {
        return buildWarOverlayStub( id );
    }
}