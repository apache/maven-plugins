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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.repository.testutil.TestInputHandler;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

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
     * Test for repository plugin with project.packaging == pom
     * 
     * @throws Exception
     */
    public void testDefaults_PomPackaging()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/pom-only/pom.xml" );

        try
        {
            BundleCreateMojo mojo = (BundleCreateMojo) lookupMojo( "bundle-create", testPom );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File bundleSource =
            new File( getBasedir(), "target/test/unit/pom-only/target/pom-only-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        Set entryNames = new HashSet();
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        assertZipContents( entryNames, Collections.EMPTY_SET, bundleSource );
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

        File bundleSource =
            new File( getBasedir(), "target/test/unit/default-configuration/target/default-configuration-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        Set entryNames = new HashSet();
        entryNames.add( "default-configuration-javadoc.jar" );
        entryNames.add( "default-configuration-sources.jar" );
        entryNames.add( "default-configuration.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        assertZipContents( entryNames, Collections.EMPTY_SET, bundleSource );
    }

    public void testDefaultconfiguration_RemoveOne()
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

            // NOTE: This is sensitive to the lookupMojo method timing...
            TestInputHandler ih = (TestInputHandler) lookup( InputHandler.ROLE, "default" );

            Stack responses = new Stack();
            responses.push( "2" );
            ih.setLineResponses( responses );

            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File bundleSource =
            new File( getBasedir(), "target/test/unit/default-configuration/target/default-configuration-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        Set entryNames = new HashSet();
        entryNames.add( "default-configuration-sources.jar" );
        entryNames.add( "default-configuration.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        Set bannedNames = new HashSet();
        bannedNames.add( "default-configuration-javadoc.jar" );

        assertZipContents( entryNames, bannedNames, bundleSource );
    }

    public void testDefaultconfiguration_RemoveTwo()
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

            // NOTE: This is sensitive to the lookupMojo method timing...
            TestInputHandler ih = (TestInputHandler) lookup( InputHandler.ROLE, "default" );

            Stack responses = new Stack();
            responses.push( "2,3" );
            ih.setLineResponses( responses );

            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File bundleSource =
            new File( getBasedir(), "target/test/unit/default-configuration/target/default-configuration-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        Set entryNames = new HashSet();
        entryNames.add( "default-configuration.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        Set bannedNames = new HashSet();
        bannedNames.add( "default-configuration-javadoc.jar" );
        bannedNames.add( "default-configuration-sources.jar" );

        assertZipContents( entryNames, bannedNames, bundleSource );
    }

    public void testDefaultconfiguration_RemoveTwoUnordered()
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

            // NOTE: This is sensitive to the lookupMojo method timing...
            TestInputHandler ih = (TestInputHandler) lookup( InputHandler.ROLE, "default" );

            Stack responses = new Stack();
            responses.push( "3,2" );
            ih.setLineResponses( responses );

            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File bundleSource =
            new File( getBasedir(), "target/test/unit/default-configuration/target/default-configuration-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        Set entryNames = new HashSet();
        entryNames.add( "default-configuration.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        Set bannedNames = new HashSet();
        bannedNames.add( "default-configuration-javadoc.jar" );
        bannedNames.add( "default-configuration-sources.jar" );

        assertZipContents( entryNames, bannedNames, bundleSource );
    }

    public void testDefaultconfiguration_RemoveTwoWithSpace()
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

            // NOTE: This is sensitive to the lookupMojo method timing...
            TestInputHandler ih = (TestInputHandler) lookup( InputHandler.ROLE, "default" );

            Stack responses = new Stack();
            responses.push( "2, 3" );
            ih.setLineResponses( responses );

            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File bundleSource =
            new File( getBasedir(), "target/test/unit/default-configuration/target/default-configuration-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        Set entryNames = new HashSet();
        entryNames.add( "default-configuration.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        Set bannedNames = new HashSet();
        bannedNames.add( "default-configuration-javadoc.jar" );
        bannedNames.add( "default-configuration-sources.jar" );

        assertZipContents( entryNames, bannedNames, bundleSource );
    }

    private void assertZipContents( Set requiredNames, Set bannedNames, File bundleSource )
        throws IOException
    {
        ZipFile zf = new ZipFile( bundleSource );

        Set missing = new HashSet();
        for ( Iterator it = requiredNames.iterator(); it.hasNext(); )
        {
            String name = (String) it.next();
            if ( zf.getEntry( name ) == null )
            {
                missing.add( name );
            }
        }

        Set banned = new HashSet();
        for ( Iterator it = bannedNames.iterator(); it.hasNext(); )
        {
            String name = (String) it.next();
            if ( zf.getEntry( name ) != null )
            {
                banned.add( name );
            }
        }

        if ( !missing.isEmpty() || !banned.isEmpty() )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "The following REQUIRED entries were missing from the bundle archive:\n" );

            if ( missing.isEmpty() )
            {
                msg.append( "\nNone." );
            }
            else
            {
                for ( Iterator it = missing.iterator(); it.hasNext(); )
                {
                    String name = (String) it.next();

                    msg.append( "\n" ).append( name );
                }
            }

            msg.append( "\n\nThe following BANNED entries were present from the bundle archive:\n" );

            if ( banned.isEmpty() )
            {
                msg.append( "\nNone.\n" );
            }
            else
            {
                for ( Iterator it = banned.iterator(); it.hasNext(); )
                {
                    String name = (String) it.next();

                    msg.append( "\n" ).append( name );
                }
            }

            fail( msg.toString() );
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

        Set entryNames = new HashSet();
        entryNames.add( "no-javadocjar-sources.jar" );
        entryNames.add( "no-javadocjar.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        assertZipContents( entryNames, Collections.singleton( "no-javadocjar-javadoc.jar" ), bundleSource );
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

        Set entryNames = new HashSet();
        entryNames.add( "no-sourcesjar-javadoc.jar" );
        entryNames.add( "no-sourcesjar.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        assertZipContents( entryNames, Collections.singleton( "no-sourcesjar-sources.jar" ), bundleSource );
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

        File bundleSource =
            new File( getBasedir(), "target/test/unit/no-javadoc-sources/target/no-javadoc-sources-bundle.jar" );
        assertTrue( FileUtils.fileExists( bundleSource.getAbsolutePath() ) );

        Set entryNames = new HashSet();
        entryNames.add( "no-javadoc-sources.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        Set bannedNames = new HashSet();
        bannedNames.add( "no-javadoc-sources-sources.jar" );
        bannedNames.add( "no-javadoc-sources-javadoc.jar" );

        assertZipContents( entryNames, bannedNames, bundleSource );
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
     * @param fileName
     *            the file name of the jar file(s) to be created
     * @param createJavadocJar
     *            specifies whether a javadoc jar file will be created or not
     * @param createSourcesJar
     *            specifies whether a sources jar file will be created or not
     * @param destDir
     *            the destination directory where the jar file(s) are to be created
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