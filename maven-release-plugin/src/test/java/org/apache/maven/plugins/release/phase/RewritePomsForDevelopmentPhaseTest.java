package org.apache.maven.plugins.release.phase;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.model.Scm;
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForDevelopmentPhaseTest
    extends AbstractRewritingReleasePhaseTestCase
{
    private static final String NEXT_VERSION = "1.1-SNAPSHOT";

    private static final String ALTERNATIVE_NEXT_VERSION = "2.1-SNAPSHOT";

    private static final String RELEASE_VERSION = "1.0";

    private static final String ALTERNATIVE_RELEASE_VERSION = "2.0";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "rewrite-poms-for-development" );
    }

    public void testSimulateRewrite()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );

        String expected = readTestProjectFile( "basic-pom/pom.xml" );

        phase.simulate( config, null, reactorProjects );

        String actual = readTestProjectFile( "basic-pom/pom.xml" );
        assertEquals( "Check the original POM untouched", expected, actual );

        expected = readTestProjectFile( "basic-pom/expected-pom.xml" );
        actual = readTestProjectFile( "basic-pom/pom.xml.next" );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    public void testClean()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-development/basic-pom/pom.xml.next" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.simulate( config, null, reactorProjects );

        assertTrue( testFile.exists() );

        phase.clean( reactorProjects );

        assertFalse( testFile.exists() );
    }

    public void testCleanNotExists()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-development/basic-pom/pom.xml.next" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.clean( reactorProjects );

        assertFalse( testFile.exists() );
    }

    public void testRewriteBasicPomUnmappedScm()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "basic-pom", true );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        mapNextVersion( config, "groupId:artifactId" );

        try
        {
            phase.execute( config, null, reactorProjects );

            fail( "Expected failure" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertTrue( true );
        }
    }

    protected String readTestProjectFile( String fileName )
        throws IOException
    {
        return FileUtils.fileRead( getTestFile( "target/test-classes/projects/rewrite-for-development/" + fileName ) );
    }

    protected List createReactorProjects( String path, boolean copyFiles )
        throws Exception
    {
        return createReactorProjects( "rewrite-for-development/", path, copyFiles );
    }

    protected ReleaseDescriptor createDescriptorFromBasicPom( List reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = super.createDescriptorFromBasicPom( reactorProjects );

        mapScm( config );

        return config;
    }

    private void mapScm( ReleaseDescriptor config )
    {
        Scm scm = new Scm();
        scm.setConnection( "scm:svn:file://localhost/tmp/scm-repo/trunk" );
        scm.setDeveloperConnection( "scm:svn:file://localhost/tmp/scm-repo/trunk" );
        scm.setUrl( "file://localhost/tmp/scm-repo/trunk" );
        config.mapOriginalScmInfo( "groupId:artifactId", scm );
    }

    protected void mapAlternateNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( projectId, ALTERNATIVE_NEXT_VERSION );
    }

    protected void mapNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, RELEASE_VERSION );
        config.mapDevelopmentVersion( projectId, NEXT_VERSION );
    }

    protected void unmapNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, RELEASE_VERSION );
    }

    protected ReleaseDescriptor createConfigurationForPomWithParentAlternateNextVersion( List reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        mapScm( config );

        return config;
    }

    protected ReleaseDescriptor createConfigurationForWithParentNextVersion( List reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject1", NEXT_VERSION );
        mapScm( config );

        return config;
    }

    public void testRewriteBasicPomWithCvs()
        throws Exception
    {

        List reactorProjects = createReactorProjects( "basic-pom-with-cvs" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        Scm scm = new Scm();
        scm.setConnection( "scm:cvs:pserver:anoncvs@localhost:/tmp/scm-repo:module" );
        scm.setDeveloperConnection( "scm:cvs:ext:${username}@localhost:/tmp/scm-repo:module" );
        scm.setUrl( "http://localhost/viewcvs.cgi/module" );
        config.mapOriginalScmInfo( "groupId:artifactId", scm );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithCvsFromTag()
        throws Exception
    {

        List reactorProjects = createReactorProjects( "basic-pom-with-cvs-from-tag" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        Scm scm = new Scm();
        scm.setConnection( "scm:cvs:pserver:anoncvs@localhost:/tmp/scm-repo:module" );
        scm.setDeveloperConnection( "scm:cvs:ext:${username}@localhost:/tmp/scm-repo:module" );
        scm.setUrl( "http://localhost/viewcvs.cgi/module" );
        scm.setTag( "original-label" );
        config.mapOriginalScmInfo( "groupId:artifactId", scm );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithInheritedScm()
        throws Exception
    {

        List reactorProjects = createReactorProjects( "basic-pom-inherited-scm" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subproject1", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subsubproject", RELEASE_VERSION );
        config.mapDevelopmentVersion( "groupId:subsubproject", NEXT_VERSION );
        config.mapOriginalScmInfo( "groupId:artifactId", null );
        config.mapOriginalScmInfo( "groupId:subproject1", null );
        config.mapOriginalScmInfo( "groupId:subsubproject", null );

        phase.execute( config, null, reactorProjects );

        assertTrue( compareFiles( reactorProjects ) );
    }
}
