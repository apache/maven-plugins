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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.util.HtmlTools;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.report.projectinfo.AbstractProjectInfoRenderer;
import org.apache.maven.report.projectinfo.ProjectInfoReportUtils;
import org.apache.maven.report.projectinfo.dependencies.Dependencies;
import org.apache.maven.report.projectinfo.dependencies.DependenciesReportConfiguration;
import org.apache.maven.report.projectinfo.dependencies.RepositoryUtils;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.jar.JarData;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

/**
 * Renderer the dependencies report.
 *
 * @version $Id$
 * @since 2.1
 */
public class DependenciesRenderer
    extends AbstractProjectInfoRenderer
{
    /** URL for the 'icon_info_sml.gif' image */
    private static final String IMG_INFO_URL = "./images/icon_info_sml.gif";

    /** URL for the 'close.gif' image */
    private static final String IMG_CLOSE_URL = "./images/close.gif";

    /** Random used to generate a UID */
    private static final SecureRandom RANDOM;

    /** Used to format decimal values in the "Dependency File Details" table */
    protected static final DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat( "#,##0" );

    private static final Set<String> JAR_SUBTYPE = new HashSet<String>();

    /**
     * An HTML script tag with the Javascript used by the dependencies report.
     */
    private static final String JAVASCRIPT;

    private final DependencyNode dependencyTreeNode;

    private final Dependencies dependencies;

    private final DependenciesReportConfiguration configuration;

    private final Log log;

    private final Settings settings;

    private final RepositoryUtils repoUtils;

    /** Used to format file length values */
    private final DecimalFormat fileLengthDecimalFormat;

    /**
     * @since 2.1.1
     */
    private int section;

    /**
     * Will be filled with license name / set of projects.
     */
    private Map<String, Object> licenseMap = new HashMap<String, Object>()
    {
        private static final long serialVersionUID = 1L;

        /** {@inheritDoc} */
        public Object put( String key, Object value )
        {
            // handle multiple values as a set to avoid duplicates
            @SuppressWarnings( "unchecked" )
            SortedSet<Object> valueList = (SortedSet<Object>) get( key );
            if ( valueList == null )
            {
                valueList = new TreeSet<Object>();
            }
            valueList.add( value );
            return super.put( key, valueList );
        }
    };

    private final ArtifactFactory artifactFactory;

    private final MavenProjectBuilder mavenProjectBuilder;

    private final List<ArtifactRepository> remoteRepositories;

    private final ArtifactRepository localRepository;

    static
    {
        JAR_SUBTYPE.add( "jar" );
        JAR_SUBTYPE.add( "war" );
        JAR_SUBTYPE.add( "ear" );
        JAR_SUBTYPE.add( "sar" );
        JAR_SUBTYPE.add( "rar" );
        JAR_SUBTYPE.add( "par" );
        JAR_SUBTYPE.add( "ejb" );

        try
        {
            RANDOM = SecureRandom.getInstance( "SHA1PRNG" );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RuntimeException( e );
        }

        StringBuffer sb = new StringBuffer();
        sb.append( "<script language=\"javascript\" type=\"text/javascript\">" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "      function toggleDependencyDetail( divId, imgId )" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "      {" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "        var div = document.getElementById( divId );" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "        var img = document.getElementById( imgId );" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "        if( div.style.display == '' )" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "        {" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "          div.style.display = 'none';" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "          img.src='" + IMG_INFO_URL + "';" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "        }" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "        else" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "        {" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "          div.style.display = '';" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "          img.src='" + IMG_CLOSE_URL + "';" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "        }" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "      }" ).append( SystemUtils.LINE_SEPARATOR );
        sb.append( "</script>" ).append( SystemUtils.LINE_SEPARATOR );
        JAVASCRIPT = sb.toString();
    }

    /**
     * Default constructor.
     *
     * @param sink
     * @param locale
     * @param i18n
     * @param log
     * @param settings
     * @param dependencies
     * @param dependencyTreeNode
     * @param config
     * @param repoUtils
     * @param artifactFactory
     * @param mavenProjectBuilder
     * @param remoteRepositories
     * @param localRepository
     */
    public DependenciesRenderer( Sink sink, Locale locale, I18N i18n, Log log, Settings settings,
                                 Dependencies dependencies, DependencyNode dependencyTreeNode,
                                 DependenciesReportConfiguration config, RepositoryUtils repoUtils,
                                 ArtifactFactory artifactFactory, MavenProjectBuilder mavenProjectBuilder,
                                 List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
    {
        super( sink, i18n, locale );

        this.log = log;
        this.settings = settings;
        this.dependencies = dependencies;
        this.dependencyTreeNode = dependencyTreeNode;
        this.repoUtils = repoUtils;
        this.configuration = config;
        this.artifactFactory = artifactFactory;
        this.mavenProjectBuilder = mavenProjectBuilder;
        this.remoteRepositories = remoteRepositories;
        this.localRepository = localRepository;

        // Using the right set of symbols depending of the locale
        DEFAULT_DECIMAL_FORMAT.setDecimalFormatSymbols( new DecimalFormatSymbols( locale ) );

        this.fileLengthDecimalFormat = new FileDecimalFormat( i18n, locale );
        this.fileLengthDecimalFormat.setDecimalFormatSymbols( new DecimalFormatSymbols( locale ) );
    }

    @Override
    protected String getI18Nsection()
    {
        return "dependencies";
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void renderBody()
    {
        // Dependencies report

        if ( !dependencies.hasDependencies() )
        {
            startSection( getTitle() );

            // TODO: should the report just be excluded?
            paragraph( getI18nString( "nolist" ) );

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
            // === Section: Dependency Repository Locations.
            renderSectionDependencyRepositoryLocations();
        }
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    // workaround for MPIR-140
    // TODO Remove me when maven-reporting-impl:2.1-SNAPSHOT is out
    protected void startSection( String name )
    {
        startSection( name, name );
    }

    /**
     * Start section with a name and a specific anchor.
     *
     * @param anchor not null
     * @param name not null
     */
    protected void startSection( String anchor, String name )
    {
        section = section + 1;

        super.sink.anchor( HtmlTools.encodeId( anchor ) );
        super.sink.anchor_();

        switch ( section )
        {
            case 1:
                sink.section1();
                sink.sectionTitle1();
                break;
            case 2:
                sink.section2();
                sink.sectionTitle2();
                break;
            case 3:
                sink.section3();
                sink.sectionTitle3();
                break;
            case 4:
                sink.section4();
                sink.sectionTitle4();
                break;
            case 5:
                sink.section5();
                sink.sectionTitle5();
                break;

            default:
                // TODO: warning - just don't start a section
                break;
        }

        text( name );

        switch ( section )
        {
            case 1:
                sink.sectionTitle1_();
                break;
            case 2:
                sink.sectionTitle2_();
                break;
            case 3:
                sink.sectionTitle3_();
                break;
            case 4:
                sink.sectionTitle4_();
                break;
            case 5:
                sink.sectionTitle5_();
                break;

            default:
                // TODO: warning - just don't start a section
                break;
        }
    }

    /** {@inheritDoc} */
    // workaround for MPIR-140
    // TODO Remove me when maven-reporting-impl:2.1-SNAPSHOT is out
    protected void endSection()
    {
        switch ( section )
        {
            case 1:
                sink.section1_();
                break;
            case 2:
                sink.section2_();
                break;
            case 3:
                sink.section3_();
                break;
            case 4:
                sink.section4_();
                break;
            case 5:
                sink.section5_();
                break;

            default:
                // TODO: warning - just don't start a section
                break;
        }

        section = section - 1;

        if ( section < 0 )
        {
            throw new IllegalStateException( "Too many closing sections" );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @param withClassifier <code>true</code> to include the classifier column, <code>false</code> otherwise.
     * @param withOptional <code>true</code> to include the optional column, <code>false</code> otherwise.
     * @return the dependency table header with/without classifier/optional column
     * @see #renderArtifactRow(Artifact, boolean, boolean)
     */
    private String[] getDependencyTableHeader( boolean withClassifier, boolean withOptional )
    {
        String groupId = getI18nString( "column.groupId" );
        String artifactId = getI18nString( "column.artifactId" );
        String version = getI18nString( "column.version" );
        String classifier = getI18nString( "column.classifier" );
        String type = getI18nString( "column.type" );
        String license = getI18nString( "column.license" );
        String optional = getI18nString( "column.optional" );

        if ( withClassifier )
        {
            if ( withOptional )
            {
                return new String[] { groupId, artifactId, version, classifier, type, license, optional };
            }

            return new String[] { groupId, artifactId, version, classifier, type, license };
        }

        if ( withOptional )
        {
            return new String[] { groupId, artifactId, version, type, license, optional };
        }

        return new String[] { groupId, artifactId, version, type, license };
    }

    private void renderSectionProjectDependencies()
    {
        startSection( getTitle() );

        // collect dependencies by scope
        Map<String, List<Artifact>> dependenciesByScope = dependencies.getDependenciesByScope( false );

        renderDependenciesForAllScopes( dependenciesByScope, false );

        endSection();
    }

    /**
     * @param dependenciesByScope map with supported scopes as key and a list of <code>Artifact</code> as values.
     * @param isTransitive <code>true</code> if it is transitive dependencies rendering.
     * @see Artifact#SCOPE_COMPILE
     * @see Artifact#SCOPE_PROVIDED
     * @see Artifact#SCOPE_RUNTIME
     * @see Artifact#SCOPE_SYSTEM
     * @see Artifact#SCOPE_TEST
     */
    private void renderDependenciesForAllScopes( Map<String, List<Artifact>> dependenciesByScope, boolean isTransitive )
    {
        renderDependenciesForScope( Artifact.SCOPE_COMPILE, dependenciesByScope.get( Artifact.SCOPE_COMPILE ),
                                    isTransitive );
        renderDependenciesForScope( Artifact.SCOPE_RUNTIME, dependenciesByScope.get( Artifact.SCOPE_RUNTIME ),
                                    isTransitive );
        renderDependenciesForScope( Artifact.SCOPE_TEST, dependenciesByScope.get( Artifact.SCOPE_TEST ), isTransitive );
        renderDependenciesForScope( Artifact.SCOPE_PROVIDED, dependenciesByScope.get( Artifact.SCOPE_PROVIDED ),
                                    isTransitive );
        renderDependenciesForScope( Artifact.SCOPE_SYSTEM, dependenciesByScope.get( Artifact.SCOPE_SYSTEM ),
                                    isTransitive );
    }

    private void renderSectionProjectTransitiveDependencies()
    {
        Map<String, List<Artifact>> dependenciesByScope = dependencies.getDependenciesByScope( true );

        startSection( getI18nString( "transitive.title" ) );

        if ( dependenciesByScope.values().isEmpty() )
        {
            paragraph( getI18nString( "transitive.nolist" ) );
        }
        else
        {
            paragraph( getI18nString( "transitive.intro" ) );

            renderDependenciesForAllScopes( dependenciesByScope, true );
        }

        endSection();
    }

    private void renderSectionProjectDependencyGraph()
    {
        startSection( getI18nString( "graph.title" ) );

        // === SubSection: Dependency Tree
        renderSectionDependencyTree();

        endSection();
    }

    private void renderSectionDependencyTree()
    {
        sink.rawText( JAVASCRIPT );

        // for Dependencies Graph Tree
        startSection( getI18nString( "graph.tree.title" ) );

        sink.list();
        printDependencyListing( dependencyTreeNode );
        sink.list_();

        endSection();
    }

    private void renderSectionDependencyFileDetails()
    {
        startSection( getI18nString( "file.details.title" ) );

        List<Artifact> alldeps = dependencies.getAllDependencies();
        Collections.sort( alldeps, getArtifactComparator() );

        // i18n
        String filename = getI18nString( "file.details.column.file" );
        String size = getI18nString( "file.details.column.size" );
        String entries = getI18nString( "file.details.column.entries" );
        String classes = getI18nString( "file.details.column.classes" );
        String packages = getI18nString( "file.details.column.packages" );
        String jdkrev = getI18nString( "file.details.column.jdkrev" );
        String debug = getI18nString( "file.details.column.debug" );
        String sealed = getI18nString( "file.details.column.sealed" );

        int[] justification =
            new int[] { Sink.JUSTIFY_LEFT, Sink.JUSTIFY_RIGHT, Sink.JUSTIFY_RIGHT, Sink.JUSTIFY_RIGHT,
                Sink.JUSTIFY_RIGHT, Sink.JUSTIFY_CENTER, Sink.JUSTIFY_CENTER, Sink.JUSTIFY_CENTER };

        startTable( justification, false );

        TotalCell totaldeps = new TotalCell( DEFAULT_DECIMAL_FORMAT );
        TotalCell totaldepsize = new TotalCell( fileLengthDecimalFormat );
        TotalCell totalentries = new TotalCell( DEFAULT_DECIMAL_FORMAT );
        TotalCell totalclasses = new TotalCell( DEFAULT_DECIMAL_FORMAT );
        TotalCell totalpackages = new TotalCell( DEFAULT_DECIMAL_FORMAT );
        double highestjdk = 0.0;
        TotalCell totaldebug = new TotalCell( DEFAULT_DECIMAL_FORMAT );
        TotalCell totalsealed = new TotalCell( DEFAULT_DECIMAL_FORMAT );

        boolean hasSealed = hasSealed( alldeps );

        // Table header
        String[] tableHeader;
        if ( hasSealed )
        {
            tableHeader = new String[] { filename, size, entries, classes, packages, jdkrev, debug, sealed };
        }
        else
        {
            tableHeader = new String[] { filename, size, entries, classes, packages, jdkrev, debug };
        }
        tableHeader( tableHeader );

        // Table rows
        for ( Artifact artifact : alldeps )
        {
            if ( artifact.getFile() == null )
            {
                log.error( "Artifact: " + artifact.getId() + " has no file." );
                continue;
            }

            File artifactFile = artifact.getFile();

            totaldeps.incrementTotal( artifact.getScope() );
            totaldepsize.addTotal( artifactFile.length(), artifact.getScope() );

            if ( JAR_SUBTYPE.contains( artifact.getType().toLowerCase() ) )
            {
                try
                {
                    JarData jarDetails = dependencies.getJarDependencyDetails( artifact );

                    String debugstr = "release";
                    if ( jarDetails.isDebugPresent() )
                    {
                        debugstr = "debug";
                        totaldebug.incrementTotal( artifact.getScope() );
                    }

                    totalentries.addTotal( jarDetails.getNumEntries(), artifact.getScope() );
                    totalclasses.addTotal( jarDetails.getNumClasses(), artifact.getScope() );
                    totalpackages.addTotal( jarDetails.getNumPackages(), artifact.getScope() );

                    try
                    {
                        if ( jarDetails.getJdkRevision() != null )
                        {
                            highestjdk = Math.max( highestjdk, Double.parseDouble( jarDetails.getJdkRevision() ) );
                        }
                    }
                    catch ( NumberFormatException e )
                    {
                        // ignore
                    }

                    String sealedstr = "";
                    if ( jarDetails.isSealed() )
                    {
                        sealedstr = "sealed";
                        totalsealed.incrementTotal( artifact.getScope() );
                    }

                    tableRow( hasSealed, new String[] { artifactFile.getName(),
                        fileLengthDecimalFormat.format( artifactFile.length() ),
                        DEFAULT_DECIMAL_FORMAT.format( jarDetails.getNumEntries() ),
                        DEFAULT_DECIMAL_FORMAT.format( jarDetails.getNumClasses() ),
                        DEFAULT_DECIMAL_FORMAT.format( jarDetails.getNumPackages() ), jarDetails.getJdkRevision(),
                        debugstr, sealedstr } );
                }
                catch ( IOException e )
                {
                    createExceptionInfoTableRow( artifact, artifactFile, e, hasSealed );
                }
            }
            else
            {
                tableRow( hasSealed, new String[] { artifactFile.getName(),
                    fileLengthDecimalFormat.format( artifactFile.length() ), "", "", "", "", "", "" } );
            }
        }

        // Total raws
        tableHeader[0] = getI18nString( "file.details.total" );
        tableHeader( tableHeader );

        justification[0] = Sink.JUSTIFY_RIGHT;
        justification[6] = Sink.JUSTIFY_RIGHT;

        for ( int i = -1; i < TotalCell.SCOPES_COUNT; i++ )
        {
            if ( totaldeps.getTotal( i ) > 0 )
            {
                tableRow( hasSealed, new String[] { totaldeps.getTotalString( i ), totaldepsize.getTotalString( i ),
                    totalentries.getTotalString( i ), totalclasses.getTotalString( i ),
                    totalpackages.getTotalString( i ), ( i < 0 ) ? String.valueOf( highestjdk ) : "",
                    totaldebug.getTotalString( i ), totalsealed.getTotalString( i ) } );
            }
        }

        endTable();
        endSection();
    }

    private void tableRow( boolean fullRow, String[] content )
    {
        sink.tableRow();

        int count = fullRow ? content.length : ( content.length - 1 );

        for ( int i = 0; i < count; i++ )
        {
            tableCell( content[i] );
        }

        sink.tableRow_();
    }

    private void createExceptionInfoTableRow( Artifact artifact, File artifactFile, Exception e, boolean hasSealed )
    {
        tableRow( hasSealed, new String[] { artifact.getId(), artifactFile.getAbsolutePath(), e.getMessage(), "", "",
            "", "", "" } );
    }

    private void populateRepositoryMap( Map<String, ArtifactRepository> repos, List<ArtifactRepository> rowRepos )
    {
        for ( ArtifactRepository repo : rowRepos )
        {
            repos.put( repo.getId(), repo );
        }
    }

    private void blacklistRepositoryMap( Map<String, ArtifactRepository> repos,
                                         List<String> repoUrlBlackListed )
    {
        for ( ArtifactRepository repo : repos.values() )
        {
            // ping repo
            if ( !repo.isBlacklisted() )
            {
                if ( !repoUrlBlackListed.contains( repo.getUrl() ) )
                {
                    try
                    {
                        URL repoUrl = new URL( repo.getUrl() );
                        if ( ProjectInfoReportUtils.getInputStream( repoUrl, settings ) == null )
                        {
                            log.warn( "The repository url '" + repoUrl + "' has no stream - Repository '"
                                + repo.getId() + "' will be blacklisted." );
                            repo.setBlacklisted( true );
                            repoUrlBlackListed.add( repo.getUrl() );
                        }
                    }
                    catch ( IOException e )
                    {
                        log.warn( "The repository url '" + repo.getUrl() + "' is invalid - Repository '" + repo.getId()
                            + "' will be blacklisted." );
                        repo.setBlacklisted( true );
                        repoUrlBlackListed.add( repo.getUrl() );
                    }
                }
                else
                {
                    repo.setBlacklisted( true );
                }
            }
            else
            {
                repoUrlBlackListed.add( repo.getUrl() );
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private void renderSectionDependencyRepositoryLocations()
    {
        startSection( getI18nString( "repo.locations.title" ) );

        // Collect Alphabetical Dependencies
        List<Artifact> alldeps = dependencies.getAllDependencies();
        Collections.sort( alldeps, getArtifactComparator() );

        // Collect Repositories
        Map<String, ArtifactRepository> repoMap = new HashMap<String, ArtifactRepository>();

        populateRepositoryMap( repoMap, repoUtils.getRemoteArtifactRepositories() );
        for ( Artifact artifact : alldeps )
        {
            try
            {
                MavenProject artifactProject = repoUtils.getMavenProjectFromRepository( artifact );
                populateRepositoryMap( repoMap, artifactProject.getRemoteArtifactRepositories() );
            }
            catch ( ProjectBuildingException e )
            {
                log.warn( "Unable to create Maven project from repository.", e );
            }
        }

        List<String> repoUrlBlackListed = new ArrayList<String>();
        blacklistRepositoryMap( repoMap, repoUrlBlackListed );

        // Render Repository List

        printRepositories( repoMap, repoUrlBlackListed );

        // Render Artifacts locations

        printArtifactsLocations( repoMap, repoUrlBlackListed, alldeps );

        endSection();
    }

    private void renderSectionDependencyLicenseListing()
    {
        startSection( getI18nString( "graph.tables.licenses" ) );
        printGroupedLicenses();
        endSection();
    }

    private void renderDependenciesForScope( String scope, List<Artifact> artifacts, boolean isTransitive )
    {
        if ( artifacts != null )
        {
            boolean withClassifier = hasClassifier( artifacts );
            boolean withOptional = hasOptional( artifacts );
            String[] tableHeader = getDependencyTableHeader( withClassifier, withOptional );

            // can't use straight artifact comparison because we want optional last
            Collections.sort( artifacts, getArtifactComparator() );

            String anchorByScope =
                ( isTransitive ? getI18nString( "transitive.title" ) + "_" + scope
                                : getI18nString( "title" ) + "_" + scope );
            startSection( anchorByScope, scope );

            paragraph( getI18nString( "intro." + scope ) );

            startTable();
            tableHeader( tableHeader );
            for ( Artifact artifact : artifacts )
            {
                renderArtifactRow( artifact, withClassifier, withOptional );
            }
            endTable();

            endSection();
        }
    }

    private Comparator<Artifact> getArtifactComparator()
    {
        return new Comparator<Artifact>()
        {
            public int compare( Artifact a1, Artifact a2 )
            {
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

    /**
     * @param artifact not null
     * @param withClassifier <code>true</code> to include the classifier column, <code>false</code> otherwise.
     * @param withOptional <code>true</code> to include the optional column, <code>false</code> otherwise.
     * @see #getDependencyTableHeader(boolean, boolean)
     */
    private void renderArtifactRow( Artifact artifact, boolean withClassifier, boolean withOptional )
    {
        String isOptional =
            artifact.isOptional() ? getI18nString( "column.isOptional" )
                            : getI18nString( "column.isNotOptional" );

        String url =
            ProjectInfoReportUtils.getArtifactUrl( artifactFactory, artifact, mavenProjectBuilder, remoteRepositories,
                                                   localRepository );
        String artifactIdCell = ProjectInfoReportUtils.getArtifactIdCell( artifact.getArtifactId(), url );

        MavenProject artifactProject;
        StringBuffer sb = new StringBuffer();
        try
        {
            artifactProject = repoUtils.getMavenProjectFromRepository( artifact );
            @SuppressWarnings( "unchecked" )
            List<License> licenses = artifactProject.getLicenses();
            for ( Iterator<License> iterator = licenses.iterator(); iterator.hasNext(); )
            {
                License license = iterator.next();
                String artifactIdCell2 = ProjectInfoReportUtils.getArtifactIdCell( license.getName(), license.getUrl() );
                sb.append( artifactIdCell2 );
            }
        }
        catch ( ProjectBuildingException e )
        {
            log.warn( "Unable to create Maven project from repository.", e );
        }

        String content[];
        if ( withClassifier )
        {
            content =
                new String[] { artifact.getGroupId(), artifactIdCell, artifact.getVersion(), artifact.getClassifier(),
                    artifact.getType(), sb.toString(), isOptional };
        }
        else
        {
            content =
                new String[] { artifact.getGroupId(), artifactIdCell, artifact.getVersion(), artifact.getType(),
                    sb.toString(), isOptional };
        }

        tableRow( withOptional, content );
    }

    private void printDependencyListing( DependencyNode node )
    {
        Artifact artifact = node.getArtifact();
        String id = artifact.getId();
        String dependencyDetailId = getUUID();
        String imgId = getUUID();

        sink.listItem();

        sink.text( id + ( StringUtils.isNotEmpty( artifact.getScope() ) ? " (" + artifact.getScope() + ") " : " " ) );
        sink.rawText( "<img id=\"" + imgId + "\" src=\"" + IMG_INFO_URL
            + "\" alt=\"Information\" onclick=\"toggleDependencyDetail( '" + dependencyDetailId + "', '" + imgId
            + "' );\" style=\"cursor: pointer;vertical-align:text-bottom;\"></img>" );

        printDescriptionsAndURLs( node, dependencyDetailId );

        if ( !node.getChildren().isEmpty() )
        {
            boolean toBeIncluded = false;
            List<DependencyNode> subList = new ArrayList<DependencyNode>();
            for ( @SuppressWarnings( "unchecked" )
            Iterator<DependencyNode> deps = node.getChildren().iterator(); deps.hasNext(); )
            {
                DependencyNode dep = deps.next();

                if ( !dependencies.getAllDependencies().contains( dep.getArtifact() ) )
                {
                    continue;
                }

                subList.add( dep );
                toBeIncluded = true;
            }

            if ( toBeIncluded )
            {
                sink.list();
                for ( DependencyNode dep : subList )
                {
                    printDependencyListing( dep );
                }
                sink.list_();
            }
        }

        sink.listItem_();
    }

    private void printDescriptionsAndURLs( DependencyNode node, String uid )
    {
        Artifact artifact = node.getArtifact();
        String id = artifact.getId();
        String unknownLicenseMessage = getI18nString( "graph.tables.unknown" );

        sink.rawText( "<div id=\"" + uid + "\" style=\"display:none\">" );

        sink.table();

        if ( !Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            try
            {
                MavenProject artifactProject = repoUtils.getMavenProjectFromRepository( artifact );
                String artifactDescription = artifactProject.getDescription();
                String artifactUrl = artifactProject.getUrl();
                String artifactName = artifactProject.getName();
                @SuppressWarnings( "unchecked" )
                List<License> licenses = artifactProject.getLicenses();

                sink.tableRow();
                sink.tableHeaderCell();
                sink.text( artifactName );
                sink.tableHeaderCell_();
                sink.tableRow_();

                sink.tableRow();
                sink.tableCell();

                sink.paragraph();
                sink.bold();
                sink.text( getI18nString( "column.description" ) + ": " );
                sink.bold_();
                if ( StringUtils.isNotEmpty( artifactDescription ) )
                {
                    sink.text( artifactDescription );
                }
                else
                {
                    sink.text( getI18nString( "index", "nodescription" ) );
                }
                sink.paragraph_();

                if ( StringUtils.isNotEmpty( artifactUrl ) )
                {
                    sink.paragraph();
                    sink.bold();
                    sink.text( getI18nString( "column.url" ) + ": " );
                    sink.bold_();
                    if ( ProjectInfoReportUtils.isArtifactUrlValid( artifactUrl ) )
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
                }

                sink.paragraph();
                sink.bold();
                sink.text( getI18nString( "license", "title" ) + ": " );
                sink.bold_();
                if ( !licenses.isEmpty() )
                {
                    for ( License element : licenses )
                    {
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
                    sink.text( getI18nString( "license", "nolicense" ) );

                    licenseMap.put( unknownLicenseMessage, artifactName );
                }
                sink.paragraph_();
            }
            catch ( ProjectBuildingException e )
            {
                log.warn( "Unable to create Maven project from repository.", e );
            }
        }
        else
        {
            sink.tableRow();
            sink.tableHeaderCell();
            sink.text( id );
            sink.tableHeaderCell_();
            sink.tableRow_();

            sink.tableRow();
            sink.tableCell();

            sink.paragraph();
            sink.bold();
            sink.text( getI18nString( "column.description" ) + ": " );
            sink.bold_();
            sink.text( getI18nString( "index", "nodescription" ) );
            sink.paragraph_();

            if ( artifact.getFile() != null )
            {
                sink.paragraph();
                sink.bold();
                sink.text( getI18nString( "column.url" ) + ": " );
                sink.bold_();
                sink.text( artifact.getFile().getAbsolutePath() );
                sink.paragraph_();
            }
        }

        sink.tableCell_();
        sink.tableRow_();

        sink.table_();

        sink.rawText( "</div>" );
    }

    private void printGroupedLicenses()
    {
        for ( Map.Entry<String, Object> entry : licenseMap.entrySet() )
        {
            String licenseName = entry.getKey();
            sink.paragraph();
            sink.bold();
            if ( StringUtils.isEmpty( licenseName ) )
            {
                sink.text( getI18nString( "unamed" ) );
            }
            else
            {
                sink.text( licenseName );
            }
            sink.text( ": " );
            sink.bold_();

            @SuppressWarnings( "unchecked" )
            SortedSet<String> projects = (SortedSet<String>) entry.getValue();

            for ( Iterator<String> iterator = projects.iterator(); iterator.hasNext(); )
            {
                String projectName = iterator.next();
                sink.text( projectName );
                if ( iterator.hasNext() )
                {
                    sink.text( ", " );
                }
            }

            sink.paragraph_();
        }
    }

    private void printRepositories( Map<String, ArtifactRepository> repoMap, List<String> repoUrlBlackListed )
    {
        // i18n
        String repoid = getI18nString( "repo.locations.column.repoid" );
        String url = getI18nString( "repo.locations.column.url" );
        String release = getI18nString( "repo.locations.column.release" );
        String snapshot = getI18nString( "repo.locations.column.snapshot" );
        String blacklisted = getI18nString( "repo.locations.column.blacklisted" );
        String releaseEnabled = getI18nString( "repo.locations.cell.release.enabled" );
        String releaseDisabled = getI18nString( "repo.locations.cell.release.disabled" );
        String snapshotEnabled = getI18nString( "repo.locations.cell.snapshot.enabled" );
        String snapshotDisabled = getI18nString( "repo.locations.cell.snapshot.disabled" );
        String blacklistedEnabled = getI18nString( "repo.locations.cell.blacklisted.enabled" );
        String blacklistedDisabled = getI18nString( "repo.locations.cell.blacklisted.disabled" );

        // Table header

        String[] tableHeader;
        int[] justificationRepo;
        if ( repoUrlBlackListed.isEmpty() )
        {
            tableHeader = new String[] { repoid, url, release, snapshot };
            justificationRepo =
                new int[] { Sink.JUSTIFY_LEFT, Sink.JUSTIFY_LEFT, Sink.JUSTIFY_CENTER, Sink.JUSTIFY_CENTER };
        }
        else
        {
            tableHeader = new String[] { repoid, url, release, snapshot, blacklisted };
            justificationRepo =
                new int[] { Sink.JUSTIFY_LEFT, Sink.JUSTIFY_LEFT, Sink.JUSTIFY_CENTER, Sink.JUSTIFY_CENTER,
                    Sink.JUSTIFY_CENTER };
        }

        startTable( justificationRepo, false );

        tableHeader( tableHeader );

        // Table rows

        for ( ArtifactRepository repo : repoMap.values() )
        {
            sink.tableRow();
            tableCell( repo.getId() );

            sink.tableCell();
            if ( repo.isBlacklisted() )
            {
                sink.text( repo.getUrl() );
            }
            else
            {
                sink.link( repo.getUrl() );
                sink.text( repo.getUrl() );
                sink.link_();
            }
            sink.tableCell_();

            ArtifactRepositoryPolicy releasePolicy = repo.getReleases();
            tableCell( releasePolicy.isEnabled() ? releaseEnabled : releaseDisabled );

            ArtifactRepositoryPolicy snapshotPolicy = repo.getSnapshots();
            tableCell( snapshotPolicy.isEnabled() ? snapshotEnabled : snapshotDisabled );

            tableCell( repoUrlBlackListed.contains( repo.getUrl() ) ? blacklistedEnabled : blacklistedDisabled );

            sink.tableRow_();
        }

        endTable();
    }

    private void printArtifactsLocations( Map<String, ArtifactRepository> repoMap, List<String> repoUrlBlackListed, List<Artifact> alldeps )
    {
        // i18n
        String artifact = getI18nString( "repo.locations.column.artifact" );

        sink.paragraph();
        sink.text( getI18nString( "repo.locations.artifact.breakdown" ) );
        sink.paragraph_();

        List<String> repoIdList = new ArrayList<String>();
        // removed blacklisted repo
        for ( Map.Entry<String, ArtifactRepository> entry : repoMap.entrySet() )
        {
            String repokey = entry.getKey();
            ArtifactRepository repo = entry.getValue();
            if ( !(repo.isBlacklisted() || repoUrlBlackListed.contains( repo.getUrl() ) ) )
            {
                repoIdList.add( repokey );
            }
        }

        String[] tableHeader = new String[repoIdList.size() + 1];
        int[] justificationRepo = new int[repoIdList.size() + 1];

        tableHeader[0] = artifact;
        justificationRepo[0] = Sink.JUSTIFY_LEFT;

        int idnum = 1;
        for ( String id : repoIdList )
        {
            tableHeader[idnum] = id;
            justificationRepo[idnum] = Sink.JUSTIFY_CENTER;
            idnum++;
        }

        Map<String, Integer> totalByRepo = new HashMap<String, Integer>();
        TotalCell totaldeps = new TotalCell( DEFAULT_DECIMAL_FORMAT );

        startTable( justificationRepo, false );

        tableHeader( tableHeader );

        for ( Artifact dependency : alldeps )
        {
            totaldeps.incrementTotal( dependency.getScope() );

            sink.tableRow();

            if ( !Artifact.SCOPE_SYSTEM.equals( dependency.getScope() ) )
            {
                tableCell( dependency.getId() );

                for ( String repokey : repoIdList )
                {
                    ArtifactRepository repo = repoMap.get( repokey );

                    String depUrl = repoUtils.getDependencyUrlFromRepository( dependency, repo );

                    @SuppressWarnings( "cast" )
                    Integer old = (Integer) totalByRepo.get( repokey );
                    if ( old == null )
                    {
                        totalByRepo.put( repokey, new Integer( 0 ) );
                        old = new Integer( 0 );
                    }

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
                        if ( StringUtils.isNotEmpty( depUrl ) )
                        {
                            sink.link( depUrl );
                        }
                        else
                        {
                            sink.text( depUrl );
                        }

                        sink.figure();
                        sink.figureCaption();
                        sink.text( "Found at " + repo.getUrl() );
                        sink.figureCaption_();
                        sink.figureGraphics( "images/icon_success_sml.gif" );
                        sink.figure_();

                        sink.link_();
                        sink.tableCell_();

                        totalByRepo.put( repokey, new Integer( old.intValue() + 1 ) );
                    }
                    else
                    {
                        tableCell( "-" );
                    }
                }
            }
            else
            {
                tableCell( dependency.getId() );

                for ( @SuppressWarnings( "unused" ) String repoId : repoIdList )
                {
                    tableCell( "-" );
                }
            }

            sink.tableRow_();
        }

        // Total row

        // reused key
        tableHeader[0] = getI18nString( "file.details.total" );
        tableHeader( tableHeader );
        String[] totalRow = new String[repoIdList.size() + 1];
        totalRow[0] = totaldeps.toString();
        idnum = 1;
        for ( String repokey : repoIdList )
        {
            Integer deps = totalByRepo.get( repokey );
            totalRow[idnum++] = deps != null ? deps.toString() : "0";
        }

        tableRow( totalRow );

        endTable();
    }

    /**
     * @param artifacts not null
     * @return <code>true</code> if one artifact in the list has a classifier, <code>false</code> otherwise.
     */
    private boolean hasClassifier( List<Artifact> artifacts )
    {
        for ( Artifact artifact : artifacts )
        {
            if ( StringUtils.isNotEmpty( artifact.getClassifier() ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * @param artifacts not null
     * @return <code>true</code> if one artifact in the list is optional, <code>false</code> otherwise.
     */
    private boolean hasOptional( List<Artifact> artifacts )
    {
        for ( Artifact artifact : artifacts )
        {
            if ( artifact.isOptional() )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * @param artifacts not null
     * @return <code>true</code> if one artifact in the list is sealed, <code>false</code> otherwise.
     */
    private boolean hasSealed( List<Artifact> artifacts )
    {
        for ( Artifact artifact : artifacts )
        {
            // TODO site:run Why do we need to resolve this...
            if ( artifact.getFile() == null && !Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
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
                    if ( ( dependencies.getProject().getGroupId().equals( artifact.getGroupId() ) )
                        && ( dependencies.getProject().getArtifactId().equals( artifact.getArtifactId() ) )
                        && ( dependencies.getProject().getVersion().equals( artifact.getVersion() ) ) )
                    {
                        log.warn( "The artifact of this project has never been deployed." );
                    }
                    else
                    {
                        log.error( "Artifact: " + artifact.getId() + " has no file.", e );
                    }

                    continue;
                }
            }

            if ( JAR_SUBTYPE.contains( artifact.getType().toLowerCase() ) )
            {
                try
                {
                    JarData jarDetails = dependencies.getJarDependencyDetails( artifact );
                    if ( jarDetails.isSealed() )
                    {
                        return true;
                    }
                }
                catch ( IOException e )
                {
                    log.error( "IOException: " + e.getMessage(), e );
                }
            }
        }
        return false;
    }

    /**
     * @return a valid HTML ID respecting
     * <a href="http://www.w3.org/TR/xhtml1/#C_8">XHTML 1.0 section C.8. Fragment Identifiers</a>
     */
    private static String getUUID()
    {
        return "_" + Math.abs( RANDOM.nextInt() );
    }

    /**
     * Formats file length with the associated <a href="http://en.wikipedia.org/wiki/SI_prefix#Computing">SI</a>
     * unit (GB, MB, kB) and using the pattern <code>########.00</code> by default.
     *
     * @see <a href="http://en.wikipedia.org/wiki/SI_prefix#Computing>
     * http://en.wikipedia.org/wiki/SI_prefix#Computing</a>
     * @see <a href="http://en.wikipedia.org/wiki/Binary_prefix">
     * http://en.wikipedia.org/wiki/Binary_prefix</a>
     * @see <a href="http://en.wikipedia.org/wiki/Octet_(computing)">
     * http://en.wikipedia.org/wiki/Octet_(computing)</a>
     */
    static class FileDecimalFormat
        extends DecimalFormat
    {
        private static final long serialVersionUID = 4062503546523610081L;

        private final I18N i18n;

        private final Locale locale;

        /**
         * Default constructor
         *
         * @param i18n
         * @param locale
         */
        public FileDecimalFormat( I18N i18n, Locale locale )
        {
            super( "#,###.00" );

            this.i18n = i18n;
            this.locale = locale;
        }

        /** {@inheritDoc} */
        public StringBuffer format( long fs, StringBuffer result, FieldPosition fieldPosition )
        {
            if ( fs > 1024 * 1024 * 1024 )
            {
                result = super.format( (float) fs / ( 1024 * 1024 * 1024 ), result, fieldPosition );
                result.append( " " ).append( getString( "report.dependencies.file.details.column.size.gb" ) );
                return result;
            }

            if ( fs > 1024 * 1024 )
            {
                result = super.format( (float) fs / ( 1024 * 1024 ), result, fieldPosition );
                result.append( " " ).append( getString( "report.dependencies.file.details.column.size.mb" ) );
                return result;
            }

            result = super.format( (float) fs / ( 1024 ), result, fieldPosition );
            result.append( " " ).append( getString( "report.dependencies.file.details.column.size.kb" ) );
            return result;
        }

        private String getString( String key )
        {
            return i18n.getString( "project-info-report", locale, key );
        }
    }

    /**
     * Combine total and total by scope in a cell.
     */
    static class TotalCell
    {
        static final int SCOPES_COUNT = 5;

        final DecimalFormat decimalFormat;

        long total = 0;

        long totalCompileScope = 0;

        long totalTestScope = 0;

        long totalRuntimeScope = 0;

        long totalProvidedScope = 0;

        long totalSystemScope = 0;

        TotalCell( DecimalFormat decimalFormat )
        {
            this.decimalFormat = decimalFormat;
        }

        void incrementTotal( String scope )
        {
            addTotal( 1, scope );
        }

        static String getScope( int index )
        {
            switch ( index )
            {
                case 0:
                    return Artifact.SCOPE_COMPILE;
                case 1:
                    return Artifact.SCOPE_TEST;
                case 2:
                    return Artifact.SCOPE_RUNTIME;
                case 3:
                    return Artifact.SCOPE_PROVIDED;
                case 4:
                    return Artifact.SCOPE_SYSTEM;
                default:
                    return null;
            }
        }

        long getTotal( int index )
        {
            switch ( index )
            {
                case 0:
                    return totalCompileScope;
                case 1:
                    return totalTestScope;
                case 2:
                    return totalRuntimeScope;
                case 3:
                    return totalProvidedScope;
                case 4:
                    return totalSystemScope;
                default:
                    return total;
            }
        }

        String getTotalString( int index )
        {
            long totalString = getTotal( index );

            if ( totalString <= 0 )
            {
                return "";
            }

            StringBuffer sb = new StringBuffer();
            if ( index >= 0 )
            {
                sb.append( getScope( index ) ).append( ": " );
            }
            sb.append( decimalFormat.format( getTotal( index ) ) );
            return sb.toString();
        }

        void addTotal( long add, String scope )
        {
            total += add;

            if ( Artifact.SCOPE_COMPILE.equals( scope ) )
            {
                totalCompileScope += add;
            }
            else if ( Artifact.SCOPE_TEST.equals( scope ) )
            {
                totalTestScope += add;
            }
            else if ( Artifact.SCOPE_RUNTIME.equals( scope ) )
            {
                totalRuntimeScope += add;
            }
            else if ( Artifact.SCOPE_PROVIDED.equals( scope ) )
            {
                totalProvidedScope += add;
            }
            else if ( Artifact.SCOPE_SYSTEM.equals( scope ) )
            {
                totalSystemScope += add;
            }
        }

        /** {@inheritDoc} */
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append( decimalFormat.format( total ) );
            sb.append( " (" );

            boolean needSeparator = false;
            for ( int i = 0; i < SCOPES_COUNT; i++ )
            {
                if ( getTotal( i ) > 0 )
                {
                    if ( needSeparator )
                    {
                        sb.append( ", " );
                    }
                    sb.append( getTotalString( i ) );
                    needSeparator = true;
                }
            }

            sb.append( ")" );

            return sb.toString();
        }
    }
}
