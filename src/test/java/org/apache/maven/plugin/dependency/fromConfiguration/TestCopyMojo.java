/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.maven.plugin.dependency.fromConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractDependencyMojoTestCase;
import org.apache.maven.plugin.dependency.testUtils.DependencyTestUtils;
import org.apache.maven.plugin.dependency.testUtils.stubs.StubArtifactRepository;
import org.apache.maven.plugin.dependency.testUtils.stubs.StubArtifactResolver;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;

public class TestCopyMojo
    extends AbstractDependencyMojoTestCase
{

    CopyMojo mojo;

    public TestCopyMojo()
    {
        super();
    }

    protected void setUp()
        throws Exception
    {
        super.setUp( "copy", false );

        File testPom = new File( getBasedir(), "target/test-classes/unit/copy-test/plugin-config.xml" );
        mojo = (CopyMojo) lookupMojo( "copy", testPom );
        mojo.outputDirectory = new File( this.testDir, "outputDirectory" );
        mojo.silent = true;

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        // MavenProject project = mojo.getProject();
        // init classifier things
        mojo.setFactory( DependencyTestUtils.getArtifactFactory() );
        mojo.setResolver( new StubArtifactResolver( stubFactory, false, false ) );
        mojo.setLocal( new StubArtifactRepository( this.testDir.getAbsolutePath() ) );
    }

    public ArtifactItem getSingleArtifactItem( boolean removeVersion )
        throws MojoExecutionException
    {
        ArrayList list = mojo.getArtifactItems( removeVersion );
        return (ArtifactItem) list.get( 0 );
    }

    public void testGetArtifactItems()
        throws MojoExecutionException
    {

        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifact" );
        item.setGroupId( "groupId" );
        item.setVersion( "1.0" );

        ArrayList list = new ArrayList( 1 );
        list.add( item );

        mojo.artifactItems = list;

        ArtifactItem result = getSingleArtifactItem( false );
        assertFalse( result.isDoOverWrite() );

        item.setOverWrite( "true" );
        result = getSingleArtifactItem( false );
        assertTrue( result.isDoOverWrite() );
        assertEquals(mojo.outputDirectory,result.getOutputDirectory());

        item.setOverWrite( "false" );
        result = getSingleArtifactItem( false );
        assertFalse( result.isDoOverWrite() );

        item.setOverWrite( "" );
        result = getSingleArtifactItem( false );
        assertFalse( result.isDoOverWrite() );
        
        item.setOverWrite( "blah" );
        File output = new File(mojo.outputDirectory,"override");
        item.setOutputDirectory(output);
        result = getSingleArtifactItem( false );
        assertFalse( result.isDoOverWrite() );
        assertEquals(output,result.getOutputDirectory());
    }

    public void assertFilesExist( Collection items, boolean exist )
    {
        Iterator iter = items.iterator();
        while ( iter.hasNext() )
        {
            assertFileExists( (ArtifactItem) iter.next(), exist );
        }
    }

    public void assertFileExists( ArtifactItem item, boolean exist )
    {
        File file = new File( item.getOutputDirectory(), item.getDestFileName() );
        assertEquals( exist, file.exists() );
    }

    public void testMojoDefaults()
    {
        CopyMojo mojo = new CopyMojo();

        assertFalse( mojo.isStripVersion() );
    }

    public void testCopyFile()
        throws IOException, MojoExecutionException
    {
        ArrayList list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );

        mojo.artifactItems = list;

        mojo.execute();

        assertFilesExist( list, true );
    }

    public void testCopyToLocation()
        throws IOException, MojoExecutionException
    {
        ArrayList list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );
        ArtifactItem item = (ArtifactItem) list.get( 0 );
        item.setOutputDirectory( new File( mojo.outputDirectory, "testOverride" ) );

        mojo.artifactItems = list;

        mojo.execute();

        assertFilesExist( list, true );
    }

    public void testCopyStripVersion()
        throws IOException, MojoExecutionException
    {
        ArrayList list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );
        ArtifactItem item = (ArtifactItem) list.get( 0 );
        item.setOutputDirectory( new File( mojo.outputDirectory, "testOverride" ) );
        mojo.setStripVersion( true );

        mojo.artifactItems = list;

        mojo.execute();

        assertFilesExist( list, true );
    }

    // TODO: test overwrite / overwrite if newer / overwrite release / overwrite
    // snapshot
    // TODO: test non classifier
    // TODO: test missing version - from dependency and from dependency
    // management
}
