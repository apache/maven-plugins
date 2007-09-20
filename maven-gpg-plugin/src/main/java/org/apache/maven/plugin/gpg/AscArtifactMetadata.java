package org.apache.maven.plugin.gpg;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.AbstractArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataStoreException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.util.FileUtils;

public class AscArtifactMetadata extends AbstractArtifactMetadata
{
    File file;
    boolean isPom;
    
    public AscArtifactMetadata( Artifact artifact,
                               File file,
                               boolean isPom )
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
        return "gpg signature " + artifact.getGroupId() + ":" + artifact.getArtifactId() 
            + ":" + artifact.getType() + ":" + artifact.getClassifier() +
            (isPom ? ":pom" : "");
    }
    
    private String getFilename()
    {
        StringBuffer buf = new StringBuffer( getArtifactId() );
        buf.append( "-" ).append( artifact.getVersion() );
        if ( isPom ) 
        {
            buf.append( ".pom" );
        } 
        else
        {
            if ( artifact.getClassifier() != null
                 && !"".equals( artifact.getClassifier() ) )
            {
                buf.append( "-" ).append( artifact.getClassifier() );
            }
            buf.append( "." ).append( artifact.getType() );
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
            throw new IllegalStateException( "Cannot add two different pieces of metadata for: "
                                             + getKey() );
        }
    }

    public void storeInLocalRepository( ArtifactRepository localRepository,
                                       ArtifactRepository remoteRepository )
        throws RepositoryMetadataStoreException 
    {
        File destination = new File( localRepository.getBasedir(),
                                     localRepository.pathOfLocalRepositoryMetadata( this, remoteRepository ) );

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
