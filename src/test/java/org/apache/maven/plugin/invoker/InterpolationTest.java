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
package org.apache.maven.plugin.invoker;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.IOUtil;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 22 nov. 07
 * @version $Id$
 */
public class InterpolationTest
    extends AbstractMojoTestCase
{
    
    protected MavenProjectStub buildMavenProjectStub()
    {
        ExtendedMavenProjectStub project = new ExtendedMavenProjectStub();
        project.setVersion( "1.0-SNAPSHOT" );
        project.setArtifactId( "foo" );
        project.setGroupId( "bar" );
        Scm scm = new Scm();
        scm.setConnection( "http://blabla" );
        project.setScm( scm );
        return project;
    }
    
    public void testCompositeMap()
        throws Exception
    {

        Properties properties = new Properties();
        properties.put( "foo", "bar" );
        properties.put( "version", "2.0-SNAPSHOT" );
        CompositeMap compositeMap = new CompositeMap( buildMavenProjectStub(), properties );
        assertEquals( "1.0-SNAPSHOT", compositeMap.get( "pom.version" ) );
        assertEquals( "bar", compositeMap.get( "foo" ) );
        assertEquals( "bar", compositeMap.get( "pom.groupId" ) );
        assertEquals( "http://blabla", compositeMap.get( "pom.scm.connection" ) );
    }
    
    public void testInterpolationGoalsFile()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "goals.txt" );
        setVariableValueToObject( invokerMojo, "project", buildMavenProjectStub() );
        Properties properties = new Properties();
        properties.put( "cleanProps", "clean" );
        properties.put( "version", "2.0-SNAPSHOT" );
        setVariableValueToObject( invokerMojo, "interpolationsProperties", properties );
        String dirPath = getBasedir() + File.separatorChar + "src" + File.separatorChar + "test"
        + File.separatorChar + "resources" + File.separatorChar + "unit" + File.separatorChar + "interpolation";
        List goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( goals.toString(), 2, goals.size() );
        assertEquals( "clean", goals.get( 0 ) );
        assertEquals( "bar:foo:1.0-SNAPSHOT:mygoal", goals.get( 1 ) );
    }
    
    public void testPomInterpolation()
        throws Exception
    {
        FileReader fileReader = null;
        File interpolatedPomFile = null;
        try
        {
            InvokerMojo invokerMojo = new InvokerMojo();
            setVariableValueToObject( invokerMojo, "goalsFile", "goals.txt" );
            setVariableValueToObject( invokerMojo, "project", buildMavenProjectStub() );
            Properties properties = new Properties();
            properties.put( "foo", "bar" );
            properties.put( "version", "2.0-SNAPSHOT" );
            setVariableValueToObject( invokerMojo, "interpolationsProperties", properties );
            String dirPath = getBasedir() + File.separatorChar + "src" + File.separatorChar + "test"
                + File.separatorChar + "resources" + File.separatorChar + "unit" + File.separatorChar + "interpolation";
            interpolatedPomFile = invokerMojo.buildInterpolatedPomFile( new File( dirPath, "pom.xml" ),
                                                                        new File( getBasedir() + File.separatorChar
                                                                            + "target" ) );
            fileReader = new FileReader( interpolatedPomFile );
            String content = IOUtil.toString( fileReader );
            assertTrue( content.indexOf( "<interpolateValue>bar</interpolateValue>" ) > 0 );
        }
        finally
        {
            if ( fileReader != null )
            {
                fileReader.close();
            }
        }
    }
    
    public void testProfilesFromFile()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "profilesFile", "profiles.txt" );
        String dirPath = getBasedir() + File.separatorChar + "src" + File.separatorChar + "test" + File.separatorChar
            + "resources" + File.separatorChar + "unit" + File.separatorChar + "profiles-from-file";
        List profiles = invokerMojo.getProfiles( new File( dirPath ) );
        assertEquals( 2, profiles.size() );
        assertTrue( profiles.contains( "foo" ) );
    }
    
    public void testEmptyProfilesFromFile()
        throws Exception
    {

        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "profiles", Arrays.asList( new String[] { "zloug" } ) );
        setVariableValueToObject( invokerMojo, "profilesFile", "emptyProfiles.txt" );
        String dirPath = getBasedir() + File.separatorChar + "src" + File.separatorChar + "test" + File.separatorChar
            + "resources" + File.separatorChar + "unit" + File.separatorChar + "profiles-from-file";
        List profiles = invokerMojo.getProfiles( new File( dirPath ) );
        assertFalse( profiles.contains( "zloug" ) );
        assertEquals( 0, profiles.size() );

    }    
    
    public void testProfilesWithNoFile()
        throws Exception
    {

        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "profiles", Arrays.asList( new String[] { "zloug" } ) );
        setVariableValueToObject( invokerMojo, "profilesFile", "zorglubProfiles.txt" );
        String dirPath = getBasedir() + File.separatorChar + "src" + File.separatorChar + "test" + File.separatorChar
            + "resources" + File.separatorChar + "unit" + File.separatorChar + "profiles-from-file";
        List profiles = invokerMojo.getProfiles( new File( dirPath ) );
        assertTrue( profiles.contains( "zloug" ) );
        assertEquals( 1, profiles.size() );

    }     
}
