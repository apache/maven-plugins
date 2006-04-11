package org.apache.maven.plugin.rar;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.maven.plugin.rar.stubs.RarMavenProjectStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;

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
        
        mojo.execute();
        
        //check the working directory
        File workDirectory = new File( workDir );
        
        assertTrue( workDirectory.exists() );
        
        assertTrue( workDirectory.isDirectory() );
        
        File[] fileNames = workDirectory.listFiles();
        
        assertEquals( 3, fileNames.length );
        
        assertEquals( "maven-artifact01-1.0-SNAPSHOT.jar", fileNames[0].getName() );
        
        assertEquals( "maven-artifact02-1.0-SNAPSHOT.jar", fileNames[1].getName() );
        
        //check the generated rar file
        File rarFile = new File( outputDir, finalName + ".rar" );
     
        assertTrue( rarFile.exists() );

        //expected files/directories inside the rar file
        List expectedFiles = new ArrayList();
        
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/pom.properties" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/pom.xml" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/maven-rar-test/" );
        expectedFiles.add( "META-INF/maven/org.apache.maven.test/" );
        expectedFiles.add( "META-INF/maven/" );
        expectedFiles.add( "META-INF/MANIFEST.MF" );
        expectedFiles.add( "META-INF/" );
        expectedFiles.add( "maven-artifact01-1.0-SNAPSHOT.jar" );
        expectedFiles.add( "maven-artifact02-1.0-SNAPSHOT.jar" );
        
        ZipFile rar = new ZipFile( rarFile );
        
        Enumeration entries = rar.getEntries();
        
        assertTrue( entries.hasMoreElements() );
 
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
        assertEquals( 0, expectedFiles.size() );
    }
    
    
}
