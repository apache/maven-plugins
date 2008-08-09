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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.invoker.Invoker;
import org.codehaus.plexus.util.FileUtils;

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
        String dirPath = getBasedir() + "/src/test/resources/unit/goals-from-file/";
        List goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 3, goals.size() );
    }

    public void testSimpleRunValidate()
        throws Exception
    {
        MavenProjectStub project = new MavenProjectStub();
        project.setTestClasspathElements( Collections.EMPTY_LIST );

        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "validate-goal.txt" );
        setVariableValueToObject( invokerMojo, "project", project );
        String dirPath = getBasedir() + "/src/test/resources/unit/goals-from-file/";
        List goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        List pomIncludes = new ArrayList();
        pomIncludes.add( "pom.xml" );
        setVariableValueToObject( invokerMojo, "pomIncludes", pomIncludes );
        setVariableValueToObject( invokerMojo, "invoker", getContainer().lookup( Invoker.ROLE ) );
        File cloneProjectsTo = new File( getBasedir(), "target/unit/goals-from-file/" );
        // clean if exists
        if ( cloneProjectsTo.exists() )
        {
            FileUtils.deleteDirectory( cloneProjectsTo );
        }
        //cloneProjectsTo.getParent()
        setVariableValueToObject( invokerMojo, "cloneProjectsTo", cloneProjectsTo );
        setVariableValueToObject( invokerMojo, "postBuildHookScript", "verify.bsh" );
        setVariableValueToObject( invokerMojo, "settings", new Settings() );
        invokerMojo.execute();
    }

    public void testSingleInvokerTest()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "validate-goal.txt" );
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerTest", "*dummy*" );
        String[] poms = invokerMojo.getPoms();
        System.out.println( Arrays.asList( poms ) );
        assertEquals( 1, poms.length );
    }

    public void testMultiInvokerTest()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "validate-goal.txt" );
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerTest", "*dummy*,*terpolatio*" );
        String[] poms = invokerMojo.getPoms();
        System.out.println( Arrays.asList( poms ) );
        assertEquals( 2, poms.length );
    }

    public void testFullPatternInvokerTest()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "validate-goal.txt" );
        String dirPath = getBasedir() + "/src/test/resources/unit";
        List goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "invokerTest", "*" );
        String[] poms = invokerMojo.getPoms();
        System.out.println( Arrays.asList( poms ) );
        assertEquals( 6, poms.length );
    }

    public void testAlreadyCloned()
        throws Exception
    {
        assertFalse( InvokerMojo.alreadyCloned( "dir", Collections.EMPTY_LIST ) );
        assertTrue( InvokerMojo.alreadyCloned( "dir", Collections.singletonList( "dir" ) ) );
        assertTrue( InvokerMojo.alreadyCloned( "dir" + File.separator + "sub", Collections.singletonList( "dir" ) ) );
        assertFalse( InvokerMojo.alreadyCloned( "dirs", Collections.singletonList( "dir" ) ) );
    }    

    public void testProjectCloning()
        throws Exception
    {
        String dirPath = getBasedir() + "/src/test/resources/unit/nested-projects";

        File cloneProjectsTo = new File( getBasedir(), "target/unit/nested-projects" );
        if ( cloneProjectsTo.exists() )
        {
            FileUtils.deleteDirectory( cloneProjectsTo );
        }

        MavenProjectStub project = new MavenProjectStub();
        project.setTestClasspathElements( Collections.EMPTY_LIST );

        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goals", Collections.singletonList( "validate" ) );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "pomIncludes", Collections.singletonList( "**/pom.xml" ) );
        setVariableValueToObject( invokerMojo, "pomExcludes", Collections.singletonList( "pom.xml" ) );
        setVariableValueToObject( invokerMojo, "cloneProjectsTo", cloneProjectsTo );
        setVariableValueToObject( invokerMojo, "project", project );
        setVariableValueToObject( invokerMojo, "settings", new Settings() );
        setVariableValueToObject( invokerMojo, "invoker", getContainer().lookup( Invoker.ROLE ) );

        invokerMojo.execute();

        // NOTE: It is part of the test design that "module" is a prefix of "module-1"
        assertTrue( new File( cloneProjectsTo, "module" ).isDirectory() );
        assertTrue( new File( cloneProjectsTo, "module-1" ).isDirectory() );
        assertTrue( new File( cloneProjectsTo, "module-1/sub-module" ).isDirectory() );
    }

    public void testPomLessMavenInvocation()
        throws Exception
    {
        String dirPath = getBasedir() + "/src/test/resources/unit";

        File cloneProjectsTo = new File( getBasedir(), "target/unit" );
        if ( cloneProjectsTo.exists() )
        {
            FileUtils.deleteDirectory( cloneProjectsTo );
        }

        MavenProjectStub project = new MavenProjectStub();
        project.setTestClasspathElements( Collections.EMPTY_LIST );

        String pomIndependentMojo = "org.apache.maven.plugins:maven-deploy-plugin:2.4:help";

        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goals", Collections.singletonList( pomIndependentMojo ) );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        setVariableValueToObject( invokerMojo, "pomIncludes", Collections.singletonList( "no-pom" ) );
        setVariableValueToObject( invokerMojo, "cloneProjectsTo", cloneProjectsTo );
        setVariableValueToObject( invokerMojo, "project", project );
        setVariableValueToObject( invokerMojo, "settings", new Settings() );
        setVariableValueToObject( invokerMojo, "invoker", getContainer().lookup( Invoker.ROLE ) );

        invokerMojo.execute();

        assertTrue( new File( cloneProjectsTo, "no-pom" ).isDirectory() );
        assertTrue( new File( cloneProjectsTo, "no-pom/build.log" ).isFile() );
    }

}
