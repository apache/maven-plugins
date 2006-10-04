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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

public class TypeFilter
    implements ArtifactsFilter
{
    private List includeTypes;

    private List excludeTypes;

    private String includeString;

    private String excludeString;

    public TypeFilter( String include, String exclude )
    {
        setExcludeTypes( exclude );
        setIncludeTypes( include );
    }

    /**
     * This function determines if filtering needs to be performed. Excludes are
     * ignored if Includes are used.
     * 
     * @param dependencies
     *            the set of dependencies to filter.
     * 
     * @return a Set of filtered dependencies.
     */
    public Set filter( Set artifacts, Log log )
    {
        Set results = artifacts;

        if ( this.includeTypes != null && !this.includeTypes.isEmpty() )
        {
            log.debug( "Including only Types: " + this.includeString );
            results = filterIncludes( artifacts, this.includeTypes );
        }
        else
        {
            if ( this.excludeTypes != null && !this.excludeTypes.isEmpty() )
            {
                log.debug( "Excluding Types: " + this.excludeString );
                results = filterExcludes( artifacts, this.excludeTypes );
            }
        }
        return results;
    }

    /**
     * Processes the dependencies list and includes the dependencies that match
     * a type in the list.
     * 
     * @param depends
     *            List of dependencies.
     * @param types
     *            List of types to include.
     * 
     * @return a set of filtered types.
     */
    private Set filterIncludes( Set artifacts, List types )
    {
        Set result = new HashSet();

        Iterator typeIter = types.iterator();
        while ( typeIter.hasNext() )
        {
            String artifactType = (String) typeIter.next();
            Iterator iter = artifacts.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = (Artifact) iter.next();

                // if the type matches the type, add to the
                // results
                if ( artifact.getType().equals( artifactType ) )
                {
                    result.add( artifact );
                }
            }
        }
        return result;
    }

    /**
     * Processes the dependencies list and excludes the dependencies that match
     * a type in the list.
     * 
     * @param depends
     *            List of dependencies.
     * @param types
     *            List of types to exclude.
     * 
     * @return a set of filtered types.
     */
    private Set filterExcludes( Set artifacts, List types )
    {
        Set result = new HashSet();

        Iterator iter = artifacts.iterator();
        while ( iter.hasNext() )
        {
            boolean exclude = false;
            Artifact artifact = (Artifact) iter.next();

            // look through all types. If no matches are found
            // then it can be added to the results.
            Iterator typeIter = types.iterator();
            while ( typeIter.hasNext() )
            {
                String artifactType = (String) typeIter.next();
                if ( artifact.getType().equals( artifactType ) )
                {
                    exclude = true;
                    break;
                }
            }

            if ( !exclude )
            {
                result.add( artifact );
            }
        }

        return result;
    }

    /**
     * @param includeTypes
     *            The includeTypes to set.
     */
    public void setExcludeTypes( String excludeTypeString )
    {
        this.excludeString = excludeTypeString;

        if ( StringUtils.isNotEmpty( excludeTypeString ) )
        {
            this.excludeTypes = Arrays.asList( StringUtils.split( excludeTypeString, "," ) );
        }
    }

    /**
     * @param includeTypes
     *            The includeTypes to set.
     */
    public void setIncludeTypes( String includeTypeString )
    {
        this.includeString = includeTypeString;

        if ( StringUtils.isNotEmpty( includeTypeString ) )
        {
            this.includeTypes = Arrays.asList( StringUtils.split( includeTypeString, "," ) );
        }
    }

    /**
     * @return Returns the excludeTypes.
     */
    public List getExcludeTypes()
    {
        return this.excludeTypes;
    }

    /**
     * @return Returns the includeTypes.
     */
    public List getIncludeTypes()
    {
        return this.includeTypes;
    }

}
