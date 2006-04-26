package org.apache.maven.plugins.release.phase;

/*
 * Copyright 2005-2006 The Apache Software Foundation.  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at       http://www.apache.org/licenses/LICENSE-2.0  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForReleasePhaseTest
    extends PlexusTestCase
{
    private ReleasePhase phase;

    private MavenProjectBuilder projectBuilder;

    private ArtifactRepository localRepository;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "rewrite-poms-for-release" );

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        String localRepoPath = getTestFile( "target/local-repository" ).getAbsolutePath().replace( '\\', '/' );
        localRepository = new DefaultArtifactRepository( "local", "file://" + localRepoPath, layout );
    }

    public void testRewriteBasicPom()
        throws ReleaseExecutionException, ProjectBuildingException, IOException
    {
        File testFile = getCopiedTestFile( "rewrite-for-release/basic-pom.xml" );
        MavenProject project = projectBuilder.build( testFile, localRepository, null );
        ReleaseConfiguration config = createReleaseConfiguration( Collections.singletonList( project ) );

        phase.execute( config );

        // TODO: assertions
    }

    public void testRewriteAddSchema()
        throws ReleaseExecutionException, ProjectBuildingException, IOException
    {
        File testFile = getCopiedTestFile( "rewrite-for-release/basic-pom.xml" );
        MavenProject project = projectBuilder.build( testFile, localRepository, null );
        ReleaseConfiguration config = createReleaseConfiguration( Collections.singletonList( project ) );
        config.setAddSchema( true );

        // Run a second time to check they are not duplicated
        for ( int i = 0; i < 2; i++ )
        {
            phase.execute( config );

            String content = FileUtils.fileRead( testFile ).replace( '\n', ' ' );
            Matcher m = Pattern.compile( "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" ).matcher( content );
            assertTrue( "check for schema location", m.find() );
            assertFalse( "check schema location is not duplicated", m.find() );

            m = Pattern.compile(
                "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\"" ).matcher(
                content );
            assertTrue( "check for schema", m.find() );
            assertFalse( "check for duplicated schema", m.find() );

            m = Pattern.compile( "xmlns=\"http://maven.apache.org/POM/4.0.0\"" ).matcher( content );
            assertTrue( "check for namespace", m.find() );
            assertFalse( "check for duplicated namespace", m.find() );
        }
    }

    private static File getCopiedTestFile( String fileName )
        throws IOException
    {
        File testFile = getTestFile( "target/test-classes/projects/" + fileName );
        FileUtils.copyFile( getTestFile( "src/test/resources/projects/" + fileName ), testFile );
        return testFile;
    }

    private static ReleaseConfiguration createReleaseConfiguration( List reactorProjects )
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm:svn:file://localhost/tmp/scm-repo" );
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test/checkout" ) );
        releaseConfiguration.setReactorProjects( reactorProjects );
        return releaseConfiguration;
    }
}
