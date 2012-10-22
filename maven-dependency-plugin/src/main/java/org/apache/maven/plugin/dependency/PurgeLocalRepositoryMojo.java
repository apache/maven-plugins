package org.apache.maven.plugin.dependency;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Remove the project dependencies from the local repository, and optionally re-resolve them.
 * 
 * @author jdcasey
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "purge-local-repository", aggregator = true, threadSafe = true )
public class PurgeLocalRepositoryMojo
    extends AbstractMojo
{

    public static final String FILE_FUZZINESS = "file";

    public static final String VERSION_FUZZINESS = "version";

    public static final String ARTIFACT_ID_FUZZINESS = "artifactId";

    public static final String GROUP_ID_FUZZINESS = "groupId";

    /**
     * The projects in the current build. Each of these is subject to refreshing.
     */
    @Parameter( defaultValue = "${reactorProjects}", readonly = true, required = true )
    private List<MavenProject> projects;

    /**
     * The list of dependencies in the form of groupId:artifactId which should BE deleted/purged from the local
     * repository. Note that using this parameter will deactivate the normal process for purging the current project
     * dependency tree. If this parameter is used, only the included artifacts will be purged. The manualIncludes
     * parameter should not be used in combination with the includes/excludes parameters.
     * 
     * @since 2.6
     */
    @Parameter
    private List<String> manualIncludes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used to manually include artifacts for
     * deletion. This is a command-line alternative to the <code>manualIncludes</code> parameter, since List parameters
     * are not currently compatible with CLI specification.
     * 
     * @since 2.6
     */
    @Parameter( property = "manualInclude" )
    private String manualInclude;

    /**
     * The list of dependencies in the form of groupId:artifactId which should BE deleted/refreshed.
     * 
     * @since 2.6
     */
    @Parameter
    private List<String> includes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used to include artifacts for
     * deletion/refresh. This is a command-line alternative to the <code>includes</code> parameter, since List
     * parameters are not currently compatible with CLI specification.
     * 
     * @since 2.6
     */
    @Parameter( property = "include" )
    private String include;

    /**
     * The list of dependencies in the form of groupId:artifactId which should NOT be deleted/refreshed.
     */
    @Parameter
    private List<String> excludes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used to exclude artifacts from
     * deletion/refresh. This is a command-line alternative to the <code>excludes</code> parameter, since List
     * parameters are not currently compatible with CLI specification.
     */
    @Parameter( property = "exclude" )
    private String exclude;

    /**
     * Whether to re-resolve the artifacts once they have been deleted from the local repository. If you are running
     * this mojo from the command-line, you may want to disable this. By default, artifacts will be re-resolved.
     */
    @Parameter( property = "reResolve", defaultValue = "true" )
    private boolean reResolve;

    /**
     * The local repository, from which to delete artifacts.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true )
    protected List<ArtifactRepository> remoteRepos;

    /**
     * The artifact resolver used to re-resolve dependencies, if that option is enabled.
     */
    @Component
    private ArtifactResolver resolver;

    /**
     * The artifact metadata source used to resolve dependencies
     */
    @Component
    private ArtifactMetadataSource metadataSource;

    /**
     * The artifact resolution listener used to resolve ranges
     */
    // @Component
    // private ResolutionListener listener;

    /**
     * Determines how liberally the plugin will delete an artifact from the local repository. Values are: <br/>
     * <ul>
     * <li><b>file</b> - Eliminate only the artifact's file.</li>
     * <li><b>version</b> <i>(default)</i> - Eliminate all files associated with the version of the artifact.</li>
     * <li><b>artifactId</b> - Eliminate all files associated with the artifact's artifactId.</li>
     * <li><b>groupId</b> - Eliminate all files associated with the artifact's groupId.</li>
     * </ul>
     */
    @Parameter( property = "resolutionFuzziness", defaultValue = "version" )
    private String resolutionFuzziness;

    /**
     * Whether this mojo should act on all transitive dependencies. Default value is true.
     */
    @Parameter( property = "actTransitively", defaultValue = "true" )
    private boolean actTransitively;

    /**
     * Used to construct artifacts for deletion/resolution...
     */
    @Component
    private ArtifactFactory factory;

    /**
     * Whether this plugin should output verbose messages. Default is false.
     */
    @Parameter( property = "verbose", defaultValue = "false" )
    private boolean verbose;

    /**
     * Whether to purge only snapshot artifacts.
     * 
     * @since 2.4
     */
    @Parameter( property = "snapshotsOnly", defaultValue = "false" )
    private boolean snapshotsOnly;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !StringUtils.isEmpty( manualInclude ) )
        {
            manualIncludes = this.parseIncludes( manualInclude );
        }

        // If it's a manual purge, the only step is to delete from the local repo
        if ( manualIncludes != null && manualIncludes.size() > 0 )
        {
            manualPurge( manualIncludes );
            return;
        }

        ArtifactFilter artifactFilter = createPurgeArtifactsFilter();

        for ( MavenProject project : projects )
        {
            Set<Artifact> unresolvedArtifacts = getProjectArtifacts( project );
            Set<Artifact> resolvedArtifactsToPurge =
                getFilteredResolvedArtifacts( project, unresolvedArtifacts, artifactFilter );

            if ( resolvedArtifactsToPurge.isEmpty() )
            {
                getLog().info( "No artifacts included for purge for project: " + project.getId() );
                continue;
            }

            verbose( "Purging dependencies for project: " + project.getId() );
            purgeArtifacts( resolvedArtifactsToPurge );

            if ( this.reResolve )
            {
                try
                {
                    this.reResolveArtifacts( project, resolvedArtifactsToPurge, artifactFilter );
                }
                catch ( ArtifactResolutionException e )
                {
                    String failureMessage = "Failed to refresh project dependencies for: " + project.getId();
                    MojoFailureException failure = new MojoFailureException( failureMessage );
                    failure.initCause( e );

                    throw failure;
                }
                catch ( ArtifactNotFoundException e )
                {
                    String failureMessage = "Failed to refresh project dependencies for: " + project.getId();
                    MojoFailureException failure = new MojoFailureException( failureMessage );
                    failure.initCause( e );

                    throw failure;
                }
            }
        }
    }

    private class SystemScopeExcludeFilter
        implements ArtifactFilter
    {
        public boolean include( Artifact artifact )
        {
            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                return false;
            }
            else
            {
                return true;
            }
        }
    }

    private class SnapshotsFilter
        implements ArtifactFilter
    {
        public boolean include( Artifact artifact )
        {
            return artifact.isSnapshot();
        }
    }

    /**
     * Create the includes exclude filter to use when resolving and purging dependencies Also excludes any "system"
     * scope dependencies
     */
    private ArtifactFilter createPurgeArtifactsFilter()
    {
        AndArtifactFilter andFilter = new AndArtifactFilter();

        // System dependencies should never be purged
        andFilter.add( new SystemScopeExcludeFilter() );

        if ( this.snapshotsOnly )
        {
            andFilter.add( new SnapshotsFilter() );
        }

        // The CLI includes/excludes overrides configuration in the pom
        if ( !StringUtils.isEmpty( this.include ) )
        {
            this.includes = parseIncludes( this.include );
        }
        if ( !StringUtils.isEmpty( this.exclude ) )
        {
            this.excludes = parseIncludes( this.exclude );
        }

        if ( this.includes != null )
        {
            andFilter.add( new PatternIncludesArtifactFilter( includes ) );
        }
        if ( this.excludes != null )
        {
            andFilter.add( new PatternExcludesArtifactFilter( excludes ) );
        }

        return andFilter;
    }

    /**
     * Purge/Delete artifacts from the local repository according to the given patterns.
     * 
     * @param inclusionPatterns
     * @throws MojoExecutionException
     */
    private void manualPurge( List<String> includes )
        throws MojoExecutionException
    {
        for ( String pattern : includes )
        {
            if ( StringUtils.isEmpty( pattern ) )
            {
                throw new MojoExecutionException( "The groupId:artifactId for manualIncludes cannot be empty" );
            }
            String relativePath = groupIdArtifactIdtoPath( pattern );
            File purgeDir = new File( localRepository.getBasedir(), relativePath );
            if ( purgeDir.exists() )
            {
                try
                {
                    verbose( "Deleting directory: " + purgeDir );
                    FileUtils.deleteDirectory( purgeDir );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to purge directory: " + purgeDir );
                }
            }
        }
    }

    /**
     * Convert a groupId:artifactId to a file system path
     * 
     * @param ga
     * @return
     */
    private String groupIdArtifactIdtoPath( String groupIdArtifactId )
    {
        if ( StringUtils.isEmpty( groupIdArtifactId ) )
        {
            return null;
        }
        String[] pathComponents = groupIdArtifactId.split( ":" );
        String groupIdPath = groupIdToPath( pathComponents[0] );

        if ( pathComponents.length == 1 || pathComponents[1] == "*" )
        {
            return groupIdPath;
        }
        else
        {
            return groupIdPath + "/" + pathComponents[1];
        }
    }

    private String groupIdToPath( String groupId )
    {
        return groupId.replace( '.', '/' );
    }

    /**
     * Convert comma separated list of includes to List object
     * 
     * @param include
     * @return the includes list
     */
    private List<String> parseIncludes( String include )
    {
        List<String> includes = new ArrayList<String>();

        if ( include != null )
        {
            String[] elements = include.split( "," );
            includes.addAll( Arrays.asList( elements ) );
        }

        return includes;
    }

    /**
     * Get the unresolved project artifacts using the list of dependencies of a project
     * 
     * @throws ArtifactMetadataRetrievalException
     */
    private Set<Artifact> getProjectArtifacts( MavenProject project )
    {
        @SuppressWarnings( "unchecked" )
        List<Dependency> dependencies = project.getDependencies();

        Set<Artifact> artifacts = new HashSet<Artifact>();

        for ( Dependency dependency : dependencies )
        {
            try
            {
                VersionRange vr = VersionRange.createFromVersionSpec( dependency.getVersion() );
                Artifact artifact =
                    factory.createDependencyArtifact( dependency.getGroupId(), dependency.getArtifactId(), vr,
                                                      dependency.getType(), dependency.getClassifier(),
                                                      dependency.getScope(), dependency.isOptional() );

                artifacts.add( artifact );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                getLog().warn( "Invalid version in pom: " + e );
                continue;
            }

        }

        return artifacts;
    }

    private Set<Artifact> getFilteredResolvedArtifacts( MavenProject project, Set<Artifact> artifacts,
                                                        ArtifactFilter artifactFilter )
    {
        // If the transitive dependencies are included, it's necessary to resolve the
        // dependencies, even if that means going to the remote repository, to make
        // sure we get the full tree.
        if ( actTransitively )
        {
            try
            {
                ArtifactResolutionResult result =
                    resolver.resolveTransitively( artifacts, project.getArtifact(), project.getManagedVersionMap(),
                                                  localRepository, remoteRepos, metadataSource, artifactFilter,
                                                  Collections.emptyList() );

                @SuppressWarnings( "unchecked" )
                Set<Artifact> resolvedArtifacts = result.getArtifacts();

                return resolvedArtifacts;
            }
            catch ( ArtifactResolutionException e )
            {
                getLog().warn( "Unable to resolve dependencies for : " + e.getGroupId() + ":" + e.getArtifactId() + ":"
                                   + e.getVersion() + ". Falling back to non-transitive mode for artifact purge." );
            }
            catch ( ArtifactNotFoundException e )
            {
                getLog().warn( "Unable to resolve dependencies for: " + e.getGroupId() + ":" + e.getArtifactId() + ":"
                                   + e.getVersion() + ". Falling back to non-transitive mode for artifact purge." );
            }
        }

        // If we don't care about transitive dependencies, there is no need to resolve
        // from the remote repositories, we can just set the local path
        Set<Artifact> artifactSet = new HashSet<Artifact>();
        for ( Artifact artifact : artifacts )
        {
            if ( artifactFilter.include( artifact ) )
            {
                String localPath = localRepository.pathOf( artifact );
                artifact.setFile( new File( localRepository.getBasedir(), localPath ) );
                artifactSet.add( artifact );
            }
        }
        return artifactSet;
    }

    private void purgeArtifacts( Set<Artifact> artifacts )
        throws MojoFailureException
    {
        for ( Artifact artifact : artifacts )
        {
            verbose( "Purging artifact: " + artifact.getId() );

            File deleteTarget = findDeleteTarget( artifact );

            verbose( "Deleting: " + deleteTarget );

            if ( deleteTarget.isDirectory() )
            {
                try
                {
                    FileUtils.deleteDirectory( deleteTarget );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( this, "Cannot delete dependency from the local repository: "
                        + artifact.getId(), "Failed to delete: " + deleteTarget );
                }
            }
            else
            {
                deleteTarget.delete();
            }
            artifact.setResolved( false );
        }
    }

    private void reResolveArtifacts( MavenProject project, Set<Artifact> artifacts, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {

        // Always need to re-resolve the poms in case they were purged along with the artifact
        // Maven 2 will not automatically resolve them when resolving the artifact
        for ( Artifact artifact : artifacts )
        {
            try
            {
                Artifact pomArtifact =
                    factory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                            null, "pom" );
                resolver.resolveAlways( pomArtifact, project.getRemoteArtifactRepositories(), localRepository );
            }
            catch ( ArtifactResolutionException e )
            {
                verbose( e.getMessage() );
            }
            catch ( ArtifactNotFoundException e )
            {
                verbose( e.getMessage() );
            }
        }

        // If transitive we can just re-resolve the whole tree
        if ( actTransitively )
        {
            resolver.resolveTransitively( artifacts, project.getArtifact(), project.getManagedVersionMap(),
                                          localRepository, remoteRepos, metadataSource, filter,
                                          Collections.emptyList() );
        }
        // If not doing transitive dependency resolution, then we need to resolve one by one.
        else
        {
            List<Artifact> missingArtifacts = new ArrayList<Artifact>();

            for ( Artifact artifact : artifacts )
            {
                verbose( "Resolving artifact: " + artifact.getId() );

                try
                {
                    resolver.resolveAlways( artifact, project.getRemoteArtifactRepositories(), localRepository );
                }
                catch ( ArtifactResolutionException e )
                {
                    verbose( e.getMessage() );
                    missingArtifacts.add( artifact );
                }
                catch ( ArtifactNotFoundException e )
                {
                    verbose( e.getMessage() );
                    missingArtifacts.add( artifact );
                }
            }

            if ( missingArtifacts.size() > 0 )
            {
                String message = "required artifacts missing:\n";
                for ( Artifact missingArtifact : missingArtifacts )
                {
                    message += "  " + missingArtifact.getId() + "\n";
                }
                message += "\nfor the artifact:";

                throw new ArtifactResolutionException( message, project.getArtifact(),
                                                       project.getRemoteArtifactRepositories() );
            }
        }
    }

    private File findDeleteTarget( Artifact artifact )
    {
        File deleteTarget = artifact.getFile();

        if ( GROUP_ID_FUZZINESS.equals( resolutionFuzziness ) )
        {
            // get the groupId dir.
            deleteTarget = deleteTarget.getParentFile().getParentFile().getParentFile();
        }
        else if ( ARTIFACT_ID_FUZZINESS.equals( resolutionFuzziness ) )
        {
            // get the artifactId dir.
            deleteTarget = deleteTarget.getParentFile().getParentFile();
        }
        else if ( VERSION_FUZZINESS.equals( resolutionFuzziness ) )
        {
            // get the version dir.
            deleteTarget = deleteTarget.getParentFile();
        }
        // else it's file fuzziness.
        return deleteTarget;
    }

    private void verbose( String message )
    {
        if ( verbose || getLog().isDebugEnabled() )
        {
            getLog().info( message );
        }
    }

}
