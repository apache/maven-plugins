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
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 *  This mojo looks at the dependencies after final resolution and looks for mismatches in your dependencyManagement section. 
 *  In versions of maven prior to 2.0.6, it was possible to inherit versions that didn't match your dependencyManagement. See <a href="http://jira.codehaus.org/browse/MNG-1577">MNG-1577</a> for more info. 
 *  Note: Because Maven 2.0.6 fixes the problems this mojo is meant to detect, it will do nothing in versions of Maven greater than 2.0.5.
 * 
 * @author <a href="mailto:brianefox@gmail.com">Brian Fox</a>
 * @version $Id: AnalyzeMojo.java 519377 2007-03-17 17:37:26Z brianf $
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
     * @parameter expression="${mdep.analyze.failBuild}"
     */
    private boolean failBuild = false;

    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression="${component.org.apache.maven.execution.RuntimeInformation}"
     * @required
     * @readonly
     */
    protected RuntimeInformation rti;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ArtifactVersion version = rti.getApplicationVersion();
        ArtifactVersion checkVersion = new DefaultArtifactVersion( "2.0.6" );
        if ( version.compareTo( checkVersion ) < 0 )
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
    }

    // private methods --------------------------------------------------------

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
            
            //don't warn if a dependency that is directly listed overrides depMgt. That's ok.
            Set directDependencies = project.getDependencyArtifacts();
            allDependencies.removeAll( directDependencies );
       
            iter = allDependencies.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = (Artifact) iter.next();
                Dependency dep = (Dependency) map.get( getArtifactManagementKey( artifact ) );
                if ( dep != null )
                {
                    ArtifactVersion artifactVersion = new DefaultArtifactVersion( artifact.getVersion() );

                    if ( !artifact.isSnapshot() && !dep.getVersion().equals( artifact.getVersion() ) )

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

    private void logMismatch( Artifact artifact, Dependency dependency )
        throws MojoExecutionException
    {
        if ( artifact == null || dependency == null )
        {
            throw new MojoExecutionException( "Invalid params: Artifact:" + artifact + " Dependency:" + dependency );
        }

        getLog().info( "\tDependency: " + dependency.getManagementKey() );
        getLog().info( "\t\tDepMgt  : " + artifact.getVersion() );
        getLog().info( "\t\tResolved: " + dependency.getVersion() );
    }

    private String getArtifactManagementKey( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType()
            + ( !StringUtils.isEmpty( artifact.getClassifier() ) ? ":" + artifact.getClassifier() : "" );
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
     * @return the rti
     */
    public RuntimeInformation getRti()
    {
        return this.rti;
    }

    /**
     * @param theRti the rti to set
     */
    public void setRti( RuntimeInformation theRti )
    {
        this.rti = theRti;
    }
}
