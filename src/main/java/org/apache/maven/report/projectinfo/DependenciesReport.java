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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.AbstractMavenReportRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
        ReportResolutionListener listener = resolveProject();

        DependenciesRenderer r = new DependenciesRenderer( getSink(), locale, listener );

        r.render();
    }

    private ReportResolutionListener resolveProject()
    {
        Map managedVersions = null;
        try
        {
            managedVersions = createManagedVersionMap( project.getId(), project.getDependencyManagement() );
        }
        catch ( ProjectBuildingException e )
        {
            getLog().error( "An error occurred while resolving project dependencies.", e );
        }

        ReportResolutionListener listener = new ReportResolutionListener();

        try
        {
            collector.collect( project.getDependencyArtifacts(), project.getArtifact(), managedVersions,
                               localRepository, project.getRemoteArtifactRepositories(), artifactMetadataSource, null,
                               Collections.singletonList( listener ) );
        }
        catch ( ArtifactResolutionException e )
        {
            getLog().error( "An error occurred while resolving project dependencies.", e );
        }

        return listener;
    }

    private Map createManagedVersionMap( String projectId, DependencyManagement dependencyManagement )
        throws ProjectBuildingException
    {
        Map map;
        if ( dependencyManagement != null && dependencyManagement.getDependencies() != null )
        {
            map = new HashMap();
            for ( Iterator i = dependencyManagement.getDependencies().iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                try
                {
                    VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
                    Artifact artifact = factory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                          versionRange, d.getType(), d.getClassifier(),
                                                                          d.getScope() );
                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new ProjectBuildingException( projectId, "Unable to parse version '" + d.getVersion() +
                        "' for dependency '" + d.getManagementKey() + "': " + e.getMessage(), e );
                }
            }
        }
        else
        {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "dependencies";
    }

    private class DependenciesRenderer
        extends AbstractMavenReportRenderer
    {
        private final Locale locale;

        private final ReportResolutionListener listener;

        DependenciesRenderer( Sink sink, Locale locale, ReportResolutionListener listener )
        {
            super( sink );

            this.locale = locale;

            this.listener = listener;
        }

        public String getTitle()
        {
            return getReportString( "report.dependencies.title" );
        }

        public void renderBody()
        {
            // Dependencies report
            List dependencies = listener.getRootNode().getChildren();

            if ( dependencies.isEmpty() )
            {
                startSection( getTitle() );

                // TODO: should the report just be excluded?
                paragraph( getReportString( "report.dependencies.nolist" ) );

                endSection();

                return;
            }

            startSection( getTitle() );

            String groupId = getReportString( "report.dependencies.column.groupId" );
            String artifactId = getReportString( "report.dependencies.column.artifactId" );
            String version = getReportString( "report.dependencies.column.version" );
            String classifier = getReportString( "report.dependencies.column.classifier" );
            String type = getReportString( "report.dependencies.column.type" );
            String optional = getReportString( "report.dependencies.column.optional" );
            String[] tableHeader = new String[]{groupId, artifactId, version, classifier, type, optional};

            // collect dependencies by scope
            Map dependenciesByScope = getDependenciesByScope( dependencies );

            renderDependenciesForScope( Artifact.SCOPE_COMPILE,
                                        (List) dependenciesByScope.get( Artifact.SCOPE_COMPILE ), tableHeader );
            renderDependenciesForScope( Artifact.SCOPE_RUNTIME,
                                        (List) dependenciesByScope.get( Artifact.SCOPE_RUNTIME ), tableHeader );
            renderDependenciesForScope( Artifact.SCOPE_TEST, (List) dependenciesByScope.get( Artifact.SCOPE_TEST ),
                                        tableHeader );
            renderDependenciesForScope( Artifact.SCOPE_PROVIDED,
                                        (List) dependenciesByScope.get( Artifact.SCOPE_PROVIDED ), tableHeader );
            renderDependenciesForScope( Artifact.SCOPE_SYSTEM, (List) dependenciesByScope.get( Artifact.SCOPE_SYSTEM ),
                                        tableHeader );

            endSection();

            // Transitive dependencies
            List artifacts = new ArrayList( listener.getArtifacts() );
            artifacts.removeAll( dependencies );

            startSection( getReportString( "report.transitivedependencies.title" ) );

            if ( artifacts.isEmpty() )
            {
                paragraph( getReportString( "report.transitivedependencies.nolist" ) );
            }
            else
            {
                paragraph( getReportString( "report.transitivedependencies.intro" ) );

                dependenciesByScope = getDependenciesByScope( artifacts );

                renderDependenciesForScope( Artifact.SCOPE_COMPILE,
                                            (List) dependenciesByScope.get( Artifact.SCOPE_COMPILE ), tableHeader );
                renderDependenciesForScope( Artifact.SCOPE_RUNTIME,
                                            (List) dependenciesByScope.get( Artifact.SCOPE_RUNTIME ), tableHeader );
                renderDependenciesForScope( Artifact.SCOPE_TEST, (List) dependenciesByScope.get( Artifact.SCOPE_TEST ),
                                            tableHeader );
                renderDependenciesForScope( Artifact.SCOPE_PROVIDED,
                                            (List) dependenciesByScope.get( Artifact.SCOPE_PROVIDED ), tableHeader );
                renderDependenciesForScope( Artifact.SCOPE_SYSTEM,
                                            (List) dependenciesByScope.get( Artifact.SCOPE_SYSTEM ), tableHeader );
            }

            endSection();

            //for Dependencies Graph
            startSection( getReportString( "report.dependencies.graph.title" ) );

            //for Dependencies Graph Tree
            startSection( getReportString( "report.dependencies.graph.tree.title" ) );
            printDependencyListing( listener.getRootNode() );
            endSection();

            //for Artifact Descriptions / URLs
            startSection( getReportString( "report.dependencies.graph.tables.title" ) );
            printDescriptionsAndURLs( listener.getRootNode() );
            endSection();

            endSection();
        }

        private Map getDependenciesByScope( List dependencies )
        {
            Map dependenciesByScope = new HashMap();
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                ReportResolutionListener.Node node = (ReportResolutionListener.Node) i.next();
                Artifact artifact = node.getArtifact();

                List multiValue = (List) dependenciesByScope.get( artifact.getScope() );
                if ( multiValue == null )
                {
                    multiValue = new ArrayList();
                }
                multiValue.add( artifact );
                dependenciesByScope.put( artifact.getScope(), multiValue );
            }
            return dependenciesByScope;
        }

        private void renderDependenciesForScope( String scope, List artifacts, String[] tableHeader )
        {
            if ( artifacts != null )
            {
                // can't use straight artifact comparison because we want optional last
                Collections.sort( artifacts, getArtifactComparator() );

                startSection( scope );

                paragraph( getReportString( "report.dependencies.intro." + scope ) );
                startTable();
                tableHeader( tableHeader );

                for ( Iterator iterator = artifacts.iterator(); iterator.hasNext(); )
                {
                    Artifact artifact = (Artifact) iterator.next();
                    tableRow( getArtifactRow( artifact ) );
                }
                endTable();

                endSection();
            }
        }

        private Comparator getArtifactComparator()
        {
            return new Comparator()
            {
                public int compare( Object o1, Object o2 )
                {
                    Artifact a1 = (Artifact) o1;
                    Artifact a2 = (Artifact) o2;

                    // put optional last
                    if ( a1.isOptional() && !a2.isOptional() )
                    {
                        return +1;
                    }
                    else if ( !a1.isOptional() && a2.isOptional() )
                    {
                        return -1;
                    }
                    else
                    {
                        return a1.compareTo( a2 );
                    }
                }
            };
        }

        private String[] getArtifactRow( Artifact artifact )
        {
            return new String[]{artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                artifact.getClassifier(), artifact.getType(), artifact.isOptional() ? "(optional)" : " "};
        }

        private void printDependencyListing( ReportResolutionListener.Node node )
        {
            Artifact artifact = node.getArtifact();
            String id = artifact.getDependencyConflictId();

            sink.list();
            sink.listItem();

            sink.link( "#" + id );
            sink.text( id );
            sink.link_();

            for ( Iterator deps = node.getChildren().iterator(); deps.hasNext(); )
            {
                ReportResolutionListener.Node dep = (ReportResolutionListener.Node) deps.next();
                printDependencyListing( dep );
            }

            sink.listItem_();
            sink.list_();
        }

        private void printDescriptionsAndURLs( ReportResolutionListener.Node node )
        {
            Artifact artifact = node.getArtifact();
            String id = artifact.getDependencyConflictId();

            String artifactDescription = null;
            String artifactUrl = null;
            try
            {
                MavenProject artifactProject = getMavenProjectFromRepository( artifact, localRepository );
                artifactDescription = artifactProject.getDescription();
                artifactUrl = artifactProject.getUrl();
            }
            catch ( ProjectBuildingException e )
            {
                getLog().debug( e );
            }
            if ( artifactDescription == null )

            {
                artifactDescription = getReportString( "report.dependencies.graph.description.default" );
            }

            if ( artifactUrl == null )
            {
                artifactUrl = getReportString( "report.dependencies.graph.url.default" );
            }

            sink.anchor( id );
            startSection( id );
            sink.anchor_();

            sink.paragraph();
            sink.bold();
            sink.text( getReportString( "report.dependencies.column.description" ) );
            sink.bold_();
            sink.lineBreak();
            sink.text( artifactDescription );
            sink.paragraph_();

            sink.paragraph();
            sink.bold();
            sink.text( getReportString( "report.dependencies.column.url" ) );
            sink.bold_();
            sink.lineBreak();

            if ( artifactUrl != null && artifactUrl.startsWith( "http://" ) )
            {
                sink.link( artifactUrl );
                sink.text( artifactUrl );
                sink.link_();
            }
            else
            {
                sink.text( artifactUrl );
            }
            sink.paragraph_();

            endSection();

            for ( Iterator deps = node.getChildren().iterator(); deps.hasNext(); )
            {
                ReportResolutionListener.Node dep = (ReportResolutionListener.Node) deps.next();
                printDescriptionsAndURLs( dep );
            }
        }

        /**
         * Get the <code>Maven project</code> from the repository depending
         * the <code>Artifact</code> given.
         *
         * @param artifact an artifact
         * @return the Maven project for the given artifact
         * @throws org.apache.maven.project.ProjectBuildingException
         *          if any
         */
        private MavenProject getMavenProjectFromRepository( Artifact artifact, ArtifactRepository localRepository )
            throws ProjectBuildingException
        {
            Artifact projectArtifact = artifact;

            boolean allowStubModel = false;
            if ( !"pom".equals( artifact.getType() ) )
            {
                projectArtifact = factory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                                 artifact.getVersion(), artifact.getScope() );
                allowStubModel = true;
            }

            // TODO: we should use the MavenMetadataSource instead
            return mavenProjectBuilder.buildFromRepository( projectArtifact, project.getRemoteArtifactRepositories(),
                                                            localRepository, allowStubModel );
        }

        private String getReportString( String key )
        {
            return i18n.getString( "project-info-report", locale, key );
        }

    }

}
