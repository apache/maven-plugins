package org.apache.maven.plugin.war;

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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * @author Allan Ramirez
 */
public class WarManifestMojoTest
    extends AbstractMojoTestCase
{
    File testPom;

    WarManifestMojo mojo;

    public void testEnvironment()
        throws Exception
    {
        loadMojo( "target/test-classes/unit/manifest/basic-manifest-test/plugin-config.xml" );
    }

    public void testBasicManifest()
        throws Exception
    {
        loadMojo( "target/test-classes/unit/manifest/basic-manifest-test/plugin-config.xml" );

        mojo.execute();

        File warSourceDir = (File) getVariableValueFromObject( mojo, "warSourceDirectory" );

        File manifestDir = new File( warSourceDir, "META-INF" );

        File manifest = new File( manifestDir, "MANIFEST.MF" );

        assertTrue( manifest.exists() );
    }

    public void testManifestWithClasspath()
        throws Exception
    {
        loadMojo( "target/test-classes/unit/manifest/manifest-with-classpath/plugin-config.xml" );

        MavenArchiveConfiguration config = (MavenArchiveConfiguration) getVariableValueFromObject( mojo, "archive" );

        mojo.execute();

        assertTrue( config.getManifest().isAddClasspath() );

        File warSourceDir = (File) getVariableValueFromObject( mojo, "warSourceDirectory" );

        File manifestDir = new File( warSourceDir, "META-INF" );

        File manifest = new File( manifestDir, "MANIFEST.MF" );

        assertTrue( manifest.exists() );

        String content = FileUtils.fileRead( manifest );

        int idx = content.indexOf( "Class-Path" );

        assertTrue( idx >= 0 );
    }

    public void testManifestWithMainClass()
        throws Exception
    {
        loadMojo( "target/test-classes/unit/manifest/manifest-with-main-class/plugin-config.xml" );

        MavenArchiveConfiguration config = (MavenArchiveConfiguration) getVariableValueFromObject( mojo, "archive" );

        mojo.execute();

        assertEquals( "org.dummy.test.SomeClass", config.getManifest().getMainClass() );

        File warSourceDir = (File) getVariableValueFromObject( mojo, "warSourceDirectory" );

        File manifestDir = new File( warSourceDir, "META-INF" );

        File manifest = new File( manifestDir, "MANIFEST.MF" );

        assertTrue( manifest.exists() );

        String content = FileUtils.fileRead( manifest );

        int idx = content.indexOf( "Main-Class" );

        assertTrue( idx >= 0 );
    }

    public void testManifestWithOtherAttributes()
        throws Exception
    {
        loadMojo( "target/test-classes/unit/manifest/manifest-with-other-attrs/plugin-config.xml" );

        MavenArchiveConfiguration config = (MavenArchiveConfiguration) getVariableValueFromObject( mojo, "archive" );

        mojo.execute();

        assertTrue( config.getManifest().isAddExtensions() );

        File warSourceDir = (File) getVariableValueFromObject( mojo, "warSourceDirectory" );

        File manifestDir = new File( warSourceDir, "META-INF" );

        File manifest = new File( manifestDir, "MANIFEST.MF" );

        assertTrue( manifest.exists() );

        String content = FileUtils.fileRead( manifest );

        int idx = content.indexOf( "Specification-Title" );

        assertTrue( idx >= 0 );

        idx = content.indexOf( "Specification-Vendor" );

        assertTrue( idx >= 0 );

        idx = content.indexOf( "Implementation-Vendor" );

        assertTrue( idx >= 0 );  
    }

    public void testManifestWithCustomAttributes()
      throws Exception
    {
        loadMojo( "target/test-classes/unit/manifest/manifest-with-custom-attrs/plugin-config.xml" );

        MavenArchiveConfiguration config = (MavenArchiveConfiguration) getVariableValueFromObject( mojo, "archive" );

        mojo.execute();

        assertTrue( config.getManifest().isAddExtensions() );

        File warSourceDir = (File) getVariableValueFromObject( mojo, "warSourceDirectory" );

        File manifestDir = new File( warSourceDir, "META-INF" );

        File manifest = new File( manifestDir, "MANIFEST.MF" );

        assertTrue( manifest.exists() );

        String content = FileUtils.fileRead( manifest );

        int idx = content.indexOf( "Specification-Title" );

        assertTrue( idx >= 0 );
        
        idx = content.indexOf( "Custom-Version" );

        assertTrue( idx >= 0);


    }

    public void loadMojo( String pluginXml )
        throws Exception
    {
        testPom = new File( getBasedir(), pluginXml );

        mojo = (WarManifestMojo) lookupMojo( "manifest", testPom );

        assertNotNull( mojo );
    }
}
