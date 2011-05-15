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
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.jar.JarAnalyzer;
import org.apache.maven.shared.jar.JarData;
import org.apache.maven.shared.jar.classes.JarClasses;
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
    private List<Artifact> projectDependencies;

    /**
     * @since 2.1
     */
    private List<Artifact> projectTransitiveDependencies;

    /**
     * @since 2.1
     */
    private List<Artifact> allDependencies;

    /**
     * @since 2.1
     */
    private Map<String, List<Artifact>> dependenciesByScope;

    /**
     * @since 2.1
     */
    private Map<String, List<Artifact>> transitiveDependenciesByScope;

    /**
     * @since 2.1
     */
    private Map<String, JarData> dependencyDetails;

    /**
     * Default constructor
     *
     * @param project the MavenProject.
     * @param dependencyTreeNode the DependencyNode.
     * @param classesAnalyzer the JarClassesAnalysis.
     */
    public Dependencies( MavenProject project, DependencyNode dependencyTreeNode, JarClassesAnalysis classesAnalyzer )
    {
        this.project = project;
        this.dependencyTreeNode = dependencyTreeNode;
        this.classesAnalyzer = classesAnalyzer;
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
    public List<Artifact> getProjectDependencies()
    {
        if ( projectDependencies != null )
        {
            return projectDependencies;
        }

        projectDependencies = new ArrayList<Artifact>();
        @SuppressWarnings( "unchecked" )
        List<DependencyNode> deps = dependencyTreeNode.getChildren();
        for ( DependencyNode dep : deps )
        {
            projectDependencies.add( dep.getArtifact() );
        }

        return projectDependencies;
    }

    /**
     * @return a list of transitive <code>Artifact</code> from the project.
     */
    public List<Artifact> getTransitiveDependencies()
    {
        if ( projectTransitiveDependencies != null )
        {
            return projectTransitiveDependencies;
        }

        projectTransitiveDependencies = new ArrayList<Artifact>( getAllDependencies() );
        projectTransitiveDependencies.removeAll( getProjectDependencies() );

        return projectTransitiveDependencies;
    }

    /**
     * @return a list of included <code>Artifact</code> returned by the dependency tree.
     */
    public List<Artifact> getAllDependencies()
    {
        if ( allDependencies != null )
        {
            return allDependencies;
        }

        allDependencies = new ArrayList<Artifact>();

        addAllChildrenDependencies( dependencyTreeNode );

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
    public Map<String, List<Artifact>> getDependenciesByScope( boolean isTransitively )
    {
        if ( isTransitively )
        {
            if ( transitiveDependenciesByScope != null )
            {
                return transitiveDependenciesByScope;
            }

            transitiveDependenciesByScope = new HashMap<String, List<Artifact>>();
            for ( Artifact artifact : getTransitiveDependencies() )
            {
                List<Artifact> multiValue = transitiveDependenciesByScope.get( artifact.getScope() );
                if ( multiValue == null )
                {
                    multiValue = new ArrayList<Artifact>();
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

        dependenciesByScope = new HashMap<String, List<Artifact>>();
        for ( Artifact artifact : getProjectDependencies() )
        {
            List<Artifact> multiValue = dependenciesByScope.get( artifact.getScope() );
            if ( multiValue == null )
            {
                multiValue = new ArrayList<Artifact>();
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
     * @param artifact the artifact.
     * @return the jardata object from the artifact
     * @throws IOException if any
     */
    public JarData getJarDependencyDetails( Artifact artifact )
        throws IOException
    {
        if ( dependencyDetails == null )
        {
            dependencyDetails = new HashMap<String, JarData>();
        }

        JarData jarData = dependencyDetails.get( artifact.getId() );
        if ( jarData != null )
        {
            return jarData;
        }

        if ( artifact.getFile().isDirectory() )
        {
            jarData = new JarData( artifact.getFile(), null, new ArrayList<JarEntry>() );

            jarData.setJarClasses( new JarClasses() );
        }
        else
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
            
            jarData = jarAnalyzer.getJarData();
        }

        dependencyDetails.put( artifact.getId(), jarData );
    
        return jarData;
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Recursive method to get all dependencies from a given <code>dependencyNode</code>
     *
     * @param dependencyNode not null
     */
    private void addAllChildrenDependencies( DependencyNode dependencyNode )
    {
        @SuppressWarnings( "unchecked" )
        List<DependencyNode> deps = dependencyNode.getChildren();
        for ( DependencyNode subdependencyNode : deps )
        {
            if ( subdependencyNode.getState() != DependencyNode.INCLUDED )
            {
                continue;
            }

            Artifact artifact = subdependencyNode.getArtifact();

            if ( artifact.getGroupId().equals( project.getGroupId() )
                && artifact.getArtifactId().equals( project.getArtifactId() )
                && artifact.getVersion().equals( project.getVersion() ) )
            {
                continue;
            }

            if ( !allDependencies.contains( artifact ) )
            {
                allDependencies.add( artifact );
            }

            addAllChildrenDependencies( subdependencyNode );
        }
    }
}
