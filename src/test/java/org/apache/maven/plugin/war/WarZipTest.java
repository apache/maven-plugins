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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.war.overlay.DefaultOverlay;
import org.apache.maven.plugin.war.stub.MavenZipProject;
import org.apache.maven.plugin.war.stub.WarArtifactStub;
import org.apache.maven.plugin.war.stub.ZipArtifactStub;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.LinkedList;

/**
 * @author Olivier Lamy
 * @version $Id$
 * @since 7 Oct 07
 */
public class WarZipTest
    extends AbstractWarMojoTest
{
    WarMojo mojo;

    private static File pomFile = new File( getBasedir(), "src/test/resources/unit/warziptest/war-with-zip.xml" );


    protected File getTestDirectory()
    {
        return new File( getBasedir(), "target/test-classes/unit/warziptest" );
    }

    public void setUp()
        throws Exception
    {
        super.setUp();
        mojo = (WarMojo) lookupMojo( "war", pomFile );
    }

    private Artifact buildZipArtifact()
        throws Exception
    {
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        File zipFile = new File( getTestDirectory(), "foobar.zip" );
        return new ZipArtifactStub( "src/test/resources/unit/warziptest", artifactHandler, zipFile );
    }

    private File configureMojo( String testId )
        throws Exception
    {
        MavenZipProject project = new MavenZipProject();
        String outputDir = getTestDirectory().getAbsolutePath() + File.separatorChar + testId + "-output";
        // clean up
        File outputDirFile = new File( outputDir );
        if ( outputDirFile.exists() )
        {
            FileUtils.deleteDirectory( outputDirFile );
        }
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifactStub warArtifact = new WarArtifactStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[]{"web.xml"} );
        project.setArtifact( warArtifact );

        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        setVariableValueToObject( mojo, "workDirectory", new File( getTestDirectory(), "work" ) );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        project.getArtifacts().add( buildZipArtifact() );

        return webAppDirectory;
    }

    public void testOneZipWithNoSkip()
        throws Exception
    {
        File webAppDirectory = configureMojo( "one-zip" );

        Overlay overlay = new DefaultOverlay( buildZipArtifact() );
        //overlay.setSkip( false );
        overlay.setType( "zip" );
        mojo.addOverlay( overlay );
        mojo.execute();

        File foo = new File( webAppDirectory, "foo.txt" );
        assertTrue( "foo.txt not exists", foo.exists() );
        assertTrue( "foo.txt not a file", foo.isFile() );

        File barDirectory = new File( webAppDirectory, "bar" );
        assertTrue( "bar directory not exists", barDirectory.exists() );
        assertTrue( "bar not a directory", barDirectory.isDirectory() );

        File bar = new File( barDirectory, "bar.txt" );
        assertTrue( "bar/bar.txt not exists", bar.exists() );
        assertTrue( "bar/bar.txt not a file", bar.isFile() );
    }

    public void testOneZipWithTargetPathOverlay()
        throws Exception
    {
        File webAppDirectory = configureMojo( "one-zip-overlay-targetPath" );

        Overlay overlay = new DefaultOverlay( buildZipArtifact() );
        overlay.setSkip( false );
        overlay.setType( "zip" );
        overlay.setTargetPath( "overridePath" );
        mojo.addOverlay( overlay );

        mojo.execute();

        File foo = new File( webAppDirectory.getPath() + File.separatorChar + "overridePath", "foo.txt" );
        assertTrue( "foo.txt not exists", foo.exists() );
        assertTrue( "foo.txt not a file", foo.isFile() );

        File barDirectory = new File( webAppDirectory.getPath() + File.separatorChar + "overridePath", "bar" );
        assertTrue( "bar directory not exists", barDirectory.exists() );
        assertTrue( "bar not a directory", barDirectory.isDirectory() );

        File bar = new File( barDirectory, "bar.txt" );
        assertTrue( "bar/bar.txt not exists", bar.exists() );
        assertTrue( "bar/bar.txt not a file", bar.isFile() );
    }

    public void testOneZipDefaultSkip()
        throws Exception
    {
        File webAppDirectory = configureMojo( "one-zip-overlay-skip" );

        mojo.execute();

        assertZipContentNotHere( webAppDirectory );
    }

    public void testOneZipWithForceSkip()
        throws Exception
    {
        File webAppDirectory = configureMojo( "one-zip-overlay-skip" );
        Overlay overlay = new DefaultOverlay( buildZipArtifact() );
        overlay.setSkip( true );
        overlay.setType( "zip" );
        mojo.addOverlay( overlay );

        mojo.execute();
        assertZipContentNotHere( webAppDirectory );

    }

    protected void assertZipContentNotHere( File webAppDirectory )
    {
        File foo = new File( webAppDirectory.getPath() + File.separatorChar + "overridePath", "foo.txt" );
        assertFalse( "foo.txt exists", foo.exists() );
        assertFalse( "foo.txt a file", foo.isFile() );

        File barDirectory = new File( webAppDirectory.getPath() + File.separatorChar + "overridePath", "bar" );
        assertFalse( "bar directory exists", barDirectory.exists() );
        assertFalse( "bar is a directory", barDirectory.isDirectory() );

        File bar = new File( barDirectory, "bar.txt" );
        assertFalse( "bar/bar.txt exists", bar.exists() );
        assertFalse( "bar/bar.txt is a file", bar.isFile() );
    }
}
