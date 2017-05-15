package org.apache.maven.plugins.invoker;

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
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.plugins.invoker.CompositeMap;
import org.apache.maven.plugins.invoker.InvokerMojo;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * @author Olivier Lamy
 * @since 22 nov. 07
 * @version $Id: InterpolationTest.java 1783819 2017-02-21 02:30:23Z schulte $
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
        Properties properties = new Properties();
        properties.put( "fooOnProject", "barOnProject" );
        project.setProperties( properties );
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
        CompositeMap compositeMap = new CompositeMap( buildMavenProjectStub(), (Map) properties, false );
        assertEquals( "1.0-SNAPSHOT", compositeMap.get( "pom.version" ) );
        assertEquals( "bar", compositeMap.get( "foo" ) );
        assertEquals( "bar", compositeMap.get( "pom.groupId" ) );
        assertEquals( "http://blabla", compositeMap.get( "pom.scm.connection" ) );
        assertEquals( "barOnProject", compositeMap.get( "fooOnProject" ) );
    }

    public void testPomInterpolation()
        throws Exception
    {
        Reader reader = null;
        File interpolatedPomFile;
        try
        {
            InvokerMojo invokerMojo = new InvokerMojo();
            setVariableValueToObject( invokerMojo, "project", buildMavenProjectStub() );
            setVariableValueToObject( invokerMojo, "settings", new Settings() );
            Properties properties = new Properties();
            properties.put( "foo", "bar" );
            properties.put( "version", "2.0-SNAPSHOT" );
            setVariableValueToObject( invokerMojo, "filterProperties", properties );
            String dirPath = getBasedir() + File.separatorChar + "src" + File.separatorChar + "test"
                + File.separatorChar + "resources" + File.separatorChar + "unit" + File.separatorChar + "interpolation";

            interpolatedPomFile = new File( getBasedir(), "target/interpolated-pom.xml" );
            invokerMojo.buildInterpolatedFile( new File( dirPath, "pom.xml" ), interpolatedPomFile );
            reader = ReaderFactory.newXmlReader( interpolatedPomFile );
            String content = IOUtil.toString( reader );
            assertTrue( content.indexOf( "<interpolateValue>bar</interpolateValue>" ) > 0 );
            reader.close();
            reader = null;
            // recreate it to test delete if exists before creation
            invokerMojo.buildInterpolatedFile( new File( dirPath, "pom.xml" ), interpolatedPomFile );
            reader = ReaderFactory.newXmlReader( interpolatedPomFile );
            content = IOUtil.toString( reader );
            assertTrue( content.indexOf( "<interpolateValue>bar</interpolateValue>" ) > 0 );
            reader.close();
            reader = null;
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    public void testProfilesWithNoFile()
        throws Exception
    {

        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "profiles", Arrays.asList( "zloug" ) );
        setVariableValueToObject( invokerMojo, "settings", new Settings() );
        String dirPath = getBasedir() + File.separatorChar + "src" + File.separatorChar + "test" + File.separatorChar
            + "resources" + File.separatorChar + "unit" + File.separatorChar + "profiles-from-file";
        List<String> profiles = invokerMojo.getProfiles( new File( dirPath ) );
        assertTrue( profiles.contains( "zloug" ) );
        assertEquals( 1, profiles.size() );

    }
}
