package org.apache.maven.plugin.dependency.testUtils.stubs;

import java.io.File;

import org.apache.maven.plugin.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.plugin.dependency.utils.markers.UnpackFileMarkerHandler;
import org.codehaus.plexus.util.StringUtils;

public class StubUnpackFileMarkerHandler
	extends UnpackFileMarkerHandler
{
	public StubUnpackFileMarkerHandler( ArtifactItem artifactItem, File markerFilesDirectory )
    {
        super( artifactItem, markerFilesDirectory );
    }
	
	protected File getMarkerFile()
    {
		File markerFile = null;
		if ( this.artifactItem == null 
			|| ( StringUtils.isEmpty( this.artifactItem.getIncludes() )
			&&	StringUtils.isEmpty( this.artifactItem.getExcludes() ) ) )
		{
			markerFile = new StubMarkerFile( this.markerFilesDirectory, this.artifact.getId().replace( ':', '-' ) + ".marker" );
		}
		else
		{
			int includeExcludeHash = 0;
			
			if ( StringUtils.isNotEmpty( this.artifactItem.getIncludes() ) )
			{
				includeExcludeHash += this.artifactItem.getIncludes().hashCode();
			}
			
			if ( StringUtils.isNotEmpty( this.artifactItem.getExcludes() ) )
			{
				includeExcludeHash += this.artifactItem.getExcludes().hashCode();
			}
			
			markerFile = new StubMarkerFile( this.markerFilesDirectory, this.artifact.getId().replace( ':', '-' ) + includeExcludeHash );
		}
		
		return markerFile;
    }
}
