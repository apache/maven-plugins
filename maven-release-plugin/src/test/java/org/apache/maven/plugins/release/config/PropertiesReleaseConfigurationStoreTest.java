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

import org.apache.maven.model.Scm;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.io.IOException;

/**
 * Test the properties store.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PropertiesReleaseConfigurationStoreTest
    extends PlexusTestCase
{
    private PropertiesReleaseConfigurationStore store;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        store = (PropertiesReleaseConfigurationStore) lookup( ReleaseConfigurationStore.ROLE, "properties" );
    }

    public void testReadFromFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/release.properties" );

        ReleaseConfiguration config = store.read( file );

        ReleaseConfiguration expected = createExcpectedReleaseConfiguration();

        assertEquals( "check matches", expected, config );
    }

    public void testReadFromFileUsingWorkingDirectory()
        throws ReleaseConfigurationStoreException
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test-classes" ) );
        ReleaseConfiguration config = store.read( releaseConfiguration );

        ReleaseConfiguration expected = createExcpectedReleaseConfiguration();
        expected.setWorkingDirectory( releaseConfiguration.getWorkingDirectory() );

        assertEquals( "check matches", expected, config );
    }

    public void testReadFromEmptyFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/empty-release.properties" );

        ReleaseConfiguration config = store.read( file );

        assertDefaultReleaseConfiguration( config );
    }

    public void testReadMissingFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/no-release.properties" );

        ReleaseConfiguration config = store.read( file );

        assertDefaultReleaseConfiguration( config );
    }

    public void testMergeFromEmptyFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/empty-release.properties" );

        ReleaseConfiguration mergeConfiguration = createMergeConfiguration();
        ReleaseConfiguration config = store.read( mergeConfiguration, file );

        assertEquals( "Check configurations merged", mergeConfiguration, config );
    }

    public void testMergeFromMissingFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/no-release.properties" );

        ReleaseConfiguration mergeConfiguration = createMergeConfiguration();
        ReleaseConfiguration config = store.read( mergeConfiguration, file );

        assertEquals( "Check configurations merged", mergeConfiguration, config );
    }

    public void testWriteToNewFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/new-release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );

        ReleaseConfiguration config = createReleaseConfigurationForWriting();

        store.write( config, file );

        ReleaseConfiguration rereadConfiguration = store.read( file );

        assertEquals( "compare configuration", config, rereadConfiguration );
    }

    public void testWriteToWorkingDirectory()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/new/release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );
        file.getParentFile().mkdirs();

        ReleaseConfiguration config = createReleaseConfigurationForWriting();
        config.setWorkingDirectory( file.getParentFile() );

        store.write( config );

        ReleaseConfiguration rereadConfiguration = store.read( file );
        rereadConfiguration.setWorkingDirectory( file.getParentFile() );

        assertEquals( "compare configuration", config, rereadConfiguration );
    }

    public void testWriteToNewFileRequiredOnly()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/new-release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );

        ReleaseConfiguration config = new ReleaseConfiguration();
        config.setCompletedPhase( "completed-phase-write" );
        config.setUrl( "url-write" );

        store.write( config, file );

        ReleaseConfiguration rereadConfiguration = store.read( file );

        assertEquals( "compare configuration", config, rereadConfiguration );
    }

    public void testWriteToNewFileDottedIds()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/new-release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );

        ReleaseConfiguration config = new ReleaseConfiguration();
        config.setCompletedPhase( "completed-phase-write" );
        config.setUrl( "url-write" );

        config.mapReleaseVersion( "group.id:artifact.id", "1.1" );
        config.mapDevelopmentVersion( "group.id:artifact.id", "1.2-SNAPSHOT" );

        Scm scm = new Scm();
        scm.setConnection( "connection" );
        scm.setDeveloperConnection( "devConnection" );
        scm.setTag( "tag" );
        scm.setUrl( "url" );
        config.mapOriginalScmInfo( "group.id:artifact.id", scm );

        store.write( config, file );

        ReleaseConfiguration rereadConfiguration = store.read( file );

        assertEquals( "compare configuration", config, rereadConfiguration );
    }

    public void testWriteToNewFileNullMappedScm()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/new-release.properties" );
        file.delete();
        assertFalse( "Check file doesn't exist", file.exists() );

        ReleaseConfiguration config = new ReleaseConfiguration();
        config.setCompletedPhase( "completed-phase-write" );
        config.setUrl( "url-write" );

        config.mapReleaseVersion( "group.id:artifact.id", "1.1" );
        config.mapDevelopmentVersion( "group.id:artifact.id", "1.2-SNAPSHOT" );

        config.mapOriginalScmInfo( "group.id:artifact.id", null );

        store.write( config, file );

        ReleaseConfiguration rereadConfiguration = store.read( file );

        assertNull( "check null scm is mapped correctly",
                    rereadConfiguration.getOriginalScmInfo().get( "group.id:artifact.id" ) );

        assertEquals( "compare configuration", config, rereadConfiguration );
    }

    public void testOverwriteFile()
        throws ReleaseConfigurationStoreException
    {
        File file = getTestFile( "target/test-classes/rewrite-release.properties" );
        assertTrue( "Check file already exists", file.exists() );

        ReleaseConfiguration config = createReleaseConfigurationForWriting();

        store.write( config, file );

        ReleaseConfiguration rereadConfiguration = store.read( file );

        assertEquals( "compare configuration", config, rereadConfiguration );
    }

    public void testDeleteFile()
        throws ReleaseConfigurationStoreException, IOException
    {
        File file = getTestFile( "target/test-classes/delete/release.properties" );
        file.getParentFile().mkdirs();
        file.createNewFile();
        assertTrue( "Check file already exists", file.exists() );

        ReleaseConfiguration config = createReleaseConfigurationForWriting();
        config.setWorkingDirectory( file.getParentFile() );

        store.delete( config );

        assertFalse( "Check file already exists", file.exists() );
    }

    public void testMissingDeleteFile()
        throws ReleaseConfigurationStoreException, IOException
    {
        File file = getTestFile( "target/test-classes/delete/release.properties" );
        file.getParentFile().mkdirs();
        file.delete();
        assertFalse( "Check file already exists", file.exists() );

        ReleaseConfiguration config = createReleaseConfigurationForWriting();
        config.setWorkingDirectory( file.getParentFile() );

        store.delete( config );

        assertFalse( "Check file already exists", file.exists() );
    }

    private ReleaseConfiguration createReleaseConfigurationForWriting()
    {
        ReleaseConfiguration config = new ReleaseConfiguration();
        config.setCompletedPhase( "completed-phase-write" );
        config.setUrl( "url-write" );
        config.setUsername( "username-write" );
        config.setPassword( "password-write" );
        config.setPrivateKey( "private-key-write" );
        config.setPassphrase( "passphrase-write" );
        config.setTagBase( "tag-base-write" );
        config.setReleaseLabel( "tag-write" );
        config.setAdditionalArguments( "additional-args-write" );
        config.setPreparationGoals( "preparation-goals-write" );
        config.setPomFileName( "pom-file-name-write" );

        config.mapReleaseVersion( "groupId:artifactId", "1.0" );
        config.mapDevelopmentVersion( "groupId:artifactId", "1.1-SNAPSHOT" );

        Scm scm = new Scm();
        scm.setConnection( "connection-write" );
        scm.setDeveloperConnection( "developerConnection-write" );
        scm.setUrl( "url-write" );
        scm.setTag( "tag-write" );
        config.mapOriginalScmInfo( "groupId:artifactId", scm );

        scm = new Scm();
        scm.setConnection( "connection-write" );
        // omit optional elements
        config.mapOriginalScmInfo( "groupId:subproject1", scm );

        return config;
    }

    private static void assertDefaultReleaseConfiguration( ReleaseConfiguration config )
    {
        assertNull( "Expected no completedPhase", config.getCompletedPhase() );
        assertNull( "Expected no url", config.getUrl() );
        assertNull( "Expected no username", config.getUsername() );
        assertNull( "Expected no password", config.getPassword() );
        assertNull( "Expected no privateKey", config.getPrivateKey() );
        assertNull( "Expected no passphrase", config.getPassphrase() );
        assertNull( "Expected no tagBase", config.getTagBase() );
        assertNull( "Expected no tag", config.getReleaseLabel() );
        assertNull( "Expected no additional arguments", config.getAdditionalArguments() );
        assertNull( "Expected no preparation goals", config.getPreparationGoals() );
        assertNull( "Expected no pom file name", config.getPomFileName() );

        assertNull( "Expected no workingDirectory", config.getWorkingDirectory() );
        assertNull( "Expected no settings", config.getSettings() );

        assertFalse( "Expected no generateReleasePoms", config.isGenerateReleasePoms() );
        assertFalse( "Expected no useEditMode", config.isUseEditMode() );
        assertTrue( "Expected default interactive", config.isInteractive() );
        assertFalse( "Expected no addScema", config.isAddSchema() );

        assertTrue( "Expected no release version mappings", config.getReleaseVersions().isEmpty() );
        assertTrue( "Expected no dev version mappings", config.getDevelopmentVersions().isEmpty() );
        assertTrue( "Expected no scm mappings", config.getOriginalScmInfo().isEmpty() );
    }

    public ReleaseConfiguration createMergeConfiguration()
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();

        releaseConfiguration.setUrl( "scm-url" );
        releaseConfiguration.setUsername( "username" );
        // Not setting other optional SCM settings for brevity
        releaseConfiguration.setWorkingDirectory( getTestFile( "target/test-working-directory" ) );
        // Not setting non-override setting completedPhase

        return releaseConfiguration;
    }

    private ReleaseConfiguration createExcpectedReleaseConfiguration()
    {
        ReleaseConfiguration expected = new ReleaseConfiguration();
        expected.setCompletedPhase( "step1" );
        expected.setUrl( "scm-url" );
        expected.setUsername( "username" );
        expected.setPassword( "password" );
        expected.setPrivateKey( "private-key" );
        expected.setPassphrase( "passphrase" );
        expected.setTagBase( "tagBase" );
        expected.setReleaseLabel( "tag" );
        expected.setAdditionalArguments( "additional-arguments" );
        expected.setPreparationGoals( "preparation-goals" );
        expected.setPomFileName( "pom-file-name" );
        expected.setWorkingDirectory( null );
        expected.setSettings( null );
        expected.setGenerateReleasePoms( false );
        expected.setUseEditMode( false );
        expected.setInteractive( true );
        expected.setAddSchema( false );
        expected.mapReleaseVersion( "groupId:artifactId1", "2.0" );
        expected.mapReleaseVersion( "groupId:artifactId2", "3.0" );
        expected.mapDevelopmentVersion( "groupId:artifactId1", "2.1-SNAPSHOT" );
        expected.mapDevelopmentVersion( "groupId:artifactId2", "3.0.1-SNAPSHOT" );
        Scm scm = new Scm();
        scm.setConnection( "connection" );
        scm.setDeveloperConnection( "developerConnection" );
        scm.setUrl( "url" );
        scm.setTag( "tag" );
        expected.mapOriginalScmInfo( "groupId:artifactId1", scm );
        scm = new Scm();
        scm.setConnection( "connection2" );
        scm.setUrl( "url2" );
        scm.setTag( null );
        scm.setDeveloperConnection( null );
        expected.mapOriginalScmInfo( "groupId:artifactId2", scm );
        return expected;
    }

}
