package org.apache.maven.plugin.install.testingharness;

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

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.repository.ArtifactTransferListener;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.sonatype.aether.RepositorySystemSession;

public class MojoTestRepositorySystem implements RepositorySystem
{

    @Override
    public Artifact createProjectArtifact( String groupId, String artifactId, String version )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Artifact createArtifact( String groupId, String artifactId, String version, String packaging )
    {
        // called in org.apache.maven.plugin.testing.AbstractMojoTestCase.setUp()
        return new DefaultArtifact( groupId, artifactId, version, "compile", packaging, "", null );
    }

    @Override
    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type,
                                                  String classifier )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Artifact createPluginArtifact( Plugin plugin )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Artifact createDependencyArtifact( Dependency dependency )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArtifactRepository buildArtifactRepository( Repository repository )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArtifactRepository createDefaultRemoteRepository()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArtifactRepository createDefaultLocalRepository()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArtifactRepository createLocalRepository( File localRepository )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArtifactRepository createArtifactRepository( String id, String url,
                                                        ArtifactRepositoryLayout repositoryLayout,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ArtifactRepository> getEffectiveRepositories( List<ArtifactRepository> repositories )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mirror getMirror( ArtifactRepository repository, List<Mirror> mirrors )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void injectMirror( List<ArtifactRepository> repositories, List<Mirror> mirrors )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void injectProxy( List<ArtifactRepository> repositories, List<Proxy> proxies )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void injectAuthentication( List<ArtifactRepository> repositories, List<Server> servers )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void injectMirror( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void injectProxy( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void injectAuthentication( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void publish( ArtifactRepository repository, File source, String remotePath,
                         ArtifactTransferListener transferListener )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void retrieve( ArtifactRepository repository, File destination, String remotePath,
                          ArtifactTransferListener transferListener )
    {
        throw new UnsupportedOperationException();
    }

}
