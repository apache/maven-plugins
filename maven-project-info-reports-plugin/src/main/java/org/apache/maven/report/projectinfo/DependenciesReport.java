package org.apache.maven.report.projectinfo;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.i18n.I18N;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Generates the Project Dependencies report.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal dependencies
 * @plexus.component
 */
public class DependenciesReport
    extends AbstractMavenReport
{
    /**
     * Report output directory.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * Doxia Site Renderer.
     *
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Maven ArtifactFactory.
     *
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * Maven Project Builder.
     *
     * @parameter expression="${component.org.apache.maven.project.MavenProjectBuilder}"
     * @required
     * @readonly
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * Local Repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Internationalization.
     *
     * @component
     */
    private I18N i18n;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.dependencies.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.dependencies.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
    {
        DependenciesRenderer r = new DependenciesRenderer( getSink(), getProject(), i18n, locale, mavenProjectBuilder,
                                                           artifactFactory, localRepository );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "dependencies";
    }

    static class DependenciesRenderer
        extends AbstractMavenReportRenderer
    {
        private MavenProject project;

        private Locale locale;

        private ArtifactFactory artifactFactory;

        private MavenProjectBuilder mavenProjectBuilder;

        private ArtifactRepository localRepository;

        private I18N i18n;

        public DependenciesRenderer( Sink sink, MavenProject project, I18N i18n, Locale locale,
                                     MavenProjectBuilder mavenProjectBuilder, ArtifactFactory artifactFactory,
                                     ArtifactRepository localRepository )
        {
            super( sink );

            this.project = project;

            this.i18n = i18n;

            this.locale = locale;

            this.mavenProjectBuilder = mavenProjectBuilder;

            this.artifactFactory = artifactFactory;

            this.localRepository = localRepository;
        }

        public String getTitle()
        {
            return i18n.getString( "project-info-report", locale, "report.dependencies.title" );
        }

        public void renderBody()
        {
            // Dependencies report
            Set dependencies = project.getDependencyArtifacts();

            if ( dependencies == null || dependencies.isEmpty() )
            {
                startSection( getTitle() );

                // TODO: should the report just be excluded?
                paragraph( i18n.getString( "project-info-report", locale, "report.dependencies.nolist" ) );

                endSection();

                return;
            }

            startSection( getTitle() );

            startTable();

            tableCaption( i18n.getString( "project-info-report", locale, "report.dependencies.intro" ) );

            String groupId = i18n.getString( "project-info-report", locale, "report.dependencies.column.groupId" );
            String artifactId = i18n.getString( "project-info-report", locale, "report.dependencies.column.artifactId" );
            String version = i18n.getString( "project-info-report", locale, "report.dependencies.column.version" );
            String description = i18n.getString( "project-info-report", locale, "report.dependencies.column.description" );
            String url = i18n.getString( "project-info-report", locale, "report.dependencies.column.url" );

            tableHeader( new String[]{groupId, artifactId, version, description, url} );

            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                MavenProject artifactProject;
                try
                {
                    // TODO: can we use @requiresDependencyResolution instead, and capture the depth of artifacts in the artifact itself?
                    artifactProject = getMavenProjectFromRepository( artifact, localRepository );
                }
                catch ( ProjectBuildingException e )
                {
                    throw new IllegalArgumentException(
                        "Can't find a valid Maven project in the repository for the artifact [" +
                            artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() +
                            "]." );
                }

                tableRow( new String[]{artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                    artifactProject.getDescription(),
                    createLinkPatternedText( artifactProject.getUrl(), artifactProject.getUrl() )} );
            }

            endTable();

            endSection();

            // Transitive dependencies
            Set artifacts = getTransitiveDependencies( project );

            startSection( i18n.getString( "project-info-report", locale, "report.transitivedependencies.title" ) );

            if ( artifacts.isEmpty() )
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.transitivedependencies.nolist" ) );
            }
            else
            {
                startTable();

                tableCaption( i18n.getString( "project-info-report", locale, "report.transitivedependencies.intro" ) );

                tableHeader( new String[]{groupId, artifactId, version, description, url} );

                for ( Iterator i = artifacts.iterator(); i.hasNext(); )
                {
                    Artifact artifact = (Artifact) i.next();
                    
                    /* MNG-1663, ignore system dependencies */
                    if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
                    {
                        continue;
                    }

                    MavenProject artifactProject;
                    try
                    {
                        // TODO: can we use @requiresDependencyResolution instead, and capture the depth of artifacts in the artifact itself?
                        artifactProject = getMavenProjectFromRepository( artifact, localRepository );
                    }
                    catch ( ProjectBuildingException e )
                    {
                        // TODO: better exception handling needed - log PBE
                        throw new IllegalArgumentException(
                            "Can't find a valid Maven project in the repository for the artifact [" +
                                artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() +
                                "]." );
                    }
                    tableRow( new String[]{artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                        artifactProject.getDescription(),
                        createLinkPatternedText( artifactProject.getUrl(), artifactProject.getUrl() )} );
                }

                endTable();
            }

            endSection();
        }

        /**
         * Return a set of <code>Artifacts</code> which are not already
         * present in the dependencies list.
         *
         * @param project a Maven project
         * @return a set of transitive dependencies as artifacts
         */
        private Set getTransitiveDependencies( MavenProject project )
        {
            Set transitiveDependencies = new HashSet();

            Set dependencies = project.getDependencyArtifacts();
            Set artifacts = project.getArtifacts();

            if ( dependencies == null || artifacts == null )
            {
                return transitiveDependencies;
            }

            for ( Iterator j = artifacts.iterator(); j.hasNext(); )
            {
                Artifact artifact = (Artifact) j.next();

                if ( !dependencies.contains( artifact ) )
                {
                    transitiveDependencies.add( artifact );
                }
            }

            return transitiveDependencies;
        }

        /**
         * Get the <code>Maven project</code> from the repository depending
         * the <code>Artifact</code> given.
         *
         * @param artifact an artifact
         * @return the Maven project for the given artifact
         * @throws org.apache.maven.project.ProjectBuildingException if any
         */
        private MavenProject getMavenProjectFromRepository( Artifact artifact, ArtifactRepository localRepository )
            throws ProjectBuildingException
        {
            Artifact projectArtifact = artifact;

            boolean allowStubModel = false;
            if ( !"pom".equals( artifact.getType() ) )
            {
                projectArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(),
                                                                         artifact.getArtifactId(),
                                                                         artifact.getVersion(), artifact.getScope() );
                allowStubModel = true;
            }

            // TODO: we should use the MavenMetadataSource instead
            return mavenProjectBuilder.buildFromRepository( projectArtifact, project.getRemoteArtifactRepositories(),
                                                            localRepository, allowStubModel );
        }
    }
}