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
package org.apache.maven.plugin.dependency.utils.filters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 * 
 */
public class FilterArtifacts
{
    private ArrayList filters = new ArrayList();

    public FilterArtifacts()
    {
        filters = new ArrayList();
    }

    /**
     * Removes all of the elements from this list. The list will be empty after
     * this call returns.
     */
    public void clearFilters()
    {
        filters.clear();
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * @param o
     *            element to be appended to this list.
     * @return <tt>true</tt> (as per the general contract of Collection.add).
     */
    public void addFilter( ArtifactsFilter filter )
    {
        filters.add( filter );
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * 
     * @param index
     *            index at which the specified element is to be inserted.
     * @param element
     *            element to be inserted.
     * @throws IndexOutOfBoundsException
     *             if index is out of range
     *             <tt>(index &lt; 0 || index &gt; size())</tt>.
     */
    public void addFilter( int index, ArtifactsFilter filter )
    {
        filters.add( index, filter );
    }

    public Set filter( Set artifacts, Log log )
        throws MojoExecutionException
    {
        // apply filters
        Iterator filterIterator = filters.iterator();
        while ( filterIterator.hasNext() )
        {
            // log(artifacts,log);
            ArtifactsFilter filter = (ArtifactsFilter) filterIterator.next();
            artifacts = filter.filter( artifacts, log );
        }

        return artifacts;
    }

    /**
     * @return Returns the filters.
     */
    public ArrayList getFilters()
    {
        return this.filters;
    }

    /**
     * @param filters
     *            The filters to set.
     */
    public void setFilters( ArrayList filters )
    {
        this.filters = filters;
    }
}
