package org.apache.maven.plugin.source;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Enumeration;

import java.util.zip.ZipEntry;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.util.ResourceExtractor;
import org.codehaus.plexus.archiver.zip.ZipFile;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractSourcePluginTestCase
    extends TestCase
{

    protected final String FINAL_NAME_PREFIX = "maven-source-plugin-test-";

    protected final String FINAL_NAME_SUFFIX = "-99.0";

    /**
     * The base directory.
     */
    private File basedir;

    /**
     * Execute the souce plugin for the specified project.
     *
     * @param projectName   the name of the project
     * @param properties    extra properties to be used by the embedder
     * @param expectNoError whether an exception is expected or not
     * @return the base directory of the project
     * @throws Exception if an error occured
     */
    protected File executeMojo( final String projectName, final Properties properties, boolean expectNoError )
        throws Exception
    {
        File testDir = getTestDir( projectName );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        // Turn On debug logs
        //verifier.getCliOptions().add( "-X" );

        // On linux and macOSX, an exception is thrown if a build failure occurs underneath
        try
        {
            verifier.executeGoal( "package" );
        }
        catch ( VerificationException e )
        {
            //@TODO needs to be handled nicely in the verifier
            if ( expectNoError || e.getMessage().indexOf( "Exit code was non-zero" ) == -1 )
            {
                throw e;
            }
        }

        // If no error is expected make sure that error logs are free
        if ( expectNoError )
        {
            verifier.verifyErrorFreeLog();
        }
        verifier.resetStreams();
        return testDir;
    }


    /**
     * Execute the source plugin for the specified project.
     *
     * @param projectName the name of the project
     * @param properties  extra properties to be used by the embedder
     * @return the base directory of the project
     * @throws Exception if an error occured
     */
    protected File executeMojo( final String projectName, final Properties properties )
        throws Exception
    {
        return executeMojo( projectName, properties, true );
    }

    /**
     * Executes the specified projects and asserts the given artifacts.
     *
     * @param projectName             the project to test
     * @param expectSourceArchive     if a source archive is expected
     * @param expectTestSourceArchive if a test source archive is expected
     * @param expectedSourceFiles     the expected files in the source archive, if any
     * @param expectedTestSourceFiles the expected files in the test source archive, if any
     * @return the base directory of the project
     * @throws Exception if any error occurs
     */
    protected File doTestProject( final String projectName, boolean expectSourceArchive,
                                  boolean expectTestSourceArchive, final String[] expectedSourceFiles,
                                  final String[] expectedTestSourceFiles )
        throws Exception
    {
        final File baseDir = executeMojo( projectName, new Properties() );

        if ( expectSourceArchive )
        {
            assertSourceArchive( baseDir, projectName );
            assertJarContent( getSourceArchive( baseDir, projectName ), expectedSourceFiles );
        }

        if ( expectTestSourceArchive )
        {
            assertTestSourceArchive( baseDir, projectName );
            assertJarContent( getTestSourceArchive( baseDir, projectName ), expectedTestSourceFiles );
        }

        return baseDir;
    }

    /**
     * Executes the specified projects and asserts the given artifacts for a source archive.
     *
     * @param projectName         the project to test
     * @param expectedSourceFiles the expected files in the source archive, if any
     * @return the base directory of the project
     * @throws Exception if any error occurs
     */
    protected File doTestProjectWithSourceArchive( final String projectName, final String[] expectedSourceFiles )
        throws Exception
    {
        return doTestProject( projectName, true, false, expectedSourceFiles, null );
    }

    /**
     * Executes the specified projects and asserts the given artifacts for a test source archive.
     *
     * @param projectName             the project to test
     * @param expectedTestSourceFiles the expected files in the test source archive, if any
     * @return the base directory of the project
     * @throws Exception if any error occurs
     */
    protected File doTestProjectWithTestSourceArchive( final String projectName,
                                                       final String[] expectedTestSourceFiles )
        throws Exception
    {
        return doTestProject( projectName, false, true, null, expectedTestSourceFiles );
    }


    protected void assertSourceArchive( final File baseDir, final String projectName )
    {
        final File expectedFile = getSourceArchive( baseDir, projectName );
        assertTrue( "Source archive does not exist["+expectedFile.getAbsolutePath()+"]", expectedFile.exists() );
    }

    protected void assertTestSourceArchive( final File baseDir, final String projectName )
    {
        final File expectedFile = getTestSourceArchive( baseDir, projectName );
        assertTrue( "Test source archive does not exist["+expectedFile.getAbsolutePath()+"]", expectedFile.exists() );
    }

    protected File getTargetDirectory( final File basedir )
    {
        return new File( basedir, "target" );
    }

    protected File getSourceArchive( final File baseDir, final String projectName )
    {
        return new File( getTargetDirectory( baseDir ), buildFinalSourceName( projectName ) + ".jar" );
    }

    protected File getTestSourceArchive( final File baseDir, final String projectName )
    {
        return new File( getTargetDirectory( baseDir ), buildFinalTestSourceName( projectName ) + ".jar" );
    }

    protected String buildFinalSourceName( final String projectName )
    {
        return FINAL_NAME_PREFIX + projectName + FINAL_NAME_SUFFIX + "-sources";
    }

    protected String buildFinalTestSourceName( final String projectName )
    {
        return FINAL_NAME_PREFIX + projectName + FINAL_NAME_SUFFIX + "-test-sources";
    }

    protected File getTestDir( String projectName )
        throws IOException
    {
        return ResourceExtractor.simpleExtractResources( getClass(), "/projects/" + projectName );
    }

    protected File getBasedir()
    {
        if ( basedir != null )
        {
            return basedir;
        }

        final String basedirString = System.getProperty( "basedir" );
        if ( basedirString == null )
        {
            basedir = new File( "" );
        }
        else
        {
            basedir = new File( basedirString );
        }
        return basedir;
    }

    protected void assertJarContent( final File jarFile, final String[] expectedFiles )
        throws IOException
    {

        ZipFile jar = new ZipFile( jarFile );
        Enumeration entries = jar.getEntries();

        if ( expectedFiles.length == 0 )
        {
            assertFalse( "Jar file should not contain any entry", entries.hasMoreElements() );
        }
        else
        {
            assertTrue( entries.hasMoreElements() );
            for ( int i = 0; i < expectedFiles.length; i++ )
            {
                String expectedFile = expectedFiles[i];
                ZipEntry entry = (ZipEntry) entries.nextElement();
                assertEquals( expectedFile, entry.getName() );
            }

            // Now we are done, assert that there is no more element
            assertFalse( "Jar file contains more elements than expected", entries.hasMoreElements() );
        }
    }
}
