package org.apache.maven.plugin.dependency.testUtils.stubs;

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
