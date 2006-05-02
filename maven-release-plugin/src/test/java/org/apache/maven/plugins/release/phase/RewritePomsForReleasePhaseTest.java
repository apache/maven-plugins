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

    /**
     * Test for writePom with special characters in the pom
     * Currently breaking under Linux Continuum box
     *
     * @throws Exception
     */
//    public void testWritePom()
//        throws Exception
//    {
//        System.out.println( Charset.defaultCharset().name() );
//        System.out.println( System.getProperty("file.encoding") );
//
//        Model model = new Model();
//        Contributor contributor = new Contributor();
//        /* hack to avoid problems with sources encoding, this string contains accentuated "aeiou" in UTF-8 */
//        String s = new String( new byte[] { -61, -95, -61, -87, -61, -83, -61, -77, -61, -70 }, "UTF-8" );
//
//        contributor.setName( s );
//        model.addContributor( contributor );
//        File file = new File( mojo.basedir, "testWritePom.xml" );
//
//        scmHelperMock.expects( new MethodNameMatcher( new IsAnything() ) );
//        scmHelperMock.expects( new MethodNameMatcher( "getScmManager" ) ).will( new ReturnStub( scmManager ) );
//
//        mojo.writePom( file, model, "version" );
//
//        MavenXpp3Reader pomReader = new MavenXpp3Reader();
//
//        Model readModel = pomReader.read( new BufferedReader( new FileReader( file ) ) );
//        Contributor readContributor = (Contributor) readModel.getContributors().get( 0 );
//
//        String msg = "POM is written in a wrong encoding: \n"
//            + "Expected bytes: " + Arrays.toString( contributor.getName().getBytes() ) + "\n"
//            + "Returned bytes: " + Arrays.toString( readContributor.getName().getBytes() ) + "\n"
//            + "JVM default charset: " + Charset.defaultCharset() + "\n"
//            + "System property file.encoding: " + System.getProperty("file.encoding") + "\n";
//        assertEquals( msg, contributor.getName(), readContributor.getName() );
//
//        scmHelperMock.verify();
//    }
// TODO [!]
}
