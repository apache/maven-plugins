package org.apache.maven.plugins.war;

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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugins.war.AbstractWarMojo;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.WarOverlayStub;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.aether.RepositorySystemSession;

public abstract class AbstractWarMojoTest
    extends AbstractMojoTestCase
{

    protected static final File OVERLAYS_TEMP_DIR = new File( getBasedir(), "target/test-overlays/" );

    protected static final File OVERLAYS_ROOT_DIR = new File( getBasedir(), "target/test-classes/overlays/" );

    protected static final String MANIFEST_PATH = "META-INF" + File.separator + "MANIFEST.MF";

    protected abstract File getTestDirectory()
        throws Exception;

    /**
     * initialize required parameters
     *
     * @param mojo The mojo to be tested.
     * @param filters The list of filters.
     * @param classesDir The classes directory.
     * @param webAppSource The webAppSource.
     * @param webAppDir The webAppDir folder.
     * @param project The Maven project.
     * @throws Exception in case of errors
     */
    protected void configureMojo( AbstractWarMojo mojo, List<String> filters, File classesDir, File webAppSource,
                                  File webAppDir, MavenProjectBasicStub project )
                                      throws Exception
    {
        setVariableValueToObject( mojo, "filters", filters );
        setVariableValueToObject( mojo, "useCache", Boolean.FALSE );
        setVariableValueToObject( mojo, "mavenFileFilter", lookup( MavenFileFilter.class.getName() ) );
        setVariableValueToObject( mojo, "useJvmChmod", Boolean.TRUE );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setSystemProperties( System.getProperties() );

        MavenSession mavenSession =
            new MavenSession( (PlexusContainer) null, (RepositorySystemSession) null, request, null );
        setVariableValueToObject( mojo, "session", mavenSession );
        mojo.setClassesDirectory( classesDir );
        mojo.setWarSourceDirectory( webAppSource );
        mojo.setWebappDirectory( webAppDir );
        mojo.setProject( project );
    }

    /**
     * create an isolated xml dir
     *
     * @param id The id.
     * @param xmlFiles array of xml files.
     * @return The created file.
     * @throws Exception in case of errors.
     */
    protected File createXMLConfigDir( String id, String[] xmlFiles )
        throws Exception
    {
        File xmlConfigDir = new File( getTestDirectory(), "/" + id + "-test-data/xml-config" );
        File XMLFile;

        createDir( xmlConfigDir );

        if ( xmlFiles != null )
        {
            for ( String o : xmlFiles )
            {
                XMLFile = new File( xmlConfigDir, o );
                createFile( XMLFile );
            }
        }

        return xmlConfigDir;
    }

    /**
     * Returns the webapp source directory for the specified id.
     *
     * @param id the id of the test
     * @return the source directory for that test
     * @throws Exception if an exception occurs
     */
    protected File getWebAppSource( String id )
        throws Exception
    {
        return new File( getTestDirectory(), "/" + id + "-test-data/source" );
    }

    /**
     * create an isolated web source with a sample jsp file
     *
     * @param id The id.
     * @param createSamples Create example files yes or no.
     * @return The created file.
     * @throws Exception in case of errors.
     */
    protected File createWebAppSource( String id, boolean createSamples )
        throws Exception
    {
        File webAppSource = getWebAppSource( id );
        if ( createSamples )
        {
            File simpleJSP = new File( webAppSource, "pansit.jsp" );
            File jspFile = new File( webAppSource, "org/web/app/last-exile.jsp" );

            createFile( simpleJSP );
            createFile( jspFile );
        }
        return webAppSource;
    }

    protected File createWebAppSource( String id )
        throws Exception
    {
        return createWebAppSource( id, true );
    }

    /**
     * create a class directory with or without a sample class
     *
     * @param id The id.
     * @param empty true to create a class files false otherwise.
     * @return The created class file.
     * @throws Exception in case of errors.
     */
    protected File createClassesDir( String id, boolean empty )
        throws Exception
    {
        File classesDir = new File( getTestDirectory() + "/" + id + "-test-data/classes/" );

        createDir( classesDir );

        if ( !empty )
        {
            createFile( new File( classesDir + "/sample-servlet.class" ) );
        }

        return classesDir;
    }

    protected void createDir( File dir )
    {
        if ( !dir.exists() )
        {
            assertTrue( "can not create test dir: " + dir.toString(), dir.mkdirs() );
        }
    }

    protected void createFile( File testFile, String body )
        throws Exception
    {
        createDir( testFile.getParentFile() );
        FileUtils.fileWrite( testFile.toString(), body );

        assertTrue( "could not create file: " + testFile, testFile.exists() );
    }

    protected void createFile( File testFile )
        throws Exception
    {
        createFile( testFile, testFile.toString() );
    }

    /**
     * Generates test war.
     * Generates war with such a structure:
     * <ul>
     * <li>jsp
     * <ul>
     * <li>d
     * <ul>
     * <li>a.jsp</li>
     * <li>b.jsp</li>
     * <li>c.jsp</li>
     * </ul>
     * </li>
     * <li>a.jsp</li>
     * <li>b.jsp</li>
     * <li>c.jsp</li>
     * </ul>
     * </li>
     * <li>WEB-INF
     * <ul>
     * <li>classes
     * <ul>
     * <li>a.class</li>
     * <li>b.class</li>
     * <li>c.class</li>
     * </ul>
     * </li>
     * <li>lib
     * <ul>
     * <li>a.jar</li>
     * <li>b.jar</li>
     * <li>c.jar</li>
     * </ul>
     * </li>
     * <li>web.xml</li>
     * </ul>
     * </li>
     * </ul>
     * Each of the files will contain: id+'-'+path
     *
     * @param id the id of the overlay containing the full structure
     * @return the war file
     * @throws Exception if an error occurs
     */
    protected File generateFullOverlayWar( String id )
        throws Exception
    {
        final File destFile = new File( OVERLAYS_TEMP_DIR, id + ".war" );
        if ( destFile.exists() )
        {
            return destFile;
        }

        // Archive was not yet created for that id so let's create it
        final File rootDir = new File( OVERLAYS_ROOT_DIR, id );
        rootDir.mkdirs();
        String[] filePaths = new String[] { "jsp/d/a.jsp", "jsp/d/b.jsp", "jsp/d/c.jsp", "jsp/a.jsp", "jsp/b.jsp",
            "jsp/c.jsp", "WEB-INF/classes/a.class", "WEB-INF/classes/b.class", "WEB-INF/classes/c.class",
            "WEB-INF/lib/a.jar", "WEB-INF/lib/b.jar", "WEB-INF/lib/c.jar", "WEB-INF/web.xml" };

        for ( String filePath : filePaths )
        {
            createFile( new File( rootDir, filePath ), id + "-" + filePath );
        }

        createArchive( rootDir, destFile );
        return destFile;
    }

    // Overlay utilities

    /**
     * Builds a test overlay.
     *
     * @param id the id of the overlay (see test/resources/overlays)
     * @return a test war artifact with the content of the given test overlay
     */
    protected ArtifactStub buildWarOverlayStub( String id )
    {
        // Create war file
        final File destFile = new File( OVERLAYS_TEMP_DIR, id + ".war" );
        if ( !destFile.exists() )
        {
            createArchive( new File( OVERLAYS_ROOT_DIR, id ), destFile );
        }

        return new WarOverlayStub( getBasedir(), id, destFile );
    }

    protected File getOverlayFile( String id, String filePath )
    {
        final File overlayDir = new File( OVERLAYS_ROOT_DIR, id );
        final File file = new File( overlayDir, filePath );

        // Make sure the file exists
        assertTrue( "Overlay file " + filePath + " does not exist for overlay " + id + " at " + file.getAbsolutePath(),
                    file.exists() );
        return file;

    }

    protected void createArchive( final File directory, final File destinationFile )
    {
        try
        {
            // WarArchiver archiver = new WarArchiver();

            Archiver archiver = new JarArchiver();

            archiver.setUseJvmChmod( true );

            archiver.setDestFile( destinationFile );
            archiver.addDirectory( directory );

            // archiver.setWebxml( new File(directory, "WEB-INF/web.xml"));

            // create archive
            archiver.createArchive();

        }
        catch ( ArchiverException e )
        {
            e.printStackTrace();
            fail( "Failed to create overlay archive " + e.getMessage() );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail( "Unexpected exception " + e.getMessage() );
        }
    }

}