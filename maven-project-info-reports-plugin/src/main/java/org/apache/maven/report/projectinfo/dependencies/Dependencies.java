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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.jar.JarAnalyzer;
import org.apache.maven.shared.jar.JarData;
import org.apache.maven.shared.jar.classes.JarClassesAnalysis;

/**
 * @version $Id$
 * @since 2.1
 */
public class Dependencies
{
    private final MavenProject project;

    private final DependencyNode dependencyTreeNode;

    private final JarClassesAnalysis classesAnalyzer;

    /**
     * @since 2.1
     */
    private List projectDependencies;

    /**
     * @since 2.1
     */
    private List projectTransitiveDependencies;

    /**
     * @since 2.1
     */
    private List allDependencies;

    /**
     * @since 2.1
     */
    private Map dependenciesByScope;

    /**
     * @since 2.1
     */
    private Map transitiveDependenciesByScope;

    /**
     * @since 2.1
     */
    private Map dependencyDetails;

    /**
     * Default constructor
     *
     * @param project
     * @param dependencyTreeNode
     * @param classesAnalyzer
     */
    public Dependencies( MavenProject project, DependencyNode dependencyTreeNode,
                         JarClassesAnalysis classesAnalyzer )
    {
        this.project = project;
        this.dependencyTreeNode = dependencyTreeNode;
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

        mapArtifactFiles( dependencyTreeNode, projectMap );
    }

    /**
     * Getter for the project
     *
     * @return the project
     */
    public MavenProject getProject()
    {
        return project;
    }

    /**
     * @return <code>true</code> if getProjectDependencies() is not empty, <code>false</code> otherwise.
     */
    public boolean hasDependencies()
    {
        return ( getProjectDependencies() != null ) && ( !getProjectDependencies().isEmpty() );
    }

    /**
     * @return a list of <code>Artifact</code> from the project.
     */
    public List getProjectDependencies()
    {
        if ( projectDependencies != null )
        {
            return projectDependencies;
        }

        projectDependencies = new ArrayList();
        for ( Iterator i = dependencyTreeNode.getChildren().iterator(); i.hasNext(); )
        {
            DependencyNode dependencyNode = (DependencyNode) i.next();

            projectDependencies.add( dependencyNode.getArtifact() );
        }

        return projectDependencies;
    }

    /**
     * @return a list of transitive <code>Artifact</code> from the project.
     */
    public List getTransitiveDependencies()
    {
        if ( projectTransitiveDependencies != null )
        {
            return projectTransitiveDependencies;
        }

        projectTransitiveDependencies = new ArrayList( getAllDependencies() );
        projectTransitiveDependencies.removeAll( getProjectDependencies() );

        return projectTransitiveDependencies;
    }

    /**
     * @return a list of included <code>Artifact</code> returned by the dependency tree.
     */
    public List getAllDependencies()
    {
        if ( allDependencies != null )
        {
            return allDependencies;
        }

        allDependencies = new ArrayList();
        for ( Iterator i = dependencyTreeNode.getChildren().iterator(); i.hasNext(); )
        {
            DependencyNode dependencyNode = (DependencyNode) i.next();

            if ( dependencyNode.getState() != DependencyNode.INCLUDED )
            {
                continue;
            }

            if ( dependencyNode.getArtifact().getGroupId().equals( project.getGroupId() )
                && dependencyNode.getArtifact().getArtifactId().equals( project.getArtifactId() )
                && dependencyNode.getArtifact().getVersion().equals( project.getVersion() ) )
            {
                continue;
            }

            if ( !allDependencies.contains( dependencyNode.getArtifact() ) )
            {
                allDependencies.add( dependencyNode.getArtifact() );
            }
            getAllDependencies( dependencyNode );
        }

        return allDependencies;
    }

