package org.apache.maven.plugins.shade.filter;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import junit.framework.Assert;

public class MinijarFilterTest
{

//    @Test
//    public void theTestWhichIsTooExpensiveAtTheMoment()
//        throws IOException
//    {
//        ArgumentCaptor<CharSequence> logCaptor = ArgumentCaptor.forClass( CharSequence.class );
//
//        MavenProject mavenProject = mock( MavenProject.class );
//        Log log = mock( Log.class );
//
//        Artifact artifact = mock( Artifact.class );
//        when( artifact.getGroupId() ).thenReturn( "com" );
//        when( artifact.getArtifactId() ).thenReturn( "aid" );
//        when( artifact.getVersion() ).thenReturn( "1.9" );
//        when( artifact.getClassifier() ).thenReturn( "classifier1" );
//        when( artifact.getScope() ).thenReturn( Artifact.SCOPE_COMPILE );
//
//        when( mavenProject.getArtifact() ).thenReturn( artifact );
//
//        Set<Artifact> artifacts = new TreeSet<Artifact>();
//        artifacts.add( new DefaultArtifact( "dep.com", "dep.aid", "1.0", "compile", "jar", "classifier2", null ) );
//
//        when( mavenProject.getArtifacts() ).thenReturn( artifacts );
//        when( mavenProject.getArtifact().getFile() ).thenReturn( mock( File.class ) );
//
//        MinijarFilter mf = new MinijarFilter( mavenProject, log );
//
//        mf.finished();
//        verify( log, times( 1 ) ).info( logCaptor.capture() );
//
//        Assert.assertEquals( "Minimized 0 -> 0", logCaptor.getValue() );
//    }

    @Test
    public void finsishedShouldProduceMessageForClassesTotalNonZero()
    {
        ArgumentCaptor<CharSequence> logCaptor = ArgumentCaptor.forClass( CharSequence.class );

        Log log = mock( Log.class );

        MinijarFilter m = new MinijarFilter( 1, 50, log );

        m.finished();

        verify( log, times( 1 ) ).info( logCaptor.capture() );

        Assert.assertEquals( "Minimized 51 -> 1 (1%)", logCaptor.getValue() );

    }

    @Test
    public void finsishedShouldProduceMessageForClassesTotalZero()
    {
        ArgumentCaptor<CharSequence> logCaptor = ArgumentCaptor.forClass( CharSequence.class );

        Log log = mock( Log.class );

        MinijarFilter m = new MinijarFilter( 0, 0, log );

        m.finished();

        verify( log, times( 1 ) ).info( logCaptor.capture() );

        Assert.assertEquals( "Minimized 0 -> 0", logCaptor.getValue() );

    }
}
