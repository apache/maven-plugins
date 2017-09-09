package org.apache.maven.plugins.dependency.fromDependencies;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.AbstractDependencyMojo;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.dependency.utils.translators.ArtifactTranslator;
import org.apache.maven.plugins.dependency.utils.translators.ClassifierTypeTranslator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.ArtifactCoordinate;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.apache.maven.shared.repository.RepositoryManager;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.codehaus.plexus.util.StringUtils;

/**
 * Class that encapsulates the plugin parameters, and contains methods that handle dependency filtering
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * @see org.apache.maven.plugins.dependency.AbstractDependencyMojo
 */
public abstract class AbstractDependencyFilterMojo
    extends AbstractDependencyMojo
{
    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private DependencyResolver dependencyResolver;

    @Component
    private RepositoryManager repositoryManager;

    /**
     * Overwrite release artifacts
     *
     * @since 1.0
     */
    @Parameter( property = "overWriteReleases", defaultValue = "false" )
    protected boolean overWriteReleases;

    /**
     * Overwrite snapshot artifacts
     *
     * @since 1.0
     */
    @Parameter( property = "overWriteSnapshots", defaultValue = "false" )
    protected boolean overWriteSnapshots;

    /**
     * Overwrite artifacts that don't exist or are older than the source.
     *
     * @since 2.0
     */
    @Parameter( property = "overWriteIfNewer", defaultValue = "true" )
    protected boolean overWriteIfNewer;

    /**
     * If we should exclude transitive dependencies
     *
     * @since 2.0
     */
    @Parameter( property = "excludeTransitive", defaultValue = "false" )
    protected boolean excludeTransitive;

    /**
     * Comma Separated list of Types to include. Empty String indicates include everything (default).
     *
     * @since 2.0
     */
    @Parameter( property = "includeTypes", defaultValue = "" )
    protected String includeTypes;

    /**
     * Comma Separated list of Types to exclude. Empty String indicates don't exclude anything (default).
     *
     * @since 2.0
     */
    @Parameter( property = "excludeTypes", defaultValue = "" )
    protected String excludeTypes;

    /**
     * Scope to include. An Empty string indicates all scopes (default). The scopes being interpreted are the scopes as
     * Maven sees them, not as specified in the pom. In summary:
     * <ul>
     * <li><code>runtime</code> scope gives runtime and compile dependencies,</li>
     * <li><code>compile</code> scope gives compile, provided, and system dependencies,</li>
     * <li><code>test</code> (default) scope gives all dependencies,</li>
     * <li><code>provided</code> scope just gives provided dependencies,</li>
     * <li><code>system</code> scope just gives system dependencies.</li>
     * </ul>
     *
     * @since 2.0
     */
    @Parameter( property = "includeScope", defaultValue = "" )
    protected String includeScope;

    /**
     * Scope to exclude. An Empty string indicates no scopes (default).
     *
     * @since 2.0
     */
    @Parameter( property = "excludeScope", defaultValue = "" )
    protected String excludeScope;

    /**
     * Comma Separated list of Classifiers to include. Empty String indicates include everything (default).
     *
     * @since 2.0
     */
    @Parameter( property = "includeClassifiers", defaultValue = "" )
    protected String includeClassifiers;

    /**
     * Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything (default).
     *
     * @since 2.0
     */
    @Parameter( property = "excludeClassifiers", defaultValue = "" )
    protected String excludeClassifiers;

    /**
     * Specify classifier to look for. Example: sources
     *
     * @since 2.0
     */
    @Parameter( property = "classifier", defaultValue = "" )
    protected String classifier;

    /**
     * Specify type to look for when constructing artifact based on classifier. Example: java-source,jar,war
     *
     * @since 2.0
     */
    @Parameter( property = "type", defaultValue = "" )
    protected String type;

    /**
     * Comma separated list of Artifact names to exclude.
     *
     * @since 2.0
     */
    @Parameter( property = "excludeArtifactIds", defaultValue = "" )
    protected String excludeArtifactIds;

    /**
     * Comma separated list of Artifact names to include. Empty String indicates include everything (default).
     *
     * @since 2.0
     */
    @Parameter( property = "includeArtifactIds", defaultValue = "" )
    protected String includeArtifactIds;

    /**
     * Comma separated list of GroupId Names to exclude.
     *
     * @since 2.0
     */
    @Parameter( property = "excludeGroupIds", defaultValue = "" )
    protected String excludeGroupIds;

    /**
     * Comma separated list of GroupIds to include. Empty String indicates include everything (default).
     *
     * @since 2.0
     */
    @Parameter( property = "includeGroupIds", defaultValue = "" )
    protected String includeGroupIds;

    /**
     * Directory to store flag files
     *
     * @since 2.0
     */
    //CHECKSTYLE_OFF: LineLength
    @Parameter( property = "markersDirectory", defaultValue = "${project.build.directory}/dependency-maven-plugin-markers" )
    //CHECKSTYLE_ON: LineLength
    protected File markersDirectory;

    /**
     * Prepend the groupId during copy.
     *
     * @since 2.2
     */
    @Parameter( property = "mdep.prependGroupId", defaultValue = "false" )
    protected boolean prependGroupId = false;

    @Component
    private ProjectBuilder projectBuilder;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * Return an {@link ArtifactsFilter} indicating which artifacts must be filtered out.
     *
     * @return an {@link ArtifactsFilter} indicating which artifacts must be filtered out.
     */
    protected abstract ArtifactsFilter getMarkedArtifactFilter();

    /**
     * Retrieves dependencies, either direct only or all including transitive.
     *
     * @param stopOnFailure true to fail if resolution does not work or false not to fail.
     * @return A set of artifacts
     * @throws MojoExecutionException in case of errors.
     */
    protected Set<Artifact> getResolvedDependencies( boolean stopOnFailure )
        throws MojoExecutionException

    {
        DependencyStatusSets status = getDependencySets( stopOnFailure );

        return status.getResolvedDependencies();
    }

    /**
     * @param stopOnFailure true/false.
     * @return {@link DependencyStatusSets}
     * @throws MojoExecutionException in case of an error.
     */
    protected DependencyStatusSets getDependencySets( boolean stopOnFailure )
        throws MojoExecutionException
    {
        return getDependencySets( stopOnFailure, false );
    }

    /**
     * Method creates filters and filters the projects dependencies. This method also transforms the dependencies if
     * classifier is set. The dependencies are filtered in least specific to most specific order
     *
     * @param stopOnFailure true to fail if artifacts can't be resolved false otherwise.
     * @param includeParents <code>true</code> if parents should be included or not <code>false</code>.
     * @return DependencyStatusSets - Bean of TreeSets that contains information on the projects dependencies
     * @throws MojoExecutionException in case of errors.
     */
    protected DependencyStatusSets getDependencySets( boolean stopOnFailure, boolean includeParents )
        throws MojoExecutionException
    {
        // add filters in well known order, least specific to most specific
        FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter( new ProjectTransitivityFilter( getProject().getDependencyArtifacts(),
                                                         this.excludeTransitive ) );

        filter.addFilter( new ScopeFilter( DependencyUtil.cleanToBeTokenizedString( this.includeScope ),
                                           DependencyUtil.cleanToBeTokenizedString( this.excludeScope ) ) );

        filter.addFilter( new TypeFilter( DependencyUtil.cleanToBeTokenizedString( this.includeTypes ),
                                          DependencyUtil.cleanToBeTokenizedString( this.excludeTypes ) ) );

        filter.addFilter( new ClassifierFilter( DependencyUtil.cleanToBeTokenizedString( this.includeClassifiers ),
                                                DependencyUtil.cleanToBeTokenizedString( this.excludeClassifiers ) ) );

        filter.addFilter( new GroupIdFilter( DependencyUtil.cleanToBeTokenizedString( this.includeGroupIds ),
                                             DependencyUtil.cleanToBeTokenizedString( this.excludeGroupIds ) ) );

        filter.addFilter( new ArtifactIdFilter( DependencyUtil.cleanToBeTokenizedString( this.includeArtifactIds ),
                                                DependencyUtil.cleanToBeTokenizedString( this.excludeArtifactIds ) ) );

        // start with all artifacts.
        Set<Artifact> artifacts = getProject().getArtifacts();

        if ( includeParents )
        {
            // add dependencies parents
            for ( Artifact dep : new ArrayList<Artifact>( artifacts ) )
            {
                addParentArtifacts( buildProjectFromArtifact( dep ), artifacts );
            }

            // add current project parent
            addParentArtifacts( getProject(), artifacts );
        }

        // perform filtering
        try
        {
            artifacts = filter.filter( artifacts );
        }
        catch ( ArtifactFilterException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        // transform artifacts if classifier is set
        DependencyStatusSets status;
        if ( StringUtils.isNotEmpty( classifier ) )
        {
            status = getClassifierTranslatedDependencies( artifacts, stopOnFailure );
        }
        else
        {
            status = filterMarkedDependencies( artifacts );
        }

        return status;
    }

    private MavenProject buildProjectFromArtifact( Artifact artifact )
        throws MojoExecutionException
    {
        try
        {
            return projectBuilder.build( artifact, session.getProjectBuildingRequest() ).getProject();
        }
        catch ( ProjectBuildingException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private void addParentArtifacts( MavenProject project, Set<Artifact> artifacts )
        throws MojoExecutionException
    {
        while ( project.hasParent() )
        {
            project = project.getParent();

            if ( artifacts.contains( project.getArtifact() ) )
            {
                // artifact already in the set
                break;
            }
            try
            {
                ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();

                Artifact resolvedArtifact =
                    artifactResolver.resolveArtifact( buildingRequest, project.getArtifact() ).getArtifact();

                artifacts.add( resolvedArtifact );
            }
            catch ( ArtifactResolverException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }
    }

    /**
     * Transform artifacts
     *
     * @param artifacts set of artifacts {@link Artifact}.
     * @param stopOnFailure true/false.
     * @return DependencyStatusSets - Bean of TreeSets that contains information on the projects dependencies
     * @throws MojoExecutionException in case of an error.
     */
    protected DependencyStatusSets getClassifierTranslatedDependencies( Set<Artifact> artifacts, boolean stopOnFailure )
        throws MojoExecutionException
    {
        Set<Artifact> unResolvedArtifacts = new LinkedHashSet<Artifact>();
        Set<Artifact> resolvedArtifacts = artifacts;
        DependencyStatusSets status = new DependencyStatusSets();

        // possibly translate artifacts into a new set of artifacts based on the
        // classifier and type
        // if this did something, we need to resolve the new artifacts
        if ( StringUtils.isNotEmpty( classifier ) )
        {
            ArtifactTranslator translator =
                new ClassifierTypeTranslator( artifactHandlerManager, this.classifier, this.type );
            Collection<ArtifactCoordinate> coordinates = translator.translate( artifacts, getLog() );

            status = filterMarkedDependencies( artifacts );

            // the unskipped artifacts are in the resolved set.
            artifacts = status.getResolvedDependencies();

            // resolve the rest of the artifacts
            resolvedArtifacts = resolve( new LinkedHashSet<ArtifactCoordinate>( coordinates ), stopOnFailure );

            // calculate the artifacts not resolved.
            unResolvedArtifacts.addAll( artifacts );
            unResolvedArtifacts.removeAll( resolvedArtifacts );
        }

        // return a bean of all 3 sets.
        status.setResolvedDependencies( resolvedArtifacts );
        status.setUnResolvedDependencies( unResolvedArtifacts );

        return status;
    }

    /**
     * Filter the marked dependencies
     *
     * @param artifacts The artifacts set {@link Artifact}.
     * @return status set {@link DependencyStatusSets}.
     * @throws MojoExecutionException in case of an error.
     */
    protected DependencyStatusSets filterMarkedDependencies( Set<Artifact> artifacts )
        throws MojoExecutionException
    {
        // remove files that have markers already
        FilterArtifacts filter = new FilterArtifacts();
        filter.clearFilters();
        filter.addFilter( getMarkedArtifactFilter() );

        Set<Artifact> unMarkedArtifacts;
        try
        {
            unMarkedArtifacts = filter.filter( artifacts );
        }
        catch ( ArtifactFilterException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        // calculate the skipped artifacts
        Set<Artifact> skippedArtifacts = new LinkedHashSet<Artifact>();
        skippedArtifacts.addAll( artifacts );
        skippedArtifacts.removeAll( unMarkedArtifacts );

        return new DependencyStatusSets( unMarkedArtifacts, null, skippedArtifacts );
    }

    /**
     * @param coordinates The set of artifact coordinates{@link ArtifactCoordinate}.
     * @param stopOnFailure <code>true</code> if we should fail with exception if an artifact couldn't be resolved
     *            <code>false</code> otherwise.
     * @return the resolved artifacts. {@link Artifact}.
     * @throws MojoExecutionException in case of error.
     */
    protected Set<Artifact> resolve( Set<ArtifactCoordinate> coordinates, boolean stopOnFailure )
        throws MojoExecutionException
    {
        ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();

        Set<Artifact> resolvedArtifacts = new LinkedHashSet<Artifact>();
        for ( ArtifactCoordinate coordinate : coordinates )
        {
            try
            {
                Artifact artifact = artifactResolver.resolveArtifact( buildingRequest, coordinate ).getArtifact();
                resolvedArtifacts.add( artifact );
            }
            catch ( ArtifactResolverException ex )
            {
                // an error occurred during resolution, log it an continue
                getLog().debug( "error resolving: " + coordinate );
                getLog().debug( ex );
                if ( stopOnFailure )
                {
                    throw new MojoExecutionException( "error resolving: " + coordinate, ex );
                }
            }
        }
        return resolvedArtifacts;
    }

    /**
     * @return Returns the markersDirectory.
     */
    public File getMarkersDirectory()
    {
        return this.markersDirectory;
    }

    /**
     * @param theMarkersDirectory The markersDirectory to set.
     */
    public void setMarkersDirectory( File theMarkersDirectory )
    {
        this.markersDirectory = theMarkersDirectory;
    }

    // TODO: Set marker files.

    /**
     * @return true, if the groupId should be prepended to the filename.
     */
    public boolean isPrependGroupId()
    {
        return prependGroupId;
    }

    /**
     * @param prependGroupId - true if the groupId must be prepended during the copy.
     */
    public void setPrependGroupId( boolean prependGroupId )
    {
        this.prependGroupId = prependGroupId;
    }

    protected final ArtifactResolver getArtifactResolver()
    {
        return artifactResolver;
    }

    protected final DependencyResolver getDependencyResolver()
    {
        return dependencyResolver;
    }

    protected final RepositoryManager getRepositoryManager()
    {
        return repositoryManager;
    }
}
