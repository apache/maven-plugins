package org.apache.maven.plugin.dependency;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.project.MavenProject;

public class TestCopyDependenciesMojo2
    extends AbstractDependencyMojoTestCase
{

    CopyDependenciesMojo mojo;

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "copy-dependencies", true );

        File testPom = new File( getBasedir(), "target/test-classes/unit/copy-dependencies-test/plugin-config.xml" );
        mojo = (CopyDependenciesMojo) lookupMojo( "copy-dependencies", testPom );
        mojo.outputDirectory = new File( this.testDir, "outputDirectory" );
        // mojo.silent = true;

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        MavenProject project = mojo.getProject();

        Set artifacts = this.stubFactory.getScopedArtifacts();
        Set directArtifacts = this.stubFactory.getReleaseAndSnapshotArtifacts();
        artifacts.addAll( directArtifacts );

        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( directArtifacts );
        mojo.markersDirectory = new File( this.testDir, "markers" );

    }

    public void assertNoMarkerFile( Artifact artifact )
    {
        DefaultFileMarkerHandler handle = new DefaultFileMarkerHandler( artifact, mojo.markersDirectory );
        try
        {
            assertFalse( handle.isMarkerSet() );
        }
        catch ( MojoExecutionException e )
        {
            fail( e.getLongMessage() );
        }

    }


    public void testCopyDependenciesMojoIncludeCompileScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "compile";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.includeScope );

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( saf.include( artifact ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeTestScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "test";

        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.includeScope );

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( saf.include( artifact ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeRuntimeScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "runtime";
        mojo.execute();
        ScopeArtifactFilter saf = new ScopeArtifactFilter( mojo.includeScope );

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( saf.include( artifact ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludeprovidedScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "provided";

        mojo.execute();
        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );
            assertEquals( Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ), file.exists() );
        }
    }

    public void testCopyDependenciesMojoIncludesystemScope()
        throws Exception
    {
        mojo.project.setArtifacts( stubFactory.getScopedArtifacts() );
        mojo.project.setDependencyArtifacts( new HashSet() );
        mojo.includeScope = "system";

        mojo.execute();

        Iterator iter = mojo.project.getArtifacts().iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            String fileName = DependencyUtil.getFormattedFileName( artifact, false );
            File file = new File( mojo.outputDirectory, fileName );

            assertEquals( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ), file.exists() );
        }
    }

}
