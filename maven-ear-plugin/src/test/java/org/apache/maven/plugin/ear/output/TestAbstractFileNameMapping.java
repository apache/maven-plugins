package org.apache.maven.plugin.ear.output;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.junit.Test;

public class TestAbstractFileNameMapping
{
    private AbstractFileNameMapping abstractFileNameMapping = new AbstractFileNameMapping()
    {
        public String mapFileName( Artifact a )
        {
            return null;
        }
    };

    @Test
    public void test()
    {
        ArtifactHandler handler = mock( ArtifactHandler.class );
        when( handler.getExtension() ).thenReturn( "jar" );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getArtifactHandler() ).thenReturn( handler );
        when( artifact.getArtifactId() ).thenReturn( "mear149" );
        when( artifact.getVersion() ).thenReturn( "1.0-SNAPSHOT" );
        when( artifact.getBaseVersion() ).thenReturn( "1.0-20130423.042904" );

        // default behavior: use -SNAPSHOT
        assertEquals( "mear149-1.0-SNAPSHOT.jar", abstractFileNameMapping.generateFileName( artifact, true ) );
        abstractFileNameMapping.setUseBaseVersion( true );
        assertEquals( "mear149-1.0-20130423.042904.jar", abstractFileNameMapping.generateFileName( artifact, true ) );
        abstractFileNameMapping.setUseBaseVersion( false );
        assertEquals( "mear149-1.0-SNAPSHOT.jar", abstractFileNameMapping.generateFileName( artifact, true ) );
    }

}
