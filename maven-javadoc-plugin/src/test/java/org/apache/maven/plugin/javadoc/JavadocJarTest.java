package org.apache.maven.plugin.javadoc;

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
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class JavadocJarTest
    extends AbstractMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
    }

    /**
     * Test when default configuration is provided
     *
     * @throws Exception
     */
    public void testDefaultConfig()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/javadocjar-default/javadocjar-default-plugin-config.xml" );
        JavadocJar mojo = (JavadocJar) lookupMojo( "jar", testPom );
        mojo.execute();

        //check if the javadoc jar file was generated
        File generatedFile =
            new File( getBasedir(), "target/test/unit/javadocjar-default/target/javadocjar-default-javadoc.jar" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        //validate contents of jar file
        ZipFile jar = new ZipFile( generatedFile );
        Enumeration entries = jar.getEntries();
        assertTrue( entries.hasMoreElements() );

        while ( entries.hasMoreElements() )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if( entry.getName().equals( "stylesheet.css" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "resources/inherit.gif" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "javadocjar/def/package-use.html" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "javadocjar/def/package-tree.html" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "javadocjar/def/package-summary.html" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "javadocjar/def/package-frame.html" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "javadocjar/def/class-use/AppSample.html" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "index.html" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "javadocjar/def/App.html" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "javadocjar/def/AppSample.html" ) )
            {
                assertTrue( true );
            }
            else if( entry.getName().equals( "javadocjar/def/class-use/App.html" ) )
            {
                assertTrue( true );
            }
        }

        //check if the javadoc files were created
        generatedFile =
            new File( getBasedir(), "target/test/unit/javadocjar-default/target/site/apidocs/javadocjar/def/App.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/javadocjar-default/target/site/apidocs/javadocjar/def/AppSample.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

    }

    /**
     * Test when the specified destDir parameter has an invalid value
     *
     * @throws Exception
     */
    public void testInvalidDestdir()
        throws Exception
    {

        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/javadocjar-invalid-destdir/javadocjar-invalid-destdir-plugin-config.xml" );
        JavadocJar mojo = (JavadocJar) lookupMojo( "jar", testPom );
        mojo.execute();

        //check if the javadoc jar file was generated
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/javadocjar-invalid-destdir/target/javadocjar-invalid-destdir-javadoc.jar" );
        assertTrue( !FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

    }   

    protected void tearDown()
        throws Exception
    {

    }

}
