/*
 *  Copyright 2005-2006 Brian Fox (brianefox@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package org.apache.maven.plugin.dependency.utils.filters;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 *
 */
public class TestMarkerFileFilter
    extends TestCase
{
    Set artifacts = new HashSet();

    Log log = new SilentLog();
    
    File outputFolder;
    
    ArtifactStubFactory fact;
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
 
        //pick random output location
        Random a = new Random();
        outputFolder = new File("target/markers"+a.nextLong()+"/");
        outputFolder.delete();
        assertFalse(outputFolder.exists());
        
        this.fact = new ArtifactStubFactory(outputFolder,false);
        artifacts = fact.getReleaseAndSnapshotArtifacts();
    }
    
    protected void tearDown()
    {

    }
    
    public void testMarkerFile() throws MojoExecutionException
    {
        MarkerFileFilter filter = new MarkerFileFilter(true,true,false,outputFolder);
        Set result = filter.filter(artifacts,log);
        assertEquals(2,result.size());
        
        filter.setOverWriteReleases(false);
        filter.setOverWriteSnapshots(false);
        result = filter.filter(artifacts,log);
        assertEquals(2,result.size());    
    }
    
    public void testMarkerSnapshots () throws MojoExecutionException
    {
        DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler(fact.getSnapshotArtifact(),outputFolder);
        handler.setMarker();
        
        MarkerFileFilter filter = new MarkerFileFilter(true,false,false,outputFolder);
        Set result = filter.filter(artifacts,log);
        assertEquals(1,result.size());
        
        filter.setOverWriteSnapshots(true);
        result = filter.filter(artifacts,log);
        assertEquals(2,result.size());
        assertTrue(handler.clearMarker());
        outputFolder.delete();
        assertFalse(outputFolder.exists()); 
    }
    
    public void testMarkerRelease () throws MojoExecutionException
    {
        DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler(fact.getReleaseArtifact(),outputFolder);
        handler.setMarker();
        
        MarkerFileFilter filter = new MarkerFileFilter(false,false,false,outputFolder);
        Set result = filter.filter(artifacts,log);
        assertEquals(1,result.size());
        
        filter.setOverWriteReleases(true);
        result = filter.filter(artifacts,log);
        assertEquals(2,result.size());
     
        assertTrue(handler.clearMarker());
        outputFolder.delete();
        assertFalse(outputFolder.exists()); 
    }
    
    public void testMarkerTimestamp () throws MojoExecutionException, IOException
    {
        ArtifactStubFactory fileFact = new ArtifactStubFactory(outputFolder,true);
        Artifact snap = fileFact.getSnapshotArtifact();
        Artifact release = fileFact.getReleaseArtifact();
        HashSet tempArtifacts = new HashSet();
        tempArtifacts.add(snap);
        tempArtifacts.add(release);
        snap.getFile().setLastModified(snap.getFile().lastModified()+222);
        DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler(snap,outputFolder);
        handler.setMarker();
        MarkerFileFilter filter = new MarkerFileFilter(false,false,true,outputFolder);
        Set result = filter.filter(tempArtifacts,log);
        assertEquals(2,result.size());
        
        snap.getFile().setLastModified(snap.getFile().lastModified()-10000);
        
        result = filter.filter(tempArtifacts,log);
        assertEquals(1,result.size());
        
        assertTrue(handler.clearMarker());
        assertFalse(handler.isMarkerSet());
        snap.getFile().delete();
        release.getFile().delete();
        outputFolder.delete();
        assertFalse(outputFolder.exists());    
    }
    
    
}
