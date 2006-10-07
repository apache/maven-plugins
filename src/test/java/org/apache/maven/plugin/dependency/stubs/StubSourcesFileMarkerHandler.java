package org.apache.maven.plugin.dependency.stubs;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.dependency.utils.markers.SourcesFileMarkerHandler;


public class StubSourcesFileMarkerHandler
extends SourcesFileMarkerHandler
{

    public StubSourcesFileMarkerHandler( Artifact artifact, File markerFilesDirectory, boolean resolved )
    {
        super( artifact, markerFilesDirectory, resolved );
        // TODO Auto-generated constructor stub
    }

    protected File getMarkerFile( boolean res )
    {
        String suffix;
        if ( res )
        {
            suffix = ".resolved";
        }
        else
        {
            suffix = ".unresolved";
        }

        return new StubMarkerFile( this.markerFilesDirectory, this.artifact.getId().replace( ':', '-' ) + suffix );
    }



}
