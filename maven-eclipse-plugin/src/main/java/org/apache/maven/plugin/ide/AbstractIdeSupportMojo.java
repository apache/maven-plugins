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
package org.apache.maven.plugin.ide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

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
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;

/**
 * Abstract base plugin which takes care of the common stuff usually needed by maven IDE plugins. A plugin extending
 * AbstractIdeSupportMojo should implement the <code>setup()</code> and <code>writeConfiguration()</code> methods,
 * plus the getters needed to get the various configuration flags and required components. The lifecycle:
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
     * The project packaging.
     * 
     * @parameter expression="${project.packaging}"
     */
    protected String packaging;

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
     * If the executed project is a reactor project, this will contains the full list of projects in the reactor.
     * 
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    /**
     * Skip the operation when true.
     * 
     * @parameter expression="${eclipse.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Enables/disables the downloading of source attachments. Defaults to false. When this flag is <code>true</code>
     * remote repositories are checked for sources: in order to avoid repeated check for unavailable source archives, a
     * status cache is mantained into the target dir of the root project. Run <code>mvn:clean</code> or delete the
     * file <code>mvn-eclipse-cache.properties</code> in order to reset this cache.
     * 
     * @parameter expression="${downloadSources}"
     */
    protected boolean downloadSources;

    /**
     * Enables/disables the downloading of javadoc attachments. Defaults to false. When this flag is <code>true</code>
     * remote repositories are checked for javadocs: in order to avoid repeated check for unavailable javadoc archives,
     * a status cache is mantained into the target dir of the root project. Run <code>mvn:clean</code> or delete the
     * file <code>mvn-eclipse-cache.properties</code> in order to reset this cache.
     * 
     * @parameter expression="${downloadJavadocs}"
     */
    protected boolean downloadJavadocs;

    /**
     * Plexus logger needed for debugging manual artifact resolution.
     */
    private Logger logger;

    /**
     * This eclipse workspace is read and all artifacts detected there will be connected as eclipse projects and will
     * not be linked to the jars in the local repository. Requirement is that it was created with the similar wtp
     * settings as the reactor projects, but the project name template my differ. The pom's in the workspace projects
     * may not contain variables in the artefactId, groupId and version tags.
     * 
     * @since 2.5
     * @parameter expression="${eclipse.workspace}"
     */
    protected String workspace;

    /**
     * Limit the use of project references to the current workspace. No project references will be created to projects
     * in the reactor when they are not available in the workspace.
     * 
     * @parameter expression="${eclipse.limitProjectReferencesToWorkspace}" default-value="false"
     */
    protected boolean limitProjectReferencesToWorkspace;

    /**
     * Getter for <code>artifactMetadataSource</code>.
     * 
     * @return Returns the artifactMetadataSource.
     */
    public ArtifactMetadataSource getArtifactMetadataSource()
    {
        return artifactMetadataSource;
    }

    /**
     * Setter for <code>artifactMetadataSource</code>.
     * 
     * @param artifactMetadataSource The artifactMetadataSource to set.
     */
    public void setArtifactMetadataSource( ArtifactMetadataSource artifactMetadataSource )
    {
        this.artifactMetadataSource = artifactMetadataSource;
    }

    /**
     * Getter for <code>project</code>.
     * 
     * @return Returns the project.
     */
    public MavenProject getProject()
    {
        return project;
    }

    /**
     * Setter for <code>project</code>.
     * 
     * @param project The project to set.
     */
    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * Getter for <code>reactorProjects</code>.
     * 
     * @return Returns the reactorProjects.
     */
    public List getReactorProjects()
    {
        return reactorProjects;
    }

    /**
     * Setter for <code>reactorProjects</code>.
     * 
     * @param reactorProjects The reactorProjects to set.
     */
    public void setReactorProjects( List reactorProjects )
    {
        this.reactorProjects = reactorProjects;
    }

    /**
     * Getter for <code>remoteArtifactRepositories</code>.
     * 
     * @return Returns the remoteArtifactRepositories.
     */
    public List getRemoteArtifactRepositories()
    {
        return remoteArtifactRepositories;
    }

    /**
     * Setter for <code>remoteArtifactRepositories</code>.
     * 
     * @param remoteArtifactRepositories The remoteArtifactRepositories to set.
     */
    public void setRemoteArtifactRepositories( List remoteArtifactRepositories )
    {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
    }

    /**
     * Getter for <code>artifactFactory</code>.
     * 
     * @return Returns the artifactFactory.
     */
    public ArtifactFactory getArtifactFactory()
    {
        return artifactFactory;
    }

    /**
     * Setter for <code>artifactFactory</code>.
     * 
     * @param artifactFactory The artifactFactory to set.
     */
    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    /**
     * Getter for <code>artifactResolver</code>.
     * 
     * @return Returns the artifactResolver.
     */
    public ArtifactResolver getArtifactResolver()
    {
        return artifactResolver;
    }

    /**
     * Setter for <code>artifactResolver</code>.
     * 
     * @param artifactResolver The artifactResolver to set.
     */
    public void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
    }

    /**
     * Getter for <code>executedProject</code>.
     * 
     * @return Returns the executedProject.
     */
    public MavenProject getExecutedProject()
    {
        return executedProject;
    }

    /**
     * Setter for <code>executedProject</code>.
     * 
     * @param executedProject The executedProject to set.
     */
    public void setExecutedProject( MavenProject executedProject )
    {
        this.executedProject = executedProject;
    }

    /**
     * Getter for <code>localRepository</code>.
     * 
     * @return Returns the localRepository.
     */
    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    /**
     * Setter for <code>localRepository</code>.
     * 
     * @param localRepository The localRepository to set.
     */
    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    /**
     * Getter for <code>downloadJavadocs</code>.
     * 
     * @return Returns the downloadJavadocs.
     */
    public boolean getDownloadJavadocs()
    {
        return downloadJavadocs;
    }

    /**
     * Setter for <code>downloadJavadocs</code>.
     * 
     * @param downloadJavadocs The downloadJavadocs to set.
     */
    public void setDownloadJavadocs( boolean downloadJavadoc )
    {
        downloadJavadocs = downloadJavadoc;
    }

    /**
     * Getter for <code>downloadSources</code>.
     * 
     * @return Returns the downloadSources.
     */
    public boolean getDownloadSources()
    {
        return downloadSources;
    }

    /**
     * Setter for <code>downloadSources</code>.
     * 
     * @param downloadSources The downloadSources to set.
     */
    public void setDownloadSources( boolean downloadSources )
    {
        this.downloadSources = downloadSources;
    }

    protected void setResolveDependencies( boolean resolveDependencies )
    {
        this.resolveDependencies = resolveDependencies;
    }

    protected boolean isResolveDependencies()
    {
        return resolveDependencies;
    }

    /**
     * return <code>false</code> if projects available in a reactor build should be considered normal dependencies,
     * <code>true</code> if referenced project will be linked and not need artifact resolution.
     * 
     * @return <code>true</code> if referenced project will be linked and not need artifact resolution
     */
    protected abstract boolean getUseProjectReferences();

    /**
     * Hook for preparation steps before the actual plugin execution.
     * 
     * @return <code>true</code> if execution should continue or <code>false</code> if not.
     * @throws MojoExecutionException generic mojo exception
     */
    protected abstract boolean setup()
        throws MojoExecutionException;

    /**
     * Main plugin method where dependencies should be processed in order to generate IDE configuration files.
     * 
     * @param deps list of <code>IdeDependency</code> objects, with artifacts, sources and javadocs already resolved
     * @throws MojoExecutionException generic mojo exception
     */
    protected abstract void writeConfiguration( IdeDependency[] deps )
        throws MojoExecutionException;

    /**
     * Not a plugin parameter. Collect the list of dependencies with a missing source artifact for the final report.
     */
    private List missingSourceDependencies = new ArrayList();

    /**
     * Not a plugin parameter. Collect the list of dependencies with a missing javadoc artifact for the final report.
     */
    // TODO merge this with the missingSourceDependencies in a classifier based map?
    private List missingJavadocDependencies = new ArrayList();

    /**
     * Cached array of resolved dependencies.
     */
    private IdeDependency[] ideDeps;

    /**
     * Flag for mojo implementations to control whether normal maven dependencies should be resolved. Default value is
     * true.
     */
    private boolean resolveDependencies = true;

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
        if ( skip )
        {
            return;
        }

        boolean processProject = setup();
        if ( !processProject )
        {
            return;
        }

        // resolve artifacts
        IdeDependency[] deps = doDependencyResolution();

        resolveSourceAndJavadocArtifacts( deps );

        writeConfiguration( deps );

        reportMissingArtifacts();

    }

    /**
     * Resolve project dependencies. Manual resolution is needed in order to avoid resolution of multiproject artifacts
     * (if projects will be linked each other an installed jar is not needed) and to avoid a failure when a jar is
     * missing.
     * 
     * @throws MojoExecutionException if dependencies can't be resolved
     * @return resolved IDE dependencies, with attached jars for non-reactor dependencies
     */
    protected IdeDependency[] doDependencyResolution()
        throws MojoExecutionException
    {
        if ( ideDeps == null )
        {
            if ( resolveDependencies )
            {
                MavenProject project = getProject();
                ArtifactRepository localRepo = getLocalRepository();

                List deps = getProject().getDependencies();

                // Collect the list of resolved IdeDependencies.
                List dependencies = new ArrayList();

                if ( deps != null )
                {
                    Map managedVersions =
                        createManagedVersionMap( getArtifactFactory(), project.getId(),
                                                 project.getDependencyManagement() );

                    ArtifactResolutionResult artifactResolutionResult = null;

                    try
                    {

                        List listeners = new ArrayList();

                        if ( logger.isDebugEnabled() )
                        {
                            listeners.add( new DebugResolutionListener( logger ) );
                        }

                        listeners.add( new WarningResolutionListener( logger ) );

                        artifactResolutionResult =
                            artifactCollector.collect( getProjectArtifacts(), project.getArtifact(), managedVersions,
                                                       localRepo, project.getRemoteArtifactRepositories(),
                                                       getArtifactMetadataSource(), null, listeners );
                    }
                    catch ( ArtifactResolutionException e )
                    {
                        getLog().debug( e.getMessage(), e );
                        getLog().error(
                                        Messages.getString( "artifactresolution", new Object[] { //$NON-NLS-1$
                                                            e.getGroupId(), e.getArtifactId(), e.getVersion(),
                                                                e.getMessage() } ) );

                        // if we are here artifactResolutionResult is null, create a project without dependencies but
                        // don't fail
                        // (this could be a reactor projects, we don't want to fail everything)
                        // Causes MECLIPSE-185. Not sure if it should be handled this way??
                        return new IdeDependency[0];
                    }

                    // keep track of added reactor projects in order to avoid duplicates
                    Set emittedReactorProjectId = new HashSet();

                    for ( Iterator i = artifactResolutionResult.getArtifactResolutionNodes().iterator(); i.hasNext(); )
                    {

                        ResolutionNode node = (ResolutionNode) i.next();
                        int dependencyDepth = node.getDepth();
                        Artifact art = node.getArtifact();
                        boolean isReactorProject = getUseProjectReferences() && isAvailableAsAReactorProject( art );
                        boolean isWorkspaceProject = getUseProjectReferences() && isAvailableAsAWorkspaceProject( art );
                        // don't resolve jars for reactor projects
                        if ( !isReactorProject || ( limitProjectReferencesToWorkspace && !isWorkspaceProject ) )
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
                                                                   e.getGroupId(), e.getArtifactId(), e.getVersion(),
                                                                       e.getMessage() } ) );
                            }
                            catch ( ArtifactResolutionException e )
                            {
                                getLog().debug( e.getMessage(), e );
                                getLog().warn(
                                               Messages.getString( "artifactresolution", new Object[] { //$NON-NLS-1$
                                                                   e.getGroupId(), e.getArtifactId(), e.getVersion(),
                                                                       e.getMessage() } ) );
                            }
                        }

                        if ( !isReactorProject ||
                            emittedReactorProjectId.add( art.getGroupId() + '-' + art.getArtifactId() ) )
                        {

                            // the following doesn't work: art.getArtifactHandler().getPackaging() always returns "jar"
                            // also
                            // if the packaging specified in pom.xml is different.

                            // osgi-bundle packaging is provided by the felix osgi plugin
                            // eclipse-plugin packaging is provided by this eclipse plugin
                            // String packaging = art.getArtifactHandler().getPackaging();
                            // boolean isOsgiBundle = "osgi-bundle".equals( packaging ) || "eclipse-plugin".equals(
                            // packaging );

                            // we need to check the manifest, if "Bundle-SymbolicName" is there the artifact can be
                            // considered
                            // an osgi bundle
                            boolean isOsgiBundle = false;
                            String osgiSymbolicName = null;
                            if ( art.getFile() != null )
                            {
                                JarFile jarFile = null;
                                try
                                {
                                    jarFile = new JarFile( art.getFile(), false, ZipFile.OPEN_READ );

                                    Manifest manifest = jarFile.getManifest();
                                    if ( manifest != null )
                                    {
                                        osgiSymbolicName =
                                            manifest.getMainAttributes().getValue(
                                                                                   new Attributes.Name(
                                                                                                        "Bundle-SymbolicName" ) );
                                    }
                                }
                                catch ( IOException e )
                                {
                                    getLog().info( "Unable to read jar manifest from " + art.getFile() );
                                }
                                finally
                                {
                                    if ( jarFile != null )
                                    {
                                        try
                                        {
                                            jarFile.close();
                                        }
                                        catch ( IOException e )
                                        {
                                            // ignore
                                        }
                                    }
                                }
                            }

                            isOsgiBundle = osgiSymbolicName != null;

                            boolean useProjectReference = ( isReactorProject && !limitProjectReferencesToWorkspace ) || // default
                                ( limitProjectReferencesToWorkspace && isWorkspaceProject ) || // limitProjectReferencesToWorkspace
                                ( !isReactorProject && isWorkspaceProject ); // default + workspace projects

                            IdeDependency dep =
                                new IdeDependency( art.getGroupId(), art.getArtifactId(), art.getVersion(),
                                                   art.getClassifier(), useProjectReference,
                                                   Artifact.SCOPE_TEST.equals( art.getScope() ),
                                                   Artifact.SCOPE_SYSTEM.equals( art.getScope() ),
                                                   Artifact.SCOPE_PROVIDED.equals( art.getScope() ),
                                                   art.getArtifactHandler().isAddedToClasspath(), art.getFile(),
                                                   art.getType(), isOsgiBundle, osgiSymbolicName, dependencyDepth,
                                                   getProjectNameForArifact( art ) );
                            // no duplicate entries allowed. System paths can cause this problem.
                            if ( !dependencies.contains( dep ) )
                            {
                                dependencies.add( dep );
                            }
                        }

                    }

                    // @todo a final report with the list of
                    // missingArtifacts?

                }

                ideDeps = (IdeDependency[]) dependencies.toArray( new IdeDependency[dependencies.size()] );
            }
            else
            {
                ideDeps = new IdeDependency[0];
            }
        }

        return ideDeps;
    }

    /**
     * Find the name of the project as used in eclipse.
     * 
     * @param artifact The artifact to find the eclipse name for.
     * @return The name os the eclipse project.
     */
    abstract public String getProjectNameForArifact( Artifact artifact );

    /**
     * Returns the list of project artifacts. Also artifacts generated from referenced projects will be added, but with
     * the <code>resolved</code> property set to true.
     * 
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
                throw new MojoExecutionException(
                                                  Messages.getString(
                                                                      "unabletoparseversion", new Object[] { //$NON-NLS-1$
                                                                      dependency.getArtifactId(),
                                                                          dependency.getVersion(),
                                                                          dependency.getManagementKey(), e.getMessage() } ),
                                                  e );
            }

            String type = dependency.getType();
            if ( type == null )
            {
                type = Constants.PROJECT_PACKAGING_JAR;
            }
            String classifier = dependency.getClassifier();
            boolean optional = dependency.isOptional();
            String scope = dependency.getScope();
            if ( scope == null )
            {
                scope = Artifact.SCOPE_COMPILE;
            }

            Artifact art =
                getArtifactFactory().createDependencyArtifact( groupId, artifactId, versionRange, type, classifier,
                                                               scope, optional );

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
     * 
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

                if ( reactorProject.getGroupId().equals( artifact.getGroupId() ) &&
                    reactorProject.getArtifactId().equals( artifact.getArtifactId() ) )
                {
                    if ( reactorProject.getVersion().equals( artifact.getVersion() ) )
                    {
                        return true;
                    }
                    else
                    {
                        getLog().info(
                                       "Artifact " +
                                           artifact.getId() +
                                           " already available as a reactor project, but with different version. Expected: " +
                                           artifact.getVersion() + ", found: " + reactorProject.getVersion() );
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return an array with all dependencies avalaible in the workspace, to be implemented by the subclasses.
     */
    protected IdeDependency[] getWorkspaceArtefacts()
    {
        return new IdeDependency[0];
    }

    /**
     * Utility method that locates a project in the workspace for the given artifact.
     * 
     * @param artifact the artifact a project should produce.
     * @return <code>true</code> if the artifact is produced by a reactor projectart.
     */
    private boolean isAvailableAsAWorkspaceProject( Artifact artifact )
    {
        IdeDependency[] workspaceArtefacts = getWorkspaceArtefacts();
        for ( int index = 0; workspaceArtefacts != null && index < workspaceArtefacts.length; index++ )
        {
            IdeDependency workspaceArtefact = workspaceArtefacts[index];
            if ( workspaceArtefact.getGroupId().equals( artifact.getGroupId() ) &&
                workspaceArtefact.getArtifactId().equals( artifact.getArtifactId() ) )
            {
                if ( workspaceArtefact.getVersion().equals( artifact.getVersion() ) )
                {
                    workspaceArtefact.setAddedToClasspath( true );
                    logger.debug( "Using workspace project: " + workspaceArtefact.getEclipseProjectName() );
                    return true;
                }
                else
                {
                    getLog().info(
                                   "Artifact " +
                                       artifact.getId() +
                                       " already available as a workspace project, but with different version. Expected: " +
                                       artifact.getVersion() + ", found: " + workspaceArtefact.getVersion() );
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
                    Artifact artifact =
                        artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(), versionRange,
                                                                  d.getType(), d.getClassifier(), d.getScope(),
                                                                  d.isOptional() );
                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new MojoExecutionException( Messages.getString( "unabletoparseversion", new Object[] { //$NON-NLS-1$
                                                                          projectId, d.getVersion(),
                                                                              d.getManagementKey(), e.getMessage() } ),
                                                      e );
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
     * Find the reactor target dir. executedProject doesn't have the multiproject root dir set, and the only way to
     * extract it is iterating on parent projects.
     * 
     * @param prj current project
     * @return the parent target dir.
     */
    private File getReactorTargetDir( MavenProject prj )
    {
        if ( prj.getParent() != null )
        {
            if ( prj.getParent().getBasedir() != null && prj.getParent().getBasedir().exists() )
            {
                return getReactorTargetDir( prj.getParent() );
            }
        }
        return new File( prj.getBuild().getDirectory() );
    }

    /**
     * Resolve source artifacts and download them if <code>downloadSources</code> is <code>true</code>. Source and
     * javadocs artifacts will be attached to the <code>IdeDependency</code> Resolve source and javadoc artifacts. The
     * resolved artifacts will be downloaded based on the <code>downloadSources</code> and
     * <code>downloadJavadocs</code> attributes. Source and
     * 
     * @param deps resolved dependencies
     */
    private void resolveSourceAndJavadocArtifacts( IdeDependency[] deps )
    {

        File reactorTargetDir = getReactorTargetDir( project );
        File unavailableArtifactsTmpFile = new File( reactorTargetDir, "mvn-eclipse-cache.properties" );

        getLog().info( "Using source status cache: " + unavailableArtifactsTmpFile.getAbsolutePath() );

        // create target dir if missing
        if ( !unavailableArtifactsTmpFile.getParentFile().exists() )
        {
            unavailableArtifactsTmpFile.getParentFile().mkdirs();
        }

        Properties unavailableArtifactsCache = new Properties();
        if ( unavailableArtifactsTmpFile.exists() )
        {
            InputStream is = null;
            try
            {
                is = new FileInputStream( unavailableArtifactsTmpFile );
                unavailableArtifactsCache.load( is );
            }
            catch ( IOException e )
            {
                getLog().warn( "Unable to read source status for reactor projects" );
            }
            finally
            {
                IOUtil.close( is );
            }

        }

        final List missingSources =
            resolveDependenciesWithClassifier( deps, "sources", getDownloadSources(), unavailableArtifactsCache );
        missingSourceDependencies.addAll( missingSources );

        final List missingJavadocs =
            resolveDependenciesWithClassifier( deps, "javadoc", getDownloadJavadocs(), unavailableArtifactsCache );
        missingJavadocDependencies.addAll( missingJavadocs );

        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( unavailableArtifactsTmpFile );
            unavailableArtifactsCache.store( fos, "Temporary index for unavailable sources and javadocs" );
        }
        catch ( IOException e )
        {
            getLog().warn( "Unable to cache source status for reactor projects" );
        }
        finally
        {
            IOUtil.close( fos );
        }

    }

    /**
     * Resolve the required artifacts for each of the dependency. <code>sources</code> or <code>javadoc</code>
     * artifacts (depending on the <code>classifier</code>) are attached to the dependency.
     * 
     * @param deps resolved dependencies
     * @param classifier the classifier we are looking for (either <code>sources</code> or <code>javadoc</code>)
     * @param includeRemoteRepositories flag whether we should search remote repositories for the artifacts or not
     * @param unavailableArtifactsCache cache of unavailable artifacts
     * @return the list of dependencies for which the required artifact was not found
     */
    private List resolveDependenciesWithClassifier( IdeDependency[] deps, String inClassifier,
                                                    boolean includeRemoteRepositories,
                                                    Properties unavailableArtifactsCache )
    {
        List missingClassifierDependencies = new ArrayList();

        // if downloadSources is off, just check
        // local repository for reporting missing source jars
        List remoteRepos = includeRemoteRepositories ? getRemoteArtifactRepositories() : Collections.EMPTY_LIST;

        for ( int j = 0; j < deps.length; j++ )
        {
            IdeDependency dependency = deps[j];

            if ( dependency.isReferencedProject() || dependency.isSystemScoped() )
            {
                // artifact not needed
                continue;
            }

            // MECLIPSE-151 - if the dependency is a test, get the correct classifier for it. (ignore for javadocs)
            String classifier = inClassifier;
            if ( "sources".equals( classifier ) && "tests".equals( dependency.getClassifier() ) )
            {
                classifier = "test-sources";
            }

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug(
                                "Searching for sources for " + dependency.getId() + ":" + dependency.getClassifier() +
                                    " at " + dependency.getId() + ":" + classifier );
            }
            if ( !unavailableArtifactsCache.containsKey( dependency.getId() + ":" + classifier ) )
            {
                Artifact artifact =
                    IdeUtils.resolveArtifactWithClassifier( dependency.getGroupId(), dependency.getArtifactId(),
                                                            dependency.getVersion(), classifier, localRepository,
                                                            artifactResolver, artifactFactory, remoteRepos, getLog() );
                if ( artifact.isResolved() )
                {
                    if ( "sources".equals( classifier ) || "test-sources".equals( classifier ) )
                    {
                        dependency.setSourceAttachment( artifact.getFile() );
                    }
                    else if ( "javadoc".equals( classifier ) )
                    {
                        dependency.setJavadocAttachment( artifact.getFile() );
                    }
                }
                else
                {
                    unavailableArtifactsCache.put( dependency.getId() + ":" + classifier, Boolean.TRUE.toString() );
                    // add the dependencies to the list
                    // of those lacking the required
                    // artifact
                    missingClassifierDependencies.add( dependency );
                }
            }
        }

        // return the list of dependencies missing the
        // required artifact
        return missingClassifierDependencies;

    }

    /**
     * Output a message with the list of missing dependencies and info on how turn download on if it was disabled.
     */
    private void reportMissingArtifacts()
    {
        StringBuffer msg = new StringBuffer();

        if ( !missingSourceDependencies.isEmpty() )
        {
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
        }

        if ( !missingJavadocDependencies.isEmpty() )
        {
            if ( getDownloadJavadocs() )
            {
                msg.append( Messages.getString( "javadocnotavailable" ) ); //$NON-NLS-1$
            }
            else
            {
                msg.append( Messages.getString( "javadocnotdownloaded" ) ); //$NON-NLS-1$
            }

            for ( Iterator it = missingJavadocDependencies.iterator(); it.hasNext(); )
            {
                IdeDependency art = (IdeDependency) it.next();
                msg.append( Messages.getString( "javadocmissingitem", art.getId() ) ); //$NON-NLS-1$
            }
            msg.append( "\n" ); //$NON-NLS-1$
        }
        getLog().info( msg );
    }
}
