/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
/**
 * 
 */
package org.apache.maven.plugin.dependency.utils.markers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.stubs.StubSourcesFileMarkerHandler;
import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 * 
 */
public class TestSourcesMarkerFileHandler
    extends TestCase
{
    List artifacts = new ArrayList();

    Log log = new SilentLog();

    File outputFolder;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactHandler ah = new DefaultArtifactHandler();
        VersionRange vr = VersionRange.createFromVersion( "1.1" );
        Artifact artifact = new DefaultArtifact( "test", "1", vr, Artifact.SCOPE_COMPILE, "jar", "", ah, false );
        artifacts.add( artifact );
        artifact = new DefaultArtifact( "test", "2", vr, Artifact.SCOPE_PROVIDED, "war", "", ah, false );
        artifacts.add( artifact );
        artifact = new DefaultArtifact( "test", "3", vr, Artifact.SCOPE_TEST, "sources", "", ah, false );
        artifacts.add( artifact );
        artifact = new DefaultArtifact( "test", "4", vr, Artifact.SCOPE_RUNTIME, "zip", "", ah, false );
        artifacts.add( artifact );

        // pick random output location
        Random a = new Random();
        outputFolder = new File( "target/markers" + a.nextLong() + "/" );
        outputFolder.delete();
        assertFalse( outputFolder.exists() );
    }

    protected void tearDown()
    {
        outputFolder.delete();
    }

    public void testSetMarkerResolved()
        throws MojoExecutionException
    {
        DefaultFileMarkerHandler handler = new SourcesFileMarkerHandler( (Artifact) artifacts.get( 0 ),
                                                                         this.outputFolder, true );
        assertFalse( handler.isMarkerSet() );
        handler.setMarker();
        assertTrue( handler.isMarkerSet() );
        handler.clearMarker();
        assertFalse( handler.isMarkerSet() );

        handler.setMarker();
        handler.setMarker();
        assertTrue( handler.isMarkerSet() );

        handler.clearMarker();
        handler.clearMarker();
        outputFolder.delete();
        assertFalse( outputFolder.exists() );
    }

    public void testSetMarkerUnresolved()
        throws MojoExecutionException
    {
        DefaultFileMarkerHandler handler = new SourcesFileMarkerHandler( (Artifact) artifacts.get( 0 ),
                                                                         this.outputFolder, false );
        assertFalse( handler.isMarkerSet() );
        handler.setMarker();
        assertTrue( handler.isMarkerSet() );
        handler.clearMarker();
        assertFalse( handler.isMarkerSet() );

        handler.setMarker();
        handler.setMarker();
        assertTrue( handler.isMarkerSet() );

        handler.clearMarker();
        handler.clearMarker();
        outputFolder.delete();
        assertFalse( outputFolder.exists() );
    }

    public void testBothMarkers()
        throws MojoExecutionException
    {
        DefaultFileMarkerHandler handler = new SourcesFileMarkerHandler( (Artifact) artifacts.get( 1 ),
                                                                         this.outputFolder, true );
        DefaultFileMarkerHandler handler2 = new SourcesFileMarkerHandler( (Artifact) artifacts.get( 1 ),
                                                                          this.outputFolder, false );

        handler.setMarker();
        assertTrue( handler.isMarkerSet() );
        assertTrue( handler2.isMarkerSet() );

        handler2.clearMarker();
        assertFalse( handler.isMarkerSet() );
        assertFalse( handler2.isMarkerSet() );
        outputFolder.delete();
        assertFalse( outputFolder.exists() );
    }

    public void testMarkerFile()
        throws MojoExecutionException, IOException
    {
        DefaultFileMarkerHandler handler = new SourcesFileMarkerHandler( (Artifact) artifacts.get( 0 ),
                                                                         this.outputFolder, true );
        DefaultFileMarkerHandler handler2 = new SourcesFileMarkerHandler( (Artifact) artifacts.get( 0 ),
                                                                          this.outputFolder, false );

        File handle = handler.getMarkerFile();
        File handle2 = handler2.getMarkerFile();
        assertFalse( handle.exists() );
        assertFalse( handler.isMarkerSet() );
        assertFalse( handle2.exists() );
        assertFalse( handler2.isMarkerSet() );

        // if either file exists, the marker is set
        handler.setMarker();
        assertTrue( handler.isMarkerSet() );
        assertTrue( handle.exists() );
        assertTrue( handler2.isMarkerSet() );
        assertFalse( handle2.exists() );

        // if either file exists, the marker is set
        // setting 1 will clear the other marker
        handler2.setMarker();
        assertTrue( handler.isMarkerSet() );
        assertFalse( handle.exists() );
        assertTrue( handler2.isMarkerSet() );
        assertTrue( handle2.exists() );

        // reset file for next test
        handle.createNewFile();

        // only delete one, should be still true
        handle2.delete();
        assertTrue( handler.isMarkerSet() );
        assertTrue( handler2.isMarkerSet() );

        // delete the 2nd, should be false.
        handle.delete();
        assertFalse( handler.isMarkerSet() );
        assertFalse( handler2.isMarkerSet() );

        handle.createNewFile();
        assertTrue( handler.isMarkerSet() );
        assertTrue( handler2.isMarkerSet() );

        handler.clearMarker();
        assertFalse( handle.exists() );
        handler2.clearMarker();
        assertFalse( handle2.exists() );

        handle.delete();
        handle2.delete();

        outputFolder.delete();
        assertFalse( outputFolder.exists() );
    }

    public void testMarkerTimeStampResolved()
        throws MojoExecutionException, IOException, InterruptedException
    {
        doTestMarkerTimeStamp( true );
    }

    public void testMarkerTimeStampUnResolved()
        throws MojoExecutionException, IOException, InterruptedException
    {
        doTestMarkerTimeStamp( false );
    }

    public void doTestMarkerTimeStamp( boolean resolved )
        throws MojoExecutionException, IOException, InterruptedException
    {
        File theFile = new File( outputFolder, "theFile.jar" );
        outputFolder.mkdirs();
        theFile.createNewFile();
        Artifact theArtifact = (Artifact) artifacts.get( 0 );
        theArtifact.setFile( theFile );
        SourcesFileMarkerHandler resolvedHandler = new SourcesFileMarkerHandler( (Artifact) artifacts.get( 0 ),
                                                                                 this.outputFolder, resolved );
        SourcesFileMarkerHandler unResolvedHandler = new SourcesFileMarkerHandler( (Artifact) artifacts.get( 0 ),
                                                                                   this.outputFolder, !resolved );

        assertFalse( resolvedHandler.isMarkerSet() );
        assertFalse( unResolvedHandler.isMarkerSet() );
        // if the marker is not set, assume it is infinately older than the
        // artifact.
        assertTrue( resolvedHandler.isMarkerOlder( theArtifact ) );
        assertTrue( unResolvedHandler.isMarkerOlder( theArtifact ) );
        resolvedHandler.setMarker();
        assertFalse( resolvedHandler.isMarkerOlder( theArtifact ) );
        assertFalse( unResolvedHandler.isMarkerOlder( theArtifact ) );

        resolvedHandler.clearMarker();
        unResolvedHandler.setMarker();
        assertFalse( resolvedHandler.isMarkerOlder( theArtifact ) );
        assertFalse( unResolvedHandler.isMarkerOlder( theArtifact ) );

        theFile.setLastModified( theFile.lastModified() + 60000 );
        assertTrue( resolvedHandler.isMarkerOlder( theArtifact ) );
        assertTrue( unResolvedHandler.isMarkerOlder( theArtifact ) );

        theFile.delete();
        resolvedHandler.clearMarker();
        assertFalse( resolvedHandler.isMarkerSet() );
    }

    public void testMarkerFileException()
    {
        // this stub wraps the file with an object to throw exceptions
        StubSourcesFileMarkerHandler handler = new StubSourcesFileMarkerHandler( (Artifact) artifacts.get( 0 ),
                                                                                 this.outputFolder, true );
        try
        {
            handler.setMarker();
            fail( "Expected an Exception here" );
        }
        catch ( MojoExecutionException e )
        {
        }
    }
    
    public void testMarkerFileResolvedSetter()
    {
        SourcesFileMarkerHandler handler = new SourcesFileMarkerHandler(null,null,true);
        assertTrue(handler.isResolved());
        handler.setResolved(false);
        assertFalse(handler.isResolved());
    }
}
