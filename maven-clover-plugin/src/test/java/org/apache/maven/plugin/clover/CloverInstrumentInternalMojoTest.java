/*
 * Copyright 2006 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover;

import org.jmock.MockObjectTestCase;
import org.jmock.Mock;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.logging.Log;

import java.util.Collections;
import java.util.Set;
import java.io.File;

/**
 * Unit tests for {@link org.apache.maven.plugin.clover.CloverInstrumentInternalMojo}.
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverInstrumentInternalMojoTest extends MockObjectTestCase
{
    private CloverInstrumentInternalMojo mojo;

    /**
     * Class used to return a given value when lastModified is called. This is because File.getLastModified always
     * return 0L if the file doesn't exist and our tests below do not point to existing files.
     */
    public class MockFile extends File
    {
        private long lastModifiedDate;

        public MockFile(String file, long lastModifiedDate)
        {
            super(file);
            this.lastModifiedDate = lastModifiedDate;
        }

        public long lastModified()
        {
            return this.lastModifiedDate;
        }
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        this.mojo = new CloverInstrumentInternalMojo();
    }

    public void testFindCloverArtifactWithCorrectArtifactIdButWrongGroupId()
    {
        Mock mockArtifact = mock(Artifact.class);
        mockArtifact.stubs().method( "getArtifactId" ).will( returnValue( "clover" ) );
        mockArtifact.stubs().method( "getGroupId" ).will( returnValue( "notcenquaid" ) );

        Artifact clover = this.mojo.findCloverArtifact( Collections.singletonList( mockArtifact.proxy() ) );

        assertNull( "Clover artifact should not have been found!", clover );
    }

    public void testFindCloverArtifactWhenCorrectIds()
    {
        Mock mockArtifact = mock(Artifact.class);
        mockArtifact.stubs().method( "getArtifactId" ).will( returnValue( "clover" ) );
        mockArtifact.stubs().method( "getGroupId" ).will( returnValue( "com.cenqua.clover" ) );

        Artifact clover = this.mojo.findCloverArtifact( Collections.singletonList( mockArtifact.proxy() ) );

        assertNotNull( "Clover artifact should have been found!", clover );
    }

    public void testSwizzleCloverDependenciesWhenDependencyHasClassifier()
    {
        Artifact artifact = setUpMockArtifact( "some.groupId", "someArtifactId", "1.0", "jar", "compile", "whatever",
            null );

        Set resultSet = this.mojo.swizzleCloverDependencies( Collections.singleton( artifact ) );
        assertEquals( 1, resultSet.size() );
        assertTrue( "Resulting artifact should have been the original one", resultSet.contains( artifact ) );
    }

    public void testSwizzleCloverDependenciesWhenCloveredVersionOfDependencyIsNewerThanOriginal()
    {
        // Ensure that the original artifact is older than the clovered artifact so that the clovered artifact
        // is picked. Note that that we use -500/-1000 to ensure not to set the time in the future as maybe
        // this could cause some problems on some OS.
        long now = System.currentTimeMillis();
        File artifactFile = new MockFile( "some/file/artifact", now - 1000L );
        File cloveredArtifactFile = new MockFile( "some/file/cloveredArtifact", now - 500L );

        Artifact artifact = setUpMockArtifact( "some.groupId", "someArtifactId", "1.0", "jar", "compile", null,
            artifactFile );
        Artifact cloveredArtifact = setUpMockArtifact( null, null, null, null, null, null, cloveredArtifactFile );

        setUpCommonMocksForSwizzleCloverDependenciesTests(cloveredArtifact);

        Set resultSet = this.mojo.swizzleCloverDependencies( Collections.singleton( artifact ) );
        assertEquals( 1, resultSet.size() );
        assertTrue( "Resulting artifact should have been the clovered one", resultSet.contains( cloveredArtifact ) );
    }

    public void testSwizzleCloverDependenciesWhenOriginalVersionOfDependencyIsNewerThanCloveredOne()
    {
        // Ensure that the clovered artifact is older than the original artifact so that the original artifact
        // is picked. Note that that we use -500/-1000 to ensure not to set the time in the future as maybe
        // this could cause some problems on some OS.
        long now = System.currentTimeMillis();
        File artifactFile = new MockFile( "some/file/artifact", now - 500L );
        File cloveredArtifactFile = new MockFile( "some/file/cloveredArtifact", now - 1000L );

        Artifact artifact = setUpMockArtifact( "some.groupId", "someArtifactId", "1.0", "jar", "compile", null,
            artifactFile );
        Artifact cloveredArtifact = setUpMockArtifact( null, null, null, null, null, null, cloveredArtifactFile );

        setUpCommonMocksForSwizzleCloverDependenciesTests(cloveredArtifact);

        Set resultSet = this.mojo.swizzleCloverDependencies( Collections.singleton( artifact ) );
        assertEquals( 1, resultSet.size() );
        assertTrue( "Resulting artifact should have been the original one", resultSet.contains( artifact ) );
    }

    private void setUpCommonMocksForSwizzleCloverDependenciesTests(Artifact cloveredArtifact)
    {
        Mock mockArtifactFactory = mock( ArtifactFactory.class );
        mockArtifactFactory.stubs().method( "createArtifactWithClassifier" ).will(returnValue( cloveredArtifact ) );

        Mock mockArtifactResolver = mock( ArtifactResolver.class );
        mockArtifactResolver.stubs().method( "resolve" );

        Mock mockLog = mock( Log.class );
        mockLog.stubs().method( "warn" );

        this.mojo.setArtifactFactory( ( ArtifactFactory ) mockArtifactFactory.proxy() );
        this.mojo.setArtifactResolver( ( ArtifactResolver ) mockArtifactResolver.proxy() );
        this.mojo.setLog( (Log) mockLog.proxy() );
    }

    private Artifact setUpMockArtifact(String groupId, String artifactId, String version, String type, String scope,
        String classifier, File file)
    {
        Mock mockArtifact = mock( Artifact.class );
        mockArtifact.stubs().method( "getClassifier" ).will( returnValue( classifier ) );
        mockArtifact.stubs().method( "getGroupId" ).will( returnValue( groupId ) );
        mockArtifact.stubs().method( "getArtifactId" ).will( returnValue( artifactId ) );
        mockArtifact.stubs().method( "getVersion" ).will( returnValue( version ) );
        mockArtifact.stubs().method( "getType" ).will( returnValue( type ) );
        mockArtifact.stubs().method( "getScope" ).will( returnValue( scope ) );
        mockArtifact.stubs().method( "getFile" ).will( returnValue ( file ) );
        mockArtifact.stubs().method( "getId" ).will( returnValue (
            groupId + ":" + artifactId + ":" + version + ":" + classifier ) );
        mockArtifact.stubs().method( "setScope" );

        return (Artifact) mockArtifact.proxy(); 
    }
}
