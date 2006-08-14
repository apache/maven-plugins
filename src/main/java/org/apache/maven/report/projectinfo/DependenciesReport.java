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

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
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
import org.apache.maven.report.projectinfo.ReportResolutionListener.Node;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.jar.Jar;
import org.apache.maven.shared.jar.JarException;
import org.apache.maven.shared.jar.classes.JarClasses;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

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

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    private class Dependencies
    {
        private List projectDependencies;

        private ReportResolutionListener resolvedDependencies;

        public Dependencies( MavenProject project, ReportResolutionListener listener )
        {
            /* Workaround to ensure proper File objects in the 
             * Artifacts from the ReportResolutionListener
             */
            Map projectMap = new HashMap();
            Iterator it = project.getArtifacts().iterator();
            while(it.hasNext())
            {
                Artifact artifact = (Artifact) it.next();
                projectMap.put( ArtifactUtils.versionlessKey( artifact ), artifact );
            }
            
            mapArtifactFiles(listener.getRootNode(), projectMap);
            
            this.projectDependencies = listener.getRootNode().getChildren();
            this.resolvedDependencies = listener;
        }

        private void mapArtifactFiles( Node node, Map projectMap )
        {
            List childs = node.getChildren();
            if( (childs == null) || childs.isEmpty() ) {
                return;
            }
            
            Iterator it = childs.iterator();
            while(it.hasNext())
            {
                Node anode = (ReportResolutionListener.Node) it.next();
                String key = ArtifactUtils.versionlessKey( anode.getArtifact() );
                Artifact projartifact = (Artifact) projectMap.get( key );
                if( projartifact != null ) {
                    anode.getArtifact().setFile(  projartifact.getFile() );
                }
                
                mapArtifactFiles(anode, projectMap);
            }
        }

        public boolean hasDependencies()
        {
            return ( projectDependencies != null ) && ( !this.projectDependencies.isEmpty() );
        }

        public List getProjectDependencies()
        {
            return new ArrayList( projectDependencies );
        }

        public List getTransitiveDependencies()
        {
            List deps = new ArrayList( resolvedDependencies.getArtifacts() );
            deps.removeAll( projectDependencies );
            return deps;
        }

        public List getAllDependencies()
        {
            List deps = new ArrayList();

            for ( Iterator it = resolvedDependencies.getArtifacts().iterator(); it.hasNext(); )
            {
                ReportResolutionListener.Node node = (ReportResolutionListener.Node) it.next();
                Artifact artifact = node.getArtifact();
                deps.add( artifact );
            }
            return deps;
        }

        public Map getDependenciesByScope()
        {
            Map dependenciesByScope = new HashMap();
            for ( Iterator i = getAllDependencies().iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

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
            Dependencies deps = new Dependencies( project, listener );

            if ( !deps.hasDependencies() )
            {
                startSection( getTitle() );

                // TODO: should the report just be excluded?
                paragraph( getReportString( "report.dependencies.nolist" ) );

                endSection();

                return;
            }

            // === Section: Project Dependencies.
            renderSectionProjectDependencies( deps );

            // === Section: Project Transitive Dependencies.
            renderSectionProjectTransitiveDependencies( deps );

            // === Section: Project Dependency Graph.
            renderSectionProjectDependencyGraph();

            if ( dependencyDetailsEnabled )
            {
                // === Section: Dependency File Details.
                renderSectionDependencyFileDetails( deps );
            }

            if ( !settings.isOffline() && dependencyLocationsEnabled )
            {
                // === Section: Dependency Repository Locations.
                renderSectionDependencyRepositoryLocations( deps );
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
            String[] tableHeader = new String[] { groupId, artifactId, version, classifier, type, optional };
            return tableHeader;
        }

        private void renderSectionProjectDependencies( Dependencies dependencies )
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
            renderDependenciesForScope( Artifact.SCOPE_COMPILE,
                                        (List) dependenciesByScope.get( Artifact.SCOPE_COMPILE ), tableHeader );
            renderDependenciesForScope( Artifact.SCOPE_RUNTIME,
                                        (List) dependenciesByScope.get( Artifact.SCOPE_RUNTIME ), tableHeader );
            renderDependenciesForScope( Artifact.SCOPE_TEST, 
                                        (List) dependenciesByScope.get( Artifact.SCOPE_TEST ), tableHeader );
            renderDependenciesForScope( Artifact.SCOPE_PROVIDED, 
                                        (List) dependenciesByScope.get( Artifact.SCOPE_PROVIDED ), tableHeader );
            renderDependenciesForScope( Artifact.SCOPE_SYSTEM, 
                                        (List) dependenciesByScope.get( Artifact.SCOPE_SYSTEM ), tableHeader );
        }

        private void renderSectionProjectTransitiveDependencies( Dependencies dependencies )
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
            //for Dependencies Graph Tree
            startSection( getReportString( "report.dependencies.graph.tree.title" ) );
            sink.paragraph();
            sink.list();
            printDependencyListing( listener.getRootNode() );
            sink.list_();
            sink.paragraph_();
            endSection();
        }

        private void renderSectionDependencyFileDetails( Dependencies dependencies )
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
            
            int totaldeps = 0;
            long totaldepsize = 0;
            int totalentries = 0;
            int totalclasses = 0;
            int totalpackages = 0;
            double highestjdk = 0.0;
            int totaldebug = 0;
            int totalsealed = 0;
            
            DecimalFormat decFormat = new DecimalFormat("#,##0");

            for ( Iterator it = alldeps.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();
                
                if ( !Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
                {
                    if ( artifact.getFile() == null )
                    {
                        getLog().error( "Artifact: " + artifact.getId() + " has no file." );
                        continue;
                    }

                    File artifactFile = artifact.getFile();

                    totaldeps++;
                    totaldepsize += artifactFile.length();

                    if ( "jar".equalsIgnoreCase( artifact.getType() ) || "war".equalsIgnoreCase( artifact.getType() )
                        || "ear".equalsIgnoreCase( artifact.getType() ) || "sar".equalsIgnoreCase( artifact.getType() )
                        || "rar".equalsIgnoreCase( artifact.getType() ) || "par".equalsIgnoreCase( artifact.getType() )
                        || "ejb".equalsIgnoreCase( artifact.getType() ) )
                    {
                        try
                        {
                            Jar jar = (Jar) container.lookup( Jar.ROLE );
                            jar.setFile( artifactFile );
                            JarClasses jclasses = jar.getClasses();

                            String debugstr = "release";
                            if ( jclasses.isDebugPresent() )
                            {
                                debugstr = "debug";
                                totaldebug++;
                            }

                            String sealedstr = "";
                            if ( jar.isSealed() )
                            {
                                sealedstr = "sealed";
                                totalsealed++;
                            }

                            totalentries += jar.getEntries().size();
                            totalclasses += jclasses.getClassNames().size();
                            totalpackages += jclasses.getPackages().size();

                            try
                            {
                                highestjdk = Math.max( highestjdk, Double.parseDouble( jclasses.getJdkRevision() ) );
                            }
                            catch ( NumberFormatException e )
                            {
                                // ignore
                            }

                            tableRow( new String[] {
                                artifactFile.getName(),
                                decFormat.format( artifactFile.length() ),
                                decFormat.format( jar.getEntries().size() ),
                                decFormat.format( jclasses.getClassNames().size() ),
                                decFormat.format( jclasses.getPackages().size() ),
                                jclasses.getJdkRevision(),
                                debugstr,
                                sealedstr } );
                        }
                        catch ( JarException e )
                        {
                            tableRow( new String[] {
                                artifact.getId(),
                                artifactFile.getAbsolutePath(),
                                e.getMessage(),
                                "",
                                "",
                                "",
                                "",
                                "" } );
                        }
                        catch ( ComponentLookupException e )
                        {
                            tableRow( new String[] {
                                artifact.getId(),
                                artifactFile.getAbsolutePath(),
                                e.getMessage(),
                                "",
                                "",
                                "",
                                "",
                                "" } );
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
            
            tableRow( new String[] {
                "" + totaldeps + " total dependencies",
                decFormat.format( totaldepsize ),
                decFormat.format( totalentries ),
                decFormat.format( totalclasses ),
                decFormat.format( totalpackages ),
                String.valueOf( highestjdk ),
                decFormat.format( totaldebug ),
                decFormat.format( totalsealed ) } );

            endTable();
            endSection();
        }
        
        private void populateRepositoryMap( Map repos, List rawRepos )
        {
            Iterator it = rawRepos.iterator();
            while(it.hasNext())
            {
                ArtifactRepository repo = (ArtifactRepository) it.next();
                repos.put( repo.getId(), repo );
            }
        }
        
        private void renderSectionDependencyRepositoryLocations( Dependencies dependencies )
        {
            startSection( getReportString( "report.dependencies.repo.locations.title" ) );
            
            // Collect Alphabetical Dependencies
            List alldeps = dependencies.getAllDependencies();
            Collections.sort( alldeps, getArtifactComparator() );
            
            // Collect Repositories
            Map repoMap = new HashMap();
            
            populateRepositoryMap( repoMap, project.getRemoteArtifactRepositories() );
            
            for ( Iterator it = alldeps.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();
                try
                {
                    MavenProject artifactProject = getMavenProjectFromRepository( artifact, localRepository );
                    
                    populateRepositoryMap( repoMap, artifactProject.getRemoteArtifactRepositories() );
                }
                catch ( ProjectBuildingException e )
                {
                    getLog().warn( "Unable to create maven project from repository.", e );
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
            
            for (Iterator it = repoIdList.iterator(); it.hasNext();)
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
                    
                    for(Iterator itrepo = repoIdList.iterator(); itrepo.hasNext();)
                    {
                        String repokey = (String) itrepo.next();
                        ArtifactRepository repo = (ArtifactRepository) repoMap.get( repokey );
                        
                        String depUrl = repo.getUrl() + "/" + repo.pathOf( dependency );
                        
                        if(dependencyExistsInRepo(repo, dependency))
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
                        } else {
                            sink.tableCell();
                            sink.text("-");
                            sink.tableCell_();
                        }
                    }
                    
                    sink.tableRow_();
                }
            }
            
            endTable();
            
            endSection();
        }
        
        private boolean dependencyExistsInRepo( ArtifactRepository repo, Artifact artifact )
        {
            Wagon wagon;
            
            try
            {
                wagon = wagonManager.getWagon(repo.getProtocol());
            }
            catch ( UnsupportedProtocolException e )
            {
                getLog().error( "Unsupported protocol: '" + repo.getProtocol() + "'", e );
                return false;
            }
            
            try
            {
                Debug debug = new Debug();

                wagon.addSessionListener( debug );
                wagon.addTransferListener( debug );

                String id = repo.getId();
                Repository repository = new Repository( id, repo.getUrl() );
                AuthenticationInfo auth = wagonManager.getAuthenticationInfo( repo.getId() );

                ProxyInfo proxyInfo = getProxyInfo( settings );
                if ( proxyInfo != null )
                {
                    wagon.connect( repository, auth, proxyInfo );
                }
                else
                {
                    wagon.connect( repository, auth );
                }

                return ( wagon.resourceExists( repo.pathOf( artifact ) ) );
            }
            catch ( ConnectionException e )
            {
                getLog().error( "Unable to connect to: " + repo.getUrl(), e );
                return false;
            }
            catch ( AuthenticationException e )
            {
                getLog().error( "Unable to connect to: " + repo.getUrl(), e );
                return false;
            }
            catch ( TransferFailedException e )
            {
                getLog().error( "Unable to determine if resource " + artifact + " exists in " + repo.getUrl(), e );
                return false;
            }
            catch ( AuthorizationException e )
            {
                getLog().error( "Unable to connect to: " + repo.getUrl(), e );
                return false;
            }
            finally
            {
                try
                {
                    wagon.disconnect();
                }
                catch ( ConnectionException e )
                {
                    getLog().error( "Error disconnecting wagon - ignored", e );
                }
            }
        }
        
        /**
         * Convenience method to map a <code>Proxy</code> object from the user system settings to a
         * <code>ProxyInfo</code> object.
         *
         * @return a proxyInfo object instancied or null if no active proxy is define in the settings.xml
         */
        public ProxyInfo getProxyInfo( Settings settings )
        {
            ProxyInfo proxyInfo = null;
            if ( settings != null && settings.getActiveProxy() != null )
            {
                Proxy settingsProxy = settings.getActiveProxy();

                proxyInfo = new ProxyInfo();
                proxyInfo.setHost( settingsProxy.getHost() );
                proxyInfo.setType( settingsProxy.getProtocol() );
                proxyInfo.setPort( settingsProxy.getPort() );
                proxyInfo.setNonProxyHosts( settingsProxy.getNonProxyHosts() );
                proxyInfo.setUserName( settingsProxy.getUsername() );
                proxyInfo.setPassword( settingsProxy.getPassword() );
            }

            return proxyInfo;
        }        

        private void renderSectionDependencyListing()
        {
            startSection( getReportString( "report.dependencies.graph.tables.title" ) );
            printDescriptionsAndURLs( listener.getRootNode() );
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
            return new String[]{artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                artifact.getClassifier(), artifact.getType(), artifact.isOptional() ? "(optional)" : " "};
        }

        private void printDependencyListing( ReportResolutionListener.Node node )
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
                    ReportResolutionListener.Node dep = (ReportResolutionListener.Node) deps.next();
                    printDependencyListing( dep );
                }
                sink.list_();
            }

            sink.paragraph_();
            sink.listItem_();
        }

        private void printDescriptionsAndURLs( ReportResolutionListener.Node node )
        {
            Artifact artifact = node.getArtifact();
            String id = artifact.getDependencyConflictId();

            if ( !Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                try
                {
                    MavenProject artifactProject = getMavenProjectFromRepository( artifact, localRepository );
                    String artifactDescription = artifactProject.getDescription();
                    String artifactUrl = artifactProject.getUrl();
                    String artifactName = artifactProject.getName();

                    sink.paragraph();
                    sink.anchor( id );
                    //                     startSection( artifactName );
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

                    //                    endSection();
                    sink.horizontalRule();
                }
                catch ( ProjectBuildingException e )
                {
                    getLog().debug( e );
                }

                for ( Iterator deps = node.getChildren().iterator(); deps.hasNext(); )
                {
                    ReportResolutionListener.Node dep = (ReportResolutionListener.Node) deps.next();
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
