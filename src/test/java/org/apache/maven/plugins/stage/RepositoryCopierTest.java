package org.apache.maven.plugins.stage;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.util.List;

/** @author Jason van Zyl */
public class RepositoryCopierTest
    extends PlexusTestCase
{
    private String version = "2.0.6";

    private MetadataXpp3Reader reader = new MetadataXpp3Reader();

    public void testCopy()
        throws Exception
    {
        RepositoryCopier copier = (RepositoryCopier) lookup( RepositoryCopier.ROLE );

        File targetRepoSource = new File( getBasedir(), "src/test/target-repository" );

        File targetRepo = new File( getBasedir(), "target/target-repository" );

        System.out.println( "Copying target stage for tests ..." );

        FileUtils.copyDirectoryStructure( targetRepoSource, targetRepo );

        File stagingRepo = new File( getBasedir(), "src/test/staging-repository" );

        Repository sourceRepository = new Repository( "source", "file://" + stagingRepo );
        Repository targetRepository = new Repository( "target", "scp://localhost/" + targetRepo );

        copier.copy( sourceRepository, targetRepository, version );

        String s[] = {
            "maven",
            "maven-artifact",
            "maven-artifact-manager",
            "maven-artifact-test",
            "maven-core",
            "maven-error-diagnostics",
            "maven-model",
            "maven-monitor",
            "maven-plugin-api",
            "maven-plugin-descriptor",
            "maven-plugin-parameter-documenter",
            "maven-plugin-registry",
            "maven-profile",
            "maven-project",
            "maven-repository-metadata",
            "maven-script",
            "maven-script-ant",
            "maven-script-beanshell",
            "maven-settings" };

        for ( int i = 0; i < s.length; i++ )
        {
            testMavenArtifact( targetRepo, s[i] );
        }

        // leave something behind to clean it up.

        // Test merging

        // Test MD5

        // Test SHA1

        // Test new artifacts are present
    }

    private void testMavenArtifact( File repo, String artifact )
        throws Exception
    {
        File basedir = new File( repo, "org/apache/maven/" + artifact );

        File versionDir = new File( basedir, version );

        assertTrue( versionDir.exists() );

        Reader r = new FileReader( new File( basedir, RepositoryCopier.MAVEN_METADATA) );

        Metadata metadata = reader.read( r );

        // Make sure our new versions has been setup as the release.
        assertEquals( version, metadata.getVersioning().getRelease() );

        assertEquals( "20070327020553", metadata.getVersioning().getLastUpdated() );

        // Make sure we didn't whack old versions.
        List versions = metadata.getVersioning().getVersions();

        assertTrue( versions.contains( "2.0.1" ) );

        assertTrue( versions.contains( "2.0.2" ) );

        assertTrue( versions.contains( "2.0.3" ) );
        
        assertTrue( versions.contains( "2.0.4" ) );

        assertTrue( versions.contains( "2.0.5" ) );

        IOUtil.close( r );
    }
}
