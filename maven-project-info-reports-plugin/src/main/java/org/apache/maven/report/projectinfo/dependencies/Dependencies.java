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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTree;
import org.apache.maven.shared.jar.JarAnalyzer;
import org.apache.maven.shared.jar.JarData;
import org.apache.maven.shared.jar.classes.JarClassesAnalysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Dependencies
{
    private final List projectDependencies;

    private final DependencyTree dependencyTree;

    private final JarClassesAnalysis classesAnalyzer;

    public Dependencies( MavenProject project, DependencyTree dependencyTree, JarClassesAnalysis classesAnalyzer )
    {
        this.dependencyTree = dependencyTree;
        this.projectDependencies = dependencyTree.getRootNode().getChildren();
        this.classesAnalyzer = classesAnalyzer;

        /*
         * Workaround to ensure proper File objects in the Artifacts from the ReportResolutionListener
         */
        Map projectMap = new HashMap();
        Iterator it = project.getArtifacts().iterator();
        while ( it.hasNext() )
        {
            Artifact artifact = (Artifact) it.next();
            projectMap.put( ArtifactUtils.versionlessKey( artifact ), artifact );
        }

        mapArtifactFiles( dependencyTree.getRootNode(), projectMap );
    }

    private void mapArtifactFiles( DependencyNode node, Map projectMap )
    {
        List childs = node.getChildren();
        if ( ( childs == null ) || childs.isEmpty() )
        {
            return;
        }

        Iterator it = childs.iterator();
        while ( it.hasNext() )
        {
            DependencyNode anode = (DependencyNode) it.next();
            String key = ArtifactUtils.versionlessKey( anode.getArtifact() );
            Artifact projartifact = (Artifact) projectMap.get( key );
            if ( projartifact != null )
            {
                anode.getArtifact().setFile( projartifact.getFile() );
            }

            mapArtifactFiles( anode, projectMap );
        }
    }

    public boolean hasDependencies()
    {
        return ( projectDependencies != null ) && ( !this.projectDependencies.isEmpty() );
    }

    public List getProjectDependencies()
    {
        return new ArrayList( projectDependencies );
    }

    public List getTransitiveDependencies()
    {
        List deps = new ArrayList( dependencyTree.getArtifacts() );
        deps.removeAll( projectDependencies );
        return deps;
    }

    public List getAllDependencies()
    {
        return dependencyTree.getArtifacts();
    }

    public Map getDependenciesByScope()
    {
        Map dependenciesByScope = new HashMap();
        for ( Iterator i = getAllDependencies().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            List multiValue = (List) dependenciesByScope.get( artifact.getScope() );
            if ( multiValue == null )
            {
                multiValue = new ArrayList();
            }
            multiValue.add( artifact );
            dependenciesByScope.put( artifact.getScope(), multiValue );
        }
        return dependenciesByScope;
    }

    public JarData getJarDependencyDetails( Artifact artifact )
        throws IOException
    {
        JarAnalyzer jarAnalyzer = new JarAnalyzer( artifact.getFile() );

        try
        {
            classesAnalyzer.analyze( jarAnalyzer );
        }
        finally
        {
            jarAnalyzer.closeQuietly();
        }

        return jarAnalyzer.getJarData();
    }
}
