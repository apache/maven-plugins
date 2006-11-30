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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

public class PropertyUtilsTest
    extends AbstractMojoTestCase
{
    private static File testDirectory = new File( getBasedir(), "target/test-classes/unit/propertyutil-test" );

    public void testBasic()
        throws Exception
    {
        File basicProp = new File( testDirectory, "basic.properties" );

        if ( basicProp.exists() )
        {
            basicProp.delete();
        }

        basicProp.createNewFile();
        FileWriter writer = new FileWriter( basicProp );

        writer.write( "ghost=${non_existent}\n" );
        writer.write( "key=${untat_na_damgo}\n" );
        writer.write( "untat_na_damgo=gani_man\n" );
        writer.flush();
        writer.close();

        Properties prop = PropertyUtils.loadPropertyFile( basicProp, false, false );
        assertTrue( prop.getProperty( "key" ).equals( "gani_man" ) );
        assertTrue( prop.getProperty( "ghost" ).equals( "${non_existent}" ) );
    }

    public void testSystemProperties()
        throws Exception
    {
        File systemProp = new File( testDirectory, "system.properties" );

        if ( systemProp.exists() )
        {
            systemProp.delete();
        }

        systemProp.createNewFile();
        FileWriter writer = new FileWriter( systemProp );

        writer.write( "key=${user.dir}" );
        writer.flush();
        writer.close();

        Properties prop = PropertyUtils.loadPropertyFile( systemProp, false, true );
        assertTrue( prop.getProperty( "key" ).equals( System.getProperty( "user.dir" ) ) );
    }

    public void testException()
        throws Exception
    {
        File nonExistent = new File( testDirectory, "not_existent_file" );

        assertFalse( "property file exist: " + nonExistent.toString(), nonExistent.exists() );

        try
        {
            PropertyUtils.loadPropertyFile( nonExistent, true, false );
            assertTrue( "Exception failed", false );
        }
        catch ( Exception ex )
        {
            // exception ok
        }
    }
}
