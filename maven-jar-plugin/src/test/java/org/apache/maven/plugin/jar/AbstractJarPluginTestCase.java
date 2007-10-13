package org.apache.maven.plugin.jar;

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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Base abstract test case for jar tests.
 */
public abstract class AbstractJarPluginTestCase
    extends PlexusTestCase
{

    protected final String FINAL_NAME_PREFIX = "maven-jar-plugin-test-";

    protected final String FINAL_NAME_SUFFIX = "-99.0";

    /**
     * Execute the JAR plugin for the specified project.
     * 
     * @param projectName the name of the project
     * @param properties extra properties to be used by the embedder
     * @return the base directory of the project
     * @throws Exception if an error occured
     */
    protected File executeMojo( final String projectName, final Properties properties )
        throws Exception
    {
        MavenEmbedder maven = new MavenEmbedder();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        maven.setClassLoader( classLoader );
        maven.setLogger( new MavenEmbedderConsoleLogger() );
        maven.setOffline( true );
        maven.start();

        File itbasedir = new File( getBasedir(), "target/test-classes/it/" + projectName );
        MavenProject pom = maven.readProjectWithDependencies( new File( itbasedir, "pom.xml" ) );

        EventMonitor eventMonitor =
            new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );
        maven.execute( pom, Collections.singletonList( "package" ), eventMonitor, new ConsoleDownloadMonitor(),
                       properties, itbasedir );

        maven.stop();

        return itbasedir;
    }

    /**
     * Executes the specified projects and asserts the given artifacts.
     * 
     * @param projectName the project to test
     * @param expectedArtifacts the array of artifacts to be found in the JAR archive
     * @throws Exception
     */
    protected File doTestProject( final String projectName, final String classifier, final String[] expectedArtifacts )
        throws Exception
    {
        final File baseDir = executeMojo( projectName, new Properties() );
        assertJarArchive( baseDir, projectName, classifier );

        assertArchiveContent( baseDir, projectName, classifier, expectedArtifacts );

        return baseDir;
    }

    protected void assertJarArchive( final File baseDir, final String projectName, final String classifier )
    {
        assertTrue( "JAR archive does not exist", getJarArchive( baseDir, projectName, classifier ).exists() );
    }

    protected File getTargetDirectory( final File basedir )
    {
        return new File( basedir, "target" );
    }

    protected File getJarArchive( final File baseDir, final String projectName, final String classifier )
    {
        return new File( getTargetDirectory( baseDir ), buildFinalName( projectName, classifier ) + ".jar" );
    }

    protected File getJarDirectory( final File baseDir, final String projectName, final String classifier )
    {
        return new File( getTargetDirectory( baseDir ), buildFinalName( projectName, classifier ) );
    }

    protected String buildFinalName( final String projectName, final String classifier )
    {
        if ( classifier != null )
        {
            return FINAL_NAME_PREFIX + projectName + FINAL_NAME_SUFFIX + "-" + classifier;
        }
        return FINAL_NAME_PREFIX + projectName + FINAL_NAME_SUFFIX;
    }

    protected void assertArchiveContent( final File baseDir, final String projectName, final String classifier,
                                         final String[] artifactNames )
        throws IOException
    {
        Set contents = new HashSet();

        JarFile jar = new JarFile( getJarArchive( baseDir, projectName, classifier ) );
        Enumeration jarEntries = jar.entries();
        while ( jarEntries.hasMoreElements() )
        {
            JarEntry entry = (JarEntry) jarEntries.nextElement();
            if ( !entry.isDirectory() )
            {
                // Only compare files
                contents.add( entry.getName() );
            }
        }

        assertEquals( "Artifacts mismatch " + contents, artifactNames.length, contents.size() );
        for ( int i = 0; i < artifactNames.length; i++ )
        {
            String artifactName = artifactNames[i];

            assertTrue( "Artifact[" + artifactName + "] not found in jar archive", contents.contains( artifactName ) );
        }
    }
}
