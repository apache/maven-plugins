package org.apache.maven.report.projectinfo.dependencies;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTree;
import org.apache.maven.shared.jar.JarAnalyzer;
import org.apache.maven.shared.jar.JarAnalyzerException;
import org.apache.maven.shared.jar.JarAnalyzerFactory;
import org.apache.maven.shared.jar.classes.JarClasses;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Dependencies
{
    private List projectDependencies;
    private DependencyTree dependencyTree;
    private JarAnalyzerFactory jarAnalyzerFactory;

    public Dependencies( MavenProject project, DependencyTree dependencyTree, JarAnalyzerFactory jarAnalyzerFactory )
    {
        this.dependencyTree = dependencyTree;
        this.projectDependencies = dependencyTree.getRootNode().getChildren();
        this.jarAnalyzerFactory = jarAnalyzerFactory;

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

    public JarDependencyDetails getJarDependencyDetails( Artifact artifact )
        throws JarAnalyzerException
    {
        File artifactFile = artifact.getFile();
        
        JarAnalyzer jar = jarAnalyzerFactory.getJarAnalyzer( artifactFile ); 
        
        JarClasses jarClasses = jar.getClasses();

        return new JarDependencyDetails( jarClasses, jar.isSealed(), jar.getEntries() );
    }
}
