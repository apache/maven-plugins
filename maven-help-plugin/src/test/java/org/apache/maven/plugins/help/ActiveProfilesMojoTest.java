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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

/**
 * Test class for the active-profiles mojo of the Help Plugin.
 */
public class ActiveProfilesMojoTest
    extends AbstractMojoTestCase
{

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /**
     * Tests that profiles activated in the settings are resolved.
     * 
     * @throws Exception in case of errors.
     */
    public void testActiveProfilesFromSettings()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/active-profiles/plugin-config.xml" );

        ActiveProfilesMojo mojo = (ActiveProfilesMojo) lookupMojo( "active-profiles", testPom );

        MavenProject project = mock( MavenProject.class );
        when( project.getInjectedProfileIds() ).thenReturn( getProfiles( Arrays.asList( "from-settings" ),
                                                                         Collections.<String>emptyList() ) );

        setUpMojo( mojo, Arrays.asList( project ), "from-settings.txt" );

        mojo.execute();

        String file = readFile( "from-settings.txt" );
        assertTrue( file.contains( "from-settings (source: external)" ) );
    }

    /**
     * Tests that profiles activated in the POM are resolved.
     * 
     * @throws Exception in case of errors.
     */
    public void testActiveProfilesFromPom()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/active-profiles/plugin-config.xml" );

        ActiveProfilesMojo mojo = (ActiveProfilesMojo) lookupMojo( "active-profiles", testPom );

        MavenProject project = mock( MavenProject.class );
        when( project.getInjectedProfileIds() ).thenReturn( getProfiles( Collections.<String>emptyList(),
                                                                         Arrays.asList( "from-pom" ) ) );

        setUpMojo( mojo, Arrays.asList( project ), "from-pom.txt" );

        mojo.execute();

        String file = readFile( "from-pom.txt" );
        assertTrue( file.contains( "from-pom (source: org.apache.maven.test:test:1.0)" ) );
    }

    private Map<String, List<String>> getProfiles( List<String> externals, List<String> pom )
    {
        Map<String, List<String>> profiles = new HashMap<String, List<String>>();
        profiles.put( "external", externals ); // from settings
        profiles.put( "org.apache.maven.test:test:1.0", pom ); // from POM
        profiles.put( "", Collections.<String>emptyList() ); // from super POM
        return profiles;
    }

    private void setUpMojo( ActiveProfilesMojo mojo, List<MavenProject> projects, String output )
        throws IllegalAccessException
    {
        setVariableValueToObject( mojo, "projects", projects );
        setVariableValueToObject( mojo, "output",
                                  new File( getBasedir(), "target/test-classes/unit/active-profiles/" + output ) );
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

}
