package org.apache.maven.plugin.invoker;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.plugin.invoker.model.BuildJob;
import org.apache.maven.settings.Settings;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 18 nov. 07
 * @version $Id$
 */
public class InvokerMojoTest
    extends AbstractMojoTestCase
{

    /**
     * test reading goals from a file
     */
    public void testReadGoalsFromFile()
        throws Exception
    {
        MavenProjectStub project = new MavenProjectStub();
        project.setTestClasspathElements( Collections.EMPTY_LIST );

        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "goals.txt" );
        setVariableValueToObject( invokerMojo, "project", project );
        setVariableValueToObject( invokerMojo, "settings", new Settings() );
        String dirPath = getBasedir() + "/src/test/resources/unit/goals-from-file/";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 3, goals.size() );
    }

    public void testSingleInvokerTest()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "validate-goal.txt" );
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerTest", "*dummy*" );
        BuildJob[] poms = invokerMojo.getBuildJobs();
        System.out.println( Arrays.asList( poms ) );
        assertEquals( 1, poms.length );
    }

    public void testMultiInvokerTest()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "validate-goal.txt" );
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerTest", "*dummy*,*terpolatio*" );
        BuildJob[] poms = invokerMojo.getBuildJobs();
        System.out.println( Arrays.asList( poms ) );
        assertEquals( 2, poms.length );
    }

    public void testFullPatternInvokerTest()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "validate-goal.txt" );
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List<String> goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerTest", "*" );
        BuildJob[] poms = invokerMojo.getBuildJobs();
        System.out.println( Arrays.asList( poms ) );
        assertEquals( 4, poms.length );
    }

    public void testAlreadyCloned()
        throws Exception
    {
        assertFalse( AbstractInvokerMojo.alreadyCloned( "dir", Collections.<String>emptyList() ) );
        assertTrue( AbstractInvokerMojo.alreadyCloned( "dir", Collections.singletonList( "dir" ) ) );
        assertTrue( AbstractInvokerMojo.alreadyCloned( "dir" + File.separator + "sub", Collections.singletonList( "dir" ) ) );
        assertFalse( AbstractInvokerMojo.alreadyCloned( "dirs", Collections.singletonList( "dir" ) ) );
    }    

}
