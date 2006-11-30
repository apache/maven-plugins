package org.apache.maven.plugin.checkstyle;

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

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class CheckstyleViolationCheckMojoTest
    extends AbstractMojoTestCase
{
    public void testDefaultConfig()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/check-plugin-config.xml" );

        Mojo mojo = lookupMojo( "check", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        try
        {
            mojo.execute();

            fail( "Must throw an exception on violations" );
        }
        catch ( MojoFailureException e )
        {
            //expected
        }
    }

    public void testInvalidFormat()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/check-plugin-config.xml" );

        Mojo mojo = lookupMojo( "check", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        setVariableValueToObject( mojo, "outputFileFormat", "plain" );

        try
        {
            mojo.execute();

            fail( "Must throw an exception invalid format: plain" );
        }
        catch ( MojoExecutionException e )
        {
            //expected
        }
    }

    public void testNoOutputFile()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/check-plugin-config.xml" );

        Mojo mojo = lookupMojo( "check", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        setVariableValueToObject( mojo, "outputFile", new File( "target/NoSuchFile.xml" ) );

        mojo.execute();
    }

    public void testNoFail()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/check-plugin-config.xml" );

        Mojo mojo = lookupMojo( "check", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        setVariableValueToObject( mojo, "failOnViolation", Boolean.FALSE );

        mojo.execute();
    }
}
