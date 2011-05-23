package org.apache.maven.plugin.gpg;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A wrapper class for attached artifacts which have a GPG signature. Needed as attached artifacts in general do not
 * have metadata.
 */
public class AttachedSignedArtifact
    implements Artifact
{
    private final Artifact delegate;

    private final AscArtifactMetadata signature;

    public AttachedSignedArtifact( Artifact delegate, AscArtifactMetadata signature )
    {
        this.delegate = delegate;
        this.signature = signature;
    }

    public void setArtifactId( String artifactId )
    {
        delegate.setArtifactId( artifactId );
    }

    public List getAvailableVersions()
    {
        return delegate.getAvailableVersions();
    }

    public void setAvailableVersions( List availableVersions )
    {
        delegate.setAvailableVersions( availableVersions );
    }

    public String getBaseVersion()
    {
        return delegate.getBaseVersion();
    }

    public void setBaseVersion( String baseVersion )
    {
        delegate.setBaseVersion( baseVersion );
    }

    public String getDownloadUrl()
    {
        return delegate.getDownloadUrl();
    }

    public void setDownloadUrl( String downloadUrl )
    {
        delegate.setDownloadUrl( downloadUrl );
    }

    public void setGroupId( String groupId )
    {
        delegate.setGroupId( groupId );
    }

    public ArtifactRepository getRepository()
    {
        return delegate.getRepository();
    }

    public void setRepository( ArtifactRepository repository )
    {
        delegate.setRepository( repository );
    }

    public String getScope()
    {
        return delegate.getScope();
    }

    public void setScope( String scope )
    {
        delegate.setScope( scope );
    }

    public String getVersion()
    {
        return delegate.getVersion();
    }

    public void setVersion( String version )
    {
        delegate.setVersion( version );
    }

    public VersionRange getVersionRange()
    {
        return delegate.getVersionRange();
    }

    public void setVersionRange( VersionRange range )
    {
        delegate.setVersionRange( range );
    }

    public boolean isRelease()
    {
        return delegate.isRelease();
    }

    public void setRelease( boolean release )
    {
        delegate.setRelease( release );
    }

    public boolean isSnapshot()
    {
        return delegate.isSnapshot();
    }

    public void addMetadata( ArtifactMetadata metadata )
    {
        delegate.addMetadata( metadata );
    }

    public String getClassifier()
    {
        return delegate.getClassifier();
    }

    public boolean hasClassifier()
    {
        return delegate.hasClassifier();
    }

    public String getGroupId()
    {
        return delegate.getGroupId();
    }

    public String getArtifactId()
    {
        return delegate.getArtifactId();
    }

    public String getType()
    {
        return delegate.getType();
    }

    public void setFile( File file )
    {
        delegate.setFile( file );
    }

    public File getFile()
    {
        return delegate.getFile();
    }

    public String getId()
    {
        return delegate.getId();
    }

    public String getDependencyConflictId()
    {
        return delegate.getDependencyConflictId();
    }

    public String toString()
    {
        return delegate.toString();
    }

    public int hashCode()
    {
        return delegate.hashCode();
    }

    public boolean equals( Object o )
    {
        return delegate.equals( o );
    }

    public int compareTo( Object o )
    {
        return delegate.compareTo( o );
    }

    public void updateVersion( String version, ArtifactRepository localRepository )
    {
        delegate.updateVersion( version, localRepository );
    }

    public ArtifactFilter getDependencyFilter()
    {
        return delegate.getDependencyFilter();
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        delegate.setDependencyFilter( artifactFilter );
    }

    public ArtifactHandler getArtifactHandler()
    {
        return delegate.getArtifactHandler();
    }

    public List getDependencyTrail()
    {
        return delegate.getDependencyTrail();
    }

    public void setDependencyTrail( List dependencyTrail )
    {
        delegate.setDependencyTrail( dependencyTrail );
    }

    public void selectVersion( String version )
    {
        delegate.selectVersion( version );
    }

    public void setResolved( boolean resolved )
    {
        delegate.setResolved( resolved );
    }

    public boolean isResolved()
    {
        return delegate.isResolved();
    }

    public void setResolvedVersion( String version )
    {
        delegate.setResolvedVersion( version );
    }

    public void setArtifactHandler( ArtifactHandler artifactHandler )
    {
        delegate.setArtifactHandler( artifactHandler );
    }

    public boolean isOptional()
    {
        return delegate.isOptional();
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return delegate.getSelectedVersion();
    }

    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return delegate.isSelectedVersionKnown();
    }

    public void setOptional( boolean optional )
    {
        delegate.setOptional( optional );
    }

    public Collection getMetadataList()
    {
        List result = new ArrayList( delegate.getMetadataList() );
        boolean alreadySigned = false;
        for ( Iterator i = result.iterator(); i.hasNext() && !alreadySigned; )
        {
            ArtifactMetadata metadata = (ArtifactMetadata) i.next();
            alreadySigned = alreadySigned | signature.getKey().equals( metadata.getKey() );
        }
        if ( !alreadySigned )
        {
            result.add( signature );
        }
        return result;
    }
}
