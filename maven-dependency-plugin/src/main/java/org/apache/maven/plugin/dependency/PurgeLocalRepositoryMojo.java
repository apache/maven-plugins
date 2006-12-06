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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Remove the project dependencies from the local repository, and optionally
 * re-resolve them.
 * 
 * @author jdcasey
 * @since 2.0
 * @goal purge-local-repository
 * @aggregator
 * 
 */
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
     * 
     * @parameter default-value="${reactorProjects}"
     * @required
     * @readonly
     */
    private List projects;

    /**
     * The list of dependencies in the form of groupId:artifactId which should
     * NOT be deleted/refreshed. This is useful for third-party artifacts.
     * 
     * @parameter
     */
    private List excludes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used
     * to exclude artifacts from deletion/refresh. This is a command-line
     * alternative to the <code>excludes</code> parameter, since List
     * parameters are not currently compatible with CLI specification.
     * 
     * @parameter expression="${exclude}"
     */
    private String exclude;

    /**
     * Whether to re-resolve the artifacts once they have been deleted from the
     * local repository. If you are running this mojo from the command-line, you
     * may want to disable this. By default, artifacts will be re-resolved.
     * 
     * @parameter expression="${reResolve}" default-value="true"
     */
    private boolean reResolve;

    /**
     * The local repository, from which to delete artifacts.
     * 
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The artifact resolver used to re-resolve dependencies, if that option is
     * enabled.
     * 
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * The artifact metadata source used to resolve dependencies
     * 
     * @component
     */
    private ArtifactMetadataSource source;

    /**
     * Determines how liberally the plugin will delete an artifact from the
     * local repository. Values are: <br/>
     * <ul>
     * <li><b>file</b> <i>(default)</i> - Eliminate only the artifact's file.</li>
     * <li><b>version</b> - Eliminate all files associated with the artifact's
     * version.</li>
     * <li><b>artifactId</b> - Eliminate all files associated with the
     * artifact's artifactId.</li>
     * <li><b>groupId</b> - Eliminate all files associated with the artifact's
     * groupId.</li>
     * </ul>
     * 
     * @parameter expression="${resolutionFuzziness}" default-value="file"
     */
    private String resolutionFuzziness;

    /**
     * Whether this mojo should act on all transitive dependencies. Default
     * value is true.
     * 
     * @parameter expression="${actTransitively}" default-value="true"
     */
    private boolean actTransitively;

    /**
     * Used to construct artifacts for deletion/resolution...
     * 
     * @component
     */
    private ArtifactFactory factory;

    /**
     * Whether this plugin should output verbose messages. Default is false.
     * 
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        List exclusionPatterns = buildExclusionPatternsList();

        for ( Iterator it = projects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            try
            {
                refreshDependenciesForProject( project, exclusionPatterns );
            }
            catch ( ArtifactResolutionException e )
            {
                MojoFailureException failure = new MojoFailureException( this,
                                                                         "Failed to refresh project dependencies for: "
                                                                             + project.getId(),
                                                                         "Artifact resolution failed for project: "
                                                                             + project.getId() );
                failure.initCause( e );

                throw failure;
            }
        }
    }

    private List buildExclusionPatternsList()
    {
        List patterns = new ArrayList();

        if ( exclude != null )
        {
            String[] elements = exclude.split( " ?, ?" );

            patterns.addAll( Arrays.asList( elements ) );
        }
        else if ( excludes != null && !excludes.isEmpty() )
        {
            patterns.addAll( excludes );
        }

        return patterns;
    }

    private Map createArtifactMap( MavenProject project )
    {
        Map artifactMap = Collections.EMPTY_MAP;

        List dependencies = project.getDependencies();

        List remoteRepositories = Collections.EMPTY_LIST;

        Set dependencyArtifacts = new HashSet();

        for ( Iterator it = dependencies.iterator(); it.hasNext(); )
        {
            Dependency dependency = (Dependency) it.next();

            VersionRange vr = VersionRange.createFromVersion( dependency.getVersion() );

            Artifact artifact = factory.createDependencyArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                                  vr, dependency.getType(), dependency.getClassifier(),
                                                                  dependency.getScope() );
            dependencyArtifacts.add( artifact );
        }

        if ( actTransitively )
        {
            try
            {
                ArtifactResolutionResult result = resolver.resolveTransitively( dependencyArtifacts, project
                    .getArtifact(), remoteRepositories, localRepository, source );

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
        else
        {
            artifactMap = new HashMap();
            for ( Iterator it = dependencyArtifacts.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                try
                {
                    resolver.resolve( artifact, remoteRepositories, localRepository );

                    artifactMap.put( ArtifactUtils.versionlessKey( artifact ), artifact );
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

    private void refreshDependenciesForProject( MavenProject project, List exclusionPatterns )
        throws ArtifactResolutionException, MojoFailureException
    {
        Map deps = createArtifactMap( project );

        if ( deps.isEmpty() )
        {
            getLog().info( "Nothing to do for project: " + project.getId() );
            return;
        }

        if ( !exclusionPatterns.isEmpty() )
        {
            for ( Iterator it = exclusionPatterns.iterator(); it.hasNext(); )
            {
                String excludedKey = (String) it.next();

                verbose( "Excluding: " + excludedKey + " from refresh operation for project: " + project.getId() );

                deps.remove( excludedKey );
            }
        }

        verbose( "Processing dependencies for project: " + project.getId() );

        List missingArtifacts = new ArrayList();
        for ( Iterator it = deps.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            Artifact artifact = (Artifact) entry.getValue();

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
            for ( Iterator i = missingArtifacts.iterator(); i.hasNext(); )
            {
                Artifact missingArtifact = (Artifact) i.next();
                message += "  " + missingArtifact.getId() + "\n";
            }
            message += "\nfor the artifact:";

            throw new ArtifactResolutionException( message, project.getArtifact(), project
                .getRemoteArtifactRepositories() );
        }

    }

    private File findDeleteTarget( Artifact artifact )
    {
        File deleteTarget = artifact.getFile();

        if ( GROUP_ID_FUZZINESS.equals( resolutionFuzziness ) )
        {
            // get the artifactId dir.
            deleteTarget = deleteTarget.getParentFile().getParentFile();

            // get the first groupId dir.
            deleteTarget = deleteTarget.getParentFile();

            String[] path = localRepository.pathOf( artifact ).split( "\\/" );

            // subtract the artifact filename, version dir, artifactId dir, and
            // the first groupId
            // dir, since we've accounted for those above.
            int groupParts = path.length - 4;

            File parent = deleteTarget.getParentFile();
            int count = 0;
            while ( count++ < groupParts )
            {
                // prune empty dirs back to the beginning of the groupId, if
                // possible.

                // if the parent dir only has the one child file, then it's okay
                // to prune.
                if ( parent.list().length < 2 )
                {
                    deleteTarget = parent;

                    // check the parent of this newly checked dir
                    parent = deleteTarget.getParentFile();
                }
                else
                {
                    // if there are more files than the one that we're
                    // interested in killing, stop.
                    break;
                }
            }

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
