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
package org.apache.maven.plugin.dependency.utils.filters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

public class ScopeFilter
    implements ArtifactsFilter
{

    private String scope;

    public ScopeFilter( String scope )
    {
        this.scope = scope;
    }

    public Set filter( Set artifacts, Log log )
    {
        Set results = artifacts;

        if ( StringUtils.isNotEmpty( scope ) )
        {
            results = new HashSet();
            ScopeArtifactFilter saf = new ScopeArtifactFilter( scope );

            Iterator iter = artifacts.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( saf.include( artifact ) )
                {
                    results.add( artifact );
                }
            }
        }
        return results;
    }

    /**
     * @return Returns the scope.
     */
    public String getScope()
    {
        return this.scope;
    }

    /**
     * @param scope The scope to set.
     */
    public void setScope( String scope )
    {
        this.scope = scope;
    }

}
