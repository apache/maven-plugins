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

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.repository.testutil.TestInputHandler;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.components.interactivity.InputHandler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

/**
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class BundlePackMojoTest
    extends AbstractMojoTestCase
{
    public void testPack_PomPackaging()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/bundle-pack-parent/pom.xml" );

        BundlePackMojo mojo = (BundlePackMojo) lookupMojo( "bundle-pack", testPom );
        URL repoURL = new File( getBasedir(), "src/test/resources/repo" ).toURL();
        mojo.localRepository =
            new DefaultArtifactRepository( "test", repoURL.toString(), new DefaultRepositoryLayout() );

        File generatedFilesDir = new File( getBasedir(), "target/bundle-pack-parent-tests" );
        mojo.basedir = generatedFilesDir.getAbsolutePath();
        mojo.execute();

        File bundleSource = new File( generatedFilesDir, "testparent-1.0-bundle.jar" );
        Set entryNames = new HashSet();
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        assertZipContents( entryNames, Collections.EMPTY_SET, bundleSource );
    }

    public void testPack()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/bundle-pack/pom.xml" );

        BundlePackMojo mojo = (BundlePackMojo) lookupMojo( "bundle-pack", testPom );
        URL repoURL = new File( getBasedir(), "src/test/resources/repo" ).toURL();
        mojo.localRepository =
            new DefaultArtifactRepository( "test", repoURL.toString(), new DefaultRepositoryLayout() );

        File generatedFilesDir = new File( getBasedir(), "target/bundle-pack-tests" );
        mojo.basedir = generatedFilesDir.getAbsolutePath();
        mojo.execute();

        File bundleSource = new File( generatedFilesDir, "testartifact-1.0-bundle.jar" );
        Set entryNames = new HashSet();
        entryNames.add( "testartifact-1.0-javadoc.jar" );
        entryNames.add( "testartifact-1.0-sources.jar" );
        entryNames.add( "testartifact-1.0.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        assertZipContents( entryNames, Collections.EMPTY_SET, bundleSource );
    }

    public void testPack_RemoveOne()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/bundle-pack/pom.xml" );

        BundlePackMojo mojo = (BundlePackMojo) lookupMojo( "bundle-pack", testPom );
        URL repoURL = new File( getBasedir(), "src/test/resources/repo" ).toURL();
        mojo.localRepository =
            new DefaultArtifactRepository( "test", repoURL.toString(), new DefaultRepositoryLayout() );

        // NOTE: This is sensitive to the lookupMojo method timing...
        TestInputHandler ih = (TestInputHandler) lookup( InputHandler.ROLE, "default" );

        Stack responses = new Stack();
        responses.push( "3" );
        ih.setLineResponses( responses );

        File generatedFilesDir = new File( getBasedir(), "target/bundle-pack-tests" );
        mojo.basedir = generatedFilesDir.getAbsolutePath();
        mojo.execute();

        File bundleSource = new File( generatedFilesDir, "testartifact-1.0-bundle.jar" );
        Set entryNames = new HashSet();
        entryNames.add( "testartifact-1.0-javadoc.jar" );
        entryNames.add( "testartifact-1.0.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        Set bannedNames = new HashSet();
        // determined experimentally, so this could change!
        bannedNames.add( "testartifact-1.0-sources.jar" );

        assertZipContents( entryNames, bannedNames, bundleSource );
    }

    public void testPack_RemoveTwo()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/bundle-pack/pom.xml" );

        BundlePackMojo mojo = (BundlePackMojo) lookupMojo( "bundle-pack", testPom );
        URL repoURL = new File( getBasedir(), "src/test/resources/repo" ).toURL();
        mojo.localRepository =
            new DefaultArtifactRepository( "test", repoURL.toString(), new DefaultRepositoryLayout() );

        // NOTE: This is sensitive to the lookupMojo method timing...
        TestInputHandler ih = (TestInputHandler) lookup( InputHandler.ROLE, "default" );

        Stack responses = new Stack();
        responses.push( "2,3" );
        ih.setLineResponses( responses );

        File generatedFilesDir = new File( getBasedir(), "target/bundle-pack-tests" );
        mojo.basedir = generatedFilesDir.getAbsolutePath();
        mojo.execute();

        File bundleSource = new File( generatedFilesDir, "testartifact-1.0-bundle.jar" );
        Set entryNames = new HashSet();
        entryNames.add( "testartifact-1.0.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        Set bannedNames = new HashSet();
        // determined experimentally, so this could change!
        bannedNames.add( "testartifact-1.0-sources.jar" );
        bannedNames.add( "testartifact-1.0-javadoc.jar" );

        assertZipContents( entryNames, bannedNames, bundleSource );
    }

    public void testPack_RemoveTwoUnordered()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/bundle-pack/pom.xml" );

        BundlePackMojo mojo = (BundlePackMojo) lookupMojo( "bundle-pack", testPom );
        URL repoURL = new File( getBasedir(), "src/test/resources/repo" ).toURL();
        mojo.localRepository =
            new DefaultArtifactRepository( "test", repoURL.toString(), new DefaultRepositoryLayout() );

        // NOTE: This is sensitive to the lookupMojo method timing...
        TestInputHandler ih = (TestInputHandler) lookup( InputHandler.ROLE, "default" );

        Stack responses = new Stack();
        responses.push( "3,2" );
        ih.setLineResponses( responses );

        File generatedFilesDir = new File( getBasedir(), "target/bundle-pack-tests" );
        mojo.basedir = generatedFilesDir.getAbsolutePath();
        mojo.execute();

        File bundleSource = new File( generatedFilesDir, "testartifact-1.0-bundle.jar" );
        Set entryNames = new HashSet();
        entryNames.add( "testartifact-1.0.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        Set bannedNames = new HashSet();
        // determined experimentally, so this could change!
        bannedNames.add( "testartifact-1.0-sources.jar" );
        bannedNames.add( "testartifact-1.0-javadoc.jar" );

        assertZipContents( entryNames, bannedNames, bundleSource );
    }

    public void testPack_RemoveTwoWithSpace()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/bundle-pack/pom.xml" );

        BundlePackMojo mojo = (BundlePackMojo) lookupMojo( "bundle-pack", testPom );
        URL repoURL = new File( getBasedir(), "src/test/resources/repo" ).toURL();
        mojo.localRepository =
            new DefaultArtifactRepository( "test", repoURL.toString(), new DefaultRepositoryLayout() );

        // NOTE: This is sensitive to the lookupMojo method timing...
        TestInputHandler ih = (TestInputHandler) lookup( InputHandler.ROLE, "default" );

        Stack responses = new Stack();
        responses.push( "2, 3" );
        ih.setLineResponses( responses );

        File generatedFilesDir = new File( getBasedir(), "target/bundle-pack-tests" );
        mojo.basedir = generatedFilesDir.getAbsolutePath();
        mojo.execute();

        File bundleSource = new File( generatedFilesDir, "testartifact-1.0-bundle.jar" );
        Set entryNames = new HashSet();
        entryNames.add( "testartifact-1.0.jar" );
        entryNames.add( "pom.xml" );
        entryNames.add( "META-INF/MANIFEST.MF" );
        entryNames.add( "META-INF/" );

        Set bannedNames = new HashSet();
        // determined experimentally, so this could change!
        bannedNames.add( "testartifact-1.0-sources.jar" );
        bannedNames.add( "testartifact-1.0-javadoc.jar" );

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

}
