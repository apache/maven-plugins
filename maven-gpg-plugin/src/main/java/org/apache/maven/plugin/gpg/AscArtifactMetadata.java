package org.apache.maven.plugin.gpg;

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
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.AbstractArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataStoreException;
import org.codehaus.plexus.util.FileUtils;

public class AscArtifactMetadata
    extends AbstractArtifactMetadata
{

    File file;

    boolean isPom;

    public AscArtifactMetadata( Artifact artifact, File file, boolean isPom )
    {
        super( artifact );
        this.file = file;
        this.isPom = isPom;
    }

    public String getBaseVersion()
    {
        return artifact.getBaseVersion();
    }

    public Object getKey()
    {
        return "gpg signature " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType()
            + ":" + artifact.getClassifier() + ( isPom ? ":pom" : "" );
    }

    private String getFilename()
    {
        StringBuffer buf = new StringBuffer( 128 );
        buf.append( getArtifactId() );
        buf.append( "-" ).append( artifact.getVersion() );
        if ( isPom )
        {
            buf.append( ".pom" );
        }
        else
        {
            if ( artifact.getClassifier() != null && !"".equals( artifact.getClassifier() ) )
            {
                buf.append( "-" ).append( artifact.getClassifier() );
            }
            buf.append( "." ).append( artifact.getArtifactHandler().getExtension() );
        }
        buf.append( ".asc" );
        return buf.toString();
    }

    public String getLocalFilename( ArtifactRepository repository )
    {
        return getFilename();
    }

    public String getRemoteFilename()
    {
        return getFilename();
    }

    public void merge( ArtifactMetadata metadata )
    {
        AscArtifactMetadata m = (AscArtifactMetadata) metadata;
        if ( !m.file.equals( file ) )
        {
            throw new IllegalStateException( "Cannot add two different pieces of metadata for: " + getKey() );
        }
    }

    public void storeInLocalRepository( ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws RepositoryMetadataStoreException
    {
        File destination =
            new File( localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata( this,
                                                                                                   remoteRepository ) );

        try
        {
            FileUtils.copyFile( file, destination );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataStoreException( "Error copying ASC to the local repository.", e );
        }
    }

    public boolean storedInArtifactVersionDirectory()
    {
        return true;
    }

    public String toString()
    {
        return getFilename();
    }

}
