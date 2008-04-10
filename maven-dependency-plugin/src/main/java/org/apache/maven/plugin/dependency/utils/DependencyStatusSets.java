package org.apache.maven.plugin.dependency.utils;

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

/**
 * 
 */

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class DependencyStatusSets
{

    TreeSet resolvedDependencies = null;

    TreeSet unResolvedDependencies = null;

    TreeSet skippedDependencies = null;

    public DependencyStatusSets()
    {

    }

    public DependencyStatusSets( Set resolved, Set unResolved, Set skipped )
    {
        if ( resolved != null )
        {
            this.resolvedDependencies = new TreeSet( resolved );
        }
        if ( unResolved != null )
        {
            this.unResolvedDependencies = new TreeSet( unResolved );
        }
        if ( skipped != null )
        {
            this.skippedDependencies = new TreeSet( skipped );
        }
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
        if ( resolvedDependencies != null )
        {
            this.resolvedDependencies = new TreeSet( resolvedDependencies );
        }
        else
        {
            this.resolvedDependencies = null;
        }
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
        if ( skippedDependencies != null )
        {
            this.skippedDependencies = new TreeSet( skippedDependencies );
        }
        else
        {
            this.skippedDependencies = null;
        }
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
        if ( unResolvedDependencies != null )
        {
            this.unResolvedDependencies = new TreeSet( unResolvedDependencies );
        }
        else
        {
            this.unResolvedDependencies = null;
        }
    }

    public String getOutput(boolean outputAbsoluteArtifactFilename )
    {
        return getOutput(outputAbsoluteArtifactFilename, true );
    }

    public String getOutput(boolean outputAbsoluteArtifactFilename, boolean outputScope )
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "\n" );
        sb.append( "The following files have been resolved:\n" );
        if ( this.resolvedDependencies == null || this.resolvedDependencies.isEmpty() )
        {
            sb.append( "   none\n" );
        }
        else
        {
            for ( Iterator i = resolvedDependencies.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                String artifactFilename = null;
                if ( outputAbsoluteArtifactFilename )
                {
                    try
                    {
                        // we want to print the absolute file name here
                        artifactFilename = artifact.getFile().getAbsoluteFile().getPath();
                    }
                    catch ( NullPointerException e )
                    {
                        // ignore the null pointer, we'll output a null string
                        artifactFilename = null;
                    }
                }
                
                String id = outputScope ? artifact.toString() : artifact.getId();
                
                sb.append( "   " + id + ( outputAbsoluteArtifactFilename ? ":" + artifactFilename : "" ) +"\n");
            }
        }

        if ( this.skippedDependencies != null && !this.skippedDependencies.isEmpty() )
        {
            sb.append( "\n" );
            sb.append( "The following files were skipped:\n" );
            SortedSet sortedSkippedDependencies = new TreeSet();
            sortedSkippedDependencies.addAll( this.skippedDependencies );
            for ( Iterator i = sortedSkippedDependencies.iterator(); i.hasNext(); )
            {
                sb.append( "   " + ( (Artifact) i.next() ).getId()+"\n" );
            }
        }

        if ( this.unResolvedDependencies != null && !this.unResolvedDependencies.isEmpty() )
        {
            sb.append( "\n" );
            sb.append( "The following files have NOT been resolved:\n" );
            SortedSet sortedUnResolvedDependencies = new TreeSet();
            sortedUnResolvedDependencies.addAll( this.unResolvedDependencies );
            for ( Iterator i = sortedUnResolvedDependencies.iterator(); i.hasNext(); )
            {
                sb.append( "   " + ( (Artifact) i.next() ).getId()+"\n" );
            }
        }
        sb.append("\n");

        return sb.toString();
    }
}
