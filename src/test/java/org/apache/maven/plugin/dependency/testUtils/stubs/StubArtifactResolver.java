package org.apache.maven.plugin.dependency.testUtils.stubs;

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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.dependency.testUtils.ArtifactStubFactory;

public class StubArtifactResolver
    implements ArtifactResolver
{

    boolean throwArtifactResolutionException;

    boolean throwArtifactNotFoundException;

    ArtifactStubFactory factory;

    public StubArtifactResolver( ArtifactStubFactory factory, boolean throwArtifactResolutionException,
                                boolean throwArtifactNotFoundException )
    {
        this.throwArtifactNotFoundException = throwArtifactNotFoundException;
        this.throwArtifactResolutionException = throwArtifactResolutionException;
        this.factory = factory;
    }

    /*
     * Creates dummy file and sets it in the artifact to simulate resolution
     * (non-Javadoc)
     * 
     * @see org.apache.maven.artifact.resolver.ArtifactResolver#resolve(org.apache.maven.artifact.Artifact,
     *      java.util.List,
     *      org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        if ( !this.throwArtifactNotFoundException && !this.throwArtifactResolutionException )
        {
            try
            {
                if ( factory != null )
                {
                    factory.setArtifactFile( artifact );
                }
            }
            catch ( IOException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            if ( throwArtifactResolutionException )
            {
                throw new ArtifactResolutionException( "Catch!", artifact );
            }
            else
            {
                throw new ArtifactNotFoundException( "Catch!", artifact );
            }
        }
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        List remoteRepositories, ArtifactRepository localRepository,
                                                        ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        List remoteRepositories, ArtifactRepository localRepository,
                                                        ArtifactMetadataSource source, List listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        ArtifactRepository localRepository, List remoteRepositories,
                                                        ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        Map managedVersions, ArtifactRepository localRepository,
                                                        List remoteRepositories, ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        Map managedVersions, ArtifactRepository localRepository,
                                                        List remoteRepositories, ArtifactMetadataSource source,
                                                        ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                        Map managedVersions, ArtifactRepository localRepository,
                                                        List remoteRepositories, ArtifactMetadataSource source,
                                                        ArtifactFilter filter, List listeners )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void resolveAlways( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        // TODO Auto-generated method stub

    }

}
