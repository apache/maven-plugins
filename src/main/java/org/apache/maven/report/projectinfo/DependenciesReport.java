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
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.DependenciesReportConfiguration;
import org.apache.maven.report.projectinfo.dependencies.ReportResolutionListener;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.report.projectinfo.dependencies.renderer.DependenciesRenderer;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.graphing.GraphRenderer;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

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
    implements Contextualizable
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
     * @parameter expression="${dependency.locations.enabled}" default-value="false"
     */
    private boolean dependencyLocationsEnabled;
    
    /**
     * @parameter expression="${graphing.implementation}" default-value="graphviz"
     */
    private String graphingImpl;

    private PlexusContainer container;

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
        GraphRenderer graphRenderer;
        
        try
        {
            graphRenderer = (GraphRenderer) container.lookup( GraphRenderer.ROLE, graphingImpl );
        }
        catch ( ComponentLookupException e )
        {
            graphRenderer = null;
            getLog().error( "GraphRenderer implementation [" + graphingImpl + "] does not exist.", e );
        }
        
        RepositoryUtils repoUtils = new RepositoryUtils( wagonManager, settings, mavenProjectBuilder, factory, resolver,
                                                         project.getRemoteArtifactRepositories(),
                                                         project.getPluginArtifactRepositories(), localRepository );

        ReportResolutionListener listener = resolveProject();

        Dependencies dependencies = new Dependencies( project, listener, container );

        DependenciesReportConfiguration config =
            new DependenciesReportConfiguration( dependencyDetailsEnabled, dependencyLocationsEnabled );

        DependenciesRenderer r = new DependenciesRenderer( getSink(), locale, i18n, dependencies, listener, config,
                                                           repoUtils, graphRenderer, outputDirectory );

        repoUtils.setLog( getLog() );
        r.setLog( getLog() );
        r.render();
    }

    private ReportResolutionListener resolveProject()
    {
        Map managedVersions = null;
        try
        {
            managedVersions = Dependencies.getManagedVersionMap( project, factory );
        }
        catch ( ProjectBuildingException e )
        {
            getLog().error( "An error occurred while resolving project dependencies.", e );
        }

        ReportResolutionListener listener = new ReportResolutionListener();

        try
        {
            // TODO site:run Why do we need to resolve this...
            if ( project.getDependencyArtifacts() == null )
            {
                project.setDependencyArtifacts( project.createArtifacts( factory, null, null ) );
            }
            collector.collect( project.getDependencyArtifacts(), project.getArtifact(), managedVersions,
                               localRepository, project.getRemoteArtifactRepositories(), artifactMetadataSource, null,
                               Collections.singletonList( listener ) );
        }
        catch ( ArtifactResolutionException e )
        {
            getLog().error( "An error occurred while resolving project dependencies.", e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            getLog().error( "An error occurred while resolving project dependencies.", e );
        }

        return listener;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "dependencies";
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
