package org.apache.maven.plugin.ide;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.DebugResolutionListener;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.resolver.WarningResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

/**
 * Abstract base plugin which takes care of the common stuff usually needed by maven IDE plugins. A
 * plugin extending AbstractIdeSupportMojo should implement the <code>setup()</code> and
 * <code>writeConfiguration()</code> methods, plus the getters needed to get the various
 * configuration flags and required components. The lifecycle:
 * 
 * <pre>
 *       *** calls setup() where you can configure your specific stuff and stop the mojo from execute if appropriate ***
 *       - manually resolve project dependencies, NOT failing if a dependency is missing
 *       - compute project references (reactor projects) if the getUseProjectReferences() flag is set
 *       - download sources/javadocs if the getDownloadSources() flag is set
 *       *** calls writeConfiguration(), passing the list of resolved referenced dependencies ***
 *       - report the list of missing sources or just tell how to turn this feature on if the flag was disabled
 * </pre>
 * 
 * @author Fabrizio Giustina
 * @version $Id$
 */
public abstract class AbstractIdeSupportMojo
    extends AbstractMojo
    implements LogEnabled
{

    /**
     * The project whose project files to create.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The currently executed project (can be a reactor project).
     * 
     * @parameter expression="${executedProject}"
     * @readonly
     */
    protected MavenProject executedProject;

    /**
     * Artifact factory, needed to download source jars for inclusion in classpath.
     * 
     * @component role="org.apache.maven.artifact.factory.ArtifactFactory"
     * @required
     * @readonly
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Artifact resolver, needed to download source jars for inclusion in classpath.
     * 
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * Artifact collector, needed to resolve dependencies.
     * 
     * @component role="org.apache.maven.artifact.resolver.ArtifactCollector"
     * @required
     * @readonly
     */
    protected ArtifactCollector artifactCollector;

    /**
     * @component role="org.apache.maven.artifact.metadata.ArtifactMetadataSource" hint="maven"
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * Remote repositories which will be searched for source attachments.
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    protected List remoteArtifactRepositories;

    /**
     * Local maven repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * If the executed project is a reactor project, this will contains the full list of projects in
     * the reactor.
     * 
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    /**
     * Enables/disables the downloading of source attachments. Defaults to false.
     * @parameter expression="${downloadSources}"
     */
    protected boolean downloadSources;

    /**
     * Plexus logger needed for debugging manual artifact resolution.
     */
    private Logger logger;

    /**
     * Getter for <code>artifactMetadataSource</code>.
     * @return Returns the artifactMetadataSource.
     */
    public ArtifactMetadataSource getArtifactMetadataSource()
    {
        return this.artifactMetadataSource;
    }

    /**
     * Setter for <code>artifactMetadataSource</code>.
     * @param artifactMetadataSource The artifactMetadataSource to set.
     */
    public void setArtifactMetadataSource( ArtifactMetadataSource artifactMetadataSource )
    {
        this.artifactMetadataSource = artifactMetadataSource;
    }

    /**
     * Getter for <code>project</code>.
     * @return Returns the project.
     */
    public MavenProject getProject()
    {
        return this.project;
    }

    /**
     * Setter for <code>project</code>.
     * @param project The project to set.
     */
    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * Getter for <code>reactorProjects</code>.
     * @return Returns the reactorProjects.
     */
    public List getReactorProjects()
    {
        return this.reactorProjects;
    }

    /**
     * Setter for <code>reactorProjects</code>.
     * @param reactorProjects The reactorProjects to set.
     */
    public void setReactorProjects( List reactorProjects )
    {
        this.reactorProjects = reactorProjects;
    }

    /**
     * Getter for <code>remoteArtifactRepositories</code>.
     * @return Returns the remoteArtifactRepositories.
     */
    public List getRemoteArtifactRepositories()
    {
        return this.remoteArtifactRepositories;
    }

    /**
     * Setter for <code>remoteArtifactRepositories</code>.
     * @param remoteArtifactRepositories The remoteArtifactRepositories to set.
     */
    public void setRemoteArtifactRepositories( List remoteArtifactRepositories )
    {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
    }

    /**
     * Getter for <code>artifactFactory</code>.
     * @return Returns the artifactFactory.
     */
    public ArtifactFactory getArtifactFactory()
    {
        return this.artifactFactory;
    }

    /**
     * Setter for <code>artifactFactory</code>.
     * @param artifactFactory The artifactFactory to set.
     */
    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    /**
     * Getter for <code>artifactResolver</code>.
     * @return Returns the artifactResolver.
     */
    public ArtifactResolver getArtifactResolver()
    {
        return this.artifactResolver;
    }

    /**
     * Setter for <code>artifactResolver</code>.
     * @param artifactResolver The artifactResolver to set.
     */
    public void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
    }

    /**
     * Getter for <code>executedProject</code>.
     * @return Returns the executedProject.
     */
    public MavenProject getExecutedProject()
    {
        return this.executedProject;
    }

    /**
     * Setter for <code>executedProject</code>.
     * @param executedProject The executedProject to set.
     */
    public void setExecutedProject( MavenProject executedProject )
    {
        this.executedProject = executedProject;
    }

    /**
     * Getter for <code>localRepository</code>.
     * @return Returns the localRepository.
     */
    public ArtifactRepository getLocalRepository()
    {
        return this.localRepository;
    }

    /**
     * Setter for <code>localRepository</code>.
     * @param localRepository The localRepository to set.
     */
    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    /**
     * Getter for <code>downloadSources</code>.
     * @return Returns the downloadSources.
     */
    public boolean getDownloadSources()
    {
        return this.downloadSources;
    }

    /**
     * Setter for <code>downloadSources</code>.
     * @param downloadSources The downloadSources to set.
     */
    public void setDownloadSources( boolean downloadSources )
    {
        this.downloadSources = downloadSources;
    }

    /**
     * return <code>false</code> if projects available in a reactor build should be considered normal dependencies,
     * <code>true</code> if referenced project will be linked and not need artifact resolution.
     * @return <code>true</code> if referenced project will be linked and not need artifact resolution
     */
    protected abstract boolean getUseProjectReferences();

    /**
     * Hook for preparation steps before the actual plugin execution.
     * @return <code>true</code> if execution should continue or <code>false</code> if not.
     * @throws MojoExecutionException generic mojo exception
     */
    protected abstract boolean setup()
        throws MojoExecutionException;

    /**
     * Main plugin method where dependencies should be processed in order to generate IDE configuration files.
     * @param deps list of  <code>IdeDependency</code> objects, with artifacts, sources and javadocs already resolved
     * @throws MojoExecutionException generic mojo exception
     */
    protected abstract void writeConfiguration( IdeDependency[] deps )
        throws MojoExecutionException;

    /**
     * Not a plugin parameter. Collect the list of dependencies with a missing source artifact for the final report.
     */
    private List missingSourceDependencies = new ArrayList();

    /**
     * @see org.codehaus.plexus.logging.LogEnabled#enableLogging(org.codehaus.plexus.logging.Logger)
     */
    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        boolean processProject = setup();
        if ( !processProject )
        {
            return;
        }

        // resolve artifacts
        IdeDependency[] deps = doDependencyResolution();

        resolveSourceArtifacts( deps );

        writeConfiguration( deps );

        reportMissingSources();

    }

    /**
     * Resolve project dependencies. Manual resolution is needed in order to avoid resoltion of multiproject artifacts
     * (if projects will be linked each other an installed jar is not needed) and to avoid a failure when a jar is
     * missing.
     * @throws MojoExecutionException if dependencies can't be resolved
     * @return resoved IDE dependencies, with attached jars for non-reactor dependencies
     */
    protected IdeDependency[] doDependencyResolution()
        throws MojoExecutionException
    {
        MavenProject project = getProject();
        ArtifactRepository localRepo = getLocalRepository();

        List dependencies = getProject().getDependencies();

        // Collect the list of resolved IdeDependencies.
        List dependencyList = new ArrayList();

        if ( dependencies != null )
        {
            Map managedVersions = createManagedVersionMap( getArtifactFactory(), project.getId(), project
                .getDependencyManagement() );

            ArtifactResolutionResult artifactResolutionResult = null;

            try
            {

                List listeners = new ArrayList();

                if ( logger.isDebugEnabled() )
                {
                    listeners.add( new DebugResolutionListener( logger ) );
                }

                listeners.add( new WarningResolutionListener( logger ) );

                artifactResolutionResult = artifactCollector.collect( getProjectArtifacts(), project.getArtifact(),
                                                                      managedVersions, localRepo, project
                                                                          .getRemoteArtifactRepositories(),
                                                                      getArtifactMetadataSource(), null, listeners );
            }
            catch ( ArtifactResolutionException e )
            {
                getLog().debug( e.getMessage(), e );
                getLog().error(
                                Messages
                                    .getString( "artifactresolution", new Object[] { //$NON-NLS-1$
                                                e.getGroupId(), e.getArtifactId(), e.getVersion(), e.getMessage() } ) );

                // if we are here artifactResolutionResult is null, create a project without dependencies but don't fail
                // (this could be a reactor projects, we don't want to fail everything)
                return new IdeDependency[0];
            }

            // keep track of added reactor projects in order to avoid duplicates
            Set emittedReactorProjectId = new HashSet();

            for ( Iterator i = artifactResolutionResult.getArtifactResolutionNodes().iterator(); i.hasNext(); )
            {
                ResolutionNode node = (ResolutionNode) i.next();
                Artifact art = node.getArtifact();
                boolean isReactorProject = getUseProjectReferences() && isAvailableAsAReactorProject( art );

                // don't resolve jars for reactor projects
                if ( !isReactorProject )
                {
                    try
                    {
                        artifactResolver.resolve( art, node.getRemoteRepositories(), localRepository );
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        getLog().debug( e.getMessage(), e );
                        getLog().warn(
                                       Messages.getString( "artifactdownload", new Object[] { //$NON-NLS-1$
                                                               e.getGroupId(),
                                                               e.getArtifactId(),
                                                               e.getVersion(),
                                                               e.getMessage() } ) );
                    }
                    catch ( ArtifactResolutionException e )
                    {
                        getLog().debug( e.getMessage(), e );
                        getLog().warn(
                                       Messages.getString( "artifactresolution", new Object[] { //$NON-NLS-1$
                                                               e.getGroupId(),
                                                               e.getArtifactId(),
                                                               e.getVersion(),
                                                               e.getMessage() } ) );
                    }
                }

                if ( !isReactorProject || emittedReactorProjectId.add( art.getGroupId() + '-' + art.getArtifactId() ) )
                {

                    IdeDependency dep = new IdeDependency( art.getGroupId(), art.getArtifactId(), art.getVersion(),
                                                           isReactorProject, Artifact.SCOPE_TEST
                                                               .equals( art.getScope() ), Artifact.SCOPE_SYSTEM
                                                               .equals( art.getScope() ), Artifact.SCOPE_PROVIDED
                                                               .equals( art.getScope() ), art.getArtifactHandler()
                                                               .isAddedToClasspath(), art.getFile(), art.getType() );

                    dependencyList.add( dep );
                }

            }

            //@todo a final report with the list of missingArtifacts?

        }

        IdeDependency[] deps = (IdeDependency[]) dependencyList.toArray( new IdeDependency[dependencyList.size()] );

        return deps;
    }

    /**
     * Returns the list of project artifacts. Also artifacts generated from referenced projects will be added, but with
     * the <code>resolved</code> property set to true.
     * @return list of projects artifacts
     * @throws MojoExecutionException if unable to parse dependency versions
     */
    private Set getProjectArtifacts()
        throws MojoExecutionException
    {
        // keep it sorted, this should avoid random classpath order in tests
        Set artifacts = new TreeSet();

        for ( Iterator dependencies = getProject().getDependencies().iterator(); dependencies.hasNext(); )
        {
            Dependency dependency = (Dependency) dependencies.next();

            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            VersionRange versionRange;
            try
            {
                versionRange = VersionRange.createFromVersionSpec( dependency.getVersion() );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new MojoExecutionException( Messages.getString( "unabletoparseversion", new Object[] { //$NON-NLS-1$
                                                                          dependency.getArtifactId(),
                                                                          dependency.getVersion(),
                                                                          dependency.getManagementKey(),
                                                                          e.getMessage() } ), e );
            }

            String type = dependency.getType();
            if ( type == null )
            {
                type = "jar"; //$NON-NLS-1$
            }
            String classifier = dependency.getClassifier();
            boolean optional = dependency.isOptional();
            String scope = dependency.getScope();
            if ( scope == null )
            {
                scope = Artifact.SCOPE_COMPILE;
            }

            Artifact art = getArtifactFactory().createDependencyArtifact( groupId, artifactId, versionRange, type,
                                                                          classifier, scope, optional );

            if ( scope.equalsIgnoreCase( Artifact.SCOPE_SYSTEM ) )
            {
                art.setFile( new File( dependency.getSystemPath() ) );
            }

            List exclusions = new ArrayList();
            for ( Iterator j = dependency.getExclusions().iterator(); j.hasNext(); )
            {
                Exclusion e = (Exclusion) j.next();
                exclusions.add( e.getGroupId() + ":" + e.getArtifactId() ); //$NON-NLS-1$
            }

            ArtifactFilter newFilter = new ExcludesArtifactFilter( exclusions );

            art.setDependencyFilter( newFilter );

            artifacts.add( art );
        }

        return artifacts;
    }

    /**
     * Utility method that locates a project producing the given artifact.
     * @param artifact the artifact a project should produce.
     * @return <code>true</code> if the artifact is produced by a reactor projectart.
     */
    private boolean isAvailableAsAReactorProject( Artifact artifact )
    {
        if ( reactorProjects != null )
        {
            for ( Iterator iter = reactorProjects.iterator(); iter.hasNext(); )
            {
                MavenProject reactorProject = (MavenProject) iter.next();

                if ( reactorProject.getGroupId().equals( artifact.getGroupId() )
                    && reactorProject.getArtifactId().equals( artifact.getArtifactId() ) )
                {
                    if ( reactorProject.getVersion().equals( artifact.getVersion() ) )
                    {
                        return true;
                    }
                    else
                    {
                        getLog()
                            .info(
                                   "Artifact "
                                       + artifact.getId()
                                       + " already available as a reactor project, but with different version. Expected: "
                                       + artifact.getVersion() + ", found: " + reactorProject.getVersion() );
                    }
                }
            }
        }
        return false;
    }

    private Map createManagedVersionMap( ArtifactFactory artifactFactory, String projectId,
                                         DependencyManagement dependencyManagement )
        throws MojoExecutionException
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
                                                                                  versionRange, d.getType(), d
                                                                                      .getClassifier(), d.getScope(), d
                                                                                      .isOptional() );
                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new MojoExecutionException( Messages.getString( "unabletoparseversion", new Object[] { //$NON-NLS-1$
                                                                              projectId,
                                                                              d.getVersion(),
                                                                              d.getManagementKey(),
                                                                              e.getMessage() } ), e );
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
     * Resolve source artifacts and download them if <code>downloadSources</code> is <code>true</code>. Source and
     * javadocs artifacts will be attached to the <code>IdeDependency</code>
     * @param deps resolved dependencies
     */
    private void resolveSourceArtifacts( IdeDependency[] deps )
    {

        ArtifactRepository localRepository = getLocalRepository();
        ArtifactResolver artifactResolver = getArtifactResolver();
        ArtifactFactory artifactFactory = getArtifactFactory();

        // if downloadSources is off, just check local repository for reporting missing jars
        List remoteRepos = getDownloadSources() ? getRemoteArtifactRepositories() : Collections.EMPTY_LIST;

        for ( int j = 0; j < deps.length; j++ )
        {
            IdeDependency dependency = deps[j];

            if ( dependency.isReferencedProject() || dependency.isSystemScoped() )
            {
                // source artifact not needed
                continue;
            }

            // source artifact: use the "sources" classifier added by the source plugin
            Artifact sourceArtifact = IdeUtils.resolveArtifactWithClassifier( dependency.getGroupId(), dependency
                .getArtifactId(), dependency.getVersion(), "sources", localRepository, artifactResolver, //$NON-NLS-1$
                                                                              artifactFactory, remoteRepos, getLog() );

            if ( sourceArtifact.isResolved() )
            {
                dependency.setSourceAttachment( sourceArtifact.getFile() );
            }
            else
            {
                // try using a plain javadoc jar if the source jar is not available
                Artifact javadocArtifact = IdeUtils.resolveArtifactWithClassifier( dependency.getGroupId(), dependency
                    .getArtifactId(), dependency.getVersion(), "javadoc", localRepository, artifactResolver, //$NON-NLS-1$
                                                                                   artifactFactory, remoteRepos,
                                                                                   getLog() );

                if ( javadocArtifact.isResolved() )
                {
                    dependency.setJavadocAttachment( javadocArtifact.getFile() );
                }

                // @todo also report deps without a source attachment but with a javadoc one?
                missingSourceDependencies.add( dependency );
            }
        }
    }

    /**
     * Output a message with the list of missing dependencies and info on how turn download on if it was disabled.
     */
    private void reportMissingSources()
    {
        if ( missingSourceDependencies.isEmpty() )
        {
            return;
        }

        StringBuffer msg = new StringBuffer();

        if ( getDownloadSources() )
        {
            msg.append( Messages.getString( "sourcesnotavailable" ) ); //$NON-NLS-1$
        }
        else
        {
            msg.append( Messages.getString( "sourcesnotdownloaded" ) ); //$NON-NLS-1$
        }

        for ( Iterator it = missingSourceDependencies.iterator(); it.hasNext(); )
        {
            IdeDependency art = (IdeDependency) it.next();
            msg.append( Messages.getString( "sourcesmissingitem", art.getId() ) ); //$NON-NLS-1$
        }
        msg.append( "\n" ); //$NON-NLS-1$

        getLog().info( msg ); //$NON-NLS-1$

    }

}
