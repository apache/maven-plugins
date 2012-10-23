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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.dependency.utils.resolvers.DefaultArtifactsResolver;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Remove the project dependencies from the local repository, and optionally re-resolve them.
 * Note: since 2.6 this mojo acts only on the current project, and not on all projects in the reactor.
 * 
 * @author jdcasey
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "purge-local-repository", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class PurgeLocalRepositoryMojo
    extends AbstractMojo
{

    public static final String FILE_FUZZINESS = "file";

    public static final String VERSION_FUZZINESS = "version";

    public static final String ARTIFACT_ID_FUZZINESS = "artifactId";

    public static final String GROUP_ID_FUZZINESS = "groupId";

    /**
     * The current project
     */
    @Component
    protected MavenProject project;


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
        @SuppressWarnings( "unchecked" )
        Set<Artifact> projectArtifacts = actTransitively ? project.getArtifacts() : project.getDependencyArtifacts();

        ArtifactFilter artifactFilter = createPurgeArtifactsFilter();

        Set<Artifact> artifactsToPurge = filterArtifacts( project, projectArtifacts, artifactFilter );

        if ( artifactsToPurge.isEmpty() )
        {
            getLog().info( "No artifacts included for purge for project: " + project.getId() );
            return;
        }

        verbose( "Purging dependencies for project: " + project.getId() );
        purgeArtifacts( artifactsToPurge );

        if ( this.reResolve )
        {
            final boolean STOP_ON_FAILURE = false;
            DefaultArtifactsResolver artifactsResolver =
                            new DefaultArtifactsResolver( resolver, this.localRepository, this.remoteRepos, STOP_ON_FAILURE );
            
            // First re-resolve all the poms
            Set<Artifact> pomArtifacts = new HashSet<Artifact>();
            for ( Artifact artifact : artifactsToPurge )
            {
                Artifact pomArtifact =
                                factory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                                        null, "pom" );
                pomArtifacts.add( pomArtifact );
            }
            artifactsResolver.resolve( pomArtifacts, getLog() );

            // Then re-resolve the artifacts
            artifactsResolver.resolve( artifactsToPurge, getLog() );
        }
    }

    private class SystemScopeExcludeFilter
        implements ArtifactFilter
    {
        public boolean include( Artifact artifact )
        {
            return !Artifact.SCOPE_SYSTEM.equals( artifact.getScope() );
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
     * Filter and resolve the set of artifacts. If we're not doing transitive dependency resolution, we just resolve the
     * artifact file in the local repo.
     * 
     * @param project
     * @param artifacts
     * @param artifactFilter
     * @return
     */
    private Set<Artifact> filterArtifacts( MavenProject project, Set<Artifact> artifacts, ArtifactFilter artifactFilter )
    {
        Set<Artifact> filteredArtifacts = new HashSet<Artifact>();
        for ( Artifact artifact : artifacts )
        {
            if ( artifactFilter.include( artifact ) )
            {
                filteredArtifacts.add( artifact );
            }
        }
        return filteredArtifacts;
    }

    /**
     * Delete the artifacts from the local repo
     * 
     * @param artifacts
     * @throws MojoFailureException
     */
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
                if ( !deleteTarget.delete() )
                {
                    throw new MojoFailureException( this, "Cannot delete dependency from the local repository: "
                                                    + artifact.getId(), "Failed to delete: " + deleteTarget );
                }
            }
            artifact.setResolved( false );
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
