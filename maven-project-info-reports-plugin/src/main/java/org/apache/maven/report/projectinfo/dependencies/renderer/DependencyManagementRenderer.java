package org.apache.maven.report.projectinfo.dependencies.renderer;

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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.report.projectinfo.AbstractProjectInfoRenderer;
import org.apache.maven.report.projectinfo.ProjectInfoReportUtils;
import org.apache.maven.report.projectinfo.dependencies.ManagementDependencies;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Nick Stolwijk
 * @version $Id$
 * @since 2.1
 */
public class DependencyManagementRenderer
    extends AbstractProjectInfoRenderer
{
    private final ManagementDependencies dependencies;

    private final Log log;

    private final ArtifactMetadataSource artifactMetadataSource;

    private final ArtifactFactory artifactFactory;

    private final MavenProjectBuilder mavenProjectBuilder;

    private final List<ArtifactRepository> remoteRepositories;

    private final ArtifactRepository localRepository;

    private final RepositoryUtils repoUtils;

    /**
     * Default constructor
     *
     * @param sink
     * @param locale
     * @param i18n
     * @param log
     * @param artifactFactory
     * @param dependencies
     * @param mavenProjectBuilder
     * @param remoteRepositories
     * @param localRepository
     * @param repoUtils
     */
    public DependencyManagementRenderer( Sink sink, Locale locale, I18N i18n, Log log,
                                         ManagementDependencies dependencies,
                                         ArtifactMetadataSource artifactMetadataSource,
                                         ArtifactFactory artifactFactory, MavenProjectBuilder mavenProjectBuilder,
                                         List<ArtifactRepository> remoteRepositories,
                                         ArtifactRepository localRepository, RepositoryUtils repoUtils )
    {
        super( sink, i18n, locale );

        this.log = log;
        this.dependencies = dependencies;
        this.artifactMetadataSource = artifactMetadataSource;
        this.artifactFactory = artifactFactory;
        this.mavenProjectBuilder = mavenProjectBuilder;
        this.remoteRepositories = remoteRepositories;
        this.localRepository = localRepository;
        this.repoUtils = repoUtils;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    protected String getI18Nsection()
    {
        return "dependencyManagement";
    }

    @Override
    public void renderBody()
    {
        // Dependencies report

        if ( !dependencies.hasDependencies() )
        {
            startSection( getTitle() );

            paragraph( getI18nString( "nolist" ) );

            endSection();

            return;
        }

        // === Section: Project Dependencies.
        renderSectionProjectDependencies();
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private void renderSectionProjectDependencies()
    {
        startSection( getTitle() );

        // collect dependencies by scope
        Map<String, List<Dependency>> dependenciesByScope = dependencies.getManagementDependenciesByScope();

        renderDependenciesForAllScopes( dependenciesByScope );

        endSection();
    }

    private void renderDependenciesForAllScopes( Map<String, List<Dependency>> dependenciesByScope )
    {
        renderDependenciesForScope( Artifact.SCOPE_COMPILE, dependenciesByScope.get( Artifact.SCOPE_COMPILE ) );
        renderDependenciesForScope( Artifact.SCOPE_RUNTIME, dependenciesByScope.get( Artifact.SCOPE_RUNTIME ) );
        renderDependenciesForScope( Artifact.SCOPE_TEST, dependenciesByScope.get( Artifact.SCOPE_TEST ) );
        renderDependenciesForScope( Artifact.SCOPE_PROVIDED, dependenciesByScope.get( Artifact.SCOPE_PROVIDED ) );
        renderDependenciesForScope( Artifact.SCOPE_SYSTEM, dependenciesByScope.get( Artifact.SCOPE_SYSTEM ) );
    }

    private String[] getDependencyTableHeader( boolean hasClassifier )
    {
        String groupId = getI18nString( "column.groupId" );
        String artifactId = getI18nString( "column.artifactId" );
        String version = getI18nString( "column.version" );
        String classifier = getI18nString( "column.classifier" );
        String type = getI18nString( "column.type" );
        String license = getI18nString( "column.license" );

        if ( hasClassifier )
        {
            return new String[] { groupId, artifactId, version, classifier, type, license };
        }

        return new String[] { groupId, artifactId, version, type, license };
    }

    private void renderDependenciesForScope( String scope, List<Dependency> artifacts )
    {
        if ( artifacts != null )
        {
            // can't use straight artifact comparison because we want optional last
            Collections.sort( artifacts, getDependencyComparator() );

            startSection( scope );

            paragraph( getI18nString( "intro." + scope ) );
            startTable();

            boolean hasClassifier = false;
            for ( Dependency dependency : artifacts )
            {
                if ( StringUtils.isNotEmpty( dependency.getClassifier() ) )
                {
                    hasClassifier = true;
                    break;
                }
            }

            String[] tableHeader = getDependencyTableHeader( hasClassifier );
            tableHeader( tableHeader );

            for ( Dependency dependency : artifacts )
            {
                tableRow( getDependencyRow( dependency, hasClassifier ) );
            }
            endTable();

            endSection();
        }
    }

    @SuppressWarnings( "unchecked" )
    private String[] getDependencyRow( Dependency dependency, boolean hasClassifier )
    {
        Artifact artifact =
            artifactFactory.createProjectArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                   dependency.getVersion() );

        StringBuffer licensesBuffer = new StringBuffer();
        String url = null;
        try
        {
            List<ArtifactVersion> versions =
                artifactMetadataSource.retrieveAvailableVersions( artifact, localRepository, remoteRepositories );

            if ( versions.size() > 0 )
            {
                Collections.sort( versions );
                
                artifact.setVersion( versions.get( versions.size() - 1 ).toString() );
            }

            url =
                ProjectInfoReportUtils.getArtifactUrl( artifactFactory, artifact, mavenProjectBuilder,
                                                       remoteRepositories, localRepository );

            MavenProject artifactProject = repoUtils.getMavenProjectFromRepository( artifact );

            List<License> licenses = artifactProject.getLicenses();
            for ( License license : licenses )
            {
                String licenseCell = ProjectInfoReportUtils.getArtifactIdCell( license.getName(), license.getUrl() );
                if ( licensesBuffer.length() > 0 )
                {
                    licensesBuffer.append( ", " );
                }
                licensesBuffer.append( licenseCell );
            }
        }
        catch ( ArtifactMetadataRetrievalException e )
        {
            log.warn( "Unable to retrieve versions for " + artifact.getId() + " from repository.", e );
        }
        catch ( ProjectBuildingException e )
        {
            log.warn( "Unable to create Maven project for " + artifact.getId() + " from repository.", e );
        }

        String artifactIdCell = ProjectInfoReportUtils.getArtifactIdCell( artifact.getArtifactId(), url );

        if ( hasClassifier )
        {
            return new String[] { dependency.getGroupId(), artifactIdCell, dependency.getVersion(),
                dependency.getClassifier(), dependency.getType(), licensesBuffer.toString() };
        }

        return new String[] { dependency.getGroupId(), artifactIdCell, dependency.getVersion(),
            dependency.getType(), licensesBuffer.toString() };
    }

    private Comparator<Dependency> getDependencyComparator()
    {
        return new Comparator<Dependency>()
        {
            public int compare( Dependency a1, Dependency a2 )
            {
                int result = a1.getGroupId().compareTo( a2.getGroupId() );
                if ( result != 0 )
                {
                    return result;
                }

                result = a1.getArtifactId().compareTo( a2.getArtifactId() );
                if ( result != 0 )
                {
                    return result;
                }

                result = a1.getType().compareTo( a2.getType() );
                if ( result != 0 )
                {
                    return result;
                }

                if ( a1.getClassifier() == null )
                {
                    if ( a2.getClassifier() != null )
                    {
                        return 1;
                    }
                }
                else
                {
                    if ( a2.getClassifier() != null )
                    {
                        result = a1.getClassifier().compareTo( a2.getClassifier() );
                    }
                    else
                    {
                        return -1;
                    }
                }

                if ( result != 0 )
                {
                    return result;
                }

                // We don't consider the version range in the comparison, just the resolved version
                return a1.getVersion().compareTo( a2.getVersion() );
            }
        };
    }
}