    /**
     * @param isTransitively <code>true</code> to return transitive dependencies, <code>false</code> otherwise.
     * @return a map with supported scopes as key and a list of <code>Artifact</code> as values.
     * @see Artifact#SCOPE_COMPILE
     * @see Artifact#SCOPE_PROVIDED
     * @see Artifact#SCOPE_RUNTIME
     * @see Artifact#SCOPE_SYSTEM
     * @see Artifact#SCOPE_TEST
     */
    public Map getDependenciesByScope( boolean isTransitively )
    {
        if ( isTransitively )
        {
            if ( transitiveDependenciesByScope != null )
            {
                return transitiveDependenciesByScope;
            }

            transitiveDependenciesByScope = new HashMap();
            for ( Iterator i = getTransitiveDependencies().iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                List multiValue = (List) transitiveDependenciesByScope.get( artifact.getScope() );
                if ( multiValue == null )
                {
                    multiValue = new ArrayList();
                }

                if ( !multiValue.contains( artifact ) )
                {
                    multiValue.add( artifact );
                }
                transitiveDependenciesByScope.put( artifact.getScope(), multiValue );
            }

            return transitiveDependenciesByScope;
        }

        if ( dependenciesByScope != null )
        {
            return dependenciesByScope;
        }

        dependenciesByScope = new HashMap();
        for ( Iterator i = getProjectDependencies().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            List multiValue = (List) dependenciesByScope.get( artifact.getScope() );
            if ( multiValue == null )
            {
                multiValue = new ArrayList();
            }

            if ( !multiValue.contains( artifact ) )
            {
                multiValue.add( artifact );
            }
            dependenciesByScope.put( artifact.getScope(), multiValue );
        }

        return dependenciesByScope;
    }

    /**
     * @param artifact
     * @return the jardata object from the artifact
     * @throws IOException if any
     */
    public JarData getJarDependencyDetails( Artifact artifact )
        throws IOException
    {
        if ( dependencyDetails == null )
        {
            dependencyDetails = new HashMap();
        }

        JarData old = (JarData) dependencyDetails.get( artifact.getId() );
        if ( dependencyDetails.get( artifact.getId() ) != null )
        {
            return old;
        }

        JarAnalyzer jarAnalyzer = new JarAnalyzer( artifact.getFile() );
        try
        {
            classesAnalyzer.analyze( jarAnalyzer );
        }
        finally
        {
            jarAnalyzer.closeQuietly();
        }

        dependencyDetails.put( artifact.getId(), jarAnalyzer.getJarData() );

        return jarAnalyzer.getJarData();
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

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
                anode = new DependencyNode( ArtifactUtils.copyArtifact( projartifact ) );
                anode.getArtifact().setFile( projartifact.getFile() );
            }

            mapArtifactFiles( anode, projectMap );
        }
    }

    /**
     * Recursive method to get all dependencies from a given <code>dependencyNode</code>
     *
     * @param dependencyNode not null
     */
    private void getAllDependencies( DependencyNode dependencyNode )
    {
        if ( dependencyNode == null || dependencyNode.getChildren() == null )
        {
            if ( !allDependencies.contains( dependencyNode.getArtifact() ) )
            {
                allDependencies.add( dependencyNode.getArtifact() );
            }
            return;
        }

        for ( Iterator i = dependencyNode.getChildren().iterator(); i.hasNext(); )
        {
            DependencyNode subdependencyNode = (DependencyNode) i.next();

            if ( subdependencyNode.getState() != DependencyNode.INCLUDED )
            {
                continue;
            }

            if ( subdependencyNode.getArtifact().getGroupId().equals( project.getGroupId() )
                && subdependencyNode.getArtifact().getArtifactId().equals( project.getArtifactId() )
                && subdependencyNode.getArtifact().getVersion().equals( project.getVersion() ) )
            {
                continue;
            }

            if ( !allDependencies.contains( subdependencyNode.getArtifact() ) )
            {
                allDependencies.add( subdependencyNode.getArtifact() );
            }
            getAllDependencies( subdependencyNode );
        }
    }
}
