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

import org.apache.commons.io.FileUtils;
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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads a single artifact transitively from the specified remote repositories. Caveat: will always check the
 * central repository defined in the super pom. You could use a mirror entry in your settings.xml
 */
@Mojo( name = "get", requiresProject = false )
public class GetMojo
    extends AbstractMojo
{
    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile( "(.+)::(.*)::(.+)" );

    /**
     *
     */
    @Component
    private ArtifactFactory artifactFactory;

    /**
     *
     */
    @Component
    private ArtifactResolver artifactResolver;

    /**
     *
     */
    @Component
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * Map that contains the layouts.
     */
    @Component( role = ArtifactRepositoryLayout.class )
    private Map<String, ArtifactRepositoryLayout> repositoryLayouts;

    /**
     *
     */
    @Component
    private ArtifactMetadataSource source;

    /**
     *
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true )
    private ArtifactRepository localRepository;

    /**
     * The groupId of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "groupId" )
    private String groupId;

    /**
     * The artifactId of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "artifactId" )
    private String artifactId;

    /**
     * The version of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "version" )
    private String version;

    /**
     * The classifier of the artifact to download. Ignored if {@link #artifact} is used.
     *
     * @since 2.3
     */
    @Parameter( property = "classifier" )
    private String classifier;

    /**
     * The packaging of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "packaging", defaultValue = "jar" )
    private String packaging = "jar";

    /**
     * The id of the repository from which we'll download the artifact
     *
     * @deprecated Use remoteRepositories
     */
    @Parameter( property = "repoId", defaultValue = "temp" )
    private String repositoryId = "temp";

    /**
     * The url of the repository from which we'll download the artifact. DEPRECATED Use remoteRepositories
     *
     * @deprecated Use remoteRepositories
     */
    @Parameter( property = "repoUrl" )
    private String repositoryUrl;

    /**
     * Repositories in the format id::[layout]::url or just url, separated by comma.
     * ie. central::default::http://repo1.maven.apache.org/maven2,myrepo::::http://repo.acme.com,http://repo.acme2.com
     */
    @Parameter( property = "remoteRepositories" )
    private String remoteRepositories;

    /**
     * A string of the form groupId:artifactId:version[:packaging][:classifier].
     */
    @Parameter( property = "artifact" )
    private String artifact;

    /**
     * The destination file to copy the artifact to, if other than the local repository
     *
     * @since 2.4
     */
    @Parameter( property = "dest" )
    private String destination;

    /**
     *
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true )
    private List<ArtifactRepository> pomRemoteRepositories;

    /**
     * Download transitively, retrieving the specified artifact and all of its dependencies.
     */
    @Parameter( property = "transitive", defaultValue = "true" )
    private boolean transitive = true;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( artifactId == null && artifact == null )
        {
            throw new MojoFailureException( "You must specify an artifact, "
                                                + "e.g. -Dartifact=org.apache.maven.plugins:maven-downloader-plugin:1.0" );
        }
        if ( artifact != null )
        {
            String[] tokens = StringUtils.split( artifact, ":" );
            if ( tokens.length < 3 || tokens.length > 5 )
            {
                throw new MojoFailureException(
                    "Invalid artifact, you must specify groupId:artifactId:version[:packaging][:classifier] "
                        + artifact );
            }
            groupId = tokens[0];
            artifactId = tokens[1];
            version = tokens[2];
            if ( tokens.length >= 4 )
            {
                packaging = tokens[3];
            }
            if ( tokens.length == 5 )
            {
                classifier = tokens[4];
            }
            else
            {
                classifier = null;
            }
        }

        Artifact toDownload = classifier == null
            ? artifactFactory.createBuildArtifact( groupId, artifactId, version, packaging )
            : artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, packaging, classifier );
        Artifact dummyOriginatingArtifact =
            artifactFactory.createBuildArtifact( "org.apache.maven.plugins", "maven-downloader-plugin", "1.0", "jar" );

        ArtifactRepositoryPolicy always =
            new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                          ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );

        List<ArtifactRepository> repoList = new ArrayList<ArtifactRepository>();

        if ( pomRemoteRepositories != null )
        {
            repoList.addAll( pomRemoteRepositories );
        }

        if ( remoteRepositories != null )
        {
            // Use the same format as in the deploy plugin id::layout::url
            List<String> repos = Arrays.asList( StringUtils.split( remoteRepositories, "," ) );
            for ( String repo : repos )
            {
                repoList.add( parseRepository( repo, always ) );
            }
        }

        if ( repositoryUrl != null )
        {
            getLog().warn( "repositoryUrl parameter is deprecated. Use remoteRepositories instead" );
            ArtifactRepository remoteRepo =
                artifactRepositoryFactory.createArtifactRepository( repositoryId, repositoryUrl, getLayout( "default" ),
                                                                    always, always );
            repoList.add( remoteRepo );
        }

        try
        {
            if ( transitive )
            {
                artifactResolver.resolveTransitively( Collections.singleton( toDownload ), dummyOriginatingArtifact,
                                                      repoList, localRepository, source );
            }
            else
            {
                artifactResolver.resolve( toDownload, repoList, localRepository );
            }
        }
        catch ( AbstractArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Couldn't download artifact: " + e.getMessage(), e );
        }

        if ( destination != null )
        {
            File src = toDownload.getFile();
            File dest = new File( destination );
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Copying " + src.getAbsolutePath() + " to " + dest.getAbsolutePath() );
            }
            try
            {
                FileUtils.copyFile( toDownload.getFile(), new File( destination ) );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                    "Couldn't copy downloaded artifact from " + src.getAbsolutePath() + " to " + dest.getAbsolutePath()
                        + " : " + e.getMessage(), e );
            }
        }
    }

    ArtifactRepository parseRepository( String repo, ArtifactRepositoryPolicy policy )
        throws MojoFailureException
    {
        // if it's a simple url
        String id = repositoryId;
        ArtifactRepositoryLayout layout = getLayout( "default" );
        String url = repo;

        // if it's an extended repo URL of the form id::layout::url
        if ( repo.indexOf( "::" ) >= 0 )
        {
            Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher( repo );
            if ( !matcher.matches() )
            {
                throw new MojoFailureException( repo, "Invalid syntax for repository: " + repo,
                                                "Invalid syntax for repository. Use \"id::layout::url\" or \"URL\"." );
            }

            id = matcher.group( 1 ).trim();
            if ( !StringUtils.isEmpty( matcher.group( 2 ) ) )
            {
                layout = getLayout( matcher.group( 2 ).trim() );
            }
            url = matcher.group( 3 ).trim();
        }
        return artifactRepositoryFactory.createArtifactRepository( id, url, layout, policy, policy );
    }

    private ArtifactRepositoryLayout getLayout( String id )
        throws MojoFailureException
    {
        ArtifactRepositoryLayout layout = repositoryLayouts.get( id );

        if ( layout == null )
        {
            throw new MojoFailureException( id, "Invalid repository layout", "Invalid repository layout: " + id );
        }

        return layout;
    }
}
