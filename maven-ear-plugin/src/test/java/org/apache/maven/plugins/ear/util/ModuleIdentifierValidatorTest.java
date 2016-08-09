package org.apache.maven.plugins.ear.util;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.ear.EarModule;
import org.apache.maven.plugins.ear.util.ModuleIdentifierValidator;
import org.junit.Before;
import org.junit.Test;

public class ModuleIdentifierValidatorTest
{
    private List<EarModule> earModules;

    private ModuleIdentifierValidator miv;

    @Before
    public void before()
    {
        this.earModules = new ArrayList<EarModule>();
        this.miv = new ModuleIdentifierValidator( this.earModules );
    }

    private EarModule createMockEarModule( String groupId, String artifactId, String version )
    {
        EarModule earModule = mock( EarModule.class );
        Artifact artifact = mock( Artifact.class );
        when( earModule.getArtifact() ).thenReturn( artifact );
        when( earModule.getArtifact().getGroupId() ).thenReturn( groupId );
        when( earModule.getArtifact().getArtifactId() ).thenReturn( artifactId );
        when( earModule.getArtifact().getVersion() ).thenReturn( version );
        when( earModule.getArtifact().getId() ).thenReturn( groupId + ":" + artifactId + ":" + version );
        return earModule;
    }

    private EarModule createMockEarModule( String groupId, String artifactId, String classifier, String version )
    {
        EarModule earModule = mock( EarModule.class );
        Artifact artifact = mock( Artifact.class );
        when( earModule.getArtifact() ).thenReturn( artifact );
        when( earModule.getArtifact().getGroupId() ).thenReturn( groupId );
        when( earModule.getArtifact().getArtifactId() ).thenReturn( artifactId );
        when( earModule.getArtifact().getClassifier() ).thenReturn( classifier );
        when( earModule.getArtifact().getVersion() ).thenReturn( version );
        when( earModule.getArtifact().getId() ).thenReturn( groupId + ":" + artifactId + ":" + classifier + ":" + version );
        return earModule;
    }

    @Test
    public void existDuplicateShouldResultFalseWithEmptyList()
    {
        miv.checkForDuplicateArtifacts();

        assertFalse( miv.existDuplicateArtifacts() );
    }

    @Test
    public void shouldNotFailCauseTheArtifactIdsAreDifferentWithSameGroupId()
    {
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-a", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-b", "1.0.0" ) );

        assertFalse( miv.checkForDuplicateArtifacts().existDuplicateArtifacts() );

        Map<String, List<EarModule>> result = miv.getDuplicateArtifacts();

        assertTrue( result.isEmpty() );
    }

    @Test
    public void shouldNotFailCauseTheArtifactIdsAreDifferent()
    {
        earModules.add( createMockEarModule( "org.apache", "artifact-1", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache", "artifact-2", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven", "aid-1", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven", "aid-2", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-a", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-b", "1.0.0" ) );

        assertFalse( miv.checkForDuplicateArtifacts().existDuplicateArtifacts() );

        Map<String, List<EarModule>> result = miv.getDuplicateArtifacts();

        assertTrue( result.isEmpty() );
    }

    @Test
    public void shouldNotFailCauseTheClassifiersAreDifferent()
    {
        earModules.add( createMockEarModule( "org.apache", "artifact-1", "first", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache", "artifact-2", "second", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven", "aid-1", "first", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven", "aid-2", "second", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-a", "first", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-b", "second", "1.0.0" ) );

        assertFalse( miv.checkForDuplicateArtifacts().existDuplicateArtifacts() );

        Map<String, List<EarModule>> result = miv.getDuplicateArtifacts();

        assertTrue( result.isEmpty() );
    }

    @Test
    public void shouldFailCauseTheArtifactIdsAreIdenticalWithDifferentGroupIds()
    {
        EarModule earModule1 = createMockEarModule( "org.apache.maven.test", "result-artifact", "1.0.0" );
        EarModule earModule2 = createMockEarModule( "org.apache.maven", "result-artifact", "1.0.0" );
        earModules.add( earModule1 );
        earModules.add( earModule2 );

        miv.checkForDuplicateArtifacts();
        Map<String, List<EarModule>> result = miv.getDuplicateArtifacts();

        assertFalse( result.isEmpty() );
        assertEquals( 1, result.size() );
        assertTrue( result.containsKey( "result-artifact:1.0.0" ) );
        assertEquals( 2, result.get( "result-artifact:1.0.0" ).size() );
    }

    @Test
    public void shouldFailCauseTheArtifactIdsAreIdenticalWithClassifiers()
    {
        EarModule earModule1 = createMockEarModule( "org.apache.maven.test", "result-artifact", "first", "1.0.0" );
        EarModule earModule2 = createMockEarModule( "org.apache.maven", "result-artifact", "second", "1.0.0" );
        earModules.add( earModule1 );
        earModules.add( earModule2 );

        miv.checkForDuplicateArtifacts();
        Map<String, List<EarModule>> result = miv.getDuplicateArtifacts();

        assertFalse( result.isEmpty() );
        assertEquals( 1, result.size() );
        assertTrue( result.containsKey( "result-artifact:1.0.0" ) );
        assertEquals( 2, result.get( "result-artifact:1.0.0" ).size() );
    }

    @Test
    public void shouldFailCauseTheArtifactIdsAreIdentical()
    {
        earModules.add( createMockEarModule( "org.apache", "artifact-1", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache", "artifact-2", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven", "aid-1", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven", "artifact-2", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-a", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-b", "1.0.0" ) );

        miv.checkForDuplicateArtifacts();
        Map<String, List<EarModule>> result = miv.getDuplicateArtifacts();

        assertFalse( result.isEmpty() );
    }

    @Test
    public void shouldFailWithAppropriateInformationAboutTheIdenticalArtifactIds()
    {
        EarModule earModule_1 = createMockEarModule( "org.apache", "artifact-2", "1.0.0" );
        EarModule earModule_2 = createMockEarModule( "org.apache.maven", "artifact-2", "1.0.0" );

        earModules.add( createMockEarModule( "org.apache", "artifact-1", "1.0.0" ) );
        earModules.add( earModule_1 );
        earModules.add( createMockEarModule( "org.apache.maven", "aid-1", "1.0.0" ) );
        earModules.add( earModule_2 );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-a", "1.0.0" ) );
        earModules.add( createMockEarModule( "org.apache.maven.test", "result-artifact-b", "1.0.0" ) );

        miv.checkForDuplicateArtifacts();
        Map<String, List<EarModule>> result = miv.getDuplicateArtifacts();

        assertFalse( result.isEmpty() );
        assertEquals( 1, result.size() );

        assertTrue( result.containsKey( "artifact-2:1.0.0" ) );

        List<EarModule> list = result.get( "artifact-2:1.0.0" );

        assertEquals( 2, list.size() );
        assertTrue( list.contains( earModule_1 ) );
        assertTrue( list.contains( earModule_2 ) );

    }

}
