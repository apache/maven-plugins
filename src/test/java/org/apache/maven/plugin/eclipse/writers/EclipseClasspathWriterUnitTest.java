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
package org.apache.maven.plugin.eclipse.writers;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.writers.testutils.TestEclipseWriterConfig;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.shared.tools.easymock.TestFileManager;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

public class EclipseClasspathWriterUnitTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "EclipseClasspathWriter.unitTest.", "" );

    protected void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testWrite_ShouldMaskOutputDirsNestedWithinAnExistingOutputDir()
        throws MojoExecutionException, JDOMException, IOException
    {
        TestEclipseWriterConfig config = new TestEclipseWriterConfig();

        File basedir = fileManager.createTempDir();

        config.setProjectBaseDir( basedir );
        config.setEclipseProjectDirectory( basedir );

        String baseOutputDir = "target/classes";
        String maskedOutputDir = "target/classes/main-resources";

        File buildOutputDir = new File( basedir, baseOutputDir );
        buildOutputDir.mkdirs();

        config.setBuildOutputDirectory( buildOutputDir );

        new File( basedir, maskedOutputDir ).mkdirs();

        EclipseSourceDir dir =
            new EclipseSourceDir( "src/main/resources", "target/classes", true, false, null, null, false );
        EclipseSourceDir testDir =
            new EclipseSourceDir( "src\\test\\resources", "target/classes/test-resources", true, true, null, null,
                                  false );

        EclipseSourceDir[] dirs = { dir, testDir };

        config.setSourceDirs( dirs );

        config.setEclipseProjectName( "test-project" );

        TestLog log = new TestLog();

        EclipseClasspathWriter classpathWriter = new EclipseClasspathWriter();
        classpathWriter.init( log, config );
        classpathWriter.write();

        SAXBuilder builder = new SAXBuilder( false );

        Document doc = builder.build( new File( basedir, ".classpath" ) );

        XPath resourcePath = XPath.newInstance( "//classpathentry[@path='src/main/resources']" );

        assertTrue( "resources classpath entry not found.", resourcePath.selectSingleNode( doc ) != null );

        XPath testResourcePath = XPath.newInstance( "//classpathentry[@path='src/test/resources']" );

        assertTrue( "test resources (minus custom output dir) classpath entry not found.",
                    testResourcePath.selectSingleNode( doc ) != null );

        XPath stdOutputPath = XPath.newInstance( "//classpathentry[@kind='output' && @path='target/classes']" );

        assertTrue( "standard output classpath entry not found.", stdOutputPath.selectSingleNode( doc ) != null );

    }

    public void testWrite_ShouldGenerateValidJavadocURLs()
        throws MojoExecutionException, JDOMException, IOException
    {
        TestEclipseWriterConfig config = new TestEclipseWriterConfig();

        File basedir = fileManager.createTempDir();

        File repoDir = new File( basedir, "repo" );
        config.setLocalRepository( new StubArtifactRepository( repoDir.getPath() ) );

        config.setProjectBaseDir( basedir );
        config.setEclipseProjectDirectory( basedir );

        String baseOutputDir = "target/classes";
        String maskedOutputDir = "target/classes/main-resources";

        File buildOutputDir = new File( basedir, baseOutputDir );
        buildOutputDir.mkdirs();

        config.setBuildOutputDirectory( buildOutputDir );

        new File( basedir, maskedOutputDir ).mkdirs();

        config.setEclipseProjectName( "test-project" );

        IdeDependency dependency = new IdeDependency();
        dependency.setFile( new File( repoDir, "g/a/v/a-v.jar" ) );
        dependency.setGroupId( "g" );
        dependency.setArtifactId( "a" );
        dependency.setVersion( "v" );
        dependency.setAddedToClasspath( true );
        dependency.setJavadocAttachment( new File( System.getProperty( "user.home" ), ".m2/some.jar" ) );

        config.setDeps( new IdeDependency[] { dependency } );

        TestLog log = new TestLog();

        EclipseClasspathWriter classpathWriter = new EclipseClasspathWriter();
        classpathWriter.init( log, config );
        classpathWriter.write();

        SAXBuilder builder = new SAXBuilder( false );

        Document doc = builder.build( new File( basedir, ".classpath" ) );

        XPath javadocUrls = XPath.newInstance( "//attribute/@value" );
        for ( Iterator it = javadocUrls.selectNodes( doc ).iterator(); it.hasNext(); )
        {
            Attribute attribute = (Attribute) it.next();
            URL jarUrl = new URL( attribute.getValue() );
            URL fileUrl = ( (JarURLConnection) jarUrl.openConnection() ).getJarFileURL();
            String host = fileUrl.getHost();
            assertTrue( "Unexpected host: \"" + host + "\"", "".equals( host ) || "localhost".equals( host ) );
        }
    }

    private static final class TestLog
        extends SystemStreamLog
    {
        public boolean isDebugEnabled()
        {
            return true;
        }
    }

}
