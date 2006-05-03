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

import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForReleasePhaseTest
    extends AbstractRewritingReleasePhaseTestCase
{
    private static final String NEXT_VERSION = "1.0";

    private static final String ALTERNATIVE_NEXT_VERSION = "2.0";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "rewrite-poms-for-release" );
    }

    protected ReleaseConfiguration createConfigurationFromProjects( String path, boolean copyFiles )
        throws Exception
    {
        ReleaseConfiguration releaseConfiguration =
            createConfigurationFromProjects( "rewrite-for-release/", path, copyFiles );
        releaseConfiguration.setUrl( "scm:svn:file://localhost/tmp/scm-repo" );
        releaseConfiguration.setReleaseLabel( "release-label" );
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test/checkout" ) );

        return releaseConfiguration;
    }

    protected String readTestProjectFile( String fileName )
        throws IOException
    {
        return FileUtils.fileRead( getTestFile( "target/test-classes/projects/rewrite-for-release/" + fileName ) );
    }

    protected ReleaseConfiguration createConfigurationFromBasicPom( boolean copyFiles )
        throws Exception
    {
        return createConfigurationFromProjects( "basic-pom", copyFiles );
    }

    public void testSimulateRewrite()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromBasicPom();
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        String expected = readTestProjectFile( "basic-pom/pom.xml" );

        phase.simulate( config );

        String actual = readTestProjectFile( "basic-pom/pom.xml" );
        assertEquals( "Check the original POM untouched", expected, actual );

        expected = readTestProjectFile( "basic-pom/expected-pom.xml" );
        actual = readTestProjectFile( "basic-pom/pom.xml.tag" );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    public void testClean()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromBasicPom();
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-release/basic-pom/pom.xml.tag" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.simulate( config );

        assertTrue( testFile.exists() );

        phase.clean( config );

        assertFalse( testFile.exists() );
    }

    public void testCleanNotExists()
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromBasicPom();
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-release/basic-pom/pom.xml.tag" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.clean( config );

        assertFalse( testFile.exists() );
    }

    protected void mapAlternateNextVersion( ReleaseConfiguration config, String projectId )
    {
        config.mapReleaseVersion( projectId, ALTERNATIVE_NEXT_VERSION );
    }

    protected void mapNextVersion( ReleaseConfiguration config, String projectId )
    {
        config.mapReleaseVersion( projectId, NEXT_VERSION );
    }

    protected ReleaseConfiguration createConfigurationForPomWithParentAlternateNextVersion( String path )
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( path );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        return config;
    }

    protected ReleaseConfiguration createConfigurationForWithParentNextVersion( String path )
        throws Exception
    {
        ReleaseConfiguration config = createConfigurationFromProjects( path );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", NEXT_VERSION );
        return config;
    }

    protected void unmapNextVersion( ReleaseConfiguration config, String projectId )
    {
        // nothing to do
    }
}
