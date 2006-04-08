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
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;

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

/**
 * Generates the Project Dependencies report.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal dependencies
 * @requiresDependencyResolution test
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
     * @component
     */
    private Renderer siteRenderer;

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
     * Maven Artifact Resolver
     *
     * @parameter expression="${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.metadata.ArtifactMetadataSource}"
     */
    protected ArtifactMetadataSource artifactMetadataSource;

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
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
    {
        try
        {
            ReportResolutionListener listener = resolveProject();

            DependenciesRenderer r = new DependenciesRenderer( getSink(), locale, listener.getDirectDependencies(),
                                                               listener.getTransitiveDependencies(),
                                                               listener.getOmittedArtifacts(), listener.getDepTree(),
                                                               listener.getDepMap() );

            r.render();
        }
        catch ( Exception e )
        {
            getLog().error( "An error occurred while resolving project dependencies.", e );
        }
    }

    private ReportResolutionListener resolveProject()
        throws ArtifactNotFoundException, ArtifactResolutionException, ProjectBuildingException
    {
        Map managedVersions = createManagedVersionMap( project.getId(), project.getDependencyManagement() );

        ReportResolutionListener listener = new ReportResolutionListener();

        artifactResolver.resolveTransitively( project.getDependencyArtifacts(), project.getArtifact(), managedVersions,
                                              localRepository, project.getRemoteArtifactRepositories(),
                                              artifactMetadataSource, null, Collections.singletonList( listener ) );

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
                    Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                                  versionRange, d.getType(),
                                                                                  d.getClassifier(), d.getScope() );
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
        private Locale locale;

        private Map directDep;

        private Map transitiveDep;

        private Map omittedDeps;

        private Map depTree;

        private Map depMap;

        public DependenciesRenderer( Sink sink, Locale locale, Map directDependencies, Map transitiveDependencies,
                                     Map omittedDependencies, Map dependencyTree, Map dependencyMap )
        {
            super( sink );

            this.locale = locale;

            this.directDep = directDependencies;

            this.transitiveDep = transitiveDependencies;

            this.omittedDeps = omittedDependencies;

            this.depTree = dependencyTree;

            this.depMap = dependencyMap;
        }

        public String getTitle()
        {
            return getReportString( "report.dependencies.title" );
        }

        public void renderBody()
        {
            // Dependencies report
            Set dependencies = new HashSet( directDep.values() );

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
            String description = getReportString( "report.dependencies.column.description" );
            String url = getReportString( "report.dependencies.column.url" );
            String optional = getReportString( "report.dependencies.column.optional" );

            // collect dependencies by scope
            Map dependenciesByScope = new HashMap()
            {
                public Object put( Object key, Object value )
                {
                    List multiValue = (List) get( key );
                    if ( multiValue == null )
                    {
                        multiValue = new ArrayList();
                    }
                    multiValue.add( value );
                    return super.put( key, multiValue );
                }
            };

            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                dependenciesByScope.put( artifact.getScope(), getArtifactRow( artifact ) );
            }

            for ( Iterator iter = dependenciesByScope.keySet().iterator(); iter.hasNext(); )
            {
                String scope = (String) iter.next();
                List artifactsRows = (List) dependenciesByScope.get( scope );

                startSection( scope );

                paragraph( getReportString( "report.dependencies.intro." + scope ) );
                startTable();
                tableHeader( new String[]{groupId, artifactId, version, description, url, optional} );

                // optional at the end + sort group id
                Collections.sort( artifactsRows, new Comparator()
                {

                    public int compare( Object row1, Object row2 )
                    {
                        String[] s1 = (String[]) row1;
                        String[] s2 = (String[]) row2;
                        return s1[0].compareTo( s2[0] ) + ( s1[5] != null ? 1000000 : 0 ) -
                            ( s2[5] != null ? 1000000 : 0 );
                    }
                } );

                for ( Iterator iterator = artifactsRows.iterator(); iterator.hasNext(); )
                {
                    String[] row = (String[]) iterator.next();
                    tableRow( row );
                }
                endTable();

                endSection();
            }

            endSection();

            // Transitive dependencies
            Set artifacts = new HashSet( transitiveDep.values() );

            startSection( getReportString( "report.transitivedependencies.title" ) );

            if ( artifacts.isEmpty() )
            {
                paragraph( getReportString( "report.transitivedependencies.nolist" ) );
            }
            else
            {
                paragraph( getReportString( "report.transitivedependencies.intro" ) );

                startTable();

                tableHeader( new String[]{groupId, artifactId, version, description, url, optional} );

                for ( Iterator i = artifacts.iterator(); i.hasNext(); )
                {
                    Artifact artifact = (Artifact) i.next();

                    tableRow( getArtifactRow( artifact ) );
                }

                endTable();
            }

            endSection();

            //for Dependencies Graph - Tree
            startSection( getReportString( "report.dependencies.graph.title" ) );

            startSection( getReportString( "report.dependencies.graph.tree.title" ) );

            printDependencyListing( project.getArtifact() );

            endSection();

            //for Dependencies Graph - Table Listings
            startSection( getReportString( "report.dependencies.graph.tables.title" ) );

            printDependencyTable( project.getArtifact() );

            for ( Iterator deps = project.getArtifacts().iterator(); deps.hasNext(); )
            {
                Artifact dep = (Artifact) deps.next();

                printDependencyTable( dep );
            }

            endSection();

            endSection();
        }

        private String[] getArtifactRow( Artifact artifact )
        {
            String[] row;
            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                row = new String[]{artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), null, null,
                    artifact.isOptional() ? "X" : null};
            }
            else
            {
                String artifactDescription = null;
                String artifactUrl = null;
                try
                {
                    // TODO: can we use @requiresDependencyResolution instead, and capture the depth of artifacts in the artifact itself?
                    MavenProject artifactProject = getMavenProjectFromRepository( artifact, localRepository );
                    artifactDescription = artifactProject.getDescription();
                    artifactUrl = artifactProject.getUrl();
                }
                catch ( ProjectBuildingException e )
                {
                    getLog().error( "Can't find a valid Maven project in the repository for the artifact [" +
                        artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + "]." );
                }
                row = new String[]{artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                    artifactDescription, createLinkPatternedText( artifactUrl, artifactUrl ),
                    artifact.isOptional() ? "X" : null};
            }
            return row;
        }

        private void printDependencyTable( Artifact artifact )
        {
            String id = artifact.getId();

            if ( !omittedDeps.containsKey( id ) && depMap.containsKey( id ) )
            {
                sink.anchor( id );
                startSection( artifact.getArtifactId() );
                sink.anchor_();

                startTable();

                tableHeader( new String []{getReportString( "report.dependencies.graph.tables.column.groupid" ),
                    getReportString( "report.dependencies.graph.tables.column.artifactid" ),
                    getReportString( "report.dependencies.graph.tables.column.version" ),
                    getReportString( "report.dependencies.graph.tables.column.comments" )} );

                List depList = (List) depMap.get( id );
                for ( Iterator deps = depList.iterator(); deps.hasNext(); )
                {
                    Artifact dep = (Artifact) deps.next();

                    String comment = "";
                    if ( omittedDeps.containsKey( dep.getId() ) )
                    {
                        comment = getReportString( "report.dependencies.graph.tables.notAttached" );
                    }
                    else
                    {
                        comment = getReportString( "report.dependencies.graph.tables.attached" );
                    }

                    tableRow( new String[]{dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), comment} );
                }

                endTable();

                endSection();
            }
        }

        private void printDependencyListing( Artifact artifact )
        {
            String id = artifact.getId();

            if ( !omittedDeps.containsKey( id ) )
            {
                sink.list();
                sink.listItem();

                if ( depTree.containsKey( id ) )
                {
                    sink.link( "#" + id );
                    sink.text( id );
                    sink.link_();

                    List depList = (List) depTree.get( id );
                    for ( Iterator deps = depList.iterator(); deps.hasNext(); )
                    {
                        Artifact dep = (Artifact) deps.next();
                        printDependencyListing( dep );
                    }
                }
                else
                {
                    sink.text( id );
                }

                sink.listItem_();
                sink.list_();
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
                projectArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact
                    .getArtifactId(), artifact.getVersion(), artifact.getScope() );
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

    private class ReportResolutionListener
        implements ResolutionListener
    {
        private Map directDep = new HashMap();

        private Map transitiveDep = new HashMap();

        private Map replacedDep = new HashMap();

        private List parents = new ArrayList();

        private Map depTree = new HashMap();

        private Map depMap = new HashMap();

        private Map depthMap = new HashMap();

        public void testArtifact( Artifact node )
        {

        }

        public void startProcessChildren( Artifact artifact )
        {
            parents.add( artifact );
        }

        public void endProcessChildren( Artifact artifact )
        {
            parents.remove( artifact );
        }

        public void includeArtifact( Artifact artifact )
        {
            addToDepMap( artifact );

            if ( depthMap.containsKey( artifact.getId() ) )
            {
                Integer depth = (Integer) depthMap.get( artifact.getId() );
                if ( depth.intValue() <= parents.size() )
                {
                    return;
                }
            }

            //remove from tree the artifact which is farther down the dependency trail
            removeFromDepTree( artifact );

            depthMap.put( artifact.getId(), new Integer( parents.size() ) );

            addDependency( artifact );

            addToDepTree( artifact );
        }

        private void removeFromDepTree( Artifact artifact )
        {
            for ( Iterator artifactDeps = depTree.values().iterator(); artifactDeps.hasNext(); )
            {
                List depList = (List) artifactDeps.next();
                for ( Iterator artifacts = depList.iterator(); artifacts.hasNext(); )
                {
                    Artifact dep = (Artifact) artifacts.next();

                    if ( dep.getId().equals( artifact.getId() ) )
                    {
                        depList.remove( dep );
                        break;
                    }
                }
            }
        }

        private void addToDepTree( Artifact artifact )
        {
            if ( parents.size() > 0 )
            {
                Artifact parent = (Artifact) parents.get( parents.size() - 1 );

                if ( depTree.containsKey( parent.getId() ) )
                {
                    List deps = (List) depTree.get( parent.getId() );

                    deps.add( artifact );
                }
                else
                {
                    List deps = new ArrayList();
                    deps.add( artifact );
                    depTree.put( parent.getId(), deps );
                }
            }
        }

        private void addToDepMap( Artifact artifact )
        {
            if ( parents.size() > 0 )
            {
                Artifact parent = (Artifact) parents.get( parents.size() - 1 );

                if ( depMap.containsKey( parent.getId() ) )
                {
                    List deps = (List) depMap.get( parent.getId() );

                    deps.add( artifact );
                }
                else
                {
                    List deps = new ArrayList();
                    deps.add( artifact );
                    depMap.put( parent.getId(), deps );
                }
            }
        }

        public void omitForNearer( Artifact omitted, Artifact kept )
        {
            String key = omitted.getId();

            replacedDep.put( key, omitted );

            if ( directDep.containsKey( key ) )
            {
                directDep.remove( key );
            }
            else if ( transitiveDep.containsKey( key ) )
            {
                transitiveDep.remove( key );
            }

            addDependency( kept );
        }

        private Artifact getDependency( String key )
        {
            if ( directDep.containsKey( key ) )
            {
                return (Artifact) directDep.get( key );
            }
            else if ( transitiveDep.containsKey( key ) )
            {
                return (Artifact) transitiveDep.get( key );
            }
            else
            {
                return null;
            }
        }

        private void addDependency( Artifact artifact )
        {
            if ( parents.size() == 0 )
            {
                //do nothing... artifact is current project
            }
            else if ( parents.size() == 1 )
            {
                if ( !directDep.containsKey( artifact.getId() ) )
                {
                    if ( artifact.getScope() == null )
                    {
                        artifact.setScope( Artifact.SCOPE_COMPILE );
                    }
                    directDep.put( artifact.getId(), artifact );
                }
            }
            else
            {
                if ( !transitiveDep.containsKey( artifact.getId() ) )
                {
                    if ( artifact.getScope() == null )
                    {
                        Artifact parent = (Artifact) parents.get(  parents.size() - 1 );

                        artifact.setScope( parent.getScope() );
                    }

                    transitiveDep.put( artifact.getId(), artifact );
                }
            }
        }

        public void updateScope( Artifact artifact, String scope )
        {
            if ( directDep.containsKey( artifact.getId() ) )
            {
                Artifact depArtifact = (Artifact) directDep.get( artifact.getId() );

                depArtifact.setScope( scope );
            }

            if ( transitiveDep.containsKey( artifact.getId() ) )
            {
                Artifact depArtifact = (Artifact) transitiveDep.get( artifact.getId() );

                depArtifact.setScope( scope );
            }
        }

        public void manageArtifact( Artifact artifact, Artifact replacement )
        {
            omitForNearer( artifact, replacement );
        }

        public void omitForCycle( Artifact artifact )
        {
            replacedDep.put( artifact.getId(), artifact );
        }

        public void updateScopeCurrentPom( Artifact artifact, String scope )
        {
            updateScope( artifact, scope );
        }

        public void selectVersionFromRange( Artifact artifact )
        {

        }

        public void restrictRange( Artifact artifact, Artifact replacement, VersionRange newRange )
        {

        }

        public Set getArtifacts()
        {
            Set all = new HashSet();

            all.addAll( directDep.values() );

            all.addAll( transitiveDep.values() );

            return all;
        }

        public Map getTransitiveDependencies()
        {
            return Collections.unmodifiableMap( transitiveDep );
        }

        public Map getDirectDependencies()
        {
            return Collections.unmodifiableMap( directDep );
        }

        public Map getOmittedArtifacts()
        {
            return Collections.unmodifiableMap( replacedDep );
        }

        public Map getDepTree()
        {
            return depTree;
        }

        public Map getDepMap()
        {
            return depMap;
        }
    }
}
