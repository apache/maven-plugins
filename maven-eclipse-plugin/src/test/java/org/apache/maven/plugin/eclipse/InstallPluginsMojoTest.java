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
     * Has both Bundle-Name and Bundle-SymbolicName and should be installed.
     * 
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void testJira_MECLIPSE_418_correct_bundle_headers()
        throws MojoExecutionException, MojoFailureException
    {
        Artifact artifact = createArtifact( "jira.meclipse_418", "correct_bundle_headers", "1" );

        performTestInstall( null, false, artifact, "eclipse-plugin" );

        assertInstalledFileExists( artifact );
        assertInstalledDirDoesNotExist( artifact );

        mm.verifyAll();
    }

    /**
     * Has Bundle-SymbolicName but no Bundle-Name and should be installed.
     * 
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void testJira_MECLIPSE_418_no_bundle_name()
        throws MojoExecutionException, MojoFailureException
    {
        Artifact artifact = createArtifact( "jira.meclipse_418", "no_bundle_name", "1" );

        performTestInstall( null, false, artifact, "eclipse-plugin" );

        assertInstalledFileExists( artifact );
        assertInstalledDirDoesNotExist( artifact );

        mm.verifyAll();
    }

    /**
     * Has Bundle-Name but no Bundle-SymbolicName and should be installed.
     * 
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void testJira_MECLIPSE_418_no_bundle_symbolicname()
        throws MojoExecutionException, MojoFailureException
    {
        Artifact artifact = createArtifact( "jira.meclipse_418", "no_bundle_symbolicname", "1" );

        performTestInstall( null, false, artifact, "eclipse-plugin" );

        assertInstalledFileExists( artifact );
        assertInstalledDirDoesNotExist( artifact );

        mm.verifyAll();
    }

    /**
     * Has neither Bundle-Name or Bundle-SymbolicName and should NOT be installed.
     * 
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void testJira_MECLIPSE_418_no_manifest_headers()
        throws MojoExecutionException, MojoFailureException
    {
        Artifact artifact = createArtifact( "jira.meclipse_418", "no_manifest_headers", "1" );

        performTestInstall( null, false, artifact, "eclipse-plugin" );

        assertInstalledFileDoesNotExist( artifact );
        assertInstalledDirDoesNotExist( artifact );

        mm.verifyAll();
    }

    /**
     * if a jar has no manifest, do not install plugin.
     * 
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void testJira_MECLIPSE_488()
        throws MojoExecutionException, MojoFailureException
    {
        Artifact jira488_missingManifest = createArtifact( "jira", "meclipse_488", "1" );

        performTestInstall( null, false, jira488_missingManifest, "eclipse-plugin" );

        assertInstalledFileDoesNotExist( jira488_missingManifest );
        assertInstalledDirDoesNotExist( jira488_missingManifest );

        mm.verifyAll();
    }

    public void testShouldInstallAsJarWhenPropertyNotSpecified()
        throws MojoExecutionException, MojoFailureException
    {
        performTestInstall( null, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        assertInstalledFileExists( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );
        assertInstalledDirDoesNotExist( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        mm.verifyAll();
    }

    public void testShouldInstallAsJarWhenPropertyIsTrue()
        throws MojoExecutionException, MojoFailureException
    {
        performTestInstall( Boolean.TRUE, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        assertInstalledFileExists( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );
        assertInstalledDirDoesNotExist( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        mm.verifyAll();
    }

    public void testShouldInstallAsDirWhenPropertyIsFalse()
        throws MojoExecutionException, MojoFailureException
    {
        performTestInstall( Boolean.FALSE, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        assertInstalledFileDoesNotExist( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );
        assertInstalledDirExists( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        mm.verifyAll();
    }

    public void testShouldInstallWhenTypeContainedInPluginTypesListWithMultipleValues()
        throws MojoExecutionException, MojoFailureException
    {
        performTestInstall( null, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "osgi-bundle,eclipse-plugin" );

        assertInstalledFileExists( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );
        assertInstalledDirDoesNotExist( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        mm.verifyAll();
    }

    public void testShouldNotInstallWhenTypeNotContainedInPluginTypesList()
        throws MojoExecutionException, MojoFailureException
    {
        performTestInstall( null, false, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "type-not-in-this-list" );

        assertInstalledFileDoesNotExist( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );
        assertInstalledDirDoesNotExist( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        mm.verifyAll();
    }

    public void testShouldRemoveOldDirectoryBeforeInstallingNewJarWhenOverwriteIsFalse()
        throws MojoExecutionException, MojoFailureException
    {
        createPluginsDir();

        File installedDir = locateInstalledDir( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );
        installedDir.mkdir();
        assertInstalledDirExists( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        performTestInstall( Boolean.FALSE, true, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        assertInstalledFileDoesNotExist( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );
        assertInstalledDirExists( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        mm.verifyAll();
    }

    public void testShouldRemoveOldDirectoryBeforeInstallingNewJarWhenOverwriteIsTrue()
        throws MojoExecutionException, MojoFailureException
    {
        createPluginsDir();

        File installedDir = locateInstalledDir( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );
        installedDir.mkdir();
        assertInstalledDirExists( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        performTestInstall( null, true, ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME, "eclipse-plugin" );

        assertInstalledFileExists( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );
        assertInstalledDirDoesNotExist( ARTIFACT_ORG_ECLIPSE_CORE_RUNTIME );

        mm.verifyAll();
    }

    private void assertInstalledFileDoesNotExist( Artifact artifact )
    {
        File installedFile = locateInstalledFile( artifact );

        assertFalse( installedFile + " should not exist.", installedFile.exists() );
    }

    private void assertInstalledFileExists( Artifact artifact )
    {
        File installedFile = locateInstalledFile( artifact );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
    }

    private void assertInstalledDirDoesNotExist( Artifact artifact )
    {
        File installedFile = locateInstalledDir( artifact );

        assertFalse( installedFile + " should not exist.", installedFile.exists() );
    }

    private void assertInstalledDirExists( Artifact artifact )
    {
        File installedFile = locateInstalledDir( artifact );

        assertTrue( installedFile + " should exist.", installedFile.exists() );
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

    private void performTestInstall( Boolean installAsJar, boolean overwrite, Artifact artifact, String typeList )
        throws MojoExecutionException, MojoFailureException
    {
        createPluginsDir();

        String type = artifact.getType();

        ArtifactRepository localRepo = createLocalRepository();
        MavenProjectBuilder projectBuilder = createProjectBuilder( typeList.indexOf( type ) > -1, installAsJar );
        ArchiverManager archiverManager = createArchiverManager( typeList.indexOf( type ) > -1, installAsJar );
        InputHandler inputHandler = createInputHandler();

        Log log = new SystemStreamLog();

        mm.replayAll();

        InstallPluginsMojo mojo =
            new InstallPluginsMojo( eclipseDir, overwrite, Collections.singletonList( artifact ), typeList, localRepo,
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

    /**
     * @param groupId
     * @param artifactId
     * @param version
     * @return an artifact with the specified values and configured for testing to the local m2repo
     */
    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        Artifact artifact =
            new DefaultArtifact( groupId, artifactId, VersionRange.createFromVersion( version ), "scope-unused",
                                 "eclipse-plugin", "classifier-unused", null );
        artifact.setFile( locateArtifact( artifact ) );

        return artifact;
    }

    /**
     * Copied from {@link InstallPluginsMojo#formatEclipsePluginName}
     */
    private String formatEclipsePluginName( Artifact artifact )
    {
        return maven2OsgiConverter.getBundleSymbolicName( artifact ) + "_"
            + maven2OsgiConverter.getVersion( artifact.getVersion() );
    }

    /**
     * @param artifact
     * @return the installed directory for the plugin (because it is an unpacked bundle)
     */
    private File locateInstalledDir( Artifact artifact )
    {
        return new File( pluginsDir, formatEclipsePluginName( artifact ) );
    }

    /**
     * @param artifact
     * @return the installed file for the plugin bundle
     */
    private File locateInstalledFile( Artifact artifact )
    {
        return new File( pluginsDir, formatEclipsePluginName( artifact ) + ".jar" );
    }
}
