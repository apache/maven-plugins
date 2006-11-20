package org.apache.maven.report.projectinfo;

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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.DependenciesReportConfiguration;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependenciesRenderer;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.dependency.tree.DependencyTree;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.jar.JarAnalyzerFactory;

import java.util.Locale;

/**
 * Generates the Project Dependencies report.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal dependencies
 * @requiresDependencyResolution test
 */
public class DependenciesReport
    extends AbstractProjectInfoReport
{
    /**
     * Maven Project Builder.
     *
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     */
    private ArtifactCollector collector;

    /**
     * @component
     */
    private WagonManager wagonManager;
    
    /**
     * @component
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * @component
     */
    private JarAnalyzerFactory jarAnalyzerFactory;
    
    /**
     * The current user system settings for use in Maven.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * @parameter expression="${dependency.details.enabled}" default-value="true"
     */
    private boolean dependencyDetailsEnabled;

    /**
     * Display the repository locations of the dependencies. Requires Maven 2.0.5+.
     * @parameter expression="${dependency.locations.enabled}" default-value="false"
     */
    private boolean dependencyLocationsEnabled;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.dependencies.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.dependencies.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
    {
        RepositoryUtils repoUtils = new RepositoryUtils( wagonManager, settings, mavenProjectBuilder, factory, resolver,
                                                         project.getRemoteArtifactRepositories(),
                                                         project.getPluginArtifactRepositories(), localRepository );

        DependencyTree dependencyTree = resolveProject();

        Dependencies dependencies = new Dependencies( project, dependencyTree, jarAnalyzerFactory );

        DependenciesReportConfiguration config =
            new DependenciesReportConfiguration( dependencyDetailsEnabled, dependencyLocationsEnabled );

        DependenciesRenderer r =
            new DependenciesRenderer( getSink(), locale, i18n, dependencies, dependencyTree, config, repoUtils );

        repoUtils.setLog( getLog() );
        r.setLog( getLog() );
        r.render();
    }

    private DependencyTree resolveProject()
    {
        try
        {
            return dependencyTreeBuilder.buildDependencyTree( project, localRepository, factory,
                                                              artifactMetadataSource, collector );
        }
        catch ( DependencyTreeBuilderException e )
        {
            getLog().error( "Unable to build dependency tree.", e );
            return null;
        } 
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "dependencies";
    }
}
