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
package org.apache.maven.plugins.repository;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class BundleCreateMojoTest
    extends AbstractMojoTestCase
{
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();

    }

    /**
     * Test for repository plugin default configuration
     *
     * @throws Exception
     */
    public void testDefaultconfiguration()
        throws Exception
    {

        try
        {
            createTestJars( "default-configuration", true, true, getBasedir()
                + "/target/test/unit/default-configuration/target" );
        }
        catch ( IOException ie )
        {
            ie.printStackTrace();
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/default-configuration/pom.xml" );

        try
        {
            BundleCreateMojo mojo = (BundleCreateMojo) lookupMojo( "bundle-create", testPom );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File bundleSource = new File( getBasedir(),
                                      "target/test/unit/default-configuration/target/default-configuration-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        ZipFile jar = new ZipFile( bundleSource );
        Enumeration entries = jar.getEntries();
        assertTrue( entries.hasMoreElements() );

        if ( entries.hasMoreElements() )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "default-configuration-javadoc.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "default-configuration-sources.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "default-configuration.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "pom.xml" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/MANIFEST.MF" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/" );
        }

    }

    /**
     * Test repository plugin when there is no javadoc jar to be included in the bundle
     *
     * @throws Exception
     */
    public void testNoJavadocJar()
        throws Exception
    {

        try
        {
            createTestJars( "no-javadocjar", false, true, getBasedir() + "/target/test/unit/no-javadocjar/target" );
        }
        catch ( IOException ie )
        {
            ie.printStackTrace();
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/no-javadocjar/pom.xml" );

        try
        {
            BundleCreateMojo mojo = (BundleCreateMojo) lookupMojo( "bundle-create", testPom );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File bundleSource = new File( getBasedir(), "target/test/unit/no-javadocjar/target/no-javadocjar-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        ZipFile jar = new ZipFile( bundleSource );
        Enumeration entries = jar.getEntries();
        assertTrue( entries.hasMoreElements() );

        if ( entries.hasMoreElements() )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "no-javadocjar-sources.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "no-javadocjar.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "pom.xml" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/MANIFEST.MF" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/" );
        }

    }

    /**
     * Test repository plugin when there is no sources jar to be included in the bundle
     *
     * @throws Exception
     */
    public void testNoSourcesJar()
        throws Exception
    {

        try
        {
            createTestJars( "no-sourcesjar", true, false, getBasedir() + "/target/test/unit/no-sourcesjar/target" );
        }
        catch ( IOException ie )
        {
            ie.printStackTrace();
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/no-sourcesjar/pom.xml" );

        try
        {
            BundleCreateMojo mojo = (BundleCreateMojo) lookupMojo( "bundle-create", testPom );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File bundleSource = new File( getBasedir(), "target/test/unit/no-sourcesjar/target/no-sourcesjar-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        ZipFile jar = new ZipFile( bundleSource );
        Enumeration entries = jar.getEntries();
        assertTrue( entries.hasMoreElements() );

        if ( entries.hasMoreElements() )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "no-sourcesjar-javadoc.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "no-sourcesjar.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "pom.xml" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/MANIFEST.MF" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/" );
        }

    }

    /**
     * Test repository plugin when there are no javadoc and sources jar files to be included in the bundle
     *
     * @throws Exception
     */
    public void testNoJavadocSourcesJars()
        throws Exception
    {

        try
        {
            createTestJars( "no-javadoc-sources", false, false, getBasedir()
                + "/target/test/unit/no-javadoc-sources/target" );
        }
        catch ( IOException ie )
        {
            ie.printStackTrace();
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/no-javadoc-sources/pom.xml" );

        try
        {
            BundleCreateMojo mojo = (BundleCreateMojo) lookupMojo( "bundle-create", testPom );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File bundleSource = new File( getBasedir(),
                                      "target/test/unit/no-javadoc-sources/target/no-javadoc-sources-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        ZipFile jar = new ZipFile( bundleSource );
        Enumeration entries = jar.getEntries();
        assertTrue( entries.hasMoreElements() );

        if ( entries.hasMoreElements() )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "no-javadoc-sources.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "pom.xml" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/MANIFEST.MF" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/" );
        }

    }

    /**
     * Test repository plugin when the packaging specified in the pom is invalid
     *
     * @throws Exception
     */
    public void testInvalidPackaging()
        throws Exception
    {

        try
        {
            createTestJars( "invalid-packaging", false, false, getBasedir()
                + "/target/test/unit/invalid-packaging/target" );
        }
        catch ( IOException ie )
        {
            ie.printStackTrace();
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/invalid-packaging/pom.xml" );

        try
        {
            BundleCreateMojo mojo = (BundleCreateMojo) lookupMojo( "bundle-create", testPom );
            mojo.execute();
            fail( "Must throw an exception on an invalid packaging" );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }

    }

    /**
     * Test repository plugin when the scm element is null
     *
     * @throws Exception
     */
    public void testNullScm()
        throws Exception
    {
        try
        {
            createTestJars( "no-scm", false, false, getBasedir() + "/target/test/unit/no-scm/target" );
        }
        catch ( IOException ie )
        {
            ie.printStackTrace();
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/no-scm/pom.xml" );

        BundleCreateMojo mojo = (BundleCreateMojo) lookupMojo( "bundle-create", testPom );
        mojo.execute();

        // MREPOSITORY-2 project.scm.connection should not be required for bundle-create
        // fail( "Must throw an exception on a project element scm is null" );

    }

    /**
     * Test repository plugin when license file does not exist
     *
     * @throws Exception
     */
    public void testNoLicense()
        throws Exception
    {

        try
        {
            createTestJars( "no-license-file", false, false, getBasedir() + "/target/test/unit/no-license-file/target" );
        }
        catch ( IOException ie )
        {
            ie.printStackTrace();
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/no-license-file/pom.xml" );

        try
        {
            BundleCreateMojo mojo = (BundleCreateMojo) lookupMojo( "bundle-create", testPom );
            mojo.execute();
            fail( "Must throw an exception on missing license file" );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }

    }

    /**
     * Test repository plugin when there is no project name specified in the pom
     *
     * @throws Exception
     */
    public void testNoProjectName()
        throws Exception
    {
        try
        {
            createTestJars( "no-project-name", false, false, getBasedir() + "/target/test/unit/no-project-name/target" );
        }
        catch ( IOException ie )
        {
            ie.printStackTrace();
        }

        File testPom = new File( getBasedir(), "src/test/resources/unit/no-project-name/pom.xml" );

        try
        {
            BundleCreateMojo mojo = (BundleCreateMojo) lookupMojo( "bundle-create", testPom );
            mojo.execute();
            fail( "Must throw an exception on empty expression" );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }

    }

    /**
     * Method for creating the jar files that will be used in testing
     *
     * @param fileName         the file name of the jar file(s) to be created
     * @param createJavadocJar specifies whether a javadoc jar file will be created or not
     * @param createSourcesJar specifies whether a sources jar file will be created or not
     * @param destDir          the destination directory where the jar file(s) are to be created
     * @throws IOException
     */
    private void createTestJars( String fileName, boolean createJavadocJar, boolean createSourcesJar, String destDir )
        throws IOException
    {
        File targetDir = new File( destDir );
        boolean success = targetDir.mkdirs();

        if ( success )
        {
            File outputFile = new File( destDir, fileName + ".jar" );
            success = outputFile.createNewFile();

            if ( createJavadocJar )
            {
                File javadocJar = new File( destDir, fileName + "-javadoc.jar" );
                success = javadocJar.createNewFile();
            }

            if ( createSourcesJar )
            {
                File sourcesJar = new File( destDir, fileName + "-sources.jar" );
                success = sourcesJar.createNewFile();
            }
        }
        else
        {
            System.out.println( "Target directory not created." );

            return;
        }

    }

    protected void tearDown()
        throws Exception
    {

    }

}