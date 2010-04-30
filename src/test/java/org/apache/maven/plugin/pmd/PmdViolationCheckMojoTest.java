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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class PmdViolationCheckMojoTest
    extends AbstractMojoTestCase
{
    /** {@inheritDoc} */
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
            PmdViolationCheckMojo mojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
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
                                 "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        testPom = new File( getBasedir(),
                            "src/test/resources/unit/default-configuration/pmd-check-notfailonviolation-plugin-config.xml" );
        PmdViolationCheckMojo pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
        pmdViolationMojo.execute();

        assertTrue( true );
    }

    public void testFailurePriority()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/default-configuration/pmd-check-failonpriority-plugin-config.xml" );
        PmdViolationCheckMojo pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
        pmdViolationMojo.execute();

        testPom = new File( getBasedir(),
                            "src/test/resources/unit/default-configuration/pmd-check-failandwarnonpriority-plugin-config.xml" );
        pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
        try
        {
            pmdViolationMojo.execute();
            fail( "Exception Expected" );
        }
        catch ( MojoFailureException e )
        {
            System.out.println( "Caught Expected Message: " + e.getMessage() );// expected
        }

    }

    public void testException()
        throws Exception
    {
        try
        {
            File testPom = new File( getBasedir(),
                                     "src/test/resources/unit/custom-configuration/pmd-check-exception-test-plugin-config.xml" );
            PmdViolationCheckMojo mojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
            mojo.execute();

            fail( "MojoFailureException should be thrown." );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }

    }
}
