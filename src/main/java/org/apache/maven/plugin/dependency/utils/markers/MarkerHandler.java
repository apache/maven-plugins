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
package org.apache.maven.plugin.dependency.utils.markers;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author brianf
 * 
 */
public interface MarkerHandler
{
    public boolean isMarkerSet()
        throws MojoExecutionException;

    public void setMarker()
        throws MojoExecutionException;

    public boolean clearMarker()
        throws MojoExecutionException;

    public boolean isMarkerOlder( Artifact artifact )
        throws MojoExecutionException;
}
