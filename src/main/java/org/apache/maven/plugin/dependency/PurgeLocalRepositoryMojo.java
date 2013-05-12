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
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Remove the project dependencies from the local repository, and optionally re-resolve them.
 * 
 * @author jdcasey
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "purge-local-repository", threadSafe = true )
public class PurgeLocalRepositoryMojo
    extends AbstractMojo
{

    public static final String FILE_FUZZINESS = "file";

    public static final String VERSION_FUZZINESS = "version";

    public static final String ARTIFACT_ID_FUZZINESS = "artifactId";

    public static final String GROUP_ID_FUZZINESS = "groupId";

    /**
     * The current Maven project.
     */
    @Component
    private MavenProject project;

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
    protected List<ArtifactRepository> remoteRepositories;

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

    /**
     * Skip plugin execution completely.
     *
     * @since 2.7
     */
    @Parameter( property = "skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Includes only direct project dependencies.
     */
    private class DirectDependencyFilter
        implements ArtifactFilter
    {

        private Artifact projectArtifact;

        private Set<Artifact> directDependencyArtifacts;

        /**
         * Default constructor
         * 
         * @param directDependencyArtifacts Set of Artifact objects which represent the direct dependencies of the
         *            project
         */
        public DirectDependencyFilter( Artifact projectArtifact, Set<Artifact> directDependencyArtifacts )
        {
            this.projectArtifact = projectArtifact;
            this.directDependencyArtifacts = directDependencyArtifacts;
        }

        public boolean include( Artifact artifact )
        {
            if ( artifactsGAMatch( artifact, projectArtifact ) )
            {
                return true;
            }
            for ( Artifact depArtifact : directDependencyArtifacts )
            {
                if ( this.artifactsGAMatch( artifact, depArtifact ) )
                {
                    return true;
                }
            }
            return false;
        }

        /*
         * Compare the groupId:artifactId of two artifacts.
         */
        private boolean artifactsGAMatch( Artifact artifact1, Artifact artifact2 )
        {
            if ( artifact1 == artifact2 )
            {
                return true;
            }

            if ( !artifact1.getGroupId().equals( artifact2.getGroupId() ) )
            {
                getLog().debug( "Different groupId: " + artifact1 + "  " + artifact2 );
                return false;
            }
            if ( !artifact1.getArtifactId().equals( artifact2.getArtifactId() ) )
            {
                getLog().debug( "Different artifactId: " + artifact1 + "  " + artifact2 );
                return false;
            }
            return true;
        }
    }

    /**
     * Includes only artifacts that do not use system scope
     */
    private class SystemScopeExcludeFilter
        implements ArtifactFilter
    {
        public boolean include( Artifact artifact )
        {
            return !Artifact.SCOPE_SYSTEM.equals( artifact.getScope() );
        }
    }

    /**
     * Includes only snapshot artifacts
     */
    private class SnapshotsFilter
        implements ArtifactFilter
    {
        public boolean include( Artifact artifact )
        {
            return artifact.isSnapshot();
        }
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( isSkip() )
        {
            getLog().info( "Skipping plugin execution" );
            return;
        }

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

        Set<Artifact> dependencyArtifacts = null;

        try
        {
            dependencyArtifacts = project.createArtifacts( factory, null, null );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new MojoFailureException( "Unable to purge dependencies due to invalid dependency version ", e );
        }

        ArtifactFilter artifactFilter = createPurgeArtifactsFilter( dependencyArtifacts );

        Set<Artifact> resolvedArtifactsToPurge =
            getFilteredResolvedArtifacts( project, dependencyArtifacts, artifactFilter );

        if ( resolvedArtifactsToPurge.isEmpty() )
        {
            getLog().info( "No artifacts included for purge for project: " + project.getId() );
            return;
        }

        verbose( "Purging dependencies for project: " + project.getId() );
        purgeArtifacts( resolvedArtifactsToPurge );

        if ( reResolve )
        {
            try
            {
                reResolveArtifacts( project, resolvedArtifactsToPurge, artifactFilter );
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

    /**
     * Purge/Delete artifacts from the local repository according to the given patterns.
     * 
     * @param inclusionPatterns
     * @throws MojoExecutionException
     */
    private void manualPurge( List<String> includes )
        throws MojoExecutionException
    {
        for ( String gavPattern : includes )
        {
            if ( StringUtils.isEmpty( gavPattern ) )
            {
                getLog().debug( "Skipping empty gav pattern: " + gavPattern );
                continue;
            }

            String relativePath = gavToPath( gavPattern );
            if ( StringUtils.isEmpty( relativePath ) )
            {
                continue;
            }

            File purgeDir = new File( localRepository.getBasedir(), relativePath );
            if ( purgeDir.exists() )
            {
                getLog().debug( "Deleting directory: " + purgeDir );
                try
                {
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
     * Convert a groupId:artifactId:version to a file system path
     * 
     * @param gav, the groupId:artifactId:version string
     * @return
     */
    private String gavToPath( String gav )
    {
        if ( StringUtils.isEmpty( gav ) )
        {
            return null;
        }

        String[] pathComponents = gav.split( ":" );

        StringBuffer path = new StringBuffer( pathComponents[0].replace( '.', '/' ) );

        for ( int i = 1; i < pathComponents.length; ++i )
        {
            path.append( "/" + pathComponents[i] );
        }

        return path.toString();
    }

    /**
     * Create the includes exclude filter to use when resolving and purging dependencies Also excludes any "system"
     * scope dependencies
     * 
     * @param dependencyArtifacts The dependency artifacts to use as a reference if we're excluding transitive
     *            dependencies
     * @return
     */
    private ArtifactFilter createPurgeArtifactsFilter( Set<Artifact> dependencyArtifacts )
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
        if ( this.includes != null )
        {
            andFilter.add( new PatternIncludesArtifactFilter( includes ) );
        }

        if ( !StringUtils.isEmpty( this.exclude ) )
        {
            this.excludes = parseIncludes( this.exclude );
        }
        if ( this.excludes != null )
        {
            andFilter.add( new PatternExcludesArtifactFilter( excludes ) );
        }

        if ( !actTransitively )
        {
            andFilter.add( new DirectDependencyFilter( project.getArtifact(), dependencyArtifacts ) );
        }

        return andFilter;
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

    private Set<Artifact> getFilteredResolvedArtifacts( MavenProject project, Set<Artifact> artifacts,
                                                        ArtifactFilter filter )
    {
        try
        {
            ArtifactResolutionResult result =
                resolver.resolveTransitively( artifacts, project.getArtifact(), localRepository, remoteRepositories,
                                              metadataSource, filter );

            @SuppressWarnings( "unchecked" )
            Set<Artifact> resolvedArtifacts = result.getArtifacts();

            return resolvedArtifacts;
        }
        catch ( ArtifactResolutionException e )
        {
            getLog().info( "Unable to resolve all dependencies for : " + e.getGroupId() + ":" + e.getArtifactId() + ":"
                               + e.getVersion()
                               + ". Falling back to non-transitive mode for initial artifact resolution." );
        }
        catch ( ArtifactNotFoundException e )
        {
            getLog().info( "Unable to resolve all dependencies for : " + e.getGroupId() + ":" + e.getArtifactId() + ":"
                               + e.getVersion()
                               + ". Falling back to non-transitive mode for initial artifact resolution." );
        }

        Set<Artifact> resolvedArtifacts = new LinkedHashSet<Artifact>();
        // Resolve the only poms here instead of the actual artifacts, because the files will be deleted during the
        // purge anyway
        for ( Artifact artifact : artifacts )
        {
            if ( filter.include( artifact ) )
            {
                try
                {
                    resolvedArtifacts.add( artifact );
                    resolver.resolve( artifact, remoteRepositories, localRepository );
                }
                catch ( ArtifactResolutionException e )
                {
                    getLog().debug( "Unable to resolve artifact: " + artifact );
                }
                catch ( ArtifactNotFoundException e )
                {
                    getLog().debug( "Unable to resolve artifact: " + artifact );
                }
            }
        }
        return resolvedArtifacts;
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
                    getLog().warn( "Unable to purge local repository location: " + deleteTarget, e );
                }
            }
            else
            {
                if ( !deleteTarget.delete() )
                {
                    getLog().warn( "Unable to purge local repository location: " + deleteTarget );
                }
            }
            artifact.setResolved( false );
        }
    }

    private void reResolveArtifacts( MavenProject project, Set<Artifact> artifacts, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {

        // Always need to re-resolve the poms in case they were purged along with the artifact
        // because Maven 2 will not automatically re-resolve them when resolving the artifact
        for ( Artifact artifact : artifacts )
        {
            try
            {
                Artifact pomArtifact =
                    factory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                            null, "pom" );
                resolver.resolveAlways( pomArtifact, remoteRepositories, localRepository );
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

    private File findDeleteTarget( Artifact artifact )
    {
        // Use localRepository.pathOf() in case artifact.getFile() is not set
        File deleteTarget = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );

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

    public boolean isSkip()
    {
        return skip;
    }

    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }
}
