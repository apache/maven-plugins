package org.apache.maven.plugin.source;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
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
*   http://www.apache.org/licenses/LICENSE-2.0
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
    extends AbstractMojoTestCase
{

    protected final String FINAL_NAME_PREFIX = "maven-source-plugin-test-";

    protected final String FINAL_NAME_SUFFIX = "-99.0";

    protected abstract String getGoal();

    /**
     * Execute the souce plugin for the specified project.
     *
     * @param projectName the name of the project
     * @return the base directory of the project
     * @throws Exception if an error occured
     */
    protected void executeMojo( final String projectName )
        throws Exception
    {
        File testPom = new File( getBasedir(), getTestDir( projectName ) + "/pom.xml" );
        AbstractSourceJarMojo mojo = (AbstractSourceJarMojo) lookupMojo( getGoal(), testPom );

        mojo.execute();
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
        executeMojo( projectName );
        final File testTargetDir = getTestTargetDir( projectName );

        if ( expectSourceArchive )
        {
            assertSourceArchive( testTargetDir, projectName );
            assertJarContent( getSourceArchive( testTargetDir, projectName ), expectedSourceFiles );
        }

        if ( expectTestSourceArchive )
        {
            assertTestSourceArchive( testTargetDir, projectName );
            assertJarContent( getTestSourceArchive( testTargetDir, projectName ), expectedTestSourceFiles );
        }

        return testTargetDir;
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


    protected void assertSourceArchive( final File testTargetDir, final String projectName )
    {
        final File expectedFile = getSourceArchive( testTargetDir, projectName );
        assertTrue( "Source archive does not exist[" + expectedFile.getAbsolutePath() + "]", expectedFile.exists() );
    }

    protected void assertTestSourceArchive( final File testTargetDir, final String projectName )
    {
        final File expectedFile = getTestSourceArchive( testTargetDir, projectName );
        assertTrue( "Test source archive does not exist[" + expectedFile.getAbsolutePath() + "]",
                    expectedFile.exists() );
    }

    protected File getSourceArchive( final File testTargetDir, final String projectName )
    {
        return new File( testTargetDir, buildFinalSourceName( projectName ) + ".jar" );
    }

    protected File getTestSourceArchive( final File testTargetDir, final String projectName )
    {
        return new File( testTargetDir, buildFinalTestSourceName( projectName ) + ".jar" );
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
        File f = new File( "target/test-classes/unit/" + projectName );
        if ( !new File( f, "pom.xml" ).exists() )
        {
            throw new IllegalStateException( "No pom file found in " + f.getPath() );
        }
        return f;
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

            Set expected = new TreeSet( Arrays.asList( expectedFiles ) );

            while ( entries.hasMoreElements() )
            {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                assertTrue( "Not expecting " + entry.getName() + " in " + jarFile, expected.remove( entry.getName() ) );
            }

            assertTrue( "Missing entries " + expected.toString() + " in " + jarFile, expected.isEmpty() );
        }
    }

    protected File getTestTargetDir( String projectName )
    {
        return new File( getBasedir(), "target/test/unit/" + projectName + "/target" );
    }
}
