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

package org.apache.maven.plugin.dependency.resolvers;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractResolveMojo;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;

/**
 * Goal that resolves the project dependencies from the repository.
 *
 * @goal resolve
 * @requiresDependencyResolution test
 * @phase generate-sources
 * @author brianf
 */
public class ResolveDependenciesMojo
    extends AbstractResolveMojo
{
    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through displaying the resolved version.
     * 
     * @throws MojoExecutionException 
     *          with a message if an error occurs. 
     *
     */
    public void execute()
        throws MojoExecutionException
    {
        //get sets of dependencies
        DependencyStatusSets status = this.getDependencySets();
        
        status.logStatus(log, outputArtifactFilename);
    }
}
