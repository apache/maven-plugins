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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;

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
    
    Artifact snap;
    Artifact release;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactHandler ah = new DefaultArtifactHandler();
        VersionRange vr = VersionRange.createFromVersion( "1.1" );
        this.release = new DefaultArtifact( "test", "1", vr, Artifact.SCOPE_COMPILE, "jar", "", ah, false );
        artifacts.add( release );
        
        vr = VersionRange.createFromVersion( "1.1-SNAPSHOT" );
        snap = new DefaultArtifact( "test", "2", vr, Artifact.SCOPE_PROVIDED, "war", "", ah, false );
        artifacts.add( snap );
        
        //pick random output location
        Random a = new Random();
        outputFolder = new File("target/markers"+a.nextLong()+"/");
        outputFolder.delete();
        assertFalse(outputFolder.exists());
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
        DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler(snap,outputFolder);
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
        DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler(release,outputFolder);
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
        snap.setFile(new File(outputFolder,"snap.file"));
        release.setFile(new File(outputFolder,"release.file"));
        outputFolder.mkdirs();
        snap.getFile().createNewFile();
        release.getFile().createNewFile();
        snap.getFile().setLastModified(snap.getFile().lastModified()+222);
        DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler(snap,outputFolder);
        handler.setMarker();
        MarkerFileFilter filter = new MarkerFileFilter(false,false,true,outputFolder);
        Set result = filter.filter(artifacts,log);
        assertEquals(2,result.size());
        
        snap.getFile().setLastModified(snap.getFile().lastModified()-10000);
        
        result = filter.filter(artifacts,log);
        assertEquals(1,result.size());
        
        assertTrue(handler.clearMarker());
        assertFalse(handler.isMarkerSet());
        snap.getFile().delete();
        release.getFile().delete();
        outputFolder.delete();
        assertFalse(outputFolder.exists());
        
    }
    
    
}
