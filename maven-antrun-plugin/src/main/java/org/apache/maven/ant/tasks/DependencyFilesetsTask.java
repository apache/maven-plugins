package org.apache.maven.ant.tasks;

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

import org.apache.maven.ant.tasks.support.SpecificScopesArtifactFilter;
import org.apache.maven.ant.tasks.support.TypesArtifactFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.plugin.antrun.AntRunMojo;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ant task which create a fileset for each dependency in a Maven project, and a
 * fileset containing all selected dependencies.
 *
 * @author pgier
 */
public class DependencyFilesetsTask
    extends Task
{

    /**
     * The default project dependencies id.
     */
    public static final String DEFAULT_PROJECT_DEPENDENCIES_ID = "maven.project.dependencies";

    /**
     * The project ref Id of the project being used.
     */
    private String mavenProjectId = AntRunMojo.DEFAULT_MAVEN_PROJECT_REFID;

    /**
     * The id to store the dependencies fileset.
     */
    private String projectDependenciesId = DEFAULT_PROJECT_DEPENDENCIES_ID;

    /**
     * @return {@link #projectDependenciesId}
     */
    public String getProjectDependenciesId()
    {
        return projectDependenciesId;
    }

    /**
     * @param projectDependenciesId {@link #projectDependenciesId}
     */
    public void setProjectDependenciesId( String projectDependenciesId )
    {
        this.projectDependenciesId = projectDependenciesId;
    }

    /**
     * The string to prepend to all dependency filesets.
     */
    private String prefix = "";

    /**
     * A comma separated list of artifact types to include.
     */
    private String types = "";

    /**
     * A comma separated list of dependency scopes to include.
     */
    private String scopes = "";

    /**
     * Create instance.
     */
    public DependencyFilesetsTask()
    {

    }

    /** {@inheritDoc} */
    public void execute()
    {
        if ( this.getProject().getReference( mavenProjectId ) == null )
        {
            throw new BuildException( "Maven project reference not found: " + mavenProjectId );
        }

        MavenProject mavenProject = (MavenProject) this.getProject().getReference( "maven.project" );

        // Add filesets for depenedency artifacts
        @SuppressWarnings( "unchecked" ) Set<Artifact> depArtifacts = filterArtifacts( mavenProject.getArtifacts() );

        FileSet dependenciesFileSet = new FileSet();
        dependenciesFileSet.setProject( getProject() );
        ArtifactRepository localRepository = (ArtifactRepository) getProject().getReference( "maven.local.repository" );
        dependenciesFileSet.setDir( new File( localRepository.getBasedir() ) );

        for ( Artifact artifact : depArtifacts )
        {
            String relativeArtifactPath = localRepository.pathOf( artifact );
            dependenciesFileSet.createInclude().setName( relativeArtifactPath );

            String fileSetName = getPrefix() + artifact.getDependencyConflictId();

            FileSet singleArtifactFileSet = new FileSet();
            singleArtifactFileSet.setProject( getProject() );
            singleArtifactFileSet.setFile( artifact.getFile() );
            getProject().addReference( fileSetName, singleArtifactFileSet );
        }

        getProject().addReference( ( getPrefix() + projectDependenciesId ), dependenciesFileSet );
    }

    /**
     * @return {@link #mavenProjectId}
     */
    public String getMavenProjectId()
    {
        return mavenProjectId;
    }

    /**
     * @param mavenProjectId {@link #mavenProjectId}
     */
    public void setMavenProjectId( String mavenProjectId )
    {
        this.mavenProjectId = mavenProjectId;
    }

    /**
     * @return {@link #prefix}
     */
    public String getPrefix()
    {
        if ( prefix == null )
        {
            prefix = "";
        }
        return prefix;
    }

    /**
     * Prefix to be added to each of the dependency filesets. Default is empty string.
     * @param prefix {@link #prefix}
     */
    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
    }

    /**
     * @return {@link #types}
     */
    public String getTypes()
    {
        return types;
    }

    /**
     * @param types {@link #types}
     */
    public void setTypes( String types )
    {
        this.types = types;
    }

    /**
     * @return {@link #scopes}
     */
    public String getScopes()
    {
        return scopes;
    }

    /**
     * @param scopes {@link #scopes}
     */
    public void setScopes( String scopes )
    {
        this.scopes = scopes;
    }

    /**
     * Filter a set of artifacts using the scopes and type filters.
     *
     * @param artifacts {@link Artifact} set.
     * @return The set of filtered artifacts.
     */
    public Set<Artifact> filterArtifacts( Set<Artifact> artifacts )
    {
        if ( scopes == null )
        {
            scopes = "";
        }
        if ( types == null )
        {
            types = "";
        }

        if ( scopes.equals( "" ) && types.equals( "" ) )
        {
            return artifacts;
        }

        AndArtifactFilter filter = new AndArtifactFilter();
        if ( !scopes.equals( "" ) )
        {
            filter.add( new SpecificScopesArtifactFilter( getScopes() ) );
        }
        if ( !types.equals( "" ) )
        {
            filter.add( new TypesArtifactFilter( getTypes() ) );
        }

        Set<Artifact> artifactsResult = new LinkedHashSet<Artifact>();
        for ( Artifact artifact : artifacts )
        {
            if ( filter.include( artifact ) )
            {
                artifactsResult.add( artifact );
            }
        }
        return artifactsResult;
    }
}
