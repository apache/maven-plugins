/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.osgi.DefaultMaven2OsgiConverter;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;
import org.apache.maven.shared.tools.easymock.MockManager;
import org.apache.maven.shared.tools.easymock.TestFileManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

public class InstallPluginsMojoTest
    extends TestCase
{
    private static final String TEST_M2_REPO = "m2repo/eclipseinstall/";

    private static Artifact ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME;

    private TestFileManager fileManager;

    private MockManager mm = new MockManager();

    private File eclipseDir;

    private File pluginsDir;

    private Maven2OsgiConverter maven2OsgiConverter = new DefaultMaven2OsgiConverter();

    /**
     * Copied from {@link InstallPluginsMojo#formatEclipsePluginName}
     */
    private String formatEclipsePluginName( Artifact artifact )
    {
        return maven2OsgiConverter.getBundleSymbolicName( artifact ) + "_"
            + maven2OsgiConverter.getVersion( artifact.getVersion() );
    }

    private File locateInstalledDir( File pluginsDir, Artifact artifact )
    {
        return new File( pluginsDir, formatEclipsePluginName( artifact ) );
    }

    private File locateInstalledFile( File pluginsDir, Artifact artifact )
    {
        return new File( pluginsDir, formatEclipsePluginName( artifact ) + ".jar" );
    }

    public void testJira488()
        throws MojoExecutionException, MojoFailureException
    {
        Artifact jira488_missingManifest =
            createArtifact("jira", "meclipse", "488");

        File pluginsDir = performTestInstall( null, false, jira488_missingManifest, "eclipse-plugin" );

        File installedFile = locateInstalledFile( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        File installedDir = locateInstalledDir( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        assertFalse( installedFile + " should not exist as Jar has not Manifest.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    private Artifact createArtifact(String groupId, String artifactId, String version)
    {
        Artifact artifact = new DefaultArtifact( groupId, artifactId, VersionRange.createFromVersion( version ), "scope-unused",
                             "eclipse-plugin", "classifier-unused", null );
        artifact.setFile( locateArtifact( artifact ) );

        return artifact;
    }

    public void testShouldInstallAsJarWhenPropertyNotSpecified()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir = performTestInstall( null, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        File installedFile = locateInstalledFile( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        File installedDir = locateInstalledDir( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldInstallAsJarWhenPropertyIsTrue()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir = performTestInstall( Boolean.TRUE, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        File installedFile = locateInstalledFile( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        File installedDir = locateInstalledDir( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldInstallAsDirWhenPropertyIsFalse()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir =
            performTestInstall( Boolean.FALSE, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        File installedFile = locateInstalledFile( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        File installedDir = locateInstalledDir( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        assertFalse( installedFile + " should not exist.", installedFile.exists() );
        assertTrue( installedDir + " should exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldInstallWhenTypeContainedInPluginTypesListWithMultipleValues()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir =
            performTestInstall( null, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "osgi-bundle,eclipse-plugin" );

        File installedFile = locateInstalledFile( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        File installedDir = locateInstalledDir( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldNotInstallWhenTypeNotContainedInPluginTypesList()
        throws MojoExecutionException, MojoFailureException
    {
        File pluginsDir = performTestInstall( null, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "type-not-in-this-list" );

        File installedFile = locateInstalledFile( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        File installedDir = locateInstalledDir( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        assertFalse( installedFile + " should not exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldRemoveOldDirectoryBeforeInstallingNewJarWhenOverwriteIsFalse()
        throws MojoExecutionException, MojoFailureException
    {
        createPluginsDir();

        File installedDir = locateInstalledDir( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        installedDir.mkdir();

        assertTrue( installedDir + " should have been created prior to running the test.", installedDir.exists() );

        performTestInstall( Boolean.FALSE, true, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        File installedFile = locateInstalledFile( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        assertFalse( installedFile + " should not exist.", installedFile.exists() );
        assertTrue( installedDir + " should still exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void testShouldRemoveOldDirectoryBeforeInstallingNewJarWhenOverwriteIsTrue()
        throws MojoExecutionException, MojoFailureException
    {
        createPluginsDir();

        File installedDir = locateInstalledDir( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        installedDir.mkdir();

        assertTrue( installedDir + " should have been created prior to running the test.", installedDir.exists() );

        performTestInstall( null, true, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        File installedFile = locateInstalledFile( pluginsDir, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
        assertFalse( installedDir + " should not exist.", installedDir.exists() );

        mm.verifyAll();
    }

    public void setUp()
    {
        fileManager = new TestFileManager( "InstallPluginsMojo.test.", "" );

        ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME = createArtifact( "org.eclipse.core", "runtime", "3.2.0-v20060603" );

    }

    private File locateArtifact( Artifact artifact )
    {
        URL resource = null;

        String sourcepath =
            artifact.getGroupId().replace( '.', '/' ) + "/" + artifact.getArtifactId() + "/" + artifact.getVersion()
                + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";

        resource = Thread.currentThread().getContextClassLoader().getResource( TEST_M2_REPO + sourcepath );

        if ( resource == null )
        {
            throw new IllegalStateException( "Cannot find test source jar: " + TEST_M2_REPO + sourcepath
                + " in context classloader!" );
        }
        return new File( resource.getPath() );
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    // returns plugins directory
    private File performTestInstall( Boolean installAsJar, boolean overwrite, Artifact artifact, String typeList )
        throws MojoExecutionException, MojoFailureException
    {
        createPluginsDir();

        String type = artifact.getType();
        Artifact dep = createDependencyArtifact( artifact );

        ArtifactRepository localRepo = createLocalRepository();
        MavenProjectBuilder projectBuilder = createProjectBuilder( typeList.indexOf( type ) > -1, installAsJar );
        ArchiverManager archiverManager = createArchiverManager( typeList.indexOf( type ) > -1, installAsJar );
        InputHandler inputHandler = createInputHandler();

        Log log = new SystemStreamLog();

        mm.replayAll();

        InstallPluginsMojo mojo =
            new InstallPluginsMojo( eclipseDir, overwrite, Collections.singletonList( dep ), typeList, localRepo,
                                    projectBuilder, archiverManager, inputHandler, log );
        try
        {
            Field field = InstallPluginsMojo.class.getDeclaredField( "maven2OsgiConverter" );
            field.setAccessible( true );
            field.set( mojo, new DefaultMaven2OsgiConverter() );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to configure maven2OsgiConverter", e );
        }

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
                ZipUnArchiver zipUnArchiver = new ZipUnArchiver();
                zipUnArchiver.enableLogging( new ConsoleLogger( org.codehaus.plexus.logging.Logger.LEVEL_INFO,
                                                                "console" ) );
                control.setReturnValue( zipUnArchiver, MockControl.ONE_OR_MORE );
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

    private Artifact createDependencyArtifact( Artifact artifactToCopy )
    {
        MockControl control = MockControl.createControl( Artifact.class );

        mm.add( control );

        Artifact artifact = (Artifact) control.getMock();

        artifact.getFile();
        control.setReturnValue( artifactToCopy.getFile(), MockControl.ZERO_OR_MORE );

        artifact.getArtifactId();
        control.setReturnValue( artifactToCopy.getArtifactId(), MockControl.ZERO_OR_MORE );

        artifact.getVersion();
        control.setReturnValue( artifactToCopy.getVersion(), MockControl.ZERO_OR_MORE );

        artifact.getType();
        control.setReturnValue( artifactToCopy.getType(), MockControl.ZERO_OR_MORE );

        artifact.getId();
        control.setReturnValue( artifactToCopy.getGroupId() + ":" + artifactToCopy.getArtifactId() + ":"
            + artifactToCopy.getType() + ":" + artifactToCopy.getVersion(), MockControl.ZERO_OR_MORE );

        return artifact;
    }

}
