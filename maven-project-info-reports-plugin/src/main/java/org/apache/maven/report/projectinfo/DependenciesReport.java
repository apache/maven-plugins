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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
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
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;
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
    implements Contextualizable
{
    /** Images resources dir */
    private static final String RESOURCES_DIR = "org/apache/maven/report/projectinfo/resources";

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
     * Jar classes analyzer component.
     *
     * @since 2.1
     * @component
     */
    private JarClassesAnalysis classesAnalyzer;

    /**
     * Repository metadata component.
     *
     * @since 2.1
     * @component
     */
    private RepositoryMetadataManager repositoryMetadataManager;

    /**
     * Maven Artifact Factory component.
     *
     * @component
     * @since 2.1
     */
    private ArtifactFactory artifactFactory;

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
     * Remote repositories used for the project.
     *
     * @since 2.1
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List remoteRepositories;

    /**
     * Display file details for each dependency, such as: file size, number of
     * classes, number of packages etc.
     *
     * @since 2.1
     * @parameter expression="${dependency.details.enabled}" default-value="true"
     */
    private boolean dependencyDetailsEnabled;

    /**
     * Display the repository locations of the dependencies. If Maven is configured to be offline, this parameter
     * will be ignored.
     *
     * @since 2.1
     * @parameter expression="${dependency.locations.enabled}" default-value="true"
     */
    private boolean dependencyLocationsEnabled;

    /**
     * Plexus container to play with logger manager.
     *
     * @since 2.1
     */
    private PlexusContainer container;

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
        if ( settings.isOffline() && dependencyLocationsEnabled )
        {
            getLog().warn( "The parameter 'dependencyLocationsEnabled' is ignored in offline mode." );
            dependencyLocationsEnabled = false;
        }

        try
        {
            copyResources( new File( getOutputDirectory() ) );
        }
        catch ( IOException e )
        {
            getLog().error( "Cannot copy ressources", e );
        }

        RepositoryUtils repoUtils = new RepositoryUtils( getLog(), container.getLoggerManager(), wagonManager,
                                                         settings, mavenProjectBuilder, factory, resolver, project
                                                             .getRemoteArtifactRepositories(), project
                                                             .getPluginArtifactRepositories(), localRepository,
                                                         repositoryMetadataManager );

        DependencyNode dependencyTreeNode = resolveProject();

        Dependencies dependencies = new Dependencies( project, dependencyTreeNode, classesAnalyzer );

        DependenciesReportConfiguration config = new DependenciesReportConfiguration( dependencyDetailsEnabled,
                                                                                      dependencyLocationsEnabled );

        DependenciesRenderer r =
            new DependenciesRenderer( getSink(), locale, i18n, getLog(), settings, dependencies,
                                      dependencyTreeNode, config, repoUtils, artifactFactory,
                                      mavenProjectBuilder, remoteRepositories, localRepository );
        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "dependencies";
    }

    /** {@inheritDoc} */
    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @return resolve the dependency tree
     */
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

    /**
     * @param outputDirectory the wanted output directory
     * @throws IOException if any
     */
    private void copyResources( File outputDirectory )
        throws IOException
    {
        InputStream resourceList = getClass().getClassLoader().getResourceAsStream( RESOURCES_DIR + "/resources.txt" );

        if ( resourceList != null )
        {
            LineNumberReader reader = new LineNumberReader( new InputStreamReader( resourceList, ReaderFactory.US_ASCII ) );

            String line = reader.readLine();

            while ( line != null )
            {
                InputStream is = getClass().getClassLoader().getResourceAsStream( RESOURCES_DIR + "/" + line );

                if ( is == null )
                {
                    throw new IOException( "The resource " + line + " doesn't exist." );
                }

                File outputFile = new File( outputDirectory, line );

                if ( !outputFile.getParentFile().exists() )
                {
                    outputFile.getParentFile().mkdirs();
                }

                FileOutputStream w = new FileOutputStream( outputFile );

                IOUtil.copy( is, w );

                IOUtil.close( is );

                IOUtil.close( w );

                line = reader.readLine();
            }
        }
    }
}
