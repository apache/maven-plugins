package org.apache.maven.plugin.install;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.install.stubs.InstallArtifactStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 * @author aramirez
 */

public class InstallMojoTest
    extends AbstractMojoTestCase
{
   
   InstallArtifactStub artifact; 
   
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
                             "target/test-classes/unit/basic-install-test/target/maven-install-test-1.0-SNAPSHOT.jar" );
       
       artifact = ( InstallArtifactStub ) getVariableValueFromObject( mojo, "artifact" );
       
       artifact.setFile( file );
       
       mojo.execute();      
       
       String groupId = artifact.getGroupId().replace( '.', '/' );
       
       String packaging = getVariableValueFromObject( mojo, "packaging" ).toString();
       
       File installedArtifact = new File( System.getProperty( "localRepository" ) + "/" + 
                                          groupId + "/" + artifact.getArtifactId() + "/" +
                                          artifact.getVersion() + "/" + artifact.getArtifactId() + "-" +
                                          artifact.getVersion() + "." + packaging );

       assertTrue( installedArtifact.exists() );
   }
 /*  
   public void testBasicInstallWithAttachedArtifacts()
       throws Exception
   {
       File testPom = new File( getBasedir(), 
                                "target/test-classes/unit/basic-install-test-with-attached-artifacts/plugin-config.xml" );
       
       InstallMojo mojo = ( InstallMojo ) lookupMojo( "install", testPom );
       
       assertNotNull( mojo );
    
       mojo.execute();
    
       artifact = new InstallArtifactStub();
       
       String groupId = artifact.getGroupId().replace( '.', '/' );
       
       String packaging = getVariableValueFromObject( mojo, "packaging" ).toString();
       
       File installedArtifact = new File( System.getProperty( "localRepository" ) + "/" + 
                                          groupId + "/" + artifact.getArtifactId() + "/" +
                                          artifact.getVersion() + "/" + artifact.getArtifactId() + "-" +
                                          artifact.getVersion() + "." + packaging );
    
       assertTrue( installedArtifact.exists() );
   }
     */     
   public void testUpdateReleaseParamSetToTrue()
       throws Exception
   {
       File testPom = new File( getBasedir(), 
                                "target/test-classes/unit/configured-install-test/plugin-config.xml" );
       
       InstallMojo mojo = ( InstallMojo ) lookupMojo( "install", testPom );
       
       assertNotNull( mojo );

       artifact = ( InstallArtifactStub ) getVariableValueFromObject( mojo, "artifact" );

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
}
