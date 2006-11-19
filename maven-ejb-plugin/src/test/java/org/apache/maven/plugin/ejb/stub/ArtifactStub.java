package org.apache.maven.plugin.ejb.stub;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;


/**
 * Stub
 */
public class ArtifactStub
    implements Artifact
{

    boolean hasClassifier;

    boolean resolved;

    boolean optional;

    boolean release;

    ArtifactHandler artifactHandler;

    File file;

    String baseVersion;

    String type;

    String classifier;

    String identifier;

    String dependencyConflictId;

    String downloadUrl;

    String selectedVersion;

    String artifactId;

    String groupId;

    String resolvedVersion;

    String scope;

    String version;

    VersionRange versionRange;


    public ArtifactStub()
    {
        type = "testtype";
        scope = "testscope";
        classifier = "testclassifier";
        artifactHandler = new DefaultArtifactHandler();
    }

    public void populate( MavenProjectBasicStub project )
    {
        groupId = project.getGroupId();
        artifactId = project.getArtifactId();
        version = project.getVersion();
        versionRange = VersionRange.createFromVersion( version );
    }

    public boolean hasClassifier()
    {
        return true;
    }

    public String getBaseVersion()
    {
        return "Test Version";
    }

    public void setBaseVersion( String version )
    {
        baseVersion = version;
    }

    public void setFile( File _file )
    {
        file = _file;
    }

    public File getFile()
    {
        return new File( "testfile" );
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
        return version;
    }

    public void setVersion( String _version )
    {
        version = _version;
    }

    public String getScope()
    {
        return scope;
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getId()
    {
        return identifier;
    }

    public String getDependencyConflictId()
    {
        return dependencyConflictId;
    }

    public void addMetadata( ArtifactMetadata metadata )
    {

    }

    public Collection getMetadataList()
    {
        return new LinkedList();
    }

    public void setRepository( ArtifactRepository remoteRepository )
    {

    }

    public ArtifactRepository getRepository()
    {
        return null;
    }

    public void updateVersion( String version, ArtifactRepository localRepository )
    {

    }

    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    public void setDownloadUrl( String _downloadUrl )
    {
        downloadUrl = _downloadUrl;
    }

    public ArtifactFilter getDependencyFilter()
    {
        return null;
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {

    }

    public ArtifactHandler getArtifactHandler()
    {
        return artifactHandler;
    }

    public List getDependencyTrail()
    {
        return new LinkedList();
    }

    public void setDependencyTrail( List dependencyTrail )
    {

    }

    public void setScope( String _scope )
    {
        scope = _scope;
    }

    public VersionRange getVersionRange()
    {
        return versionRange;
    }

    public void setVersionRange( VersionRange newRange )
    {

    }

    public void selectVersion( String version )
    {
        selectedVersion = version;
    }

    public void setGroupId( String _groupId )
    {
        groupId = _groupId;
    }

    public void setArtifactId( String _artifactId )
    {
        artifactId = _artifactId;
    }

    public boolean isSnapshot()
    {
        return true;
    }

    public void setResolved( boolean _resolved )
    {
        resolved = _resolved;
    }

    public boolean isResolved()
    {
        return true;
    }

    public void setResolvedVersion( String version )
    {
        resolvedVersion = version;
    }


    public void setArtifactHandler( ArtifactHandler handler )
    {

    }

    public boolean isRelease()
    {
        return true;
    }

    public void setRelease( boolean _release )
    {
        release = _release;
    }

    public List getAvailableVersions()
    {
        return new LinkedList();
    }

    public void setAvailableVersions( List versions )
    {

    }

    public boolean isOptional()
    {
        return true;
    }

    public void setOptional( boolean _optional )
    {
        optional = _optional;
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return null;
    }

    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return true;
    }

    public int compareTo( Object object )
    {
        return 0;
    }
}
