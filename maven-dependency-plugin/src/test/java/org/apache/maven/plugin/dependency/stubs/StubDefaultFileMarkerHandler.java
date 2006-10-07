package org.apache.maven.plugin.dependency.stubs;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;

public class StubDefaultFileMarkerHandler
extends DefaultFileMarkerHandler
{

    public StubDefaultFileMarkerHandler( Artifact artifact, File markerFilesDirectory )
    { 
        super(artifact,markerFilesDirectory);
        // TODO Auto-generated constructor stub
    }
    
    protected File getMarkerFile()
    {
        return new StubMarkerFile( this.markerFilesDirectory, this.artifact.getId().replace( ':', '-' ) + ".marker" );
    }}
