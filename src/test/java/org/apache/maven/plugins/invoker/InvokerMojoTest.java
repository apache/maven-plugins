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
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.invoker.model.BuildJob;

/**
 * @author Olivier Lamy
 * @since 18 nov. 07
 * @version $Id: InvokerMojoTest.java 1731726 2016-02-22 19:34:37Z khmarbaise $
 */
public class InvokerMojoTest
    extends AbstractMojoTestCase
{

    public void testSingleInvokerTest()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerTest", "*dummy*" );
        BuildJob[] poms = invokerMojo.getBuildJobs();
        assertEquals( 1, poms.length );
    }

    public void testMultiInvokerTest()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerTest", "*dummy*,*terpolatio*" );
        BuildJob[] poms = invokerMojo.getBuildJobs();
        assertEquals( 2, poms.length );
    }

    public void testFullPatternInvokerTest()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerTest", "*" );
        BuildJob[] poms = invokerMojo.getBuildJobs();
        assertEquals( 4, poms.length );
    }

    public void testAlreadyCloned()
        throws Exception
    {
        assertFalse( AbstractInvokerMojo.alreadyCloned( "dir", Collections.<String>emptyList() ) );
        assertTrue( AbstractInvokerMojo.alreadyCloned( "dir", Collections.singletonList( "dir" ) ) );
        assertTrue( AbstractInvokerMojo.alreadyCloned( "dir" + File.separator + "sub",
                                                       Collections.singletonList( "dir" ) ) );
        assertFalse( AbstractInvokerMojo.alreadyCloned( "dirs", Collections.singletonList( "dir" ) ) );
    }

}
