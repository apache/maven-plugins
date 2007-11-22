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
import java.util.List;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
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
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "goals.txt" );
        String dirPath = getBasedir() + "/src/test/resources/unit/goals-from-file/";
        List goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 3, goals.size() );
    }
    
    public void testSimpleRunValidate()
        throws Exception
    {
        InvokerMojo invokerMojo = new InvokerMojo();
        setVariableValueToObject( invokerMojo, "goalsFile", "validate-goal.txt" );
        String dirPath = getBasedir() + "/src/test/resources/unit/goals-from-file/";
        List goals = invokerMojo.getGoals( new File( dirPath ) );
        assertEquals( 1, goals.size() );
        setVariableValueToObject( invokerMojo, "projectsDirectory", new File( dirPath ) );
        List pomIncludes = new ArrayList();
        pomIncludes.add( "pom.xml" );
        setVariableValueToObject( invokerMojo, "pomIncludes", pomIncludes );
        setVariableValueToObject( invokerMojo, "invoker", getContainer().lookup( Invoker.ROLE ) );
        File cloneProjectsTo = new File( "target/goals-from-file/" );
        // clean if exists
        if ( cloneProjectsTo.exists() )
        {
            FileUtils.deleteDirectory( cloneProjectsTo );
        }
        //cloneProjectsTo.getParent()
        setVariableValueToObject( invokerMojo, "cloneProjectsTo", cloneProjectsTo );
        setVariableValueToObject( invokerMojo, "postBuildHookScript", "verify.bsh" );
        invokerMojo.execute();
    }
    
}
