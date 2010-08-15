package org.apache.maven.plugin.war;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.war.stub.JarArtifactStub;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Stephane Nicoll
 */
public class WarDependenciesAnalysisTest
    extends AbstractWarExplodedMojoTest
{
    protected File getPomFile()
    {
        return new File( getBasedir(), "/target/test-classes/unit/dependencies/default.xml" );
    }

    protected File getTestDirectory()
    {
        return new File( getBasedir(), "target/test-classes/unit/dependenciesanalysis/test-dir" );
    }


    public void testNoChange()
        throws Exception
    {
        // setup test data
        final String testId = "no-change";
        final ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        ArtifactStub jarArtifact = new JarArtifactStub( getBasedir(), artifactHandler );
        jarArtifact.setArtifactId( "lib-test" );
        jarArtifact.setVersion( "1.0" );

        doTestTwiceWithUpdatedDependency( testId, new ArtifactStub[]{jarArtifact}, new ArtifactStub[]{jarArtifact},
                                          new String[]{"WEB-INF/lib/lib-test-1.0.jar"},
                                          new String[]{"WEB-INF/lib/lib-test-1.0.jar"} );

    }

    public void testRemovedDependency()
        throws Exception
    {
        // setup test data
        final String testId = "remove-dependency";
        final ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        ArtifactStub jarArtifact = new JarArtifactStub( getBasedir(), artifactHandler );
        jarArtifact.setArtifactId( "lib-test" );
        jarArtifact.setVersion( "1.0" );

        doTestTwiceWithUpdatedDependency( testId, new ArtifactStub[]{jarArtifact}, null,
                                          new String[]{"WEB-INF/lib/lib-test-1.0.jar"}, null );

    }

    public void testDependencyWithUpdatedVersion()
        throws Exception
    {
        // setup test data
        final String testId = "dependency-update-version";
        final ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        ArtifactStub jarArtifact = new JarArtifactStub( getBasedir(), artifactHandler );
        jarArtifact.setArtifactId( "lib-test" );
        jarArtifact.setVersion( "1.0" );

        ArtifactStub jarArtifact2 = new JarArtifactStub( getBasedir(), artifactHandler );
        jarArtifact2.setArtifactId( "lib-test" );
        jarArtifact2.setVersion( "2.0" );

        doTestTwiceWithUpdatedDependency( testId, new ArtifactStub[]{jarArtifact}, new ArtifactStub[]{jarArtifact2},
                                          new String[]{"WEB-INF/lib/lib-test-1.0.jar"},
                                          new String[]{"WEB-INF/lib/lib-test-2.0.jar"} );

    }

    public void testDependencyNowProvided()
        throws Exception
    {
        // setup test data
        final String testId = "dependency-now-provided";
        final ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        ArtifactStub jarArtifact = new JarArtifactStub( getBasedir(), artifactHandler );
        jarArtifact.setArtifactId( "lib-test" );
        jarArtifact.setVersion( "1.0" );

        ArtifactStub jarArtifact2 = new JarArtifactStub( getBasedir(), artifactHandler );
        jarArtifact2.setArtifactId( "lib-test" );
        jarArtifact2.setVersion( "1.0" );
        jarArtifact2.setScope( Artifact.SCOPE_PROVIDED );

        doTestTwiceWithUpdatedDependency( testId, new ArtifactStub[]{jarArtifact}, new ArtifactStub[]{jarArtifact2},
                                          new String[]{"WEB-INF/lib/lib-test-1.0.jar"}, null );

    }

    protected void doTestTwiceWithUpdatedDependency( String testId, ArtifactStub[] firstStubs,
                                                     ArtifactStub[] secondStubs, String[] firstCustomContent,
                                                     String[] secondCustomContent )
        throws Exception
    {
        // setup test data
        final File xmlSource = createXMLConfigDir( testId, new String[]{"web.xml"} );
        final File webAppDirectory = setUpMojoWithCache( testId, firstStubs );
        try
        {
            mojo.setWebXml( new File( xmlSource, "web.xml" ) );
            mojo.execute();

            final List assertedFiles = new ArrayList();
            assertedFiles.addAll( assertDefaultContent( webAppDirectory ) );
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory, firstCustomContent, "library not found" ) );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{MANIFEST_PATH} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );

            // Run the thing again and check it's ok
            setUpMojoWithCache( testId, secondStubs );

            mojo.execute();

            final List assertedFiles2 = new ArrayList();
            assertedFiles2.addAll( assertDefaultContent( webAppDirectory ) );
            assertedFiles2.addAll( assertWebXml( webAppDirectory ) );
            if ( secondCustomContent != null )
            {
                assertedFiles2.addAll(
                    assertCustomContent( webAppDirectory, secondCustomContent, "library not found" ) );

            }
            assertWebAppContent( webAppDirectory, assertedFiles2, filter );

        }
        finally
        {
            cleanDirectory( webAppDirectory );
            cleanDirectory( mojo.getWorkDirectory() );

        }
    }

    /**
     * Configures the exploded mojo for the specified test.
     *
     * @param testId        the id of the test
     * @param artifactStubs the dependencies (may be null)
     * @return the webapp directory
     * @throws Exception if an error occurs while configuring the mojo
     */
    protected File setUpMojoWithCache( final String testId, ArtifactStub[] artifactStubs )
        throws Exception
    {
        final File webappDir = setUpMojo( testId, artifactStubs, null );
        setVariableValueToObject( mojo, "useCache", Boolean.TRUE );
        final File cacheFile = new File( mojo.getWorkDirectory(), "webapp-cache.xml" );
        setVariableValueToObject( mojo, "cacheFile", cacheFile );

        return webappDir;
    }


}
