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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.report.projectinfo.dependencies.ManagementDependencies;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;

/**
 * @author Nick Stolwijk
 * @version $Id$
 * @since 2.1
 */
public class DependencyManagementRenderer
    extends AbstractMavenReportRenderer
{
    private ManagementDependencies dependencies;

    private final Locale locale;

    private I18N i18n;

    private Log log;

    public DependencyManagementRenderer( Sink sink, Locale locale, I18N i18n, ManagementDependencies dependencies )
    {
        super( sink );

        this.locale = locale;

        this.dependencies = dependencies;

        this.i18n = i18n;

    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public void setLog( Log log )
    {
        this.log = log;
    }

    /** {@inheritDoc} */
    public String getTitle()
    {
        return getReportString( "report.dependencyManagement.title" );
    }

    /** {@inheritDoc} */
    public void renderBody()
    {
        // Dependencies report

        if ( !dependencies.hasDependencies() )
        {
            startSection( getTitle() );

            // TODO: should the report just be excluded?
            paragraph( getReportString( "report.dependencyManagement.nolist" ) );

            endSection();

            return;
        }

        // === Section: Project Dependencies.
        renderSectionProjectDependencies();
    }

    private void renderSectionProjectDependencies()
    {
        String[] tableHeader = getDependencyTableHeader();

        startSection( getTitle() );

        // collect dependencies by scope
        Map dependenciesByScope = dependencies.getDependenciesByScope();

        renderDependenciesForAllScopes( tableHeader, dependenciesByScope );

        endSection();
    }

    private void renderDependenciesForAllScopes( String[] tableHeader, Map dependenciesByScope )
    {
        renderDependenciesForScope( Artifact.SCOPE_COMPILE, (List) dependenciesByScope.get( Artifact.SCOPE_COMPILE ),
                                    tableHeader );
        renderDependenciesForScope( Artifact.SCOPE_RUNTIME, (List) dependenciesByScope.get( Artifact.SCOPE_RUNTIME ),
                                    tableHeader );
        renderDependenciesForScope( Artifact.SCOPE_TEST, (List) dependenciesByScope.get( Artifact.SCOPE_TEST ),
                                    tableHeader );
        renderDependenciesForScope( Artifact.SCOPE_PROVIDED, (List) dependenciesByScope.get( Artifact.SCOPE_PROVIDED ),
                                    tableHeader );
        renderDependenciesForScope( Artifact.SCOPE_SYSTEM, (List) dependenciesByScope.get( Artifact.SCOPE_SYSTEM ),
                                    tableHeader );
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private String[] getDependencyTableHeader()
    {
        String groupId = getReportString( "report.dependencyManagement.column.groupId" );
        String artifactId = getReportString( "report.dependencyManagement.column.artifactId" );
        String version = getReportString( "report.dependencyManagement.column.version" );
        String classifier = getReportString( "report.dependencyManagement.column.classifier" );
        String type = getReportString( "report.dependencyManagement.column.type" );

        return new String[] { groupId, artifactId, version, classifier, type };
    }

    private void renderDependenciesForScope( String scope, List artifacts, String[] tableHeader )
    {
        if ( artifacts != null )
        {
            // can't use straight artifact comparison because we want optional last
            Collections.sort( artifacts, getDependencyComparator() );

            startSection( scope );

            paragraph( getReportString( "report.dependencyManagement.intro." + scope ) );
            startTable();
            tableHeader( tableHeader );

            for ( Iterator iterator = artifacts.iterator(); iterator.hasNext(); )
            {
                Dependency dependency = (Dependency) iterator.next();
                tableRow( getDependencyRow( dependency ) );
            }
            endTable();

            endSection();
        }
    }

    private String[] getDependencyRow( Dependency dependency )
    {
        return new String[] { dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
            dependency.getClassifier(), dependency.getType() };
    }

    private Comparator getDependencyComparator()
    {
        return new Comparator()
        {
            public int compare( Object o1, Object o2 )
            {
                Dependency a1 = (Dependency) o1;
                Dependency a2 = (Dependency) o2;

                int result = a1.getGroupId().compareTo( a2.getGroupId() );
                if ( result == 0 )
                {
                    result = a1.getArtifactId().compareTo( a2.getArtifactId() );
                    if ( result == 0 )
                    {
                        result = a1.getType().compareTo( a2.getType() );
                        if ( result == 0 )
                        {
                            if ( a1.getClassifier() == null )
                            {
                                if ( a2.getClassifier() != null )
                                {
                                    result = 1;
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
                                    result = -1;
                                }
                            }
                            if ( result == 0 )
                            {
                                // We don't consider the version range in the comparison, just the resolved version
                                result = a1.getVersion().compareTo( a2.getVersion() );
                            }
                        }
                    }
                }
                return result;
            }
        };
    }

    private String getReportString( String key )
    {
        return i18n.getString( "project-info-report", locale, key );
    }
}
