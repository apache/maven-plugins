package org.apache.maven.plugin.dependency.its;

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

/**
 * This class executes the IT tests. The setup will create a pom-test.xml from the plugin pom. The version is changed to
 * "test" and the tests themselves turned off to avoid an infinite loop. The test version of the plugin is then built
 * and installed to a new temporary local repo used to execute the tests. This only occurs once for the suite of tests.
 * Each test below just uses the tools to execute Maven on the named project with the passed in goals.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a> Copied from the Eclipse AbstractEclipsePluginTestCase v2.4
 * @version $Id$
 */
public class DependencyPluginTest
    extends AbstractDependencyPluginITCase
{
    protected void setUp()
        throws Exception
    {
        // super.setUp();
    }

    protected void tearDown()
        throws Exception
    {

    }

    /**
     * Test that build failures are reported. Simple Harness test essentially
     */
    public void testHarness()
    {
        /*
         * try { testProject( "check-harness", "install" ); fail("Expected an exception reporting a build failure
         * here"); } catch ( Exception e ) { //caught expected exceptions }
         */
    }

    /**
     * Test Resolve Mojo. Simple Harness test essentially
     * 
     * @throws Exception any exception thrown during test
     */
    public void testSibling()
        throws Exception
    {
        // testProject( "siblingReference", "compile" );
    }

    /**
     * Test Resolve Mojo. Simple Harness test essentially
     * 
     * @throws Exception any exception thrown during test
     */
    public void testResolve()
        throws Exception
    {
        // testProject( "resolve", "dependency:resolve" );
    }
}
