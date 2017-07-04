package org.apache.maven.plugins.help;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Profile;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.IOUtil;

/**
 * Test class for the all-profiles mojo of the Help Plugin.
 */
public class AllProfilesMojoTest
    extends AbstractMojoTestCase
{

    private InterceptingLog interceptingLogger;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        interceptingLogger =
            new InterceptingLog( getContainer().lookup( LoggerManager.class ).getLoggerForComponent( Mojo.ROLE ) );
    }

    /**
     * Tests the case when no profiles are present for the projects.
     * 
     * @throws Exception in case of errors.
     */
    public void testNoProfiles()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/all-profiles/plugin-config.xml" );

        AllProfilesMojo mojo = (AllProfilesMojo) lookupMojo( "all-profiles", testPom );

        setUpMojo( mojo, Arrays.<MavenProject>asList( new MavenProjectStub() ),
                   Collections.<org.apache.maven.settings.Profile>emptyList(), "empty.txt" );

        mojo.execute();

        assertTrue( interceptingLogger.warnLogs.contains( "No profiles detected!" ) );
    }
    
    /**
     * Tests the case when profiles are present in the POM and in a parent POM.
     * 
     * @throws Exception in case of errors.
     */
    public void testProfileFromPom()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/all-profiles/plugin-config.xml" );

        AllProfilesMojo mojo = (AllProfilesMojo) lookupMojo( "all-profiles", testPom );

        MavenProjectStub project = new MavenProjectStub();
        project.getModel().setProfiles( Arrays.asList( newPomProfile( "pro-1", "pom" ), newPomProfile( "pro-2", "pom" ) ) );
        project.setParent( new MavenProjectStub() );
        project.getParent().getModel().setProfiles( Arrays.asList( newPomProfile( "pro-3", "pom" ) ) );
        project.setActiveProfiles( Arrays.asList( newPomProfile( "pro-1", "pom" ) ) );
        
        setUpMojo( mojo, Arrays.<MavenProject>asList( project ),
                   Collections.<org.apache.maven.settings.Profile>emptyList(), "profiles-from-pom.txt" );

        mojo.execute();

        String file = readFile( "profiles-from-pom.txt" );
        assertTrue( file.contains( "Profile Id: pro-1 (Active: true , Source: pom)" ) );
        assertTrue( file.contains( "Profile Id: pro-2 (Active: false , Source: pom)" ) );
        assertTrue( file.contains( "Profile Id: pro-3 (Active: false , Source: pom)" ) );
    }

    /**
     * Tests the case when active profiles are present in the parent POM.
     * 
     * @throws Exception in case of errors.
     */
    public void testProfileFromParentPom()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/all-profiles/plugin-config.xml" );

        AllProfilesMojo mojo = (AllProfilesMojo) lookupMojo( "all-profiles", testPom );

        MavenProjectStub project = new MavenProjectStub();
        project.setParent( new MavenProjectStub() );
        project.getParent().getModel().setProfiles( Arrays.asList( newPomProfile( "pro-1", "pom" ) ) );
        project.getParent().setActiveProfiles( Arrays.asList( newPomProfile( "pro-1", "pom" ) ) );
        
        setUpMojo( mojo, Arrays.<MavenProject>asList( project ),
                   Collections.<org.apache.maven.settings.Profile>emptyList(), "profiles-from-parent-pom.txt" );

        mojo.execute();

        String file = readFile( "profiles-from-parent-pom.txt" );
        assertTrue( file.contains( "Profile Id: pro-1 (Active: true , Source: pom)" ) );
    }
    
    /**
     * Tests the case when profiles are present in the settings.
     * 
     * @throws Exception in case of errors.
     */
    public void testProfileFromSettings()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/all-profiles/plugin-config.xml" );

        AllProfilesMojo mojo = (AllProfilesMojo) lookupMojo( "all-profiles", testPom );

        MavenProject project = new MavenProjectStub();
        project.setActiveProfiles( Arrays.asList( newPomProfile( "settings-1", "settings.xml" ) ) );
        
        List<org.apache.maven.settings.Profile> settingsProfiles = new ArrayList<org.apache.maven.settings.Profile>();
        settingsProfiles.add( newSettingsProfile( "settings-1" ) );
        settingsProfiles.add( newSettingsProfile( "settings-2" ) );
        setUpMojo( mojo, Arrays.<MavenProject>asList( project ), settingsProfiles, "profiles-from-settings.txt" );

        mojo.execute();

        String file = readFile( "profiles-from-settings.txt" );
        assertTrue( file.contains( "Profile Id: settings-1 (Active: true , Source: settings.xml)" ) );
        assertTrue( file.contains( "Profile Id: settings-2 (Active: false , Source: settings.xml)" ) );
    }

    private Profile newPomProfile( String id, String source )
    {
        Profile profile = new Profile();
        profile.setId( id );
        profile.setSource( source );
        return profile;
    }
    
    private org.apache.maven.settings.Profile newSettingsProfile( String id )
    {
        org.apache.maven.settings.Profile profile = new org.apache.maven.settings.Profile();
        profile.setId( id );
        return profile;
    }

    private void setUpMojo( AllProfilesMojo mojo, List<MavenProject> projects,
                            List<org.apache.maven.settings.Profile> settingsProfiles, String output )
        throws IllegalAccessException
    {
        setVariableValueToObject( mojo, "projects", projects );
        setVariableValueToObject( mojo, "settingsProfiles", settingsProfiles );
        setVariableValueToObject( mojo, "output",
                                  new File( getBasedir(), "target/test-classes/unit/active-profiles/" + output ) );
        setVariableValueToObject( mojo, "log", interceptingLogger );
    }

    private String readFile( String path )
        throws FileNotFoundException, IOException
    {
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream( new File( getBasedir(), "target/test-classes/unit/active-profiles/" + path ) );
            return IOUtil.toString( fis );
        }
        finally
        {
            IOUtil.close( fis );
        }
    }
    
    private static final class InterceptingLog
        extends DefaultLog
    {
        List<String> warnLogs = new ArrayList<String>();

        public InterceptingLog( Logger logger )
        {
            super( logger );
        }

        @Override
        public void warn( CharSequence content )
        {
            super.warn( content );
            warnLogs.add( content.toString() );
        }
    }

}
