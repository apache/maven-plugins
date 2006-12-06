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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactsFilter;
import org.apache.maven.plugin.dependency.utils.filters.ClassifierFilter;
import org.apache.maven.plugin.dependency.utils.filters.FilterArtifacts;
import org.apache.maven.plugin.dependency.utils.filters.ScopeFilter;
import org.apache.maven.plugin.dependency.utils.filters.TransitivityFilter;
import org.apache.maven.plugin.dependency.utils.filters.TypeFilter;
import org.apache.maven.plugin.dependency.utils.resolvers.ArtifactsResolver;
import org.apache.maven.plugin.dependency.utils.resolvers.DefaultArtifactsResolver;
import org.apache.maven.plugin.dependency.utils.translators.ArtifactTranslator;
import org.apache.maven.plugin.dependency.utils.translators.ClassifierTypeTranslator;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author brianf
 * 
 */
public abstract class AbstractDependencyFilterMojo
    extends AbstractDependencyMojo
{
    /**
     * If we should exclude transitive dependencies
     * 
     * @since 2.0
     * @optional
     * @parameter expression="${excludeTransitive}" default-value="false"
     */
    protected boolean excludeTransitive;

    /**
     * Comma Separated list of Types to include. Empty String indicates include
     * everything (default).
     * 
     * @since 2.0
     * @parameter expression="${includeTypes}" default-value=""
     * @optional
     */
    protected String includeTypes;

    /**
     * Comma Separated list of Types to exclude. Empty String indicates don't
     * exclude anything (default). Ignored if includeTypes is used.
     * 
     * @since 2.0
     * @parameter expression="${excludeTypes}" default-value=""
     * @optional
     */
    protected String excludeTypes;

    /**
     * Scope to include. An Empty string indicates all scopes (default).
     * 
     * @since 2.0
     * @parameter expression="${includeScope}" default-value=""
     * @optional
     */
    protected String includeScope;

    /**
     * Scope to exclude. An Empty string indicates no scopes (default).
     * 
     * @since 2.0
     * @parameter expression="${excludeScope}" default-value=""
     * @optional
     */
    protected String excludeScope;

    /**
     * Comma Separated list of Classifiers to include. Empty String indicates
     * include everything (default).
     * 
     * @since 2.0
     * @parameter expression="${includeClassifiers}" default-value=""
     * @optional
     */
    protected String includeClassifiers;

    /**
     * Comma Separated list of Classifiers to exclude. Empty String indicates
     * don't exclude anything (default). Ignored if includeClassifiers is used.
     * 
     * @since 2.0
     * @parameter expression="${excludeClassifiers}" default-value=""
     * @optional
     */
    protected String excludeClassifiers;

    /**
     * Specify classifier to look for. Example: sources
     * 
     * @optional
     * @since 2.0
     * @parameter expression="${classifier}" default-value=""
     */
    protected String classifier;

    /**
     * Specify type to look for when constructing artifact based on classifier.
     * Example: java-source,jar,war
     * 
     * @optional
     * @since 2.0
     * @parameter expression="${type}" default-value="java-source"
     */
    protected String type;

    /**
     * Directory to store flag files
     * 
     * @parameter expression="${markersDirectory}"
     *            default-value="${project.build.directory}/dependency-maven-plugin-markers"
     * @optional
     * @since 1.0
     */
    protected File markersDirectory;

    /**
     * Overwrite release artifacts
     * 
     * @optional
     * @since 1.0
     * @parameter expression="${overWriteReleases}" default-value="false"
     */
    protected boolean overWriteReleases;

    /**
     * Overwrite snapshot artifacts
     * 
     * @optional
     * @since 1.0
     * @parameter expression="${overWriteSnapshots}" default-value="false"
     */
    protected boolean overWriteSnapshots;

    /**
     * Overwrite artifacts that don't exist or are older than the source.
     * 
     * @optional
     * @since 2.0
     * @parameter expression="${overWriteIfNewer}" default-value="true"
     */
    protected boolean overWriteIfNewer;

    protected abstract ArtifactsFilter getMarkedArtifactFilter();

    /**
     * Retrieves dependencies, either direct only or all including transitive.
     * 
     * @return A HashSet of artifacts
     * @throws MojoExecutionException
     *             if an error occured.
     */
    protected Set getDependencies( boolean stopOnFailure )
        throws MojoExecutionException
    {
        DependencyStatusSets status = getDependencySets( stopOnFailure );

        return status.getResolvedDependencies();
    }

    protected DependencyStatusSets getDependencySets( boolean stopOnFailure )
        throws MojoExecutionException
    {
        // add filters in well known order, least specific to most specific
        FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter( new TransitivityFilter( project.getDependencyArtifacts(), this.excludeTransitive ) );
        filter.addFilter( new ScopeFilter( this.includeScope, this.excludeScope ) );
        filter.addFilter( new TypeFilter( this.includeTypes, this.excludeTypes ) );
        filter.addFilter( new ClassifierFilter( this.includeClassifiers, this.excludeClassifiers ) );

        // start with all artifacts.
        Set artifacts = project.getArtifacts();

        // perform filtering
        artifacts = filter.filter( artifacts, getLog() );

        // transform artifacts if classifier is set
        DependencyStatusSets status = null;
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

    protected DependencyStatusSets getClassifierTranslatedDependencies( Set artifacts, boolean stopOnFailure )
        throws MojoExecutionException
    {
        Set unResolvedArtifacts = new HashSet();
        Set resolvedArtifacts = artifacts;
        DependencyStatusSets status = new DependencyStatusSets();

        // possibly translate artifacts into a new set of artifacts based on the
        // classifier and type
        // if this did something, we need to resolve the new artifacts
        if ( StringUtils.isNotEmpty( classifier ) )
        {
            ArtifactTranslator translator = new ClassifierTypeTranslator( this.classifier, this.type, this.factory );
            artifacts = translator.translate( artifacts, getLog() );

            status = filterMarkedDependencies( artifacts );

            // the unskipped artifacts are in the resolved set.
            artifacts = status.getResolvedDependencies();

            // resolve the rest of the artifacts
            ArtifactsResolver artifactsResolver = new DefaultArtifactsResolver( this.resolver, this.local,
                                                                                this.remoteRepos, stopOnFailure );
            resolvedArtifacts = artifactsResolver.resolve( artifacts, getLog() );

            // calculate the artifacts not resolved.
            unResolvedArtifacts.addAll( artifacts );
            unResolvedArtifacts.removeAll( resolvedArtifacts );
        }

        // return a bean of all 3 sets.
        status.setResolvedDependencies( resolvedArtifacts );
        status.setUnResolvedDependencies( unResolvedArtifacts );

        return status;
    }

    protected DependencyStatusSets filterMarkedDependencies( Set artifacts )
        throws MojoExecutionException
    {
        // remove files that have markers already
        FilterArtifacts filter = new FilterArtifacts();
        filter.clearFilters();
        filter.addFilter( getMarkedArtifactFilter() );

        Set unMarkedArtifacts = filter.filter( artifacts, getLog() );

        // calculate the skipped artifacts
        Set skippedArtifacts = new HashSet();
        skippedArtifacts.addAll( artifacts );
        skippedArtifacts.removeAll( unMarkedArtifacts );

        return new DependencyStatusSets( unMarkedArtifacts, null, skippedArtifacts );
    }

    // TODO: Set marker files.
}
