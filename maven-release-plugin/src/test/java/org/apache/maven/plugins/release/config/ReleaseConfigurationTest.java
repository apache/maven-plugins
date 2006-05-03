package org.apache.maven.plugins.release.config;

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

import junit.framework.TestCase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * ReleaseConfiguration Tester.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReleaseConfigurationTest
    extends TestCase
{
    public void testMergeConfigurationSourceEmpty()
    {
        ReleaseConfiguration mergeConfiguration = createReleaseConfiguration();

        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.merge( mergeConfiguration );

        assertEquals( "Check merge", mergeConfiguration, releaseConfiguration );
    }

    public void testMergeConfigurationDestEmpty()
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();

        releaseConfiguration.merge( new ReleaseConfiguration() );

        ReleaseConfiguration expectedConfiguration = copyReleaseConfiguration( releaseConfiguration );
        assertEquals( "Check merge", expectedConfiguration, releaseConfiguration );
    }

    public void testMergeConfiguration()
    {
        Settings settings = new Settings();
        File workingDirectory = new File( "." );

        ReleaseConfiguration mergeConfiguration =
            createMergeConfiguration( settings, workingDirectory, "completed-phase-merge" );

        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();
        releaseConfiguration.merge( mergeConfiguration );

        ReleaseConfiguration expectedConfiguration = createMergeConfiguration( releaseConfiguration.getSettings(),
                                                                               releaseConfiguration.getWorkingDirectory(),
                                                                               releaseConfiguration.getCompletedPhase() );
        expectedConfiguration.setReactorProjects( releaseConfiguration.getReactorProjects() );
        assertEquals( "Check merge", expectedConfiguration, releaseConfiguration );
    }

    public void testEquals()
    {
        ReleaseConfiguration originalReleaseConfiguration = createReleaseConfiguration();
        ReleaseConfiguration releaseConfiguration = copyReleaseConfiguration( originalReleaseConfiguration );
        doEqualsAssertions( releaseConfiguration, originalReleaseConfiguration, "other", Collections.EMPTY_LIST,
                            new File( "f" ), new Settings() );
        originalReleaseConfiguration = createReleaseConfiguration();
        releaseConfiguration = copyReleaseConfiguration( originalReleaseConfiguration );
        doEqualsAssertions( originalReleaseConfiguration, releaseConfiguration, "other", Collections.EMPTY_LIST,
                            new File( "f" ), new Settings() );

        originalReleaseConfiguration = createReleaseConfiguration();
        releaseConfiguration = copyReleaseConfiguration( originalReleaseConfiguration );
        doEqualsAssertions( releaseConfiguration, originalReleaseConfiguration, null, null, null, null );
        originalReleaseConfiguration = createReleaseConfiguration();
        releaseConfiguration = copyReleaseConfiguration( originalReleaseConfiguration );
        doEqualsAssertions( originalReleaseConfiguration, releaseConfiguration, null, null, null, null );

        assertEquals( "test ==", releaseConfiguration, releaseConfiguration );
        Object obj = this;
        assertFalse( "test class instance", releaseConfiguration.equals( obj ) );
    }

    private static void doEqualsAssertions( ReleaseConfiguration releaseConfiguration,
                                            ReleaseConfiguration originalReleaseConfiguration, String other,
                                            List reactorProjects, File otherFile, Settings otherSettings )
    {
        ReleaseConfiguration origConfig = originalReleaseConfiguration;
        ReleaseConfiguration config = releaseConfiguration;
        assertEquals( "Check original comparison", config, origConfig );

        config.setUrl( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setUrl( origConfig.getUrl() );

        config.setAdditionalArguments( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setAdditionalArguments( origConfig.getAdditionalArguments() );

        config.setAddSchema( !origConfig.isAddSchema() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setAddSchema( origConfig.isAddSchema() );

        config.setGenerateReleasePoms( !origConfig.isAddSchema() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setGenerateReleasePoms( origConfig.isGenerateReleasePoms() );

        config.setUseEditMode( !origConfig.isUseEditMode() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setUseEditMode( origConfig.isUseEditMode() );

        config.setInteractive( !origConfig.isInteractive() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setInteractive( origConfig.isInteractive() );

        config.setCompletedPhase( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setCompletedPhase( origConfig.getCompletedPhase() );

        config.setPassphrase( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setPassphrase( origConfig.getPassphrase() );

        config.setPassword( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setPassword( origConfig.getPassword() );

        config.setUsername( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setUsername( origConfig.getUsername() );

        config.setPrivateKey( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setPrivateKey( origConfig.getPrivateKey() );

        config.setPomFileName( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setPomFileName( origConfig.getPomFileName() );

        config.setPreparationGoals( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setPreparationGoals( origConfig.getPreparationGoals() );

        config.setReactorProjects( reactorProjects );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setReactorProjects( origConfig.getReactorProjects() );

        config.setSettings( otherSettings );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setSettings( origConfig.getSettings() );

        config.setReleaseLabel( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setReleaseLabel( origConfig.getReleaseLabel() );

        config.setTagBase( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setTagBase( origConfig.getTagBase() );

        config.setWorkingDirectory( otherFile );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setWorkingDirectory( origConfig.getWorkingDirectory() );

        // sanity check the test was resetting correctly
        assertEquals( "Check original comparison", config, origConfig );

        config.mapDevelopmentVersion( "groupId:artifactId", "1.0-SNAPSHOT" );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config = copyReleaseConfiguration( origConfig );

        config.mapReleaseVersion( "groupId:artifactId", "1.0" );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config = copyReleaseConfiguration( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", new Scm() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config = copyReleaseConfiguration( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", new Scm() );
        origConfig.mapOriginalScmInfo( "foo", new Scm() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        origConfig = createReleaseConfiguration();
        config = copyReleaseConfiguration( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", new Scm() );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", new Scm() );
        assertEquals( "Check original comparison", config, origConfig );
        origConfig = createReleaseConfiguration();
        config = copyReleaseConfiguration( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertEquals( "Check original comparison", config, origConfig );
        origConfig = createReleaseConfiguration();
        config = copyReleaseConfiguration( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "-", "dev", "url", "tag" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        origConfig = createReleaseConfiguration();
        config = copyReleaseConfiguration( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "-", "url", "tag" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        origConfig = createReleaseConfiguration();
        config = copyReleaseConfiguration( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "-", "tag" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        origConfig = createReleaseConfiguration();
        config = copyReleaseConfiguration( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "-" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
    }

    public void testHashCode()
    {
        ReleaseConfiguration releaseConfiguration = createReleaseConfiguration();
        assertEquals( "Check hash code", releaseConfiguration.hashCode(), createReleaseConfiguration(
            releaseConfiguration.getSettings(), releaseConfiguration.getWorkingDirectory(),
            releaseConfiguration.getReactorProjects() ).hashCode() );
    }

    private static ReleaseConfiguration copyReleaseConfiguration( ReleaseConfiguration originalReleaseConfiguration )
    {
        return createReleaseConfiguration( originalReleaseConfiguration.getSettings(),
                                           originalReleaseConfiguration.getWorkingDirectory(),
                                           originalReleaseConfiguration.getReactorProjects() );
    }

    private static Scm getScm( String connection, String developerConnection, String url, String tag )
    {
        Scm scm = new Scm();
        scm.setConnection( connection );
        scm.setDeveloperConnection( developerConnection );
        scm.setTag( tag );
        scm.setUrl( url );
        return scm;
    }

    private static ReleaseConfiguration createMergeConfiguration( Settings settings, File workingDirectory,
                                                                  String completedPhase )
    {
        ReleaseConfiguration mergeConfiguration = new ReleaseConfiguration();
        mergeConfiguration.setUrl( "scm-url-merge" );
        mergeConfiguration.setCompletedPhase( completedPhase );
        mergeConfiguration.setPassphrase( "passphrase-merge" );
        mergeConfiguration.setPassword( "password-merge" );
        mergeConfiguration.setPrivateKey( "private-key-merge" );
        mergeConfiguration.setSettings( settings );
        mergeConfiguration.setTagBase( "tag-base-merge" );
        mergeConfiguration.setReleaseLabel( "tag-merge" );
        mergeConfiguration.setUsername( "username-merge" );
        mergeConfiguration.setAdditionalArguments( "additional-arguments-merge" );
        mergeConfiguration.setPomFileName( "pom-file-name-merge" );
        mergeConfiguration.setPreparationGoals( "preparation-goals-merge" );
        mergeConfiguration.setWorkingDirectory( workingDirectory );
        return mergeConfiguration;
    }

    private static ReleaseConfiguration createReleaseConfiguration()
    {
        Settings settings = new Settings();
        File workingDirectory = new File( "." );
        MavenProject project = new MavenProject( new Model() );

        return createReleaseConfiguration( settings, workingDirectory, Collections.singletonList( project ) );
    }

    private static ReleaseConfiguration createReleaseConfiguration( Settings settings, File workingDirectory,
                                                                    List list )
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setUrl( "scm-url" );
        releaseConfiguration.setCompletedPhase( "completed-phase" );
        releaseConfiguration.setPassphrase( "passphrase" );
        releaseConfiguration.setPassword( "password" );
        releaseConfiguration.setPrivateKey( "private-key" );
        releaseConfiguration.setSettings( settings );
        releaseConfiguration.setTagBase( "tag-base" );
        releaseConfiguration.setReleaseLabel( "tag" );
        releaseConfiguration.setUsername( "username" );
        releaseConfiguration.setWorkingDirectory( workingDirectory );
        releaseConfiguration.setAdditionalArguments( "additional-arguments" );
        releaseConfiguration.setPomFileName( "pom-file-name" );
        releaseConfiguration.setPreparationGoals( "preparation-goals" );
        releaseConfiguration.setReactorProjects( list );
        return releaseConfiguration;
    }
}
