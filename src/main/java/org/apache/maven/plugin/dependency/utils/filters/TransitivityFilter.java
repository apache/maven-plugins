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


package org.apache.maven.plugin.dependency.utils.filters;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;

public class TransitivityFilter
    implements ArtifactsFilter
{

    private boolean excludeTransitive;

    private Set directDependencies;

    public TransitivityFilter( Set directDependencies, boolean excludeTransitive )
    {
        this.excludeTransitive = excludeTransitive;
        this.directDependencies = directDependencies;
    }

    public Set filter( Set artifacts, Log log )
    {
        // why not just take the directDependencies here?
        // because if this filter is run after some other process, the
        // set of artifacts may not be the same as the directDependencies.
        Set result = artifacts;

        if ( excludeTransitive )
        {
            log.debug( "Excluding Transitive Dependencies." );
            result = new HashSet();
            Iterator iterator = artifacts.iterator();
            while ( iterator.hasNext() )
            {
                Artifact artifact = (Artifact) iterator.next();
                if ( artifactIsADirectDependency( artifact ) )
                {
                    result.add( artifact );
                    log.debug( "Added: " + artifact );
                }
            }
            log.debug( "Added " + result.size() );
        }
        else
        {
            log.debug( "Including Transitive Dependencies." );
        }

        return result;
    }

    /**
     * Compares the artifact to the list of dependencies to see if it is
     * directly included by this project
     * 
     * @param artifact
     *            representing the item to compare.
     * @return true if artifact is a direct dependency
     */
    public boolean artifactIsADirectDependency( Artifact artifact )
    {
        boolean result = false;
        Iterator iterator = this.directDependencies.iterator();
        while ( iterator.hasNext() )
        {
            Artifact dependency = (Artifact) iterator.next();
            if ( dependency.getGroupId().equals( artifact.getGroupId() )
                && dependency.getArtifactId().equals( artifact.getArtifactId() ) )
            {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * @return Returns the excludeTransitive.
     */
    public boolean isExcludeTransitive()
    {
        return this.excludeTransitive;
    }

    /**
     * @param excludeTransitive
     *            The excludeTransitive to set.
     */
    public void setExcludeTransitive( boolean excludeTransitive )
    {
        this.excludeTransitive = excludeTransitive;
    }
}
