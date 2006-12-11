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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.tools.easymock.TestFileManager;

public class EclipsePluginUnitTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "EclipsePlugin.unitTest.", "" );

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testBuildDirectoryList_ShouldUseTestOutputDirFromProjectWhenBuildOutputDirIsStandard()
        throws MojoExecutionException
    {
        File basedir = fileManager.createTempDir();

        Build build = new Build();

        Resource resource = new Resource();

        String resDir = "src/main/resources";
        new File( basedir, resDir ).mkdirs();

        String resOutput = "target/main-output";

        resource.setDirectory( resDir );

        build.addTestResource( resource );
        build.setOutputDirectory( "target/classes" );
        build.setTestOutputDirectory( resOutput );

        Model model = new Model();
        model.setBuild( build );

        MavenProject project = new MavenProject( model );

        File pom = new File( basedir, "pom.xml" );
        project.setFile( pom );

        EclipseSourceDir[] result = new EclipsePlugin().buildDirectoryList( project, basedir,
                                                                            new File( "target/classes" ) );

        assertEquals( "should have added 1 resource.", 1, result.length );

        String path = result[0].getOutput();

        assertTrue( "output directory should end with: " + resOutput, path.endsWith( resOutput ) );
    }

    public void testExtractResourceDirs_ShouldUseResourceOutput()
        throws MojoExecutionException
    {
        File basedir = fileManager.createTempDir();

        Build build = new Build();

        Resource resource = new Resource();

        String resDir = "src/main/resources";
        new File( basedir, resDir ).mkdirs();

        // assumes base of target/classes.
        String resOutput = "main-output";

        resource.setDirectory( resDir );
        resource.setTargetPath( resOutput );
        build.addResource( resource );

        Model model = new Model();
        model.setBuild( build );

        MavenProject project = new MavenProject( model );

        Set result = new LinkedHashSet();

        EclipsePlugin plugin = new EclipsePlugin();

        plugin.extractResourceDirs( result, project.getBuild().getResources(), project, basedir, basedir, false,
                                    "target/classes" );

        Iterator resultIter = result.iterator();

        assertEquals( "too many resource entries added.", 1, result.size() );

        String path = ( (EclipseSourceDir) resultIter.next() ).getOutput();

        String prefix = "target/classes/";

        assertTrue( "output directory should end with: " + prefix + resOutput + "\nWas: " + path, path.endsWith( prefix
            + resOutput ) );
    }

    public void testExtractResourceDirs_ShouldUseSpecifiedOutputDirectory()
        throws MojoExecutionException
    {
        File basedir = fileManager.createTempDir();

        Build build = new Build();

        Resource resource = new Resource();

        String resDir = "src/main/resources";
        new File( basedir, resDir ).mkdirs();

        String resOutput = "target/main-output";

        resource.setDirectory( resDir );

        build.addTestResource( resource );

        Model model = new Model();
        model.setBuild( build );

        MavenProject project = new MavenProject( model );

        Set result = new LinkedHashSet();

        EclipsePlugin plugin = new EclipsePlugin();

        plugin.extractResourceDirs( result, project.getBuild().getTestResources(), project, basedir, basedir, false,
                                    resOutput );

        Iterator resultIter = result.iterator();

        assertEquals( "should have added 1 resource.", 1, result.size() );

        String path = ( (EclipseSourceDir) resultIter.next() ).getOutput();

        assertTrue( "output directory should end with: " + resOutput, path.endsWith( resOutput ) );
    }

    public void testExtractResourceDirs_ShouldIncludeMainAndTestResources()
        throws MojoExecutionException
    {
        File basedir = fileManager.createTempDir();

        runResourceExtractionTest( basedir, basedir );
    }

    public void testExtractResourceDirs_ShouldIncludeMainAndTestResourcesWhenBaseDirsDiffer()
        throws MojoExecutionException
    {
        File basedir = fileManager.createTempDir();
        File projectBasedir = fileManager.createTempDir();

        runResourceExtractionTest( basedir, projectBasedir );
    }

    private void runResourceExtractionTest( File basedir, File workspaceProjectBasedir )
        throws MojoExecutionException
    {
        Build build = new Build();

        Resource resource = new Resource();

        String resDir = "src/main/resources";
        new File( basedir, resDir ).mkdirs();

        resource.setDirectory( resDir );
        build.addResource( resource );

        Resource testResource = new Resource();

        String testResDir = "src/test/resources";
        new File( basedir, testResDir ).mkdirs();

        testResource.setDirectory( testResDir );
        build.addTestResource( testResource );

        Model model = new Model();
        model.setBuild( build );

        MavenProject project = new MavenProject( model );

        Set result = new LinkedHashSet();

        EclipsePlugin plugin = new EclipsePlugin();

        plugin.extractResourceDirs( result, project.getBuild().getResources(), project, basedir,
                                    workspaceProjectBasedir, false, "target/classes" );

        Iterator resultIter = result.iterator();

        assertEquals( "too many resource entries added.", 1, result.size() );

        String path = ( (EclipseSourceDir) resultIter.next() ).getPath();

        if ( basedir.equals( workspaceProjectBasedir ) )
        {
            assertTrue( "resource dir path: " + path + " does not end with: " + resDir, path.endsWith( resDir ) );
        }
        else
        {
            resDir = resDir.replace( '\\', '/' ).replace( '/', '-' );

            assertTrue( "resource dir path: " + path + " does not end with: " + resDir, path.endsWith( resDir ) );
        }

        plugin.extractResourceDirs( result, project.getBuild().getTestResources(), project, basedir,
                                    workspaceProjectBasedir, false, "target/test-classes" );

        resultIter = result.iterator();
        resultIter.next();

        assertEquals( "too many test-resource entries added.", 2, result.size() );

        path = ( (EclipseSourceDir) resultIter.next() ).getPath();

        if ( basedir.equals( workspaceProjectBasedir ) )
        {
            assertTrue( "test-resource dir path: " + path + " does not end with: " + testResDir, path
                .endsWith( testResDir ) );
        }
        else
        {
            testResDir = testResDir.replace( '\\', '/' ).replace( '/', '-' );

            assertTrue( "test-resource dir path: " + path + " does not end with: " + testResDir, path
                .endsWith( testResDir ) );
        }

    }
}
