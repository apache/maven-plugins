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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.doxia.parser.Parser;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.DependenciesReportConfiguration;
import org.apache.maven.report.projectinfo.dependencies.JarDependencyDetails;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTree;
import org.apache.maven.shared.jar.JarAnalyzerException;
import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DependenciesRenderer
    extends AbstractMavenReportRenderer
{
    private final Locale locale;

    static HashSet JAR_SUBTYPE = new HashSet();

    static
    {
        JAR_SUBTYPE.add( "jar" );
        JAR_SUBTYPE.add( "war" );
        JAR_SUBTYPE.add( "ear" );
        JAR_SUBTYPE.add( "sar" );
        JAR_SUBTYPE.add( "rar" );
        JAR_SUBTYPE.add( "par" );
        JAR_SUBTYPE.add( "ejb" );
    }

    private final DependencyTree dependencyTree;

    private Dependencies dependencies;

    private DependenciesReportConfiguration configuration;

    private I18N i18n;

    private Log log;

    private RepositoryUtils repoUtils;

    /**
     * Will be filled with license name / list of projects.
     */
    private Map licenseMap = new HashMap()
    {
        public Object put( Object key, Object value )
        {
            // handle multiple values as a list
            List valueList = (List) get( key );
            if ( valueList == null )
            {
                valueList = new ArrayList();
            }
            valueList.add( value );
            return super.put( key, valueList );
        }
    };

    public DependenciesRenderer( Sink sink, Locale _locale, I18N _i18n, Dependencies _dependencies,
                                 DependencyTree _depTree, DependenciesReportConfiguration _config,
                                 RepositoryUtils _repoUtils )
    {
        super( sink );

        this.locale = _locale;

        this.dependencyTree = _depTree;

        this.repoUtils = _repoUtils;

        this.dependencies = _dependencies;

        this.i18n = _i18n;

        this.configuration = _config;
    }

    public void setLog( Log _log )
    {
        log = _log;
    }

    public String getTitle()
    {
        return getReportString( "report.dependencies.title" );
    }

    public void renderBody()
    {
        // Dependencies report

        if ( !dependencies.hasDependencies() )
        {
            startSection( getTitle() );

            // TODO: should the report just be excluded?
            paragraph( getReportString( "report.dependencies.nolist" ) );

            endSection();

            return;
        }

        // === Section: Project Dependencies.
        renderSectionProjectDependencies();

        // === Section: Project Transitive Dependencies.
        renderSectionProjectTransitiveDependencies();

        // === Section: Project Dependency Graph.
        renderSectionProjectDependencyGraph();

        // === Section: Licenses
        renderSectionDependencyLicenseListing();

        if ( configuration.getDependencyDetailsEnabled() )
        {
            // === Section: Dependency File Details.
            renderSectionDependencyFileDetails();
        }

        if ( configuration.getDependencyLocationsEnabled() )
        {
            // this reports requires Wagon 1.0-beta-2 and will not work with maven <= 2.0.4
            // since we don't want to make maven 2.0.5 a requirements for the whole project info report plugin
            // let's do a check here

            // org.apache.maven.wagon.Wagon.resourceExists(Ljava/lang/String;)Z
            try
            {
                Wagon.class.getDeclaredMethod( "resourceExists", new Class[] { String.class } );
            }
            catch ( NoSuchMethodException e )
            {
                log.warn( "Dependency Locations report will not be anabled: it requires wagon 1.0-beta-2" );
                return;
            }

            // === Section: Dependency Repository Locations.
            renderSectionDependencyRepositoryLocations();
        }
    }

    private String[] getDependencyTableHeader()
    {
        String groupId = getReportString( "report.dependencies.column.groupId" );
        String artifactId = getReportString( "report.dependencies.column.artifactId" );
        String version = getReportString( "report.dependencies.column.version" );
        String classifier = getReportString( "report.dependencies.column.classifier" );
        String type = getReportString( "report.dependencies.column.type" );
        String optional = getReportString( "report.dependencies.column.optional" );
        return new String[] { groupId, artifactId, version, classifier, type, optional };
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

    private void renderSectionProjectTransitiveDependencies()
    {
        List artifacts = dependencies.getTransitiveDependencies();

        startSection( getReportString( "report.transitivedependencies.title" ) );

        if ( artifacts.isEmpty() )
        {
            paragraph( getReportString( "report.transitivedependencies.nolist" ) );
        }
        else
        {
            String[] tableHeader = getDependencyTableHeader();
            Map dependenciesByScope = dependencies.getDependenciesByScope();

            paragraph( getReportString( "report.transitivedependencies.intro" ) );

            renderDependenciesForAllScopes( tableHeader, dependenciesByScope );
        }

        endSection();
    }

    private void renderSectionProjectDependencyGraph()
    {
        startSection( getReportString( "report.dependencies.graph.title" ) );

        // === Section: Dependency Tree
        renderSectionDependencyTree();

        // === Section: Dependency Listings
        renderSectionDependencyListing();

        endSection();
    }

    private void renderSectionDependencyTree()
    {
        // for Dependencies Graph Tree
        startSection( getReportString( "report.dependencies.graph.tree.title" ) );
        sink.paragraph();
        sink.list();
        printDependencyListing( dependencyTree.getRootNode() );
        sink.list_();
        sink.paragraph_();
        endSection();
    }

    private void renderSectionDependencyFileDetails()
    {
        startSection( getReportString( "report.dependencies.file.details.title" ) );

        List alldeps = dependencies.getAllDependencies();
        Collections.sort( alldeps, getArtifactComparator() );

        startTable();
        String filename = getReportString( "report.dependencies.file.details.column.file" );
        String size = getReportString( "report.dependencies.file.details.column.size" );
        String entries = getReportString( "report.dependencies.file.details.column.entries" );
        String classes = getReportString( "report.dependencies.file.details.column.classes" );
        String packages = getReportString( "report.dependencies.file.details.column.packages" );
        String jdkrev = getReportString( "report.dependencies.file.details.column.jdkrev" );
        String debug = getReportString( "report.dependencies.file.details.column.debug" );
        String sealed = getReportString( "report.dependencies.file.details.column.sealed" );

        String[] tableHeader = new String[] { filename, size, entries, classes, packages, jdkrev, debug, sealed };
        tableHeader( tableHeader );

        int[] justification = new int[] {
            Parser.JUSTIFY_LEFT,
            Parser.JUSTIFY_RIGHT,
            Parser.JUSTIFY_RIGHT,
            Parser.JUSTIFY_RIGHT,
            Parser.JUSTIFY_RIGHT,
            Parser.JUSTIFY_CENTER,
            Parser.JUSTIFY_CENTER,
            Parser.JUSTIFY_CENTER };
        sink.tableRows( justification, true );

        int totaldeps = 0;
        long totaldepsize = 0;
        int totalentries = 0;
        int totalclasses = 0;
        int totalpackages = 0;
        double highestjdk = 0.0;
        int totaldebug = 0;
        int totalsealed = 0;

        DecimalFormat decFormat = new DecimalFormat( "#,##0" );

        for ( Iterator it = alldeps.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            if ( !Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                // TODO site:run Why do we need to resolve this...
                if ( artifact.getFile() == null )
                {
                    try
                    {
                        repoUtils.resolve( artifact );
                    }
                    catch ( ArtifactResolutionException e )
                    {
                        log.error( "Artifact: " + artifact.getId() + " has no file.", e );
                        continue;
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        log.error( "Artifact: " + artifact.getId() + " has no file.", e );
                        continue;
                    }
                }

                if ( artifact.getFile() == null )
                {
                    log.error( "Artifact: " + artifact.getId() + " has no file." );
                    continue;
                }

                File artifactFile = artifact.getFile();

                totaldeps++;
                totaldepsize += artifactFile.length();

                if ( JAR_SUBTYPE.contains( artifact.getType().toLowerCase() ) )
                {
                    try
                    {
                        JarDependencyDetails jarDetails = dependencies.getJarDependencyDetails( artifact );

                        String debugstr = "release";
                        if ( jarDetails.isDebugPresent() )
                        {
                            debugstr = "debug";
                            totaldebug++;
                        }

                        String sealedstr = "";
                        if ( jarDetails.isSealed() )
                        {
                            sealedstr = "sealed";
                            totalsealed++;
                        }

                        totalentries += jarDetails.getEntries();
                        totalclasses += jarDetails.getClassSize();
                        totalpackages += jarDetails.getPackageSize();

                        try
                        {
                            highestjdk = Math.max( highestjdk, Double.parseDouble( jarDetails.getJdkRevision() ) );
                        }
                        catch ( NumberFormatException e )
                        {
                            // ignore
                        }

                        tableRow( new String[] {
                            artifactFile.getName(),
                            decFormat.format( artifactFile.length() ),
                            decFormat.format( jarDetails.getEntries() ),
                            decFormat.format( jarDetails.getClassSize() ),
                            decFormat.format( jarDetails.getPackageSize() ),
                            jarDetails.getJdkRevision(),
                            debugstr,
                            sealedstr } );
                    }
                    catch ( JarAnalyzerException e )
                    {
                        createExceptionInfoTableRow( artifact, artifactFile, e );
                    }
                }
                else
                {
                    tableRow( new String[] {
                        artifactFile.getName(),
                        decFormat.format( artifactFile.length() ),
                        "",
                        "",
                        "",
                        "",
                        "",
                        "" } );
                }
            }
        }

        tableHeader[0] = "Total";
        tableHeader( tableHeader );
        tableRow( new String[] {
            "" + totaldeps + " total dependencies",
            decFormat.format( totaldepsize ),
            decFormat.format( totalentries ),
            decFormat.format( totalclasses ),
            decFormat.format( totalpackages ),
            String.valueOf( highestjdk ),
            decFormat.format( totaldebug ),
            decFormat.format( totalsealed ) } );

        sink.tableRows_();

        endTable();
        endSection();
    }

    private void createExceptionInfoTableRow( Artifact artifact, File artifactFile, Exception e )
    {
        tableRow( new String[] { artifact.getId(), artifactFile.getAbsolutePath(), e.getMessage(), "", "", "", "", "" } );
    }

    private void populateRepositoryMap( Map repos, List rawRepos )
    {
        Iterator it = rawRepos.iterator();
        while ( it.hasNext() )
        {
            ArtifactRepository repo = (ArtifactRepository) it.next();
            repos.put( repo.getId(), repo );
        }
    }

    private void renderSectionDependencyRepositoryLocations()
    {
        startSection( getReportString( "report.dependencies.repo.locations.title" ) );

        // Collect Alphabetical Dependencies
        List alldeps = dependencies.getAllDependencies();
        Collections.sort( alldeps, getArtifactComparator() );

        // Collect Repositories
        Map repoMap = new HashMap();

        populateRepositoryMap( repoMap, repoUtils.getRemoteArtifactRepositories() );

        for ( Iterator it = alldeps.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            try
            {
                MavenProject artifactProject = repoUtils.getMavenProjectFromRepository( artifact, repoUtils
                    .getLocalRepository() );

                populateRepositoryMap( repoMap, artifactProject.getRemoteArtifactRepositories() );
            }
            catch ( ProjectBuildingException e )
            {
                log.warn( "Unable to create maven project from repository.", e );
            }
        }

        // Render Repository List

        startTable();
        String repoid = getReportString( "report.dependencies.repo.locations.column.repoid" );
        String url = getReportString( "report.dependencies.repo.locations.column.url" );
        String release = getReportString( "report.dependencies.repo.locations.column.release" );
        String snapshot = getReportString( "report.dependencies.repo.locations.column.snapshot" );

        String[] tableHeader = new String[] { repoid, url, release, snapshot };
        tableHeader( tableHeader );

        String releaseEnabled = getReportString( "report.dependencies.repo.locations.cell.release.enabled" );
        String releaseDisabled = getReportString( "report.dependencies.repo.locations.cell.release.disabled" );

        String snapshotEnabled = getReportString( "report.dependencies.repo.locations.cell.snapshot.enabled" );
        String snapshotDisabled = getReportString( "report.dependencies.repo.locations.cell.snapshot.disabled" );

        for ( Iterator it = repoMap.keySet().iterator(); it.hasNext(); )
        {
            String key = (String) it.next();
            ArtifactRepository repo = (ArtifactRepository) repoMap.get( key );

            sink.tableRow();
            tableCell( repo.getId() );

            sink.tableCell();
            sink.link( repo.getUrl() );
            sink.text( repo.getUrl() );
            sink.link_();
            sink.tableCell_();

            ArtifactRepositoryPolicy releasePolicy = repo.getReleases();
            tableCell( releasePolicy.isEnabled() ? releaseEnabled : releaseDisabled );

            ArtifactRepositoryPolicy snapshotPolicy = repo.getSnapshots();
            tableCell( snapshotPolicy.isEnabled() ? snapshotEnabled : snapshotDisabled );
            sink.tableRow_();
        }

        endTable();

        // Render Aritfact Breakdown.

        sink.paragraph();
        sink.text( getReportString( "report.dependencies.repo.locations.artifact.breakdown" ) );
        sink.paragraph_();

        List repoIdList = new ArrayList( repoMap.keySet() );

        startTable();
        String artifact = getReportString( "report.dependencies.repo.locations.column.artifact" );
        tableHeader = new String[repoIdList.size() + 1];
        tableHeader[0] = artifact;
        int idnum = 1;

        for ( Iterator it = repoIdList.iterator(); it.hasNext(); )
        {
            String id = (String) it.next();
            tableHeader[idnum++] = id;
        }

        tableHeader( tableHeader );

        for ( Iterator it = alldeps.iterator(); it.hasNext(); )
        {
            Artifact dependency = (Artifact) it.next();

            if ( !Artifact.SCOPE_SYSTEM.equals( dependency.getScope() ) )
            {
                sink.tableRow();

                tableCell( dependency.getId() );

                for ( Iterator itrepo = repoIdList.iterator(); itrepo.hasNext(); )
                {
                    String repokey = (String) itrepo.next();
                    ArtifactRepository repo = (ArtifactRepository) repoMap.get( repokey );

                    String depUrl = repo.getUrl() + "/" + repo.pathOf( dependency );

                    boolean dependencyExists = false;

                    // check snapshots in snapshots repository only and releases in release repositories...
                    if ( ( dependency.isSnapshot() && repo.getSnapshots().isEnabled() )
                        || ( !dependency.isSnapshot() && repo.getReleases().isEnabled() ) )
                    {
                        dependencyExists = repoUtils.dependencyExistsInRepo( repo, dependency );
                    }

                    if ( dependencyExists )
                    {
                        sink.tableCell();
                        sink.link( depUrl );

                        sink.figure();
                        sink.figureCaption();
                        sink.text( "Found at " + repo.getUrl() );
                        sink.figureCaption_();
                        sink.figureGraphics( "images/icon_success_sml.gif" );
                        sink.figure_();

                        sink.link_();
                        sink.tableCell_();
                    }
                    else
                    {
                        sink.tableCell();
                        sink.text( "-" );
                        sink.tableCell_();
                    }
                }

                sink.tableRow_();
            }
        }

        endTable();

        endSection();
    }

    private void renderSectionDependencyListing()
    {
        startSection( getReportString( "report.dependencies.graph.tables.title" ) );
        printDescriptionsAndURLs( dependencyTree.getRootNode() );
        endSection();
    }

    private void renderSectionDependencyLicenseListing()
    {
        startSection( getReportString( "report.dependencies.graph.tables.licenses" ) );
        printGroupedLicenses();
        endSection();
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
        return new String[] {
            artifact.getGroupId(),
            artifact.getArtifactId(),
            artifact.getVersion(),
            artifact.getClassifier(),
            artifact.getType(),
            artifact.isOptional() ? "(optional)" : " " };
    }

    private void printDependencyListing( DependencyNode node )
    {
        Artifact artifact = node.getArtifact();
        String id = artifact.getDependencyConflictId();

        sink.listItem();
        sink.paragraph();

        sink.link( "#" + id );
        sink.text( id );
        sink.link_();

        if ( !node.getChildren().isEmpty() )
        {
            sink.list();
            for ( Iterator deps = node.getChildren().iterator(); deps.hasNext(); )
            {
                DependencyNode dep = (DependencyNode) deps.next();
                printDependencyListing( dep );
            }
            sink.list_();
        }

        sink.paragraph_();
        sink.listItem_();
    }

    private void printDescriptionsAndURLs( DependencyNode node )
    {
        Artifact artifact = node.getArtifact();
        String id = artifact.getDependencyConflictId();

        String unknownLicenseMessage = getReportString( "report.dependencies.graph.tables.unknown" );

        if ( !Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            try
            {
                MavenProject artifactProject = repoUtils.getMavenProjectFromRepository( artifact, repoUtils
                    .getLocalRepository() );
                String artifactDescription = artifactProject.getDescription();
                String artifactUrl = artifactProject.getUrl();
                String artifactName = artifactProject.getName();
                List licenses = artifactProject.getLicenses();

                sink.paragraph();
                sink.anchor( id );
                // startSection( artifactName );
                sink.bold();
                sink.text( artifactName );
                sink.bold_();
                sink.anchor_();
                sink.paragraph_();

                if ( artifactDescription != null )
                {
                    sink.paragraph();
                    sink.text( artifactDescription );
                    sink.paragraph_();
                }

                if ( artifactUrl != null )
                {
                    sink.paragraph();
                    sink.link( artifactUrl );
                    sink.text( artifactUrl );
                    sink.link_();
                    sink.paragraph_();
                }

                sink.paragraph();
                sink.text( getReportString( "report.license.title" ) + ": " );
                if ( !licenses.isEmpty() )
                {
                    for ( Iterator iter = licenses.iterator(); iter.hasNext(); )
                    {
                        License element = (License) iter.next();
                        String licenseName = element.getName();
                        String licenseUrl = element.getUrl();

                        if ( licenseUrl != null )
                        {
                            sink.link( licenseUrl );
                        }
                        sink.text( licenseName );

                        if ( licenseUrl != null )
                        {
                            sink.link_();
                        }

                        licenseMap.put( licenseName, artifactName );

                    }
                }
                else
                {
                    sink.text( getReportString( "report.license.nolicense" ) );

                    licenseMap.put( unknownLicenseMessage, artifactName );

                }
                sink.paragraph_();

                // endSection();
                sink.horizontalRule();
            }
            catch ( ProjectBuildingException e )
            {
                log.error( "ProjectBuildException error : ", e );
            }

            for ( Iterator deps = node.getChildren().iterator(); deps.hasNext(); )
            {
                DependencyNode dep = (DependencyNode) deps.next();
                printDescriptionsAndURLs( dep );
            }
        }
        else
        {
            sink.paragraph();
            sink.anchor( id );
            sink.bold();
            sink.text( id );
            sink.bold_();
            sink.anchor_();
            sink.paragraph_();

            sink.paragraph();
            sink.text( artifact.getFile().toString() );
            sink.paragraph_();
        }
    }

    private void printGroupedLicenses()
    {
        for ( Iterator iter = licenseMap.keySet().iterator(); iter.hasNext(); )
        {
            String licenseName = (String) iter.next();
            sink.paragraph();
            sink.bold();
            if ( StringUtils.isEmpty( licenseName ) )
            {
                sink.text( i18n.getString( "project-info-report", locale, "report.dependencies.unamed" ) );
            }
            else
            {
                sink.text( licenseName );
            }
            sink.text( ": " );
            sink.bold_();

            List projects = (List) licenseMap.get( licenseName );
            Collections.sort( projects );

            for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
            {
                String projectName = (String) iterator.next();
                sink.text( projectName );
                if ( iterator.hasNext() )
                {
                    sink.text( "," );
                }
                sink.text( " " );
            }

            sink.paragraph_();
        }
    }

    private String getReportString( String key )
    {
        return i18n.getString( "project-info-report", locale, key );
    }
}
