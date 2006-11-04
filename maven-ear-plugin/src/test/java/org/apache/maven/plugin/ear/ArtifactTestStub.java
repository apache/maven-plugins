package org.apache.maven.plugin.ear;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.File;
import java.util.Collection;
import java.util.List;

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

/**
 * A fake {@link Artifact} test stub.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class ArtifactTestStub
    implements Artifact

{
    private final String groupId;

    private final String artifactId;

    private final String type;

    private final String classifier;


    public ArtifactTestStub( String groupId, String artifactId, String type, String classifier )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.classifier = classifier;
    }


    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setVersion( String string )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public String getScope()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public boolean hasClassifier()
    {
        return classifier != null;
    }

    public File getFile()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setFile( File file )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public String getBaseVersion()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setBaseVersion( String string )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public String getId()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public String getDependencyConflictId()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void addMetadata( ArtifactMetadata artifactMetadata )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public Collection getMetadataList()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setRepository( ArtifactRepository artifactRepository )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public ArtifactRepository getRepository()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void updateVersion( String string, ArtifactRepository artifactRepository )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public String getDownloadUrl()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setDownloadUrl( String string )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public ArtifactFilter getDependencyFilter()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public ArtifactHandler getArtifactHandler()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public List getDependencyTrail()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setDependencyTrail( List list )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setScope( String string )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public VersionRange getVersionRange()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setVersionRange( VersionRange versionRange )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void selectVersion( String string )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setGroupId( String string )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setArtifactId( String string )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public boolean isSnapshot()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setResolved( boolean b )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public boolean isResolved()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setResolvedVersion( String string )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setArtifactHandler( ArtifactHandler artifactHandler )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public boolean isRelease()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setRelease( boolean b )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public List getAvailableVersions()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setAvailableVersions( List list )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public boolean isOptional()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public void setOptional( boolean b )
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public int compareTo( Object o )
    {
        if ( this.equals( o ) )
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ArtifactTestStub that = (ArtifactTestStub) o;

        if ( artifactId != null ? !artifactId.equals( that.artifactId ) : that.artifactId != null )
        {
            return false;
        }
        if ( classifier != null ? !classifier.equals( that.classifier ) : that.classifier != null )
        {
            return false;
        }
        if ( groupId != null ? !groupId.equals( that.groupId ) : that.groupId != null )
        {
            return false;
        }
        if ( type != null ? !type.equals( that.type ) : that.type != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = ( groupId != null ? groupId.hashCode() : 0 );
        result = 31 * result + ( artifactId != null ? artifactId.hashCode() : 0 );
        result = 31 * result + ( type != null ? type.hashCode() : 0 );
        result = 31 * result + ( classifier != null ? classifier.hashCode() : 0 );
        return result;
    }
}

