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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

public class ScopeFilter
    implements ArtifactsFilter
{

    private String includeScope;

    private String excludeScope;

    public ScopeFilter( String includeScope, String excludeScope )
    {
        this.includeScope = includeScope;
        this.excludeScope = excludeScope;
    }

    /**
     * This function determines if filtering needs to be performed. Excludes are
     * ignored if Includes are used.
     * 
     * @param dependencies
     *            the set of dependencies to filter.
     * 
     * @return a Set of filtered dependencies.
     * @throws MojoExecutionException
     */
    public Set filter( Set artifacts, Log log )
        throws MojoExecutionException
    {
        Set results = artifacts;

        if ( StringUtils.isNotEmpty( includeScope ) )
        {
            if ( !Artifact.SCOPE_COMPILE.equals( includeScope ) && !Artifact.SCOPE_TEST.equals( includeScope )
                && !Artifact.SCOPE_PROVIDED.equals( includeScope ) && !Artifact.SCOPE_RUNTIME.equals( includeScope )
                && !Artifact.SCOPE_SYSTEM.equals( includeScope ) )
            {
                throw new MojoExecutionException( "Invalid Scope in includeScope: " + includeScope );
            }

            results = new HashSet();

            if ( Artifact.SCOPE_PROVIDED.equals( includeScope ) || Artifact.SCOPE_SYSTEM.equals( includeScope ) )
            {
                results = includeSingleScope( artifacts, includeScope );
            }
            else
            {
                ScopeArtifactFilter saf = new ScopeArtifactFilter( includeScope );

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
        }
        else if ( StringUtils.isNotEmpty( excludeScope ) )
        {
            if ( !Artifact.SCOPE_COMPILE.equals( excludeScope ) && !Artifact.SCOPE_TEST.equals( excludeScope )
                && !Artifact.SCOPE_PROVIDED.equals( excludeScope ) && !Artifact.SCOPE_RUNTIME.equals( excludeScope )
                && !Artifact.SCOPE_SYSTEM.equals( excludeScope ) )
            {
                throw new MojoExecutionException( "Invalid Scope in excludeScope: " + excludeScope );
            }
            results = new HashSet();
            // plexus ScopeArtifactFilter doesn't handle the provided scope so
            // we
            // need special handling for it.
            if ( Artifact.SCOPE_TEST.equals( excludeScope ) )
            {
                throw new MojoExecutionException( " Can't exclude Test scope, this will exclude everything." );
            }
            else if ( !Artifact.SCOPE_PROVIDED.equals( excludeScope ) && !Artifact.SCOPE_SYSTEM.equals( excludeScope ) )
            {
                ScopeArtifactFilter saf = new ScopeArtifactFilter( excludeScope );

                Iterator iter = artifacts.iterator();
                while ( iter.hasNext() )
                {
                    Artifact artifact = (Artifact) iter.next();
                    if ( !saf.include( artifact ) )
                    {
                        results.add( artifact );
                    }
                }
            }
            else
            {
                excludeSingleScope( artifacts, excludeScope );
            }
        }

        return results;
    }

    private Set includeSingleScope( Set artifacts, String scope )
    {
        HashSet results = new HashSet();
        Iterator iter = artifacts.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( scope.equals( artifact.getScope() ) )
            {
                results.add( artifact );
            }
        }
        return results;
    }

    private Set excludeSingleScope( Set artifacts, String scope )
    {
        HashSet results = new HashSet();
        Iterator iter = artifacts.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( !scope.equals( artifact.getScope() ) )
            {
                results.add( artifact );
            }
        }
        return results;
    }

    /**
     * @return Returns the includeScope.
     */
    public String getIncludeScope()
    {
        return this.includeScope;
    }

    /**
     * @param includeScope
     *            The includeScope to set.
     */
    public void setIncludeScope( String scope )
    {
        this.includeScope = scope;
    }

    /**
     * @return Returns the excludeScope.
     */
    public String getExcludeScope()
    {
        return this.excludeScope;
    }

    /**
     * @param excludeScope
     *            The excludeScope to set.
     */
    public void setExcludeScope( String excludeScope )
    {
        this.excludeScope = excludeScope;
    }

}
