package org.apache.maven.report.projectinfo;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * @author Edwin Punzalan
 */
public class ReportResolutionListener
    implements ResolutionListener
{
    private Map directDep = new HashMap();

    private Map transitiveDep = new HashMap();

    private Map replacedDep = new HashMap();

    private List parents = new ArrayList();

    private Map depTree = new HashMap();

    private Map depMap = new HashMap();

    private Map depthMap = new HashMap();

    public void testArtifact( Artifact node )
    {

    }

    public void startProcessChildren( Artifact artifact )
    {
        parents.add( artifact );
    }

    public void endProcessChildren( Artifact artifact )
    {
        parents.remove( artifact );
    }

    public void includeArtifact( Artifact artifact )
    {
        addToDepMap( artifact );

        if ( depthMap.containsKey( artifact.getId() ) )
        {
            Integer depth = (Integer) depthMap.get( artifact.getId() );
            if ( depth.intValue() <= parents.size() )
            {
                return;
            }
        }

        //remove from tree the artifact which is farther down the dependency trail
        removeFromDepTree( artifact );

        depthMap.put( artifact.getId(), new Integer( parents.size() ) );

        addDependency( artifact );

        addToDepTree( artifact );
    }

    private void removeFromDepTree( Artifact artifact )
    {
        for ( Iterator artifactDeps = depTree.values().iterator(); artifactDeps.hasNext(); )
        {
            List depList = (List) artifactDeps.next();
            for ( Iterator artifacts = depList.iterator(); artifacts.hasNext(); )
            {
                Artifact dep = (Artifact) artifacts.next();

                if ( dep.getId().equals( artifact.getId() ) )
                {
                    depList.remove( dep );
                    break;
                }
            }
        }
    }

    private void addToDepTree( Artifact artifact )
    {
        if ( parents.size() > 0 )
        {
            Artifact parent = (Artifact) parents.get( parents.size() - 1 );

            if ( depTree.containsKey( parent.getId() ) )
            {
                List deps = (List) depTree.get( parent.getId() );

                deps.add( artifact );
            }
            else
            {
                List deps = new ArrayList();
                deps.add( artifact );
                depTree.put( parent.getId(), deps );
            }
        }
    }

    private void addToDepMap( Artifact artifact )
    {
        if ( parents.size() > 0 )
        {
            Artifact parent = (Artifact) parents.get( parents.size() - 1 );

            if ( depMap.containsKey( parent.getId() ) )
            {
                List deps = (List) depMap.get( parent.getId() );

                deps.add( artifact );
            }
            else
            {
                List deps = new ArrayList();
                deps.add( artifact );
                depMap.put( parent.getId(), deps );
            }
        }
    }

    public void omitForNearer( Artifact omitted, Artifact kept )
    {
        String key = omitted.getId();

        replacedDep.put( key, omitted );

        if ( directDep.containsKey( key ) )
        {
            directDep.remove( key );
        }
        else if ( transitiveDep.containsKey( key ) )
        {
            transitiveDep.remove( key );
        }

        addDependency( kept );
    }

    private void addDependency( Artifact artifact )
    {
        if ( parents.size() == 0 )
        {
            //do nothing... artifact is current project
        }
        else if ( parents.size() == 1 )
        {
            if ( !directDep.containsKey( artifact.getId() ) )
            {
                if ( artifact.getScope() == null )
                {
                    artifact.setScope( Artifact.SCOPE_COMPILE );
                }
                directDep.put( artifact.getId(), artifact );
            }
        }
        else
        {
            if ( !transitiveDep.containsKey( artifact.getId() ) )
            {
                if ( artifact.getScope() == null )
                {
                    Artifact parent = (Artifact) parents.get(  parents.size() - 1 );

                    artifact.setScope( parent.getScope() );
                }

                transitiveDep.put( artifact.getId(), artifact );
            }
        }
    }

    public void updateScope( Artifact artifact, String scope )
    {
        if ( directDep.containsKey( artifact.getId() ) )
        {
            Artifact depArtifact = (Artifact) directDep.get( artifact.getId() );

            depArtifact.setScope( scope );
        }

        if ( transitiveDep.containsKey( artifact.getId() ) )
        {
            Artifact depArtifact = (Artifact) transitiveDep.get( artifact.getId() );

            depArtifact.setScope( scope );
        }
    }

    public void manageArtifact( Artifact artifact, Artifact replacement )
    {
        omitForNearer( artifact, replacement );
    }

    public void omitForCycle( Artifact artifact )
    {
        replacedDep.put( artifact.getId(), artifact );
    }

    public void updateScopeCurrentPom( Artifact artifact, String scope )
    {
        updateScope( artifact, scope );
    }

    public void selectVersionFromRange( Artifact artifact )
    {

    }

    public void restrictRange( Artifact artifact, Artifact replacement, VersionRange newRange )
    {

    }

    public Set getArtifacts()
    {
        Set all = new HashSet();

        all.addAll( directDep.values() );

        all.addAll( transitiveDep.values() );

        return all;
    }

    public Map getTransitiveDependencies()
    {
        return Collections.unmodifiableMap( transitiveDep );
    }

    public Map getDirectDependencies()
    {
        return Collections.unmodifiableMap( directDep );
    }

    public Map getOmittedArtifacts()
    {
        return Collections.unmodifiableMap( replacedDep );
    }

    public Map getDepTree()
    {
        return depTree;
    }

    public Map getDepMap()
    {
        return depMap;
    }
}
