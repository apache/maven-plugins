package org.apache.maven.plugin.eclipse;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.tools.easymock.MockManager;
import org.apache.maven.shared.tools.easymock.TestFileManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import junit.framework.Assert;
import junit.framework.TestCase;

public class InstallPluginsMojoTest
    extends TestCase
{

    private static final String GROUP_ID = "org.codehaus.m2eclipse";

    private static final String ARTIFACT_ID = "org.maven.ide.eclipse";

    private static final String VERSION = "0.0.9";

    private static final String SOURCE_PATH = "M2REPO/org/codehaus/m2eclipse/" + ARTIFACT_ID + "/" + VERSION + "/"
        + ARTIFACT_ID + "-" + VERSION + ".jar";

    private File sourceFile;

    private TestFileManager fileManager;

    private MockManager mm = new MockManager();

    private File eclipseDir;

    private File pluginsDir;

    public void testShouldInstallAsJarWhenPropertyNotSpecified()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir = performTestInstall( null, false, "eclipse-plugin", "eclipse-plugin" );

        File installedFile = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION + ".jar" );

        File installedDir = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldInstallAsJarWhenPropertyIsTrue()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir = performTestInstall( Boolean.TRUE, false, "eclipse-plugin", "eclipse-plugin" );

        File installedFile = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION + ".jar" );

        File installedDir = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldInstallAsDirWhenPropertyIsFalse()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir = performTestInstall( Boolean.FALSE, false, "eclipse-plugin", "eclipse-plugin" );

        File installedFile = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION + ".jar" );

        File installedDir = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION );

        assertFalse( installedFile + " should not exist.", installedFile.exists() );
        assertTrue( installedDir + " should exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldInstallWhenTypeContainedInPluginTypesListWithMultipleValues()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir = performTestInstall( null, false, "eclipse-plugin", "osgi-bundle,eclipse-plugin" );

        File installedFile = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION + ".jar" );

        File installedDir = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldNotInstallWhenTypeNotContainedInPluginTypesList()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir = performTestInstall( null, false, "jar", "eclipse-plugin" );

        File installedFile = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION + ".jar" );

        File installedDir = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION );

        assertFalse( installedFile + " should not exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldRemoveOldDirectoryBeforeInstallingNewJarWhenOverwriteIsFalse()
        throws MojoExecutionException, MojoFailureException
    {
        createPluginsDir();

        File installedDir = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION );

        installedDir.mkdir();

        assertTrue( installedDir + " should have been created prior to running the test.", installedDir.exists() );

        performTestInstall( Boolean.FALSE, true, "eclipse-plugin", "eclipse-plugin" );

        File installedFile = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION + ".jar" );

        assertFalse( installedFile + " should not exist.", installedFile.exists() );
        assertTrue( installedDir + " should still exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldRemoveOldDirectoryBeforeInstallingNewJarWhenOverwriteIsTrue()
        throws MojoExecutionException, MojoFailureException
    {
        createPluginsDir();

        File installedDir = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION );

        installedDir.mkdir();

        assertTrue( installedDir + " should have been created prior to running the test.", installedDir.exists() );

        performTestInstall( null, true, "eclipse-plugin", "eclipse-plugin" );

        File installedFile = new File( pluginsDir, ARTIFACT_ID + "_" + VERSION + ".jar" );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void setUp()
    {
        fileManager = new TestFileManager( "InstallPluginsMojo.test.", "" );

        URL resource = Thread.currentThread().getContextClassLoader().getResource( SOURCE_PATH );

        if ( resource == null )
        {
            throw new IllegalStateException( "Cannot find test source jar: " + SOURCE_PATH + " in context classloader!" );
        }

        sourceFile = new File( resource.getPath() );
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    // returns plugins directory
    private File performTestInstall( Boolean installAsJar, boolean overwrite, String type, String typeList )
        throws MojoExecutionException, MojoFailureException
    {
        createPluginsDir();

        Artifact dep = createDependencyArtifact( type );

        ArtifactRepository localRepo = createLocalRepository();
        MavenProjectBuilder projectBuilder = createProjectBuilder( typeList.indexOf( type ) > -1, installAsJar );
        ArchiverManager archiverManager = createArchiverManager( typeList.indexOf( type ) > -1, installAsJar );
        InputHandler inputHandler = createInputHandler();

        Log log = new SystemStreamLog();

        mm.replayAll();

        InstallPluginsMojo mojo = new InstallPluginsMojo( eclipseDir, overwrite, Collections.singletonList( dep ),
                                                          typeList, localRepo, projectBuilder, archiverManager,
                                                          inputHandler, log );

        mojo.execute();

        return pluginsDir;
    }

    private File createPluginsDir()
    {
        if ( eclipseDir == null )
        {
            eclipseDir = fileManager.createTempDir();
        }

        if ( pluginsDir == null )
        {
            pluginsDir = new File( eclipseDir, "plugins" );

            pluginsDir.mkdir();
        }

        return pluginsDir;
    }

    private InputHandler createInputHandler()
    {
        MockControl control = MockControl.createControl( InputHandler.class );

        mm.add( control );

        InputHandler handler = (InputHandler) control.getMock();

        return handler;
    }

    private ArchiverManager createArchiverManager( boolean isReachable, Boolean installAsJar )
    {
        MockControl control = MockControl.createControl( ArchiverManager.class );

        mm.add( control );

        ArchiverManager manager = (ArchiverManager) control.getMock();

        if ( isReachable && installAsJar == Boolean.FALSE )
        {
            try
            {
                manager.getUnArchiver( (File) null );
                control.setMatcher( MockControl.ALWAYS_MATCHER );
                control.setReturnValue( new ZipUnArchiver(), MockControl.ONE_OR_MORE );
            }
            catch ( NoSuchArchiverException e )
            {
                Assert.fail( "Should never happen." );
            }
        }

        return manager;
    }

    private MavenProjectBuilder createProjectBuilder( boolean expectBuildFromRepository, Boolean installAsJar )
    {
        MockControl control = MockControl.createControl( MavenProjectBuilder.class );

        mm.add( control );

        MavenProjectBuilder projectBuilder = (MavenProjectBuilder) control.getMock();

        if ( expectBuildFromRepository )
        {
            try
            {
                Model model = new Model();

                if ( installAsJar != null )
                {
                    model.addProperty( InstallPluginsMojo.PROP_UNPACK_PLUGIN, "" + ( !installAsJar.booleanValue() ) );
                }

                MavenProject project = new MavenProject( model );

                projectBuilder.buildFromRepository( null, null, null, true );
                control.setMatcher( MockControl.ALWAYS_MATCHER );
                control.setReturnValue( project, MockControl.ONE_OR_MORE );
            }
            catch ( ProjectBuildingException e )
            {
                Assert.fail( "should never happen." );
            }
        }

        return projectBuilder;
    }

    private ArtifactRepository createLocalRepository()
    {
        MockControl control = MockControl.createControl( ArtifactRepository.class );

        mm.add( control );

        ArtifactRepository repo = (ArtifactRepository) control.getMock();

        return repo;
    }

    private Artifact createDependencyArtifact( String type )
    {
        MockControl control = MockControl.createControl( Artifact.class );

        mm.add( control );

        Artifact artifact = (Artifact) control.getMock();

        artifact.getFile();
        control.setReturnValue( sourceFile, MockControl.ZERO_OR_MORE );

        artifact.getArtifactId();
        control.setReturnValue( ARTIFACT_ID, MockControl.ZERO_OR_MORE );

        artifact.getVersion();
        control.setReturnValue( VERSION, MockControl.ZERO_OR_MORE );

        artifact.getType();
        control.setReturnValue( type, MockControl.ZERO_OR_MORE );

        artifact.getId();
        control.setReturnValue( GROUP_ID + ":" + ARTIFACT_ID + ":" + type + ":" + VERSION, MockControl.ZERO_OR_MORE );

        return artifact;
    }

}
