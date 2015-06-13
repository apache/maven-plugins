package org.apache.maven.plugins.assembly.archive.task.testutils;

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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.easymock.classextension.EasyMockSupport;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.easymock.EasyMock.expect;

public class ArtifactMock
{


    private final Artifact artifact;


    private final ArtifactHandler handler;

    private final String classifier;

    private String groupId;

    private String artifactId;

    private String baseVersion;

    private File file;

    private Boolean isSnapshot;

    private String version;

    private List<String> dependencyTrail;

    private String id;

    private String dependencyConflictId;

    private String typeAndExt;

    public ArtifactMock( final EasyMockSupport mockManager, final String groupId, final String artifactId,
                         final String version, final String type, final boolean isSnapshot )
    {
        this( mockManager, groupId, artifactId, version, type, null, isSnapshot, null, null );
    }

    public ArtifactMock( final EasyMockSupport mockManager, final String groupId, final String artifactId,
                         final String version, final String type, final boolean isSnapshot, final String baseVersion )
    {
        this( mockManager, groupId, artifactId, version, type, null, isSnapshot, baseVersion, null );
    }

    public ArtifactMock( final EasyMockSupport mockManager, final String groupId, final String artifactId,
                         final String version, final String type, final String classifier, final boolean isSnapshot )
    {
        this( mockManager, groupId, artifactId, version, type, classifier, isSnapshot, null, null );
    }

    public ArtifactMock( final EasyMockSupport mockManager, final String groupId, final String artifactId,
                         final String version, final String type, final String classifier, final boolean isSnapshot,
                         final String baseVersion )
    {
        this( mockManager, groupId, artifactId, version, type, classifier, isSnapshot, baseVersion, null );
    }

    private ArtifactMock( final EasyMockSupport mockManager, final String groupId, final String artifactId,
                          final String version, final String type, final String classifier, final boolean isSnapshot,
                          final String baseVersion, String scope )
    {

        artifact = mockManager.createMock( Artifact.class );

        if ( scope == null )
        {
            scope = Artifact.SCOPE_COMPILE;
        }

        expect( artifact.getScope() ).andReturn( scope ).anyTimes();

        handler = mockManager.createMock( ArtifactHandler.class );

        expect( artifact.getArtifactHandler() ).andReturn( handler ).anyTimes();

        this.classifier = classifier;
        expect( artifact.getClassifier() ).andReturn( classifier ).anyTimes();

        setSnapshot( isSnapshot );
        setGroupId( groupId );
        setArtifactId( artifactId );
        setVersion( version );
        setBaseVersion( baseVersion );
        setType( type );

        setId();
        setDependencyConflictId();
    }

    public void setExtension( final String extension )
    {
        setTypeAndExt( extension );
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    void setArtifactId( final String artifactId )
    {
        if ( ( artifactId != null ) && ( this.artifactId == null ) )
        {
            expect( artifact.getArtifactId() ).andReturn( artifactId ).anyTimes();
            this.artifactId = artifactId;
        }
    }

    public void setBaseVersion( final String baseVersion )
    {
        if ( ( baseVersion != null ) && ( this.baseVersion == null ) )
        {
            expect( artifact.getBaseVersion() ).andReturn( baseVersion ).anyTimes();

            this.baseVersion = baseVersion;
        }
    }

    public void setFile( final File destination )
    {
        if ( ( destination != null ) && ( file == null ) )
        {
            expect( artifact.getFile() ).andReturn( destination ).anyTimes();
            file = destination;
        }
    }

    void setGroupId( final String groupId )
    {
        if ( ( groupId != null ) && ( this.groupId == null ) )
        {
            expect( artifact.getGroupId() ).andReturn( groupId ).anyTimes();

            this.groupId = groupId;
        }
    }

    void setVersion( final String version )
    {
        if ( ( version != null ) && ( this.version == null ) )
        {
            expect( artifact.getVersion() ).andReturn( version ).anyTimes();

            this.version = version;

            if ( isSnapshot != Boolean.TRUE )
            {
                setBaseVersion( version );
                setSnapshot( false );
            }
        }
    }

    public void setDependencyTrail( final List<String> dependencyTrail )
    {
        if ( ( dependencyTrail != null ) && ( this.dependencyTrail == null ) )
        {
            expect( artifact.getDependencyTrail() ).andReturn( dependencyTrail ).anyTimes();
            this.dependencyTrail = dependencyTrail;
        }
    }

    void setId( final String id )
    {
        if ( ( id != null ) && ( this.id == null ) )
        {
            expect( artifact.getId() ).andReturn( id ).anyTimes();
            this.id = id;
        }
    }

    void setDependencyConflictId( final String id )
    {
        if ( ( id != null ) && ( dependencyConflictId == null ) )
        {
            expect( artifact.getDependencyConflictId() ).andReturn( id ).anyTimes();

            dependencyConflictId = id;
        }
    }

    void setSnapshot( final boolean snapshot )
    {
        if ( isSnapshot == null )
        {
            expect( artifact.isSnapshot() ).andReturn( snapshot ).anyTimes();
            isSnapshot = snapshot;
        }
    }

    public File setNewFile()
        throws IOException
    {
        if ( file == null )
        {
            final File newFile = File.createTempFile( "ArtifactMock.test.", "" );
            newFile.deleteOnExit();
            expect( artifact.getFile() ).andReturn( newFile ).anyTimes();
            file = newFile;
        }

        return file;
    }

    void setType( final String type )
    {
        setTypeAndExt( type );
    }

    private void setTypeAndExt( final String type )
    {
        if ( ( type != null ) && ( typeAndExt == null ) )
        {
            expect( artifact.getType() ).andReturn( type ).anyTimes();
            expect( handler.getExtension() ).andReturn( type ).anyTimes();
            typeAndExt = type;
        }
    }

    private void setDependencyConflictId()
    {
        if ( ( groupId != null ) && ( artifactId != null ) && ( typeAndExt != null ) )
        {
            final String id =
                groupId + ":" + artifactId + ":" + typeAndExt + ( classifier == null ? "" : ":" + classifier );
            setDependencyConflictId( id );
        }
    }

    private void setId()
    {
        if ( ( groupId != null ) && ( artifactId != null ) && ( typeAndExt != null ) && ( version != null ) )
        {
            final String id = groupId + ":" + artifactId + ":" + version + ":" + typeAndExt + ( classifier == null
                ? ""
                : ":" + classifier );
            setId( id );
        }
    }

    public void setNullFile()
    {
        expect( artifact.getFile() ).andReturn( null ).anyTimes();

        file = new File( "set-to-null" );
    }

}
