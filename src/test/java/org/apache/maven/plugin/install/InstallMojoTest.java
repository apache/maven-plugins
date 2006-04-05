package org.apache.maven.plugin.install;

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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.install.stubs.AttachedArtifactStub0;
import org.apache.maven.plugin.install.stubs.InstallArtifactStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author aramirez
 */

public class InstallMojoTest
    extends AbstractMojoTestCase
{
   
   InstallArtifactStub artifact; 
   
   private final String LOCAL_REPO = "target/local-repo/";
   
   public void setUp()
       throws Exception
   {
       super.setUp();
       
       artifact = new InstallArtifactStub();
       
       String groupId = dotToSlashReplacer( artifact.getGroupId() );
       
       System.out.println( ">>>Cleaning the test artifacts in " + LOCAL_REPO + "..." );
       
       FileUtils.deleteDirectory( LOCAL_REPO + 
                                  groupId + "/" + artifact.getArtifactId() );
   }
   
   public void testInstallTestEnvironment()
       throws Exception
   {
       File testPom = new File( getBasedir(), 
                                "target/test-classes/unit/basic-install-test/plugin-config.xml" );
       
       InstallMojo mojo = ( InstallMojo ) lookupMojo( "install", testPom );
       
       assertNotNull( mojo );
   }
   
   public void testBasicInstall()
       throws Exception
   {
       File testPom = new File( getBasedir(), 
                                "target/test-classes/unit/basic-install-test/plugin-config.xml" );
       
       InstallMojo mojo = ( InstallMojo ) lookupMojo( "install", testPom );
       
       assertNotNull( mojo );

       File file = new File( getBasedir(), 
                             "target/test-classes/unit/basic-install-test/target/" +
                             "maven-install-test-1.0-SNAPSHOT.jar" );
       
       artifact = ( InstallArtifactStub ) getVariableValueFromObject( mojo, "artifact" );
       
       artifact.setFile( file );
       
       mojo.execute();      
       
       String groupId = dotToSlashReplacer( artifact.getGroupId() );
       
       String packaging = getVariableValueFromObject( mojo, "packaging" ).toString();
       
       File installedArtifact = new File( LOCAL_REPO +  
                                          groupId + "/" + artifact.getArtifactId() + "/" +
                                          artifact.getVersion() + "/" + artifact.getArtifactId() + "-" +
                                          artifact.getVersion() + "." + packaging );

       assertTrue( installedArtifact.exists() );
   }
 
   public void testBasicInstallWithAttachedArtifacts()
       throws Exception
   {
       File testPom = new File( getBasedir(), 
                                "target/test-classes/unit/basic-install-test-with-attached-artifacts/" +
                                "plugin-config.xml" );
       
       InstallMojo mojo = ( InstallMojo ) lookupMojo( "install", testPom );
       
       assertNotNull( mojo );

       List attachedArtifacts = ( ArrayList ) getVariableValueFromObject( mojo, "attachedArtifacts" );

       mojo.execute();

       String packaging = getVariableValueFromObject( mojo, "packaging" ).toString();
       
       String groupId = "";
       
       for( Iterator iter=attachedArtifacts.iterator(); iter.hasNext(); )
       {
           AttachedArtifactStub0 attachedArtifact = ( AttachedArtifactStub0 ) iter.next();
       
           groupId = dotToSlashReplacer( attachedArtifact.getGroupId() );                      
           
           File installedArtifact = new File( LOCAL_REPO + 
                                              groupId + "/" + attachedArtifact.getArtifactId() + "/" +
                                              attachedArtifact.getVersion() + "/" + attachedArtifact.getArtifactId() + "-" +
                                              attachedArtifact.getVersion() + "." + packaging );
        
           assertTrue( installedArtifact.exists() );
       }
   }
        
   public void testUpdateReleaseParamSetToTrue()
       throws Exception
   {
       File testPom = new File( getBasedir(), 
                                "target/test-classes/unit/configured-install-test/plugin-config.xml" );
       
       InstallMojo mojo = ( InstallMojo ) lookupMojo( "install", testPom );
       
       assertNotNull( mojo );

       File file = new File( getBasedir(), 
                           "target/test-classes/unit/configured-install-test/target/" +
                           "maven-install-test-1.0-SNAPSHOT.jar" );
       
       artifact = ( InstallArtifactStub ) getVariableValueFromObject( mojo, "artifact" );
       
       artifact.setFile( file );

       mojo.execute();

       assertTrue( artifact.isRelease() );       
   }
  
   public void testInstallIfArtifactFileIsNull()
       throws Exception
   {
       File testPom = new File( getBasedir(), 
                                "target/test-classes/unit/basic-install-test/plugin-config.xml" );
       
       InstallMojo mojo = ( InstallMojo ) lookupMojo( "install", testPom );
       
       assertNotNull( mojo );

       artifact = ( InstallArtifactStub ) getVariableValueFromObject( mojo, "artifact" );
       
       artifact.setFile( null );       
       
       assertNull( artifact.getFile() );
       
       try
       {
           mojo.execute();
           
           fail( "Did not throw mojo execution exception" );
       }
       catch( MojoExecutionException e )
       {
           //expected
       }
   }
   
   public void testInstallIfPackagingIsPom()
       throws Exception
   {
       File testPom = new File( getBasedir(), 
                                "target/test-classes/unit/basic-install-test-packaging-pom/" +
                                "plugin-config.xml" );
       
       InstallMojo mojo = ( InstallMojo ) lookupMojo( "install", testPom );
       
       assertNotNull( mojo );
       
       String packaging = ( String ) getVariableValueFromObject( mojo, "packaging" );
       
       assertEquals( "pom", packaging );
       
       artifact = ( InstallArtifactStub ) getVariableValueFromObject( mojo, "artifact" );
       
       mojo.execute();
       
       String groupId = dotToSlashReplacer( artifact.getGroupId() );
       
       File installedArtifact = new File( LOCAL_REPO + 
                                          groupId + "/" + artifact.getArtifactId() + "/" +
                                          artifact.getVersion() + "/" + artifact.getArtifactId() + "-" +
                                          artifact.getVersion() + "." + "jar" );
    
       assertTrue( installedArtifact.exists() );
   }
   
   private String dotToSlashReplacer( String parameter )
   {
       return parameter.replace( '.', '/' );
   }
}
