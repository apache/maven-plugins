package org.apache.maven.plugin.ant;

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

import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;

/**
 * Wrapper object to resolve artifact.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class ArtifactResolverWrapper
{
    /**
     * Used for resolving artifacts
     */
    private ArtifactResolver resolver;

    /**
     * Factory for creating artifact objects
     */
    private ArtifactFactory factory;

    /**
     * The local repository where the artifacts are located
     */
    private ArtifactRepository localRepository;

    /**
     * The remote repositories where artifacts are located
     */
    private List remoteRepositories;

    /**
     * @param resolver
     * @param factory
     * @param localRepository
     * @param remoteRepositories
     */
    private ArtifactResolverWrapper( ArtifactResolver resolver, ArtifactFactory factory,
                                    ArtifactRepository localRepository, List remoteRepositories )
    {
        this.resolver = resolver;
        this.factory = factory;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
    }

    /**
     * @param resolver
     * @param factory
     * @param localRepository
     * @param remoteRepositories
     * @return an instance of ArtifactResolverWrapper
     */
    public static ArtifactResolverWrapper getInstance( ArtifactResolver resolver, ArtifactFactory factory,
                                                      ArtifactRepository localRepository, List remoteRepositories )
    {
        return new ArtifactResolverWrapper( resolver, factory, localRepository, remoteRepositories );
    }

    protected ArtifactFactory getFactory()
    {
        return factory;
    }

    protected void setFactory( ArtifactFactory factory )
    {
        this.factory = factory;
    }

    protected ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    protected void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    protected List getRemoteRepositories()
    {
        return remoteRepositories;
    }

    protected void setRemoteRepositories( List remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    }

    protected ArtifactResolver getResolver()
    {
        return resolver;
    }

    protected void setResolver( ArtifactResolver resolver )
    {
        this.resolver = resolver;
    }

    /**
     * Return the artifact path in the local repository for an artifact defined by its <code>groupId</code>,
     * its <code>artifactId</code> and its <code>version</code>.
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return the locale artifact path
     * @throws IOException if any
     */
    public String getArtifactAbsolutePath( String groupId, String artifactId, String version )
        throws IOException
    {
        Artifact artifact = factory.createArtifact( groupId, artifactId, version, "compile", "jar" );
        try
        {
            resolver.resolve( artifact, remoteRepositories, localRepository );

            return artifact.getFile().getAbsolutePath();
        }
        catch ( ArtifactResolutionException e )
        {
            throw new IOException( "Unable to resolve artifact: " + groupId + ":" + artifactId + ":" + version );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new IOException( "Unable to find artifact: " + groupId + ":" + artifactId + ":" + version );
        }
    }
}
