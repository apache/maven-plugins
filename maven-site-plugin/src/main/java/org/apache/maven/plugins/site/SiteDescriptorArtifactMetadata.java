package org.apache.maven.plugins.site;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.AbstractArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataStoreException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Attach a POM to an artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class SiteDescriptorArtifactMetadata
    extends AbstractArtifactMetadata
{
    private final File file;

    public SiteDescriptorArtifactMetadata( Artifact artifact )
    {
        this( artifact, null );
    }

    public SiteDescriptorArtifactMetadata( Artifact artifact, File file )
    {
        super( artifact );
        this.file = file;
    }

    public String getRemoteFilename()
    {
        return getFilename();
    }

    public String getLocalFilename( ArtifactRepository repository )
    {
        return getFilename();
    }

    private String getFilename()
    {
        return getArtifactId() + "-" + artifact.getVersion() + "-" + file.getName();
    }

    public void storeInLocalRepository( ArtifactRepository localRepository, ArtifactRepository remoteRepository )
        throws RepositoryMetadataStoreException
    {
        File destination = new File( localRepository.getBasedir(),
                                     localRepository.pathOfLocalRepositoryMetadata( this, remoteRepository ) );

        destination.getParentFile().mkdirs();

        try
        {
            FileUtils.copyFile( file, destination );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataStoreException( "Error saving in local repository", e );
        }
    }

    public String toString()
    {
        return "site descriptor for " + artifact.getArtifactId() + " " + artifact.getVersion() + " " + file.getName();
    }

    public boolean storedInArtifactVersionDirectory()
    {
        return true;
    }

    public String getBaseVersion()
    {
        return artifact.getBaseVersion();
    }

    public Object getKey()
    {
        return "site descriptor " + artifact.getGroupId() + ":" + artifact.getArtifactId() + " " + file.getName();
    }

    public void merge( ArtifactMetadata metadata )
    {
        SiteDescriptorArtifactMetadata m = (SiteDescriptorArtifactMetadata) metadata;
        if ( !m.file.equals( file ) )
        {
            throw new IllegalStateException( "Cannot add two different pieces of metadata for: " + getKey() );
        }
    }
}
