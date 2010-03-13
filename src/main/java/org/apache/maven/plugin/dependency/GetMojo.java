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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Downloads a single artifact transitively from a specified remote repository.
 *
 * @goal get
 * @requiresProject false
 *
 */
public class GetMojo
    extends AbstractMojo
{

    /**
     * @component
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * @component
     * @readonly
     */
    private ArtifactResolver artifactResolver;

    /**
     * @component
     * @readonly
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * @component roleHint="default"
     */
    private ArtifactRepositoryLayout repositoryLayout;

    /**
     * @component
     * @readonly
     */
    private ArtifactMetadataSource source;

    /**
     *
     * @parameter expression="${localRepository}"
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The groupId of the artifact to download
     * @parameter expression="${groupId}"
     */
    private String groupId;

    /**
     * The artifactId of the artifact to download
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * The version of the artifact to download
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * The packaging of the artifact to download
     * @parameter expression="${packaging}" default-value="jar"
     */
    private String packaging = "jar";

    /**
     * The id of the repository from which we'll download the artifact
     * @parameter expression="${repoId}" default-value="temp"
     */
    private String repositoryId = "temp";

    /**
     * The url of the repository from which we'll download the artifact
     * @parameter expression="${repoUrl}"
     * @required
     */
    private String repositoryUrl;

    /**
     * @parameter expression="${remoteRepositories}"
     * @readonly
     */
    private String remoteRepositories;

    /**
     * A string of the form groupId:artifactId:version[:packaging].
     * @parameter expression="${artifact}"
     */
    private String artifact;

    /**
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List pomRemoteRepositories;

    /**
     * Download transitively, retrieving the specified artifact and all of its dependencies.
     * @parameter expression="{$transitive}" default-value=true
     */
    private boolean transitive = true;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( artifactId == null && artifact == null )
        {
            throw new MojoFailureException( "You must specify an artifact, "
                + "e.g. -Dartifact=org.apache.maven.plugins:maven-downloader-plugin:1.0" );
        }
        if ( artifactId == null )
        {
            String[] tokens = StringUtils.split( artifact, ":" );
            if ( tokens.length != 3 && tokens.length != 4 )
            {
                throw new MojoFailureException( "Invalid artifact, you must specify "
                    + "groupId:artifactId:version[:packaging] " + artifact );
            }
            groupId = tokens[0];
            artifactId = tokens[1];
            version = tokens[2];
            if ( tokens.length == 4 )
                packaging = tokens[3];
        }
        Artifact toDownload = artifactFactory.createBuildArtifact( groupId, artifactId, version, packaging );
        Artifact dummyOriginatingArtifact =
            artifactFactory.createBuildArtifact( "org.apache.maven.plugins", "maven-downloader-plugin", "1.0", "jar" );

        ArtifactRepositoryPolicy always =
            new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                          ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
        ArtifactRepository remoteRepo =
            artifactRepositoryFactory.createArtifactRepository( repositoryId, repositoryUrl, repositoryLayout, always,
                                                                always );

        if ( pomRemoteRepositories == null )
        {
            pomRemoteRepositories = new ArrayList();
        }

        List repoList = new ArrayList( pomRemoteRepositories );
        if ( remoteRepositories != null )
        {
            repoList.addAll( Arrays.asList( StringUtils.split( remoteRepositories, "," ) ) );
        }

        repoList.add( remoteRepo );

        try
        {
            if ( transitive )
            {
                artifactResolver.resolveTransitively( Collections.singleton( toDownload ), dummyOriginatingArtifact,
                                                      repoList, localRepository, source );
            }
            else
            {
                artifactResolver.resolve ( toDownload, repoList, localRepository );
            }

        }
        catch ( AbstractArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Couldn't download artifact: " + e.getMessage(), e );
        }
    }
}
