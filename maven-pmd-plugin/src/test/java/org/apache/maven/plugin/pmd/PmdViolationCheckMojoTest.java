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

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class PmdViolationCheckMojoTest
    extends AbstractMojoTestCase
{
    /**
     * {@inheritDoc}
     */
    @Override
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
            final File testPom =
                new File( getBasedir(),
                          "src/test/resources/unit/default-configuration/pmd-check-default-configuration-plugin-config.xml" );
            final PmdViolationCheckMojo mojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
            mojo.execute();

            fail( "MojoFailureException should be thrown." );
        }
        catch ( final Exception e )
        {
            assertTrue( true );
        }
    }

    public void testNotFailOnViolation()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        final PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/pmd-check-notfailonviolation-plugin-config.xml" );
        final PmdViolationCheckMojo pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
        pmdViolationMojo.execute();

        assertTrue( true );
    }

    public void testFailurePriority()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        final PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/pmd-check-failonpriority-plugin-config.xml" );
        PmdViolationCheckMojo pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
        pmdViolationMojo.execute();

        testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/pmd-check-failandwarnonpriority-plugin-config.xml" );
        pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
        try
        {
            pmdViolationMojo.execute();
            fail( "Exception Expected" );
        }
        catch ( final MojoFailureException e )
        {
            String message = e.getMessage();
            if ( message.contains( "You have 5 PMD violations and 3 warnings." ) )
            {
                System.out.println( "Caught expected message: " + e.getMessage() );// expected
            }
            else
            {
                throw new AssertionError( "Expected: '" + message
                    + "' to contain 'You have 5 PMD violations and 3 warnings.'" );
            }
        }

    }

    public void testException()
        throws Exception
    {
        try
        {
            final File testPom =
                new File( getBasedir(),
                          "src/test/resources/unit/custom-configuration/pmd-check-exception-test-plugin-config.xml" );
            final PmdViolationCheckMojo mojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
            mojo.execute();

            fail( "MojoFailureException should be thrown." );
        }
        catch ( final Exception e )
        {
            assertTrue( true );
        }

    }

    public void testViolationExclusion()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        final PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/pmd-check-pmd-exclusions-configuration-plugin-config.xml" );
        final PmdViolationCheckMojo pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo( "check", testPom );
        pmdViolationMojo.execute();

        assertTrue( true );
    }
}
