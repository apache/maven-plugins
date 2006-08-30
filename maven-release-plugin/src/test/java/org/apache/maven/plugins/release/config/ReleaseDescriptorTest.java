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
import org.apache.maven.model.Scm;

import java.io.File;

/**
 * ReleaseDescriptor Tester.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReleaseDescriptorTest
    extends TestCase
{
    public void testMergeConfigurationSourceEmpty()
    {
        ReleaseDescriptor mergeDescriptor = createReleaseDescriptor();
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        ReleaseDescriptor mergedReleaseDescriptor = ReleaseUtils.merge( releaseDescriptor, mergeDescriptor );
        ReleaseDescriptor mergedMergeDescriptor = ReleaseUtils.merge( mergeDescriptor, releaseDescriptor );

        assertEquals( "Check merge", mergedReleaseDescriptor, mergedMergeDescriptor );
    }

    public void testMergeConfigurationDestEmpty()
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();
        ReleaseDescriptor mergedReleaseDescriptor = ReleaseUtils.merge( releaseDescriptor, new ReleaseDescriptor() );
        ReleaseDescriptor expectedDescriptor = copyReleaseDescriptor( mergedReleaseDescriptor );

        assertEquals( "Check merge", expectedDescriptor, releaseDescriptor );
    }

    public void testMergeConfiguration()
    {
/*
        File workingDirectory = new File( "." );

        ReleaseDescriptor mergeDescriptor =
            createMergeDescriptor( workingDirectory.getAbsolutePath(), "completed-phase-merge" );
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();
        ReleaseDescriptor mergedReleaseDescriptor = ReleaseUtils.merge( releaseDescriptor, mergeDescriptor );
        ReleaseDescriptor expectedDescriptor =
            createMergeDescriptor( releaseDescriptor.getWorkingDirectory(), releaseDescriptor.getCompletedPhase() );
        ReleaseDescriptor mergedExpectedDescriptor = ReleaseUtils.merge( expectedDescriptor, releaseDescriptor );

        assertEquals( "Check merge", mergedExpectedDescriptor, mergedReleaseDescriptor );
*/

        File workingDirectory = new File( "." );

        ReleaseDescriptor mergeDescriptor =
            createMergeDescriptor( workingDirectory.getAbsolutePath(), "completed-phase-merge" );

        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();
        releaseDescriptor = ReleaseUtils.merge( releaseDescriptor, mergeDescriptor );

        ReleaseDescriptor expected =
            createMergeDescriptor( releaseDescriptor.getWorkingDirectory(), releaseDescriptor.getCompletedPhase() );
        assertEquals( "Check merge", expected, releaseDescriptor );
    }

    public void testEquals()
    {
        ReleaseDescriptor originalReleaseDescriptor = createReleaseDescriptor();
        ReleaseDescriptor releaseDescriptor = copyReleaseDescriptor( originalReleaseDescriptor );
        doEqualsAssertions( releaseDescriptor, originalReleaseDescriptor, "other", new File( "f" ) );
        originalReleaseDescriptor = createReleaseDescriptor();
        releaseDescriptor = copyReleaseDescriptor( originalReleaseDescriptor );
        doEqualsAssertions( originalReleaseDescriptor, releaseDescriptor, "other", new File( "f" ) );

        originalReleaseDescriptor = createReleaseDescriptor();
        releaseDescriptor = copyReleaseDescriptor( originalReleaseDescriptor );
        doEqualsAssertions( releaseDescriptor, originalReleaseDescriptor, null, null );
        originalReleaseDescriptor = createReleaseDescriptor();
        releaseDescriptor = copyReleaseDescriptor( originalReleaseDescriptor );
        doEqualsAssertions( originalReleaseDescriptor, releaseDescriptor, null, null );

        assertEquals( "test ==", releaseDescriptor, releaseDescriptor );
        Object obj = this;
        assertFalse( "test class instance", releaseDescriptor.equals( obj ) );
    }

    private static void doEqualsAssertions( ReleaseDescriptor releaseDescriptor,
                                            ReleaseDescriptor originalReleaseDescriptor, String other, File otherFile )
    {
        ReleaseDescriptor origConfig = originalReleaseDescriptor;
        ReleaseDescriptor config = releaseDescriptor;
        assertEquals( "Check original comparison", config, origConfig );

        config.setScmSourceUrl( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setScmSourceUrl( origConfig.getScmSourceUrl() );

        config.setAdditionalArguments( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setAdditionalArguments( origConfig.getAdditionalArguments() );

        config.setAddSchema( !origConfig.isAddSchema() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setAddSchema( origConfig.isAddSchema() );

        config.setGenerateReleasePoms( !origConfig.isAddSchema() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setGenerateReleasePoms( origConfig.isGenerateReleasePoms() );

        config.setScmUseEditMode( !origConfig.isScmUseEditMode() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setScmUseEditMode( origConfig.isScmUseEditMode() );

        config.setInteractive( !origConfig.isInteractive() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setInteractive( origConfig.isInteractive() );

        config.setCompletedPhase( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setCompletedPhase( origConfig.getCompletedPhase() );

        config.setScmPrivateKeyPassPhrase( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setScmPrivateKeyPassPhrase( origConfig.getScmPrivateKeyPassPhrase() );

        config.setScmPassword( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setScmPassword( origConfig.getScmPassword() );

        config.setScmUsername( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setScmUsername( origConfig.getScmUsername() );

        config.setScmPrivateKey( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setScmPrivateKey( origConfig.getScmPrivateKey() );

        config.setPomFileName( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setPomFileName( origConfig.getPomFileName() );

        config.setPreparationGoals( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setPreparationGoals( origConfig.getPreparationGoals() );

        config.setScmReleaseLabel( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setScmReleaseLabel( origConfig.getScmReleaseLabel() );

        config.setScmTagBase( other );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config.setScmTagBase( origConfig.getScmTagBase() );

        if ( otherFile != null )
        {
            config.setWorkingDirectory( otherFile.getAbsolutePath() );
            assertFalse( "Check original comparison", config.equals( origConfig ) );
        }

        config.setWorkingDirectory( origConfig.getWorkingDirectory() );

        // sanity check the test was resetting correctly
        assertEquals( "Check original comparison", config, origConfig );

        config.mapDevelopmentVersion( "groupId:artifactId", "1.0-SNAPSHOT" );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config = copyReleaseDescriptor( origConfig );

        config.mapReleaseVersion( "groupId:artifactId", "1.0" );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config = copyReleaseDescriptor( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", new Scm() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        config = copyReleaseDescriptor( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", new Scm() );
        origConfig.mapOriginalScmInfo( "foo", new Scm() );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        origConfig = createReleaseDescriptor();
        config = copyReleaseDescriptor( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", new Scm() );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", new Scm() );
        assertEquals( "Check original comparison", config, origConfig );
        origConfig = createReleaseDescriptor();
        config = copyReleaseDescriptor( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertEquals( "Check original comparison", config, origConfig );
        origConfig = createReleaseDescriptor();
        config = copyReleaseDescriptor( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "-", "dev", "url", "tag" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        origConfig = createReleaseDescriptor();
        config = copyReleaseDescriptor( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "-", "url", "tag" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        origConfig = createReleaseDescriptor();
        config = copyReleaseDescriptor( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "-", "tag" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
        origConfig = createReleaseDescriptor();
        config = copyReleaseDescriptor( origConfig );

        config.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "-" ) );
        origConfig.mapOriginalScmInfo( "groupId:artifactId", getScm( "conn", "dev", "url", "tag" ) );
        assertFalse( "Check original comparison", config.equals( origConfig ) );
    }

    public void testHashCode()
    {
        ReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

        assertEquals( "Check hash code", releaseDescriptor.hashCode(),
                      createReleaseDescriptor( releaseDescriptor.getWorkingDirectory() ).hashCode() );
    }

    private static ReleaseDescriptor copyReleaseDescriptor( ReleaseDescriptor originalReleaseDescriptor )
    {
        return createReleaseDescriptor( originalReleaseDescriptor.getWorkingDirectory() );
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

    private static ReleaseDescriptor createMergeDescriptor( String workingDirectory, String completedPhase )
    {
        ReleaseDescriptor mergeDescriptor = new ReleaseDescriptor();
        mergeDescriptor.setScmSourceUrl( "scm-url-merge" );
        mergeDescriptor.setCompletedPhase( completedPhase );
        mergeDescriptor.setScmPrivateKeyPassPhrase( "passphrase-merge" );
        mergeDescriptor.setScmPassword( "password-merge" );
        mergeDescriptor.setScmPrivateKey( "private-key-merge" );
        mergeDescriptor.setScmTagBase( "tag-base-merge" );
        mergeDescriptor.setScmReleaseLabel( "tag-merge" );
        mergeDescriptor.setScmUsername( "username-merge" );
        mergeDescriptor.setAdditionalArguments( "additional-arguments-merge" );
        mergeDescriptor.setPomFileName( "pom-file-name-merge" );
        mergeDescriptor.setPreparationGoals( "preparation-goals-merge" );
        mergeDescriptor.setWorkingDirectory( workingDirectory );
        return mergeDescriptor;
    }

    private static ReleaseDescriptor createReleaseDescriptor()
    {
        File workingDirectory = new File( "." );

        return createReleaseDescriptor( workingDirectory.getAbsolutePath() );
    }

    private static ReleaseDescriptor createReleaseDescriptor( String workingDirectory )
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setCompletedPhase( "completed-phase" );
        releaseDescriptor.setScmPrivateKeyPassPhrase( "passphrase" );
        releaseDescriptor.setScmPassword( "password" );
        releaseDescriptor.setScmPrivateKey( "private-key" );
        releaseDescriptor.setScmTagBase( "tag-base" );
        releaseDescriptor.setScmReleaseLabel( "tag" );
        releaseDescriptor.setScmUsername( "username" );
        releaseDescriptor.setWorkingDirectory( workingDirectory );
        releaseDescriptor.setAdditionalArguments( "additional-arguments" );
        releaseDescriptor.setPomFileName( "pom-file-name" );
        releaseDescriptor.setPreparationGoals( "preparation-goals" );

        return releaseDescriptor;
    }
}
