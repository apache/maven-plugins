package org.apache.maven.plugin.javadoc;

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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class JavadocJarTest
    extends AbstractMojoTestCase
{
    /**
     * Test when default configuration is provided
     *
     * @throws Exception if any
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
        Set<String> set = new HashSet<String>();
        for( Enumeration<ZipEntry> entries = jar.getEntries(); entries.hasMoreElements(); )
        {
            ZipEntry entry = entries.nextElement();
            set.add( entry.getName() );
        }

        assertTrue( set.contains( "stylesheet.css" ) );
        assertTrue( set.contains( "resources/inherit.gif" ) );
        assertTrue( set.contains( "javadocjar/def/package-use.html" ) );
        assertTrue( set.contains( "javadocjar/def/package-tree.html" ) );
        assertTrue( set.contains( "javadocjar/def/package-summary.html" ) );
        assertTrue( set.contains( "javadocjar/def/package-frame.html" ) );
        assertTrue( set.contains( "javadocjar/def/class-use/AppSample.html" ) );
        assertTrue( set.contains( "index.html" ) );
        assertTrue( set.contains( "javadocjar/def/App.html" ) );
        assertTrue( set.contains( "javadocjar/def/AppSample.html" ) );
        assertTrue( set.contains( "javadocjar/def/class-use/App.html" ) );

        assertFalse( set.contains( AbstractJavadocMojo.ARGFILE_FILE_NAME ) );
        assertFalse( set.contains( AbstractJavadocMojo.FILES_FILE_NAME ) );
        assertFalse( set.contains( AbstractJavadocMojo.OPTIONS_FILE_NAME ) );
        assertFalse( set.contains( AbstractJavadocMojo.PACKAGES_FILE_NAME ) );

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
     * @throws Exception if any
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
}
