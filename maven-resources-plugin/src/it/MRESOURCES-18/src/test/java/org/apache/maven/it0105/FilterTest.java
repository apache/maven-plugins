package org.apache.maven.it0105;

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


import junit.framework.TestCase;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FilterTest
    extends TestCase
{
    private String basedir;

    private Properties properties;

    protected void setUp()
        throws Exception
    {
        basedir = System.getProperty( "basedir" );

        properties = new Properties();

        File testPropertiesFile = new File( basedir, "target/classes/test.properties" );

        assertTrue( testPropertiesFile.exists() );

        properties.load( new FileInputStream( testPropertiesFile ) );
    }
    
    public void testSystemPropertyInterpolation()
        throws IOException
    {
        assertEquals( "System property", System.getProperty( "java.version" ), properties.getProperty( "system.property" ) );
    }    

    public void testCommandLineParameterInterpolation()
        throws IOException
    {
        assertEquals( "CLI Parameter", "CLI", properties.getProperty( "cli.param" ) );
    }    

    public void testPomPropertyInterpolation()
        throws IOException
    {
        assertEquals( "Pom Property", "foo", properties.getProperty( "pom.property" ) );
    }    

}
