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
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Remove the project dependencies from the local repository, and optionally
 * re-resolve them.
 *
 * @author jdcasey
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "purge-local-repository", aggregator = true )
public class PurgeLocalRepositoryMojo
    extends AbstractMojo
{

    public static final String FILE_FUZZINESS = "file";

    public static final String VERSION_FUZZINESS = "version";

    public static final String ARTIFACT_ID_FUZZINESS = "artifactId";

    public static final String GROUP_ID_FUZZINESS = "groupId";

    /**
     * The projects in the current build. Each of these is subject to
     * refreshing.
     */
    @Parameter( defaultValue = "${reactorProjects}", readonly = true, required = true )
    private List<MavenProject> projects;

    /**
     * The list of dependencies in the form of groupId:artifactId which should
     * BE deleted/purged from the local repository.  Note that using this
     * parameter will deactivate the normal process for purging the current project
     * dependency tree.  If this parameter is used, only the included artifacts will
     * be purged.
     * 
     * The includes parameter should not be used in combination with the
     * includes/excludes parameters.
     * 
     * @since 2.6
     */
    @Parameter
    private List<String> manualIncludes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used
     * to manually include artifacts for deletion. This is a command-line
     * alternative to the <code>manualIncludes</code> parameter, since List
     * parameters are not currently compatible with CLI specification.
     * 
     * @since 2.6
     */
    @Parameter( property = "manualInclude" )
    private String manualInclude;

    /**
     * The list of dependencies in the form of groupId:artifactId which should
     * BE deleted/refreshed. 
     * 
     * @since 2.6
     */
    @Parameter
    private List<String> includes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used
     * to include artifacts for deletion/refresh. This is a command-line
     * alternative to the <code>includes</code> parameter, since List
     * parameters are not currently compatible with CLI specification.
     * 
     * @since 2.6
     */
    @Parameter( property = "include" )
    private String include;

    /**
     * The list of dependencies in the form of groupId:artifactId which should
     * NOT be deleted/refreshed.
     */
    @Parameter
    private List<String> excludes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used
     * to exclude artifacts from deletion/refresh. This is a command-line
     * alternative to the <code>excludes</code> parameter, since List
     * parameters are not currently compatible with CLI specification.
     */
    @Parameter( property = "exclude" )
    private String exclude;

    /**
     * Whether to re-resolve the artifacts once they have been deleted from the
     * local repository. If you are running this mojo from the command-line, you
     * may want to disable this. By default, artifacts will be re-resolved.
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
     * The artifact resolver used to re-resolve dependencies, if that option is
     * enabled.
     */
    @Component
    private ArtifactResolver resolver;

    /**
     * The artifact metadata source used to resolve dependencies
     */
    @Component
    private ArtifactMetadataSource source;

    /**
     * Determines how liberally the plugin will delete an artifact from the
     * local repository. Values are: <br/>
     * <ul>
     * <li><b>file</b> - Eliminate only the artifact's file.</li>
     * <li><b>version</b> <i>(default)</i> - Eliminate all files associated 
     * with the version of the artifact.</li>
     * <li><b>artifactId</b> - Eliminate all files associated with the
     * artifact's artifactId.</li>
     * <li><b>groupId</b> - Eliminate all files associated with the artifact's
     * groupId.</li>
     * </ul>
     */
    @Parameter( property = "resolutionFuzziness", defaultValue = "version" )
    private String resolutionFuzziness;

    /**
     * Whether this mojo should act on all transitive dependencies. Default
     * value is true.
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

        List<String> manualInclusionPatterns = buildInclusionPatternsList( manualIncludes, manualInclude );

        if ( manualInclusionPatterns.size() > 0 )
        {
            manualPurge( manualInclusionPatterns );
            return;
        }

        List<String> inclusionPatterns = buildInclusionPatternsList(includes, include);

        List<String> exclusionPatterns = buildInclusionPatternsList(excludes, exclude);

        for ( MavenProject project : projects )
        {
            try
            {
                refreshDependenciesForProject( project, inclusionPatterns, exclusionPatterns );
            }
            catch ( ArtifactResolutionException e )
            {
                MojoFailureException failure =
                    new MojoFailureException( this, "Failed to refresh project dependencies for: " + project.getId(),
                                              "Artifact resolution failed for project: " + project.getId() );
                failure.initCause( e );

                throw failure;
            }
        }
    }

    /**
     * Purge artifacts from the local repository according to the given patterns.
     * 
     * @param inclusionPatterns
     * @throws MojoExecutionException
     */
    private void manualPurge( List<String> inclusionPatterns )
        throws MojoExecutionException
    {
        for ( String pattern : inclusionPatterns )
        {
            if ( StringUtils.isEmpty( pattern ) )
            {
                throw new MojoExecutionException( "The groupId:artifactId for manualIncludes cannot be empty" );
            }
            String relativePath = gaStringtoPath( pattern );
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
    private String gaStringtoPath( String ga )
    {
        if ( ga == null || ga.equals( "" ) )
        {
            return null;
        }
        // Replace wildcard with empty path
        String path = ga.replace( ":*", "" );
        path = path.replace( '.', '/' ).replace( ':', '/' );
        return path;
    }

    private List<String> buildInclusionPatternsList(List<String> includes, String include)
    {
        List<String> patterns = new ArrayList<String>();

        if ( include != null )
        {
            String[] elements = include.split( " ?, ?" );

            patterns.addAll( Arrays.asList( elements ) );
        }
        else if ( includes != null && !includes.isEmpty() )
        {
            patterns.addAll( includes );
        }

        return patterns;
    }

    /**
     * Map the groupId:artifactId identifiers to the artifact objects for the current project
     * @param project The current Maven project
     * @return
     */
    private Map<String, Artifact> createProjectArtifactMap( MavenProject project )
    {
        Map<String, Artifact> artifactMap = Collections.emptyMap();

        @SuppressWarnings( "unchecked" ) List<Dependency> dependencies = project.getDependencies();

        Set<Artifact> dependencyArtifacts = new HashSet<Artifact>();

        for ( Dependency dependency : dependencies )
        {
            if ( Artifact.SCOPE_SYSTEM.equals( dependency.getScope() ))
            {
                // Don't try to purge system dependencies
                continue;
            }

            VersionRange vr = VersionRange.createFromVersion( dependency.getVersion() );

            Artifact artifact =
                factory.createDependencyArtifact( dependency.getGroupId(), dependency.getArtifactId(), vr,
                                                  dependency.getType(), dependency.getClassifier(),
                                                  dependency.getScope() );
            if ( snapshotsOnly && !artifact.isSnapshot() )
            {
                continue;
            }
            dependencyArtifacts.add( artifact );
        }

        // If the transitive dependencies are included, it's necessary to resolve the
        // dependencies, even if that means going to the remote repository, to make
        // sure we get the full tree.
        if ( actTransitively )
        {
            try
            {
                ArtifactResolutionResult result;

                if ( snapshotsOnly )
                {
                    result = resolver.resolveTransitively( dependencyArtifacts, project.getArtifact(), localRepository,
                                                           remoteRepos, source, new ArtifactFilter()
                    {
                        public boolean include( Artifact artifact )
                        {
                            return artifact.isSnapshot();
                        }
                    } );
                }
                else
                {
                    result =
                        resolver.resolveTransitively( dependencyArtifacts, project.getArtifact(), remoteRepos,
                                                      localRepository, source );
                }

                artifactMap = ArtifactUtils.artifactMapByVersionlessId( result.getArtifacts() );
            }
            catch ( ArtifactResolutionException e )
            {
                verbose( "Skipping: " + e.getArtifactId() + ". It cannot be resolved." );
            }
            catch ( ArtifactNotFoundException e )
            {
                verbose( "Skipping: " + e.getArtifactId() + ". It cannot be resolved." );
            }
        }
        // If we don't care about transitive dependencies, there is no need to resolve
        // from the remote repositories, we can just use the local path
        else
        {
            artifactMap = new HashMap<String, Artifact>();
            for ( Artifact artifact : dependencyArtifacts )
            {
                String localPath = localRepository.pathOf( artifact ); 
                artifact.setFile( new File( localRepository.getBasedir(), localPath ) );
                artifactMap.put( ArtifactUtils.versionlessKey( artifact ), artifact );
            }
        }

        return artifactMap;
    }

    private void verbose( String message )
    {
        if ( verbose || getLog().isDebugEnabled() )
        {
            getLog().info( message );
        }
    }

    private void refreshDependenciesForProject( MavenProject project, List<String> inclusionPatterns, List<String> exclusionPatterns )
        throws ArtifactResolutionException, MojoFailureException
    {
        Map<String, Artifact> artifactMap = createProjectArtifactMap( project );

        if ( artifactMap.isEmpty() )
        {
            getLog().info( "Nothing to do for project: " + project.getId() );
            return;
        }

        Map<String, Artifact> depsAfterInclusion = new HashMap<String, Artifact>();

        if ( !inclusionPatterns.isEmpty() )
        {
            for ( Iterator<Map.Entry<String, Artifact>> artifactIter = artifactMap.entrySet().iterator(); artifactIter.hasNext(); )
            {
                Map.Entry<String, Artifact> artifactEntry = artifactIter.next();

                Artifact artifact = artifactEntry.getValue();

                if ( resolutionFuzziness.equals( GROUP_ID_FUZZINESS ) )
                {
                    if ( inclusionPatterns.contains( artifact.getGroupId() ) )
                    {
                        verbose( "Including groupId: " + artifact.getGroupId() + " for refresh operation for project: "
                            + project.getId() );
                        depsAfterInclusion.put( artifactEntry.getKey(), artifactEntry.getValue() );
                    }
                }
                else
                {
                    String artifactKey = ArtifactUtils.versionlessKey( artifact );
                    if ( inclusionPatterns.contains( artifactKey ) )
                    {
                        verbose( "Including artifact: " + artifactKey + " for refresh operation for project: "
                            + project.getId() );
                        depsAfterInclusion.put( artifactEntry.getKey(), artifactEntry.getValue() );
                    }
                }
            }

            if ( depsAfterInclusion.isEmpty() )
            {
                getLog().info( "Nothing to include for project: " + project.getId() + ". Ending purge." );
                return;
            }

            // replacing deps by the one included in order to apply the exclusion pattern.
            artifactMap = depsAfterInclusion;
        }

        if ( !exclusionPatterns.isEmpty() )
        {
            for ( String excludedKey : exclusionPatterns )
            {
                if ( resolutionFuzziness.equals( GROUP_ID_FUZZINESS ) )
                {
                    verbose( "Excluding groupId: " + excludedKey + " from refresh operation for project: "
                                 + project.getId() );

                    for ( Iterator<Map.Entry<String, Artifact>> artifactIter = artifactMap.entrySet().iterator();
                                    artifactIter.hasNext(); )
                    {
                        Map.Entry<String, Artifact> artifactEntry = artifactIter.next();

                        Artifact artifact = artifactEntry.getValue();

                        if ( artifact.getGroupId().equals( excludedKey ) )
                        {
                            artifactIter.remove();
                        }
                    }
                }
                else
                {
                    verbose( "Excluding: " + excludedKey + " from refresh operation for project: " + project.getId() );

                    artifactMap.remove( excludedKey );
                }
            }
        }

        verbose( "Processing dependencies for project: " + project.getId() );

        List<Artifact> missingArtifacts = new ArrayList<Artifact>();
        for ( Map.Entry<String, Artifact> entry : artifactMap.entrySet() )
        {
            Artifact artifact = entry.getValue();

            verbose( "Processing artifact: " + artifact.getId() );

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

            if ( reResolve )
            {
                verbose( "Re-resolving." );

                artifact.setResolved( false );

                try
                {
                    resolver.resolveAlways( artifact, project.getRemoteArtifactRepositories(), localRepository );
                }
                catch ( ArtifactResolutionException e )
                {
                    getLog().debug( e.getMessage() );
                    missingArtifacts.add( artifact );
                }
                catch ( ArtifactNotFoundException e )
                {
                    getLog().debug( e.getMessage() );
                    missingArtifacts.add( artifact );
                }
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

}
