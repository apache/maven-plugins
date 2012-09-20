package org.apache.maven.plugin.war;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugin.war.stub.ResourceStub;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author Olivier Lamy
 * @since 21 juil. 2008
 * @version $Id$
 */
public class WarExplodedMojoFilteringTest
    extends AbstractWarExplodedMojoTest
{

    protected File getPomFile()
    {
        return new File( getBasedir(), "/target/test-classes/unit/warexplodedmojo/plugin-config.xml" );
    }

    protected File getTestDirectory()
    {
        return new File( getBasedir(), "target/test-classes/unit/warexplodedmojo/test-dir" );
    }
    

    /**
     * @throws Exception
     */
    public void testExplodedWar_WithResourceFiltering()
        throws Exception
    {
        // setup test data
        String testId = "ExplodedWar_WithResourceFiltering";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppDirectory = new File( getTestDirectory(), testId );
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, false );
        File webAppResource = new File( getTestDirectory(), testId + "-test-data/resources" );
        File sampleResource = new File( webAppResource, "custom-setting.cfg" );
        File sampleResourceWDir = new File( webAppResource, "custom-config/custom-setting.cfg" );
        List filterList = new LinkedList();
        ResourceStub[] resources = new ResourceStub[]{new ResourceStub()};

        createFile( sampleResource );
        createFile( sampleResourceWDir );

        String ls = System.getProperty( "line.separator" );
        final String comment = "# this is comment created by author@somewhere@";
        // prepare web resources
        String content = comment + ls;
        content += "system_key_1=${user.dir}" + ls;
        content += "system_key_2=@user.dir@" + ls;
        content += "project_key_1=${is_this_simple}" + ls;
        content += "project_key_2=@is_this_simple@" + ls;
        content += "project_name_1=${project.name}" + ls;
        content += "project_name_2=@project.name@" + ls;
        content += "system_property_1=${system.property}" + ls;
        content += "system_property_2=@system.property@" + ls;
        FileUtils.fileWrite( sampleResourceWDir.getAbsolutePath(), content );
        FileUtils.fileWrite( sampleResource.getAbsolutePath(), content );

        System.setProperty( "system.property", "system-property-value" );

        // configure mojo
        project.addProperty( "is_this_simple", "i_think_so" );
        resources[0].setDirectory( webAppResource.getAbsolutePath() );
        resources[0].setFiltering( true );
        this.configureMojo( mojo, filterList, classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "webResources", resources );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );
        File expectedResourceFile = new File( webAppDirectory, "custom-setting.cfg" );
        File expectedResourceWDirFile = new File( webAppDirectory, "custom-config/custom-setting.cfg" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "resource file not found:" + expectedResourceFile.toString(), expectedResourceFile.exists() );
        assertTrue( "resource file with dir not found:" + expectedResourceWDirFile.toString(),
                    expectedResourceWDirFile.exists() );

        // validate filtered file
        content = FileUtils.fileRead( expectedResourceWDirFile );
        BufferedReader reader = new BufferedReader( new StringReader( content ) );

        assertEquals( "error in filtering using System Properties", comment, reader.readLine() );

        String line = reader.readLine();
        System.out.println( " line " + line );
        System.out.println( " need " + System.getProperty( "user.dir" ) );
        assertEquals( "error in filtering using System properties", "system_key_1=" + System.getProperty( "user.dir" ),
                      line );
        line = reader.readLine();
        System.out.println(" line " + line );
        assertEquals( "error in filtering using System properties", "system_key_2=" + System.getProperty( "user.dir" ),
                      line );

        assertEquals( "error in filtering using project properties", "project_key_1=i_think_so", reader.readLine() );
        assertEquals( "error in filtering using project properties", "project_key_2=i_think_so", reader.readLine() );

        assertEquals( "error in filtering using project properties", "project_name_1=Test Project ", reader.readLine() );
        assertEquals( "error in filtering using project properties", "project_name_2=Test Project ", reader.readLine() );

        assertEquals( "error in filtering using System properties", "system_property_1=system-property-value",
                      reader.readLine() );
        assertEquals( "error in filtering using System properties", "system_property_2=system-property-value",
                reader.readLine() );

        // update property, and generate again
        System.setProperty( "system.property", "new-system-property-value" );

        mojo.execute();

        // validate filtered file
        content = FileUtils.fileRead( expectedResourceWDirFile );
        reader = new BufferedReader( new StringReader( content ) );

        assertEquals( "error in filtering using System Properties", comment, reader.readLine() );

        assertEquals( "error in filtering using System properties", "system_key_1=" + System.getProperty( "user.dir" ),
                      reader.readLine() );
        assertEquals( "error in filtering using System properties", "system_key_2=" + System.getProperty( "user.dir" ),
                reader.readLine() );

        assertEquals( "error in filtering using project properties", "project_key_1=i_think_so", reader.readLine() );
        assertEquals( "error in filtering using project properties", "project_key_2=i_think_so", reader.readLine() );

        assertEquals( "error in filtering using project properties", "project_name_1=Test Project ", reader.readLine() );
        assertEquals( "error in filtering using project properties", "project_name_2=Test Project ", reader.readLine() );

        assertEquals( "error in filtering using System properties", "system_property_1=new-system-property-value",
                      reader.readLine() );
        assertEquals( "error in filtering using System properties", "system_property_2=new-system-property-value",
                reader.readLine() );

        // update property, and generate again
        File filterFile = new File( getTestDirectory(), testId + "-test-data/filters/filter.properties" );
        createFile( filterFile );
        filterList.add( filterFile.getAbsolutePath() );
        content += "resource_key_1=${resource_value_1}\n";
        content += "resource_key_2=@resource_value_2@\n" + content;
        FileUtils.fileWrite( sampleResourceWDir.getAbsolutePath(), content );
        FileUtils.fileWrite( sampleResource.getAbsolutePath(), content );
        String filterContent = "resource_value_1=this_is_filtered\n";
        filterContent += "resource_value_2=this_is_filtered";
        FileUtils.fileWrite( filterFile.getAbsolutePath(), filterContent );

        mojo.execute();

        // validate filtered file
        content = FileUtils.fileRead( expectedResourceWDirFile );
        reader = new BufferedReader( new StringReader( content ) );

        assertEquals( "error in filtering using System Properties", comment, reader.readLine() );

        assertEquals( "error in filtering using System properties", "system_key_1=" + System.getProperty( "user.dir" ),
                      reader.readLine() );
        assertEquals( "error in filtering using System properties", "system_key_2=" + System.getProperty( "user.dir" ),
                reader.readLine() );

        assertEquals( "error in filtering using project properties", "project_key_1=i_think_so", reader.readLine() );
        assertEquals( "error in filtering using project properties", "project_key_2=i_think_so", reader.readLine() );

        assertEquals( "error in filtering using project properties", "project_name_1=Test Project ", reader.readLine() );
        assertEquals( "error in filtering using project properties", "project_name_2=Test Project ", reader.readLine() );

        assertEquals( "error in filtering using System properties", "system_property_1=new-system-property-value",
                      reader.readLine() );
        assertEquals( "error in filtering using System properties", "system_property_2=new-system-property-value",
                reader.readLine() );

        assertEquals( "error in filtering using filter files", "resource_key_1=this_is_filtered", reader.readLine() );
        assertEquals( "error in filtering using filter files", "resource_key_2=this_is_filtered", reader.readLine() );

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedResourceFile.delete();
        expectedResourceWDirFile.delete();
    }    

}
