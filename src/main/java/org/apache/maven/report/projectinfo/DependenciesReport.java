package org.apache.maven.report.projectinfo;

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
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.DependenciesReportConfiguration;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependenciesRenderer;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.jar.classes.JarClassesAnalysis;

import java.util.Locale;

/**
 * Generates the Project Dependencies report.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 * @goal dependencies
 * @requiresDependencyResolution test
 */
public class DependenciesReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Maven Project Builder component.
     *
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * Artifact metadata source component.
     *
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * Artifact collector component.
     *
     * @component
     */
    private ArtifactCollector collector;

    /**
     * Wagon manager component.
     *
     * @since 2.1
     * @component
     */
    private WagonManager wagonManager;

    /**
     * Dependency tree builder component.
     *
     * @since 2.1
     * @component
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * Jar classes analyser component.
     *
     * @since 2.1
     * @component
     */
    private JarClassesAnalysis classesAnalyzer;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The current user system settings for use in Maven.
     *
     * @since 2.1
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * Display file details for each dependency, such as: file size, number of
     * classes, number of packages etc.
     *
     * @since 2.1
     * @parameter expression="${dependency.details.enabled}" default-value="true"
     */
    private boolean dependencyDetailsEnabled;

    /**
     * Display the repository locations of the dependencies. Requires Maven 2.0.5+.
     *
     * @since 2.1
     * @parameter expression="${dependency.locations.enabled}" default-value="false"
     */
    private boolean dependencyLocationsEnabled;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.dependencies.name" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.dependencies.description" );
    }

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
    {
        RepositoryUtils repoUtils = new RepositoryUtils( wagonManager, settings, mavenProjectBuilder, factory, resolver,
                                                         project.getRemoteArtifactRepositories(),
                                                         project.getPluginArtifactRepositories(), localRepository );

        DependencyNode dependencyTreeNode = resolveProject();

        Dependencies dependencies = new Dependencies( project, dependencyTreeNode, classesAnalyzer );

        DependenciesReportConfiguration config =
            new DependenciesReportConfiguration( dependencyDetailsEnabled, dependencyLocationsEnabled );

        DependenciesRenderer r =
            new DependenciesRenderer( getSink(), locale, i18n, dependencies, dependencyTreeNode, config, repoUtils );

        repoUtils.setLog( getLog() );
        r.setLog( getLog() );
        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "dependencies";
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private DependencyNode resolveProject()
    {
        try
        {
            ArtifactFilter artifactFilter = new ScopeArtifactFilter( Artifact.SCOPE_TEST );
            return dependencyTreeBuilder.buildDependencyTree( project, localRepository, factory,
                                                              artifactMetadataSource, artifactFilter, collector );
        }
        catch ( DependencyTreeBuilderException e )
        {
            getLog().error( "Unable to build dependency tree.", e );
            return null;
        }
    }
}
