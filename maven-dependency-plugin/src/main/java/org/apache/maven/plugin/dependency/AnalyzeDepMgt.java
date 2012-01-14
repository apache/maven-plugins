package org.apache.maven.plugin.dependency;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * This mojo looks at the dependencies after final resolution and looks for
 * mismatches in your dependencyManagement section. In versions of maven prior
 * to 2.0.6, it was possible to inherit versions that didn't match your
 * dependencyManagement. See <a
 * href="http://jira.codehaus.org/browse/MNG-1577">MNG-1577</a> for more info.
 * This mojo is also useful for just detecting projects that override the
 * dependencyManagement directly. Set ignoreDirect to false to detect these
 * otherwise normal conditions.
 *
 * @author <a href="mailto:brianefox@gmail.com">Brian Fox</a>
 * @version $Id$
 * @goal analyze-dep-mgt
 * @requiresDependencyResolution test
 * @since 2.0-alpha-3
 */
public class AnalyzeDepMgt
    extends AbstractMojo
{
    // fields -----------------------------------------------------------------

    /**
     *
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Fail the build if a problem is detected.
     *
     * @parameter expression="${mdep.analyze.failBuild}" default-value="false"
     */
    private boolean failBuild = false;

    /**
     * Ignore Direct Dependency Overrides of dependencyManagement section.
     *
     * @parameter expression="${mdep.analyze.ignore.direct}" default-value="true"
     */
    private boolean ignoreDirect = true;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        boolean result = checkDependencyManagement();
        if ( result )
        {
            if ( this.failBuild )

            {
                throw new MojoExecutionException( "Found Dependency errors." );
            }
            else
            {
                getLog().warn( "Potential problems found in Dependency Management " );
            }
        }
    }

    /**
     * Does the work of checking the DependencyManagement Section.
     * @return true if errors are found.
     * @throws MojoExecutionException
     */
    private boolean checkDependencyManagement()
        throws MojoExecutionException
    {
        boolean foundError = false;

        getLog().info( "Found Resolved Dependency / DependencyManagement mismatches:" );

        List<Dependency> depMgtDependencies = null;

        DependencyManagement depMgt = project.getDependencyManagement();
        if ( depMgt != null )
        {
            depMgtDependencies = depMgt.getDependencies();
        }

        if ( depMgtDependencies != null && !depMgtDependencies.isEmpty() )
        {
            // put all the dependencies from depMgt into a map for quick lookup
            Map<String, Dependency> depMgtMap = new HashMap<String, Dependency>();
            Map<String, Exclusion> exclusions = new HashMap<String, Exclusion>();
            for ( Dependency depMgtDependency : depMgtDependencies )
            {
                depMgtMap.put( depMgtDependency.getManagementKey(), depMgtDependency );

                // now put all the exclusions into a map for quick lookup
                exclusions.putAll( addExclusions( depMgtDependency.getExclusions() ) );
            }

            // get dependencies for the project (including transitive)
            Set<Artifact> allDependencyArtifacts = new HashSet<Artifact>( project.getArtifacts() );

            // don't warn if a dependency that is directly listed overrides
            // depMgt. That's ok.
            if ( this.ignoreDirect )
            {
                getLog().info( "\tIgnoring Direct Dependencies." );
                Set<Artifact> directDependencies = project.getDependencyArtifacts();
                allDependencyArtifacts.removeAll( directDependencies );
            }

            // log exclusion errors
            List<Artifact> exclusionErrors = getExclusionErrors( exclusions, allDependencyArtifacts );
            for ( Artifact exclusion : exclusionErrors )
            {
                getLog().info(
                               StringUtils.stripEnd( getArtifactManagementKey( exclusion ), ":" )
                                   + " was excluded in DepMgt, but version " + exclusion.getVersion()
                                   + " has been found in the dependency tree." );
                foundError = true;
            }

            // find and log version mismatches
            Map<Artifact, Dependency> mismatch = getMismatch( depMgtMap, allDependencyArtifacts );
            for ( Map.Entry<Artifact, Dependency> entry : mismatch.entrySet() )
            {
                logMismatch( entry.getKey(), entry.getValue() );
                foundError = true;
            }
            if ( !foundError )
            {
                getLog().info( "   None" );
            }
        }
        else
        {
            getLog().info( "   Nothing in DepMgt." );
        }



        return foundError;
    }

    /**
     * Returns a map of the exclusions using the Dependency ManagementKey as the
     * keyset.
     *
     * @param exclusionList
     *            to be added to the map.
     * @return a map of the exclusions using the Dependency ManagementKey as the
     *         keyset.
     */
    public Map<String, Exclusion> addExclusions( List<Exclusion> exclusionList )
    {
        Map<String, Exclusion> exclusions = new HashMap<String, Exclusion>();
        if ( exclusionList != null )
        {
            for ( Exclusion exclusion : exclusionList )
            {
                exclusions.put( getExclusionKey( exclusion ), exclusion );
            }
        }
        return exclusions;
    }

    /**
     * Returns a List of the artifacts that should have been excluded, but were
     * found in the dependency tree.
     *
     * @param exclusions
     *            a map of the DependencyManagement exclusions, with the
     *            ManagementKey as the key and Dependency as the value.
     * @param allDependencyArtifacts
     *            resolved artifacts to be compared.
     * @return list of artifacts that should have been excluded.
     */
    public List<Artifact> getExclusionErrors( Map<String, Exclusion> exclusions, Set<Artifact> allDependencyArtifacts )
    {
        List<Artifact> list = new ArrayList<Artifact>();

        for ( Artifact artifact : allDependencyArtifacts )
        {
            if ( exclusions.containsKey( getExclusionKey( artifact ) ) )
            {
                list.add( artifact );
            }
        }

        return list;
    }

    public String getExclusionKey( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    public String getExclusionKey( Exclusion ex )
    {
        return ex.getGroupId() + ":" + ex.getArtifactId();
    }

    /**
     * Calculate the mismatches between the DependencyManagement and resolved
     * artifacts
     *
     * @param depMgtMap
     *            contains the Dependency.GetManagementKey as the keyset for
     *            quick lookup.
     * @param allDependencyArtifacts
     *            contains the set of all artifacts to compare.
     * @return a map containing the resolved artifact as the key and the listed
     *         dependency as the value.
     */
    public Map<Artifact, Dependency> getMismatch( Map<String, Dependency> depMgtMap,
                                                  Set<Artifact> allDependencyArtifacts )
    {
        Map<Artifact, Dependency> mismatchMap = new HashMap<Artifact, Dependency>();

        for ( Artifact dependencyArtifact : allDependencyArtifacts )
        {
            Dependency depFromDepMgt = depMgtMap.get( getArtifactManagementKey( dependencyArtifact ) );
            if ( depFromDepMgt != null )
            {
                //workaround for MNG-2961
                dependencyArtifact.isSnapshot();

                if (!depFromDepMgt.getVersion().equals( dependencyArtifact.getBaseVersion()) )
                {
                    mismatchMap.put( dependencyArtifact, depFromDepMgt );
                }
            }
        }
        return mismatchMap;
    }

    /**
     * This function displays the log to the screen showing the versions and
     * information about the artifacts that don't match.
     *
     * @param dependencyArtifact
     *            the artifact that was resolved.
     * @param dependencyFromDepMgt
     *            the dependency listed in the DependencyManagement section.
     * @throws MojoExecutionException
     */
    public void logMismatch( Artifact dependencyArtifact, Dependency dependencyFromDepMgt )
        throws MojoExecutionException
    {
        if ( dependencyArtifact == null || dependencyFromDepMgt == null )
        {
            throw new MojoExecutionException( "Invalid params: Artifact:" + dependencyArtifact + " Dependency:"
                + dependencyFromDepMgt );
        }

        getLog().info( "\tDependency: " + StringUtils.stripEnd( dependencyFromDepMgt.getManagementKey(), ":" ) );
        getLog().info( "\t\tDepMgt  : " + dependencyFromDepMgt.getVersion() );
        getLog().info( "\t\tResolved: " + dependencyArtifact.getBaseVersion() );
    }

    /**
     * This function returns a string comparable with Dependency.GetManagementKey.
     *
     * @param artifact
     *            to gen the key for
     * @return a string in the form: groupId:ArtifactId:Type[:Classifier]
     */
    public String getArtifactManagementKey( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType()
            + ( ( artifact.getClassifier() != null ) ? ":" + artifact.getClassifier() : "" );
    }

    /**
     * @return the failBuild
     */
    public boolean isFailBuild()
    {
        return this.failBuild;
    }

    /**
     * @param theFailBuild
     *            the failBuild to set
     */
    public void setFailBuild( boolean theFailBuild )
    {
        this.failBuild = theFailBuild;
    }

    /**
     * @return the project
     */
    public MavenProject getProject()
    {
        return this.project;
    }

    /**
     * @param theProject
     *            the project to set
     */
    public void setProject( MavenProject theProject )
    {
        this.project = theProject;
    }

    /**
     * @return the ignoreDirect
     */
    public boolean isIgnoreDirect()
    {
        return this.ignoreDirect;
    }

    /**
     * @param theIgnoreDirect
     *            the ignoreDirect to set
     */
    public void setIgnoreDirect( boolean theIgnoreDirect )
    {
        this.ignoreDirect = theIgnoreDirect;
    }
}
