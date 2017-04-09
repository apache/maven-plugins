package org.apache.maven.plugins.dependency.analyze;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugins.dependency.analyze.AnalyzeDepMgt;
import org.apache.maven.plugins.dependency.testUtils.DependencyArtifactStubFactory;
import org.apache.maven.plugins.dependency.testUtils.stubs.DependencyProjectStub;
import org.apache.maven.project.MavenProject;

public class TestAnalyzeDepMgt
    extends TestCase
{

    AnalyzeDepMgt mojo;

    DependencyArtifactStubFactory stubFactory;

    Dependency exclusion;
    Dependency exclusionMatchingVersion;

    Exclusion ex;

    Artifact exclusionArtifact;

    DependencyManagement depMgt;
    DependencyManagement depMgtMatchingVersion;
    DependencyManagement depMgtNoExclusions;
    protected void setUp()
        throws Exception
    {

        mojo = new AnalyzeDepMgt();
        MavenProject project = new DependencyProjectStub();

        stubFactory = new DependencyArtifactStubFactory( new File( "" ), false );

        Set<Artifact> allArtifacts = stubFactory.getMixedArtifacts();
        Set<Artifact> directArtifacts = stubFactory.getClassifiedArtifacts();


        exclusionArtifact = stubFactory.getReleaseArtifact();
        directArtifacts.add( exclusionArtifact );
        ex = new Exclusion();
        ex.setArtifactId( exclusionArtifact.getArtifactId() );
        ex.setGroupId( exclusionArtifact.getGroupId() );

        exclusion = new Dependency();
        exclusion.setArtifactId( exclusionArtifact.getArtifactId() );
        exclusion.setGroupId( exclusionArtifact.getGroupId() );
        exclusion.setType( exclusionArtifact.getType() );
        exclusion.setClassifier( "" );
        exclusion.setVersion( "3.0" );

        exclusion.addExclusion( ex );
        List<Dependency> list = new ArrayList<Dependency>();
        list.add( exclusion );

        depMgt = new DependencyManagement();
        depMgt.setDependencies( list );

        exclusionMatchingVersion = new Dependency();
        exclusionMatchingVersion.setArtifactId( exclusionArtifact.getArtifactId() );
        exclusionMatchingVersion.setGroupId( exclusionArtifact.getGroupId() );
        exclusionMatchingVersion.setType( exclusionArtifact.getType() );
        exclusionMatchingVersion.setClassifier( "" );
        exclusionMatchingVersion.setVersion( exclusionArtifact.getVersion() );

        exclusionMatchingVersion.addExclusion( ex );

        list = new ArrayList<Dependency>();
        list.add( exclusionMatchingVersion );

        depMgtMatchingVersion = new DependencyManagement();
        depMgtMatchingVersion.setDependencies( list );

        project.setArtifacts( allArtifacts );
        project.setDependencyArtifacts( directArtifacts );

        mojo.setProject( project );

    }

    public void testGetManagementKey()
        throws IOException
    {
        Dependency dep = new Dependency();
        dep.setArtifactId( "artifact" );
        dep.setClassifier( "class" );
        dep.setGroupId( "group" );
        dep.setType( "type" );

        // version isn't used in the key, it can be different
        dep.setVersion( "1.1" );

        Artifact artifact = stubFactory.createArtifact( "group", "artifact", "1.0", Artifact.SCOPE_COMPILE, "type",
                                                        "class" );

        // basic case ok
        assertEquals( dep.getManagementKey(), mojo.getArtifactManagementKey( artifact ) );

        // now change each one and make sure it fails, then set it back and make
        // sure it's ok before
        // testing the next one
        dep.setType( "t" );
        assertFalse( dep.getManagementKey().equals( mojo.getArtifactManagementKey( artifact ) ) );

        dep.setType( "type" );
        assertEquals( dep.getManagementKey(), mojo.getArtifactManagementKey( artifact ) );

        dep.setArtifactId( "a" );
        assertFalse( dep.getManagementKey().equals( mojo.getArtifactManagementKey( artifact ) ) );

        dep.setArtifactId( "artifact" );
        assertEquals( dep.getManagementKey(), mojo.getArtifactManagementKey( artifact ) );

        dep.setClassifier( "c" );
        assertFalse( dep.getManagementKey().equals( mojo.getArtifactManagementKey( artifact ) ) );

        dep.setClassifier( "class" );
        assertEquals( dep.getManagementKey(), mojo.getArtifactManagementKey( artifact ) );

        dep.setGroupId( "g" );
        assertFalse( dep.getManagementKey().equals( mojo.getArtifactManagementKey( artifact ) ) );

        dep.setGroupId( "group" );
        dep.setClassifier( null );
        artifact = stubFactory.createArtifact( "group", "artifact", "1.0", Artifact.SCOPE_COMPILE, "type", null );
        assertEquals( dep.getManagementKey(), mojo.getArtifactManagementKey( artifact ) );

        dep.setClassifier( "" );
        artifact = stubFactory.createArtifact( "group", "artifact", "1.0", Artifact.SCOPE_COMPILE, "type", "" );
        assertEquals( dep.getManagementKey(), mojo.getArtifactManagementKey( artifact ) );
    }

    public void testAddExclusions()
    {

        assertEquals( 0, mojo.addExclusions( null ).size() );

        List<Exclusion> list = new ArrayList<Exclusion>();
        list.add( ex );
        Map<String, Exclusion> map = mojo.addExclusions( list );

        assertEquals( 1,map.size() );
        assertTrue( map.containsKey( mojo.getExclusionKey( ex ) ) );
        assertSame( ex, map.get( mojo.getExclusionKey( ex ) ) );

        mojo.setIgnoredExclusions( Collections.singleton( mojo.getExclusionKey( ex ) ) );
        assertEquals( 0, mojo.addExclusions( list ).size() );

        mojo.setIgnoredExclusions( Collections.singleton( ex.getGroupId() + ":*" ) );
        assertEquals( 0, mojo.addExclusions( list ).size() );

        mojo.setIgnoredExclusions( Collections.singleton( "*:" + ex.getArtifactId() ) );
        assertEquals( 0, mojo.addExclusions( list ).size() );

        mojo.setIgnoredExclusions( Collections.singleton( "*:*" ) );
        assertEquals( 0, mojo.addExclusions( list ).size() );
    }

    public void testGetExclusionErrors()
    {
        List<Exclusion> list = new ArrayList<Exclusion>();
        list.add( ex );

        // already tested this method so I can trust it.
        Map<String, Exclusion> map = mojo.addExclusions( list );

        List<Artifact> l = mojo.getExclusionErrors( map, mojo.getProject().getArtifacts() );

        assertEquals( 1, l.size() );

        assertEquals( mojo.getExclusionKey( ex ), mojo.getExclusionKey(l.get( 0 )) );
    }

    public void testGetMismatch()
        throws IOException
    {
        Map<String, Dependency> depMgtMap = new HashMap<String, Dependency>();

        depMgtMap.put( exclusion.getManagementKey(), exclusion );

        Map<Artifact, Dependency> results = mojo.getMismatch( depMgtMap, mojo.getProject().getArtifacts() );

        assertEquals( 1, results.size() );
        // the release artifact is used to create the exclusion
        assertTrue( results.containsKey( stubFactory.getReleaseArtifact() ) );
        assertSame( exclusion, results.get( stubFactory.getReleaseArtifact() ) );
    }

    public void testMojo() throws IOException
    {
        mojo.setIgnoreDirect( false );
        try
        {
            // test with nothing in depMgt
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail( "Caught Unexpected Exception:" + e.getLocalizedMessage() );
        }

        try
        {
            DependencyProjectStub project = (DependencyProjectStub) mojo.getProject();
            project.setDependencyManagement( depMgt );
            // test with exclusion
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail( "Caught Unexpected Exception:" + e.getLocalizedMessage() );
        }

        try
        {
            DependencyProjectStub project = (DependencyProjectStub) mojo.getProject();
            project.setDependencyManagement( depMgt );
            // test with exclusion
            mojo.setFailBuild( true );
            mojo.execute();
            fail( "Expected exception to fail the build." );
        }
        catch ( Exception e )
        {
            System.out.println( "Caught Expected Exception:" + e.getLocalizedMessage() );
        }

        try
        {
            DependencyProjectStub project = (DependencyProjectStub) mojo.getProject();
            project.setDependencyManagement( depMgt );
            // test with exclusion
            mojo.setFailBuild( true );
            mojo.setIgnoreDirect( true );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail( "Caught Unexpected Exception:" + e.getLocalizedMessage() );
        }

        try
        {
            DependencyProjectStub project = (DependencyProjectStub) mojo.getProject();
            project.setDependencyManagement( depMgtMatchingVersion );
            // test without ignoring the exclusion error
            mojo.setFailBuild( true );
            mojo.setIgnoreDirect( false );
            mojo.execute();
            fail( "Expected exception to fail the build." );
        }
        catch ( Exception e )
        {
            System.out.println("Caught Expected Exception:" + e.getLocalizedMessage());
        }

        try
        {
            DependencyProjectStub project = (DependencyProjectStub) mojo.getProject();
            project.setDependencyManagement( depMgtMatchingVersion );
            // test with the exclusion error ignored
            mojo.setFailBuild( true );
            mojo.setIgnoreDirect( false );
            mojo.setIgnoredExclusions( Collections.singleton( mojo.getExclusionKey( ex ) ) );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail( "Caught Unexpected Exception:" + e.getLocalizedMessage() );
        }
    }
}
