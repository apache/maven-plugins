package org.apache.maven.plugin.pmd;

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


/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class CpdViolationCheckMojoTest
    extends AbstractMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void testDefaultConfiguration()
        throws Exception
    {
        try
        {
            File testPom = new File( getBasedir(),
                                     "src/test/resources/unit/default-configuration/pmd-check-default-configuration-plugin-config.xml" );
            CpdViolationCheckMojo mojo = (CpdViolationCheckMojo) lookupMojo( "cpd-check", testPom );
            mojo.execute();

            fail( "MojoFailureException should be thrown." );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
    }

    public void testNotFailOnViolation()
        throws Exception
    {

        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/default-configuration/cpd-default-configuration-plugin-config.xml" );
        CpdReport mojo = (CpdReport) lookupMojo( "cpd", testPom );
        mojo.execute();

        testPom = new File( getBasedir(),
                            "src/test/resources/unit/default-configuration/pmd-check-notfailonviolation-plugin-config.xml" );
        CpdViolationCheckMojo cpdViolationMojo = (CpdViolationCheckMojo) lookupMojo( "cpd-check", testPom );
        cpdViolationMojo.execute();

        assertTrue( true );
    }

    public void testException()
        throws Exception
    {
        try
        {
            File testPom = new File( getBasedir(),
                                     "src/test/resources/unit/custom-configuration/pmd-check-exception-test-plugin-config.xml" );
            CpdViolationCheckMojo mojo = (CpdViolationCheckMojo) lookupMojo( "cpd-check", testPom );
            mojo.execute();

            fail( "MojoFailureException should be thrown." );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
    }

    protected void tearDown()
        throws Exception
    {

    }
}
