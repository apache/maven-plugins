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
package org.apache.maven.plugin.dependency.utils;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.CollectionUtils;

/**
 * @author brianf
 * 
 */
public class DependencyStatusSets
{

    Set resolvedDependencies = null;

    Set unResolvedDependencies = null;

    Set skippedDependencies = null;

    public DependencyStatusSets()
    {

    }

    public DependencyStatusSets( Set resolved, Set unResolved, Set skipped )
    {
        this.resolvedDependencies = resolved;
        this.unResolvedDependencies = unResolved;
        this.skippedDependencies = skipped;
    }

    /**
     * @return Returns the resolvedDependencies.
     */
    public Set getResolvedDependencies()
    {
        return this.resolvedDependencies;
    }

    /**
     * @param resolvedDependencies
     *            The resolvedDependencies to set.
     */
    public void setResolvedDependencies( Set resolvedDependencies )
    {
        this.resolvedDependencies = resolvedDependencies;
    }

    /**
     * @return Returns the skippedDependencies.
     */
    public Set getSkippedDependencies()
    {
        return this.skippedDependencies;
    }

    /**
     * @param skippedDependencies
     *            The skippedDependencies to set.
     */
    public void setSkippedDependencies( Set skippedDependencies )
    {
        this.skippedDependencies = skippedDependencies;
    }

    /**
     * @return Returns the unResolvedDependencies.
     */
    public Set getUnResolvedDependencies()
    {
        return this.unResolvedDependencies;
    }

    /**
     * @param unResolvedDependencies
     *            The unResolvedDependencies to set.
     */
    public void setUnResolvedDependencies( Set unResolvedDependencies )
    {
        this.unResolvedDependencies = unResolvedDependencies;
    }

    public void logStatus( Log log, boolean outputArtifactFilename )
    {
        log.info( "" );
        log.info( "The following files have been resolved: " );
        if ( this.resolvedDependencies == null || this.resolvedDependencies.isEmpty() )
        {
            log.info( "   none" );
        }
        else
        {
            SortedSet sortedResolvedDependencies = new TreeSet();
            sortedResolvedDependencies.addAll( resolvedDependencies );
            for ( Iterator i = sortedResolvedDependencies.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                String artifactFilename = null;
                if ( outputArtifactFilename )
                {
                    try
                    {
                        artifactFilename = artifact.getFile().getAbsoluteFile().getName();
                    }
                    catch ( NullPointerException e )
                    {
                        // ignore the null pointer, we'll output a null string
                    }
                }
                log.info( "   " + artifact.getId() + ( outputArtifactFilename ? ":" + artifactFilename : "" ) );
            }
        }

        if ( this.skippedDependencies != null && !this.skippedDependencies.isEmpty() )
        {
            log.info( "" );
            log.info( "The following files where skipped: " );
            SortedSet sortedSkippedDependencies = new TreeSet();
            sortedSkippedDependencies.addAll( this.skippedDependencies );
            for ( Iterator i = sortedSkippedDependencies.iterator(); i.hasNext(); )
            {
                log.info( "   " + ( (Artifact) i.next() ).getId() );
            }
        }
        log.info( "" );

        if ( this.unResolvedDependencies != null && !this.unResolvedDependencies.isEmpty() )
        {
            log.info( "The following files have NOT been resolved: " );
            SortedSet sortedUnResolvedDependencies = new TreeSet();
            sortedUnResolvedDependencies.addAll( this.unResolvedDependencies );
            for ( Iterator i = sortedUnResolvedDependencies.iterator(); i.hasNext(); )
            {
                log.info( "   " + ( (Artifact) i.next() ).getId() );
            }
        }
        log.info( "" );

    }
}
