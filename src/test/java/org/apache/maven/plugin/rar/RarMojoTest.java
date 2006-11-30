package org.apache.maven.plugin.rar;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class RarMojoTest
    extends AbstractMojoTestCase
{
    public void testRarTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-rar-test/plugin-config.xml" );

        RarMojo mojo = ( RarMojo ) lookupMojo( "rar", testPom );

        assertNotNull( mojo );
    }

    public void testBasicRar()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-rar-test/plugin-config.xml" );

        RarMojo mojo = ( RarMojo ) lookupMojo( "rar", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        Boolean includeJar = ( Boolean ) getVariableValueFromObject(  mojo, "includeJar" );

        assertTrue( includeJar.booleanValue() );

        //include the project jar to the rar
        File projectJar = new File( getBasedir(), "src/test/resources/unit/basic-rar-test/target/test-rar.jar" );

        FileUtils.copyFileToDirectory( projectJar, new File( outputDir ) );

        mojo.execute();

        //check the working directory
        File workDirectory = new File( workDir );

        assertTrue( workDirectory.exists() );

        assertTrue( workDirectory.isDirectory() );

        File[] fileNames = workDirectory.listFiles();

        List expectedFiles = new ArrayList();
        
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "test-rar.jar" );
        
        assertEquals( "Files in working directory", expectedFiles.size(), fileNames.length );

        
        for( int i=0; i<fileNames.length; i++ )
        {
            String fileName = fileNames[i].getName();
            
            assertTrue( expectedFiles.contains( fileName ) );
            
            if( expectedFiles.contains( fileName ) )
            {
                expectedFiles.remove( fileName );
                assertFalse( expectedFiles.contains( fileName ) );
            }
            else
            {
                fail( fileName + " is not included in expected files." );
            }
        }

        assertEquals( 0, expectedFiles.size() );

        //check the generated rar file
        File rarFile = new File( outputDir, finalName + ".rar" );

        assertTrue( rarFile.exists() );

        //expected files/directories inside the rar file
        expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "test-rar.jar" );

        ZipFile rar = new ZipFile( rarFile );

        Enumeration entries = rar.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertEquals( 0, getSizeOfExpectedFiles( entries, expectedFiles ) );
    }

    public void testBasicRarWithDescriptor()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-rar-with-descriptor/plugin-config.xml" );

        RarMojo mojo = ( RarMojo ) lookupMojo( "rar", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();

        //check the working directory
        File workDirectory = new File( workDir );

        assertTrue( workDirectory.exists() );

        assertTrue( workDirectory.isDirectory() );

        File[] fileNames = workDirectory.listFiles();

        List expectedFiles = new ArrayList();
        List fileList = new ArrayList();

        for( int i=0; i<fileNames.length; i++ )
        {
            addFileToList( fileNames[i], fileList );
        }

        expectedFiles.add( "ra.xml" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "META-INF" );

        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );

        //check the generated rar file
        File rarFile = new File( outputDir, finalName + ".rar" );

        assertTrue( rarFile.exists() );

        //expected files/directories inside the rar file
        expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "META-INF/ra.xml" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile rar = new ZipFile( rarFile );

        Enumeration entries = rar.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertEquals( 0, getSizeOfExpectedFiles( entries, expectedFiles ) );
    }

    public void testBasicRarWithManifest()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-rar-with-manifest/plugin-config.xml" );

        RarMojo mojo = ( RarMojo ) lookupMojo( "rar", testPom );

        assertNotNull( mojo );

        String finalName = ( String ) getVariableValueFromObject( mojo, "finalName" );

        String workDir = ( String ) getVariableValueFromObject( mojo, "workDirectory" );

        String outputDir = ( String ) getVariableValueFromObject( mojo, "outputDirectory" );

        mojo.execute();

        //check the working directory
        File workDirectory = new File( workDir );

        assertTrue( workDirectory.exists() );

        assertTrue( workDirectory.isDirectory() );

        File[] fileNames = workDirectory.listFiles();

        List expectedFiles = new ArrayList();
        List fileList = new ArrayList();

        for( int i=0; i<fileNames.length; i++ )
        {
            addFileToList( fileNames[i], fileList );
        }

        expectedFiles.add( "ra.xml" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "META-INF" );
        expectedFiles.add( "MANIFEST.MF" );

        assertEquals( expectedFiles.size(), fileList.size() );

        assertEquals( 0, getSizeOfExpectedFiles( fileList, expectedFiles ) );

        //check the generated rar file
        File rarFile = new File( outputDir, finalName + ".rar" );

        assertTrue( rarFile.exists() );

        //expected files/directories inside the rar file
        expectedFiles = new ArrayList();

        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "META-INF/ra.xml" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );

        ZipFile rar = new ZipFile( rarFile );

        Enumeration entries = rar.getEntries();

        assertTrue( entries.hasMoreElements() );

        assertEquals( 0, getSizeOfExpectedFiles( entries, expectedFiles ) );
    }

    private int getSizeOfExpectedFiles( Enumeration entries, List expectedFiles )
    {
        while( entries.hasMoreElements() )
        {
            ZipEntry entry = ( ZipEntry ) entries.nextElement();

            if( expectedFiles.contains( entry.getName() ) )
            {
                expectedFiles.remove( entry.getName() );
                assertFalse( expectedFiles.contains( entry.getName() ) );
            }
            else
            {
                fail( entry.getName() + " is not included in the expected files" );
            }
        }
        return expectedFiles.size();
    }

    private int getSizeOfExpectedFiles( List fileList, List expectedFiles )
    {
        for( Iterator iter=fileList.iterator(); iter.hasNext(); )
        {
            String fileName = ( String ) iter.next();

            if( expectedFiles.contains(  fileName ) )
            {
                expectedFiles.remove( fileName );
                assertFalse( expectedFiles.contains( fileName ) );
            }
            else
            {
                fail( fileName + " is not included in the expected files" );
            }
        }
        return expectedFiles.size();
    }

    private void addFileToList( File file, List fileList )
    {
        if( !file.isDirectory() )
        {
            fileList.add( file.getName() );
        }
        else
        {
            fileList.add( file.getName() );

            File[] files = file.listFiles();

            for( int i=0; i<files.length; i++ )
            {
                addFileToList( files[i], fileList );
            }
        }
    }
}
