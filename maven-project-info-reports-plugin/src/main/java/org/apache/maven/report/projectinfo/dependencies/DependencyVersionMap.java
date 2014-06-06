package org.apache.maven.report.projectinfo.dependencies;

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
/**
 * @author Simon Wang
 * @version $Id$
 * @since 2.8
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;



public class DependencyVersionMap implements DependencyNodeVisitor
{
    private boolean uniqueVersions;

    private Map<String, List<DependencyNode>> idsToNode;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------
    
    public DependencyVersionMap()
    {
        idsToNode = new HashMap<String, List<DependencyNode>>();
    }

    public void setUniqueVersions( boolean uniqueVersions )
    {
        this.uniqueVersions = uniqueVersions;
    }

    public boolean visit( DependencyNode node )
    {
        addDependency( node );
        return !containsConflicts( node );
    }

    public boolean endVisit( DependencyNode node )
    {
        return true;
    }
    
    /**
     * Get conflicting nodes groups
     * 
     * @return conflicting nodes groups
     */
    public List<List<DependencyNode>> getConflictedVersionNumbers()
    {
        List<List<DependencyNode>> output = new ArrayList<List<DependencyNode>>();
        for ( List<DependencyNode> nodes : idsToNode.values() )
        {
            if ( containsConflicts( nodes ) )
            {
                output.add( nodes );
            }
        }
        return output;
    }
    
    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------
    
    private void addDependency( DependencyNode node )
    {
        String key = constructKey( node );
        List<DependencyNode> nodes = idsToNode.get( key );
        if ( nodes == null )
        {
            nodes = new ArrayList<DependencyNode>();
            idsToNode.put( key, nodes );
        }
        nodes.add( node );
    }
    
    private String constructKey( DependencyNode node )
    {
        return constructKey( node.getArtifact() );
    }

    private String constructKey( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    private String getVersion( Artifact artifact )
    {
        return uniqueVersions ? artifact.getVersion() : artifact.getBaseVersion();
    }

    private boolean containsConflicts( DependencyNode node )
    {
        return containsConflicts( node.getArtifact() );
    }

    private boolean containsConflicts( Artifact artifact )
    {
        return containsConflicts( idsToNode.get( constructKey( artifact ) ) );
    }

    /**
     * Check whether given dependency nodes contains conflicts
     * 
     * @param nodes
     * @return contains:true; not contains:false;
     */
    private boolean containsConflicts( List<DependencyNode> nodes )
    {
        String version = null;
        for ( DependencyNode node : nodes )
        {
            if ( version == null )
            {
                version = getVersion( node.getArtifact() );
            }
            else
            {
                if ( version.compareTo( getVersion( node.getArtifact() ) ) != 0 )
                {
                    return true;
                }
            }
        }
        return false;
    }
  
}