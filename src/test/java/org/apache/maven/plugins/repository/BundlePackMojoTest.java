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
import java.net.URL;
import java.util.Enumeration;

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;

/**
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class BundlePackMojoTest
    extends AbstractMojoTestCase
{

    public void testPack()
        throws Exception
    {

        File testPom = new File( getBasedir(), "src/test/resources/unit/bundle-pack/pom.xml" );

        BundlePackMojo mojo = (BundlePackMojo) lookupMojo( "bundle-pack", testPom );
        URL repoURL = new File( getBasedir(), "src/test/resources/repo" ).toURL();
        mojo.localRepository = new DefaultArtifactRepository( "test", repoURL.toString(), new DefaultRepositoryLayout() );

        File generatedFilesDir = new File( getBasedir(), "target/bundle-pack-tests" );
        mojo.basedir = generatedFilesDir.getAbsolutePath();
        mojo.execute();

        ZipFile jar = new ZipFile( new File( generatedFilesDir, "testartifact-1.0-bundle.jar" ) );
        Enumeration entries = jar.getEntries();
        assertTrue( entries.hasMoreElements() );

        if ( entries.hasMoreElements() )
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "testartifact-1.0-javadoc.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "testartifact-1.0-sources.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "testartifact-1.0.jar" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "pom.xml" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/MANIFEST.MF" );

            entry = (ZipEntry) entries.nextElement();
            assertEquals( entry.getName(), "META-INF/" );
        }

    }

}
