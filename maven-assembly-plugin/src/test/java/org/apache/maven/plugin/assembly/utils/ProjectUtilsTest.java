package org.apache.maven.plugin.assembly.utils;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

public class ProjectUtilsTest
    extends TestCase
{

    private MavenProject createTestProject( String artifactId, String groupId, String version )
    {
        Model model = new Model();
        model.setArtifactId( artifactId );
        model.setGroupId( groupId );
        model.setVersion( version );

        MavenProject project = new MavenProject( model );

        return project;
    }

    public void testGetProjectModules_ShouldIncludeDirectModuleOfMasterProject()
        throws IOException
    {
        MavenProject master = createTestProject( "test", "testGroup", "1.0" );

        master.setFile( new File( "pom.xml" ) );

        master.getModel().addModule( "module" );

        MavenProject module = createTestProject( "module", "testGroup", "1.0" );

        module.setFile( new File( "module/pom.xml" ) );

        List projects = new ArrayList( 2 );

        projects.add( master );
        projects.add( module );

        Set result =
            ProjectUtils.getProjectModules( master, projects, true, new ConsoleLogger( Logger.LEVEL_INFO, "test" ) );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( module.getId(), ( ( MavenProject ) result.iterator().next() ).getId() );
    }

    public void testGetProjectModules_ShouldNotIncludeMasterProject()
        throws IOException
    {
        MavenProject master = createTestProject( "test", "testGroup", "1.0" );

        Set result =
            ProjectUtils.getProjectModules( master, Collections.singletonList( master ), true,
                                            new ConsoleLogger( Logger.LEVEL_INFO, "test" ) );

        assertNotNull( result );
        assertTrue( result.isEmpty() );
    }

    public void testGetProjectModules_ShouldIncludeInDirectModuleOfMasterWhenIncludeSubModulesIsTrue()
        throws IOException
    {
        MavenProject master = createTestProject( "test", "testGroup", "1.0" );

        master.setFile( new File( "project/pom.xml" ) );

        master.getModel().addModule( "module" );

        MavenProject module = createTestProject( "module", "testGroup", "1.0" );

        module.getModel().addModule( "submodule" );

        module.setFile( new File( "project/module/pom.xml" ) );

        MavenProject subModule = createTestProject( "sub-module", "testGroup", "1.0" );

        subModule.setFile( new File( "project/module/submodule/pom.xml" ) );

        List projects = new ArrayList( 3 );

        projects.add( master );
        projects.add( module );
        projects.add( subModule );

        Set result =
            ProjectUtils.getProjectModules( master, projects, true, new ConsoleLogger( Logger.LEVEL_INFO, "test" ) );

        assertNotNull( result );
        assertEquals( 2, result.size() );

        List verify = new ArrayList( projects );
        verify.remove( master );

        verifyProjectsPresent( verify, result );
    }

    public void testGetProjectModules_ShouldExcludeInDirectModuleOfMasterWhenIncludeSubModulesIsFalse()
        throws IOException
    {
        MavenProject master = createTestProject( "test", "testGroup", "1.0" );

        master.setFile( new File( "project/pom.xml" ) );

        master.getModel().addModule( "module" );

        MavenProject module = createTestProject( "module", "testGroup", "1.0" );

        module.getModel().addModule( "submodule" );

        module.setFile( new File( "project/module/pom.xml" ) );

        MavenProject subModule = createTestProject( "sub-module", "testGroup", "1.0" );

        subModule.setFile( new File( "project/module/submodule/pom.xml" ) );

        List projects = new ArrayList( 3 );

        projects.add( master );
        projects.add( module );
        projects.add( subModule );

        Set result =
            ProjectUtils.getProjectModules( master, projects, false, new ConsoleLogger( Logger.LEVEL_INFO, "test" ) );

        assertNotNull( result );
        assertEquals( 1, result.size() );

        List verify = new ArrayList( projects );
        verify.remove( master );
        verify.remove( subModule );

        verifyProjectsPresent( verify, result );
    }

    public void testGetProjectModules_ShouldExcludeNonModuleOfMasterProject()
        throws IOException
    {
        MavenProject master = createTestProject( "test", "testGroup", "1.0" );

        master.setFile( new File( "project/pom.xml" ) );

        MavenProject other = createTestProject( "other", "testGroup", "1.0" );

        other.setFile( new File( "other/pom.xml" ) );

        List projects = new ArrayList( 3 );

        projects.add( master );
        projects.add( other );

        Set result =
            ProjectUtils.getProjectModules( master, projects, true, new ConsoleLogger( Logger.LEVEL_INFO, "test" ) );

        assertNotNull( result );
        assertTrue( result.isEmpty() );
    }

    private void verifyProjectsPresent( List verify, Set result )
    {
        List verifyCopy = new ArrayList( verify );

        List unexpected = new ArrayList();

        for ( Iterator it = result.iterator(); it.hasNext(); )
        {
            MavenProject project = ( MavenProject ) it.next();

            boolean removed = false;

            for ( Iterator verifyIterator = verifyCopy.iterator(); verifyIterator.hasNext(); )
            {
                MavenProject verification = ( MavenProject ) verifyIterator.next();

                if ( verification.getId().equals( project.getId() ) )
                {
                    verifyIterator.remove();
                    removed = true;
                }
            }

            if ( !removed )
            {
                unexpected.add( project );
            }
        }

        if ( !verifyCopy.isEmpty() )
        {
            fail( "Failed to verify presence of: " + verifyCopy );
        }

        if ( !unexpected.isEmpty() )
        {
            fail( "Found unexpected projects in result: " + unexpected );
        }
    }

    // private void verifyProjectsNotPresent( List verify, Set result )
    // {
    // List verifyCopy = new ArrayList( verify );
    //
    // for ( Iterator it = result.iterator(); it.hasNext(); )
    // {
    // MavenProject project = (MavenProject) it.next();
    //
    // for ( Iterator verifyIterator = verifyCopy.iterator(); verifyIterator.hasNext(); )
    // {
    // MavenProject verification = (MavenProject) verifyIterator.next();
    //
    // if ( verification.getId().equals( project.getId() ) )
    // {
    // verifyIterator.remove();
    // }
    // }
    // }
    //
    // if ( verifyCopy.size() != verify.size() )
    // {
    // List found = new ArrayList( verify );
    // found.removeAll( verifyCopy );
    //
    // fail( "Failed to verify absence of: " + found );
    // }
    // }

}
