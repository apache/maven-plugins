package org.apache.maven.plugin.dependency.utils.markers;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.plugin.dependency.testUtils.DependencyArtifactStubFactory;
import org.apache.maven.plugin.dependency.testUtils.DependencyTestUtils;
import org.apache.maven.plugin.dependency.testUtils.stubs.StubUnpackFileMarkerHandler;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.SilentLog;

public class TestUnpackMarkerFileHandler
	extends AbstractMojoTestCase
{
	List<ArtifactItem> artifactItems = new ArrayList<ArtifactItem>();

    Log log = new SilentLog();

    File outputFolder;
    
    protected File testDir;
    
    protected DependencyArtifactStubFactory stubFactory;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        testDir = new File( getBasedir(), "target" + File.separatorChar + "unit-tests" + File.separatorChar
                + "unpack-markers" + File.separatorChar );
        DependencyTestUtils.removeDirectory( testDir );
        assertFalse( testDir.exists() );

        stubFactory = new DependencyArtifactStubFactory( this.testDir, false );
        Artifact artifact = stubFactory.createArtifact( "test", "test", "1" );
        ArtifactItem artifactItem = stubFactory.getArtifactItem( artifact );
        artifactItems.add( stubFactory.getArtifactItem( stubFactory.createArtifact( "test", "test", "1" ) ) );
        artifact = stubFactory.createArtifact( "test2", "test2", "2" );
        artifactItem = new ArtifactItem( artifact );
        artifactItem.setIncludes( "**/*.xml" );
        artifactItems.add( artifactItem );
        artifact = stubFactory.createArtifact( "test3", "test3", "3" );
        artifactItem = new ArtifactItem( artifact );
        artifactItem.setExcludes( "**/*.class" );
        artifactItems.add( artifactItem );
        artifact = stubFactory.createArtifact( "test4", "test4", "4" );
        artifactItem = new ArtifactItem( artifact );
        artifactItem.setIncludes( "**/*.xml" );
        artifactItem.setExcludes( "**/*.class" );
        artifactItems.add( artifactItem );

        outputFolder = new File( "target/markers/" );
        DependencyTestUtils.removeDirectory( this.outputFolder );
        assertFalse( outputFolder.exists() );
    }

    protected void tearDown()
        throws IOException
    {
        DependencyTestUtils.removeDirectory( this.outputFolder );
    }

    /**
     * 
     * Assert that default functionallity still exists
     * 
     */
    
    public void testSetMarker()
        throws MojoExecutionException
    {
        UnpackFileMarkerHandler handler = new UnpackFileMarkerHandler( artifactItems.get( 0 ),
                                                                         this.outputFolder );
        assertFalse( handler.isMarkerSet() );
        handler.setMarker();
        assertTrue( handler.isMarkerSet() );
        handler.clearMarker();
        assertFalse( handler.isMarkerSet() );

        handler.setMarker();
        assertTrue( handler.isMarkerSet() );
        handler.setMarker();
        assertTrue( handler.isMarkerSet() );

        handler.clearMarker();
        assertFalse( handler.isMarkerSet() );
        handler.clearMarker();
        assertFalse( handler.isMarkerSet() );
    }

    public void testMarkerFile()
        throws MojoExecutionException, IOException
    {
    	UnpackFileMarkerHandler handler = new UnpackFileMarkerHandler( artifactItems.get( 0 ),
                                                                         this.outputFolder );

        File handle = handler.getMarkerFile();
        assertFalse( handle.exists() );
        assertFalse( handler.isMarkerSet() );

        handler.setMarker();
        assertTrue( handler.isMarkerSet() );
        assertTrue( handle.exists() );

        handle.delete();
        assertFalse( handler.isMarkerSet() );

        handle.createNewFile();
        assertTrue( handler.isMarkerSet() );

        handler.clearMarker();
        assertFalse( handle.exists() );
    }

    public void testMarkerTimeStamp()
        throws MojoExecutionException, IOException, InterruptedException
    {
        File theFile = new File( outputFolder, "theFile.jar" );
        outputFolder.mkdirs();
        theFile.createNewFile();
        ArtifactItem theArtifactItem = artifactItems.get( 0 );
        Artifact theArtifact = theArtifactItem.getArtifact();
        theArtifact.setFile( theFile );
        UnpackFileMarkerHandler handler = new UnpackFileMarkerHandler( theArtifactItem, this.outputFolder );
        assertFalse( handler.isMarkerSet() );
        // if the marker is not set, assume it is infinately older than the
        // artifact.
        assertTrue( handler.isMarkerOlder( theArtifact ) );
        handler.setMarker();
        assertFalse( handler.isMarkerOlder( theArtifact ) );

        theFile.setLastModified( theFile.lastModified() + 60000 );
        assertTrue( handler.isMarkerOlder( theArtifact ) );

        theFile.delete();
        handler.clearMarker();
        assertFalse( handler.isMarkerSet() );
    }

    public void testMarkerFileException()
    {
        // this stub wraps the file with an object to throw exceptions
        StubUnpackFileMarkerHandler handler = new StubUnpackFileMarkerHandler( artifactItems.get( 0 ),
                                                                                 this.outputFolder );
        try
        {
            handler.setMarker();
            fail( "Expected an Exception here" );
        }
        catch ( MojoExecutionException e )
        {

        }
    }

    public void testGetterSetter()
    {
        UnpackFileMarkerHandler handler = new UnpackFileMarkerHandler( null, null );
        assertTrue( handler.getArtifactItem() == null );
        assertTrue( handler.getArtifact() == null );
        handler.setArtifactItem( artifactItems.get( 0 ) );
        assertSame( artifactItems.get( 0 ), handler.getArtifactItem() );
        assertSame( artifactItems.get( 0 ).getArtifact(), handler.getArtifact() );

        assertTrue( handler.getMarkerFilesDirectory() == null );
        handler.setMarkerFilesDirectory( outputFolder );
        assertSame( outputFolder, handler.getMarkerFilesDirectory() );
    }

    public void testNullParent()
        throws MojoExecutionException
    {
        // the parent isn't set so this will create the marker in the local
        // folder. We must clear the
        // marker to avoid leaving test droppings in root.
    	UnpackFileMarkerHandler handler = new UnpackFileMarkerHandler( null, null );
        handler.setArtifactItem( artifactItems.get( 0 ) );
        handler.setMarker();
        assertTrue( handler.isMarkerSet() );
        handler.clearMarker();
        assertFalse( handler.isMarkerSet() );
    }
    
    public void testIncludesMarker()
    	throws MojoExecutionException, IOException
	{
    	UnpackFileMarkerHandler handler = new UnpackFileMarkerHandler( artifactItems.get( 1 ), outputFolder );
    	File handle = handler.getMarkerFile();
        assertFalse( handle.exists() );
        assertFalse( handler.isMarkerSet() );

        handler.setMarker();
        assertTrue( handler.isMarkerSet() );
        assertTrue( handle.exists() );
        String hashCode = "" + ( 0 + "**/*.xml".hashCode() );
        assertTrue( handle.getName().indexOf( hashCode ) > -1 );

        handle.delete();
        assertFalse( handler.isMarkerSet() );

        handle.createNewFile();
        assertTrue( handler.isMarkerSet() );

        handler.clearMarker();
        assertFalse( handle.exists() );
	}
    
    public void testExcludesMarker()
		throws MojoExecutionException, IOException
	{
		UnpackFileMarkerHandler handler = new UnpackFileMarkerHandler( artifactItems.get( 2 ), outputFolder );
		File handle = handler.getMarkerFile();
	    assertFalse( handle.exists() );
	    assertFalse( handler.isMarkerSet() );
	
	    handler.setMarker();
	    assertTrue( handler.isMarkerSet() );
	    assertTrue( handle.exists() );
	    String hashCode = "" + ( 0 + "**/*.class".hashCode() );
	    assertTrue( handle.getName().indexOf( hashCode ) > -1 );
	
	    handle.delete();
	    assertFalse( handler.isMarkerSet() );
	
	    handle.createNewFile();
	    assertTrue( handler.isMarkerSet() );
	
	    handler.clearMarker();
	    assertFalse( handle.exists() );
	}
    
    public void testIncludesExcludesMarker()
		throws MojoExecutionException, IOException
	{
		UnpackFileMarkerHandler handler = new UnpackFileMarkerHandler( artifactItems.get( 3 ), outputFolder );
		File handle = handler.getMarkerFile();
	    assertFalse( handle.exists() );
	    assertFalse( handler.isMarkerSet() );
	
	    handler.setMarker();
	    assertTrue( handler.isMarkerSet() );
	    assertTrue( handle.exists() );
	    String hashCode = "" + ( 0 + "**/*.class".hashCode() + "**/*.xml".hashCode() );
	    assertTrue( handle.getName().indexOf( hashCode ) > -1 );
	
	    handle.delete();
	    assertFalse( handler.isMarkerSet() );
	
	    handle.createNewFile();
	    assertTrue( handler.isMarkerSet() );
	
	    handler.clearMarker();
	    assertFalse( handle.exists() );
	}
}


