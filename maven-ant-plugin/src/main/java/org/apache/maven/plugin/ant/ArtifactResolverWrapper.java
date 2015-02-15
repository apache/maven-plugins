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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;

import java.io.IOException;
import java.util.List;

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
     * @param resolver {@link ArtifactResolver}
     * @param factory {@link ArtifactFactory}
     * @param localRepository {@link ArtifactRepository}
     * @param remoteRepositories {@link XX}.
     * @return an instance of ArtifactResolverWrapper
     */
    public static ArtifactResolverWrapper getInstance( ArtifactResolver resolver, ArtifactFactory factory,
                                                       ArtifactRepository localRepository, List remoteRepositories )
    {
        return new ArtifactResolverWrapper( resolver, factory, localRepository, remoteRepositories );
    }

    /**
     * @return {@link #factory}
     */
    protected ArtifactFactory getFactory()
    {
        return factory;
    }

    /**
     * @param factory {@link ArtifactFactory}
     */
    protected void setFactory( ArtifactFactory factory )
    {
        this.factory = factory;
    }

    /**
     * @return {@link #localRepository}
     */
    protected ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    /**
     * @param localRepository set {@link #localRepository}
     */
    protected void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    /**
     * @return {@link #remoteRepositories}
     */
    protected List getRemoteRepositories()
    {
        return remoteRepositories;
    }

    /**
     * @param remoteRepositories {@link #remoteRepositories}
     */
    protected void setRemoteRepositories( List remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    }

    /**
     * @return {@link #resolver}
     */
    protected ArtifactResolver getResolver()
    {
        return resolver;
    }

    /**
     * @param resolver {@link #resolver}
     */
    protected void setResolver( ArtifactResolver resolver )
    {
        this.resolver = resolver;
    }

    /**
     * Return the artifact path in the local repository for an artifact defined by its <code>groupId</code>,
     * its <code>artifactId</code> and its <code>version</code>.
     *
     * @param groupId The groupId.
     * @param artifactId The artifactId.
     * @param version The version.
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

    /**
     * Gets the path to the specified artifact relative to the local repository's base directory. Note that this method
     * does not actually resolve the artifact, it merely calculates the path at which the artifact is or would be stored
     * in the local repository.
     *
     * @param artifact The artifact whose path should be determined, must not be <code>null</code>.
     * @return The path to the artifact, never <code>null</code>.
     */
    public String getLocalArtifactPath( Artifact artifact )
    {
        /*
         * NOTE: Don't use Artifact.getFile() here because this method could return the path to a JAR from the build
         * output, e.g. ".../target/some-0.1.jar". The other special case are system-scope artifacts that reside
         * somewhere outside of the local repository.
         */
        return localRepository.pathOf( artifact );
    }

}
