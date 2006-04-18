package org.apache.maven.plugin.source;

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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Enumeration;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class JarTestSourceMojoTest
    extends AbstractMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
        
    }


    public void testDefaultConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/default-configuration/default-configuration-config.xml" );

        try
        {
            JarTestSourceMojo mojo = (JarTestSourceMojo) lookupMojo( "test-jar", testPom );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        //check if the jar file exists
        File sourceJar = new File( getBasedir(),
                                   "target/test/unit/default-configuration/target/default-configuration-test-sources.jar" );
        assertTrue( FileUtils.fileExists( sourceJar.getAbsolutePath() ) );

        ZipFile jar = new ZipFile( sourceJar );
        Enumeration entries = jar.getEntries();
        assertTrue( entries.hasMoreElements() );

        if ( entries.hasMoreElements() )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "test-default-configuration.properties" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "def/configuration/AppTest.java" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "def/configuration/" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "def/" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/MANIFEST.MF" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/" );
        }

    }

    public void testCustomConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/custom-configuration/custom-configuration-config.xml" );

        try
        {
            JarTestSourceMojo mojo = (JarTestSourceMojo) lookupMojo( "test-jar", testPom );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        //check if the jar file exists
        File sourceJar = new File( getBasedir(),
                                   "target/test/unit/custom-configuration/target/custom-configuration-test-sources.jar" );
        assertTrue( FileUtils.fileExists( sourceJar.getAbsolutePath() ) );

        //verify the contents of the jar file
        ZipFile jar = new ZipFile( sourceJar );
        Enumeration entries = jar.getEntries();
        assertTrue( entries.hasMoreElements() );

        if ( entries.hasMoreElements() )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "test-custom-configuration.properties" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "custom/configuration/AppTest.java" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "custom/configuration/" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "custom/" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/MANIFEST.MF" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/" );
        }


    }

    public void testInvalidPackaging()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/invalid-packaging/invalid-packaging-config.xml" );

        try
        {
            JarTestSourceMojo mojo = (JarTestSourceMojo) lookupMojo( "test-jar", testPom );
            mojo.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        File sourceJar =
            new File( getBasedir(), "target/test/unit/invalid-packaging/target/invalid-packaging-test-sources.jar" );
        assertFalse( FileUtils.fileExists( sourceJar.getAbsolutePath() ) );

    }

    protected void tearDown()
        throws Exception
    {

    }
}
