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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzerException;
import org.codehaus.plexus.util.StringUtils;

/**
 * 
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @goal analyze
 * @requiresDependencyResolution test
 * 
 * @since 2.0-alpha-3
 */
public class AnalyzeMojo
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
     * Check for dependency / dependencyMgt conflicts
     * 
     * @parameter expression=${mdep.checkDepMgt}
     */
    private boolean checkDependencyMgt = true;

    /**
     * Check dependency conflicts
     * 
     * @parameter expression=${mdep.checkDependencies}
     */
    private boolean checkDependencies = false;

    /**
     * Fail Build on problem
     * 
     * @parameter expression=${mdep.failBuild}
     */
    private boolean failBuild = false;

    /**
     * 
     * 
     * @parameter expression="${component.org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer}"
     * @required
     * @readonly
     */
    private ProjectDependencyAnalyzer analyzer;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        boolean result = false;
        if ( this.checkDependencies )
        {
            checkDependencies();
        }

        if ( this.checkDependencyMgt )
        {
            result = checkDependencyManagement();
        }

        if ( result && this.failBuild )
        {
            throw new MojoExecutionException( "Found errors." );
        }
    }

    // private methods --------------------------------------------------------

    private void checkDependencies()
        throws MojoExecutionException
    {
        try
        {
            ProjectDependencyAnalysis analysis = analyzer.analyze( project );

            getLog().info( "Used declared dependencies:" );

            logArtifacts( analysis.getUsedDeclaredArtifacts() );

            getLog().info( "Used undeclared dependencies:" );

            logArtifacts( analysis.getUsedUndeclaredArtifacts() );

            getLog().info( "Unused declared dependencies:" );

            logArtifacts( analysis.getUnusedDeclaredArtifacts() );
        }
        catch ( ProjectDependencyAnalyzerException exception )
        {
            throw new MojoExecutionException( "Cannot analyze dependencies", exception );
        }

    }

    private boolean checkDependencyManagement()
        throws MojoExecutionException
    {
        boolean foundMismatch = false;

        getLog().info( "Found Resolved Dependency / DependencyManagement mismatches:" );

        List depMgtDependencies = null;
        DependencyManagement depMgt = project.getDependencyManagement();
        if ( depMgt != null )
        {
            depMgtDependencies = depMgt.getDependencies();
        }

        if ( depMgtDependencies != null && !depMgtDependencies.isEmpty() )
        {
            // put all the dependencies from depMgt into a map for quick lookup
            Map map = new HashMap();
            Iterator iter = depMgtDependencies.iterator();
            while ( iter.hasNext() )
            {
                Dependency dependency = (Dependency) iter.next();
                map.put( dependency.getManagementKey(), dependency );
            }

            Set allDependencies = project.getArtifacts();
            iter = allDependencies.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = (Artifact) iter.next();
               // getLog().info( "a:"+getArtifactManagementKey( artifact ) );
                // see if this artifact matches anything in the dependencyMgt
                // list
                Dependency dep = (Dependency) map.get( getArtifactManagementKey( artifact ) );
                if ( dep != null )
                {
                 //   getLog().info( "Compare:" + dep.getManagementKey()+" v:"+dep.getVersion()+"a:"+artifact.getVersion());
                   // ArtifactVersion depVersion = new DefaultArtifactVersion(dep.getVersion());
                    ArtifactVersion artifactVersion = new DefaultArtifactVersion(artifact.getVersion());
                    
                    if (!artifact.isSnapshot() && !dep.getVersion().equals( artifact.getVersion() ) )

                    {
                        logMismatch( artifact, dep );
                        foundMismatch = true;
                    }
                }
            }
        }
        else
        {
            getLog().info( "   Nothing in DepMgt." );
        }

        if ( !foundMismatch )
        {
            getLog().info( "   None" );
        }

        return foundMismatch;
    }

    private void logArtifacts( Set artifacts )
    {
        if ( artifacts.isEmpty() )
        {
            getLog().info( "   None" );
        }
        else
        {
            for ( Iterator iterator = artifacts.iterator(); iterator.hasNext(); )
            {
                Artifact artifact = (Artifact) iterator.next();

                getLog().info( "   " + artifact );
            }
        }
    }

    private void logMismatch( Artifact artifact, Dependency dependency )
        throws MojoExecutionException
    {
        if ( artifact == null || dependency == null )
        {
            throw new MojoExecutionException( "Invalid params: Artifact:" + artifact + " Dependency:" + dependency );
        }

        getLog().info(
                       "   Resolved Dependency: " + dependency.getManagementKey() +":"+dependency.getVersion()+ " Dependency Management: "
                           + getArtifactManagementKey( artifact )+":"+artifact.getVersion() );
    }

    private String getArtifactManagementKey( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType()
            + ( !StringUtils.isEmpty( artifact.getClassifier() ) ? ":" + artifact.getClassifier() : "" );
    }
}
