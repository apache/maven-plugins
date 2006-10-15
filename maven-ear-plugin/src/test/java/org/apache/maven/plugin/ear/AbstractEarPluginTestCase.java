package org.apache.maven.plugin.ear;

import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Stephane Nicoll <stephane.nicoll@bsb.com>
 * @author $Author: sni $ (last edit)
 * @version $Revision: 1.5 $
 */
public abstract class AbstractEarPluginTestCase
    extends PlexusTestCase
{

    protected final String FINAL_NAME_PREFIX = "maven-ear-plugin-test-";

    protected final String FINAL_NAME_SUFFIX = "-99.0";

    /**
     * The embedder.
     */
    protected MavenEmbedder maven;

    /**
     * Test repository directory.
     */
    protected File localRepositoryDir = getTestFile( "target/test-classes/m2repo" );

    /**
     * @see org.codehaus.plexus.PlexusTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        this.maven = new MavenEmbedder();
        this.maven.setClassLoader( Thread.currentThread().getContextClassLoader() );
        this.maven.setLogger( new MavenEmbedderConsoleLogger() );
        this.maven.setLocalRepositoryDirectory( localRepositoryDir );
        this.maven.setOffline( true );
        this.maven.start();

        super.setUp();
    }

    /**
     * @see org.codehaus.plexus.PlexusTestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        maven.stop();
        super.tearDown();
    }

    /**
     * Execute the EAR plugin for the specified project.
     *
     * @param projectName the name of the project
     * @param properties  extra properties to be used by the embedder
     * @return the base directory of the project
     * @throws Exception if an error occured
     */
    protected File executeMojo( final String projectName, final Properties properties )
        throws Exception
    {
        File basedir = getTestFile( "target/test-classes/projects/" + projectName );

        MavenProject project = maven.readProjectWithDependencies( new File( basedir, "pom.xml" ) );

        EventMonitor eventMonitor =
            new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );

        this.maven.execute( project, Arrays.asList( new String[]{"org.apache.maven.plugins:maven-clean-plugin:clean",
            "org.apache.maven.plugins:maven-ear-plugin:generate-application-xml",
            "org.apache.maven.plugins:maven-ear-plugin:ear"} ), eventMonitor, new ConsoleDownloadMonitor(), properties,
                                                                basedir );

        assertEarArchive( basedir, projectName );
        assertEarDirectory( basedir, projectName );

        return basedir;
    }


    /**
     * Executes the specified projects and asserts the given artifacts.
     *
     * @param projectName        the project to test
     * @param expectedArtifacts  the list of artifacts to be found in the EAR archive
     * @param artifactsDirectory whether the artifact is an exploded artifactsDirectory or not
     * @throws Exception
     */
    protected void doTestProject( final String projectName, final String[] expectedArtifacts,
                                  final boolean[] artifactsDirectory )
        throws Exception
    {
        final File baseDir = executeMojo( projectName, new Properties() );

        assertArchiveContent( baseDir, projectName, expectedArtifacts, artifactsDirectory );

    }

    /**
     * Executes the specified projects and asserts the given artifacts as
     * artifacts (non directory)
     *
     * @param projectName       the project to test
     * @param expectedArtifacts the list of artifacts to be found in the EAR archive
     * @throws Exception
     */
    protected void doTestProject( final String projectName, final String[] expectedArtifacts )
        throws Exception
    {
        doTestProject( projectName, expectedArtifacts, new boolean[expectedArtifacts.length] );
    }

    protected void assertEarArchive( final File baseDir, final String projectName )
    {
        assertTrue( "EAR archive does not exist", getEarArchive( baseDir, projectName ).exists() );
    }

    protected void assertEarDirectory( final File baseDir, final String projectName )
    {
        assertTrue( "EAR archive directory does not exist", getEarDirectory( baseDir, projectName ).exists() );
    }

    protected File getTargetDirectory( final File basedir )
    {
        return new File( basedir, "target" );
    }

    protected File getEarArchive( final File baseDir, final String projectName )
    {
        return new File( getTargetDirectory( baseDir ), buildFinalName( projectName ) + ".ear" );
    }

    protected File getEarDirectory( final File baseDir, final String projectName )
    {
        return new File( getTargetDirectory( baseDir ), buildFinalName( projectName ) );
    }

    protected String buildFinalName( final String projectName )
    {
        return FINAL_NAME_PREFIX + projectName + FINAL_NAME_SUFFIX;
    }

    protected void assertArchiveContent( final File baseDir, final String projectName, final String[] artifactNames,
                                         final boolean[] artifactsDirectory )
    {
        // sanity check
        assertEquals( "Wrong parameter, artifacts mist match directory flag", artifactNames.length,
                      artifactsDirectory.length );

        File dir = getEarDirectory( baseDir, projectName );
        final List actualFiles = buildFiles( dir );
        assertEquals( "Artifacts mismatch " + actualFiles, artifactNames.length, actualFiles.size() );
        for ( int i = 0; i < artifactNames.length; i++ )
        {
            String artifactName = artifactNames[i];
            final boolean isDirectory = artifactsDirectory[i];
            File expectedFile = new File( dir, artifactName );

            assertEquals( "Artifact[" + artifactName + "] not in the right form (exploded/archive", isDirectory,
                          expectedFile.isDirectory() );
            assertTrue( "Artifact[" + artifactName + "] not found in ear archive",
                        actualFiles.contains( expectedFile ) );

        }
    }

    protected List buildFiles( final File baseDir )
    {
        final List result = new ArrayList();
        addFiles( baseDir, result );

        return result;
    }

    private void addFiles( final File directory, final List files )
    {
        File[] result = directory.listFiles( new FilenameFilter()
        {
            public boolean accept( File dir, String name )
            {
                if ( name.equals( "META-INF" ) )
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }

        } );

        for ( int i = 0; i < result.length; i++ )
        {
            File file = result[i];
            files.add( file );
            /*
             Here's we can introduce a more complex
             file filtering if necessary
             */
        }
    }
}
