/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.maven.plugin.eclipse.reader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import junit.framework.TestCase;

import org.apache.maven.plugin.eclipse.TempEclipseWorkspace;
import org.apache.maven.plugin.eclipse.WorkspaceConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.tools.easymock.MockManager;
import org.easymock.MockControl;

/**
 * @author <a href="mailto:baerrach@apache.org">Barrie Treloar</a>
 * @version $Id$
 */
public class ReadWorkspaceLocationsTest
    extends TestCase
{

    private static final File PROJECTS_DIRECTORY = new File( "target/test-classes/eclipse" );

    private static final File DYNAMIC_WORKSPACE_DIRECTORY = new File( PROJECTS_DIRECTORY, "dynamicWorkspace" );

    private static final File WORKSPACE_DIRECTORY = new File( DYNAMIC_WORKSPACE_DIRECTORY, "workspace" );

    private static final File WORKSPACE_PROJECT_METADATA_DIRECTORY =
        new File( WORKSPACE_DIRECTORY, ReadWorkspaceLocations.METADATA_PLUGINS_ORG_ECLIPSE_CORE_RESOURCES_PROJECTS );

    private MockManager mm = new MockManager();

    /**
     * {@inheritDoc}
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /**
     * Project's at the workspace level do not have a .location file.
     * <p>
     * Therefore their project location is directly under the workspace.
     * 
     * @throws Exception
     */
    public void testGetProjectLocation_ForProjectAtWorkspaceLevel()
        throws Exception
    {
        ReadWorkspaceLocations objectUnderTest = new ReadWorkspaceLocations();

        File metadataProjectDirectory = new File( WORKSPACE_PROJECT_METADATA_DIRECTORY, "project-A" );

        File projectLocation = objectUnderTest.getProjectLocation( WORKSPACE_DIRECTORY, metadataProjectDirectory );

        File expectedProjectDirectory = new File( WORKSPACE_DIRECTORY, "project-A" );
        assertFileEquals( expectedProjectDirectory, projectLocation );
    }

    /**
     * Project's located other than at the workspace level have a .location file.
     * <p>
     * This URI specifies the fully qualified path to the project. Which may be located outside of the workspace as
     * well.
     * 
     * @throws Exception
     */
    public void testGetProjectLocation_ForProjectsWithinProjects()
        throws Exception
    {
        ReadWorkspaceLocations objectUnderTest = new ReadWorkspaceLocations();

        File metadataProjectDirectory = new File( WORKSPACE_PROJECT_METADATA_DIRECTORY, "module-A1" );
        File expectedProjectDirectory =
            new File( TempEclipseWorkspace.getFixtureEclipseDynamicWorkspace().workspaceLocation, "project-A/module-A1" );

        File projectLocation = objectUnderTest.getProjectLocation( WORKSPACE_DIRECTORY, metadataProjectDirectory );

        assertFileEquals( expectedProjectDirectory, projectLocation );
    }

    /**
     * Project's located other than at the workspace level have a .location file.
     * <p>
     * This URI specifies the fully qualified path to the project. Which may be located outside of the workspace as
     * well.
     * 
     * @throws Exception
     */
    public void testGetProjectLocation_ForProjectsOutsideWorkspace()
        throws Exception
    {
        ReadWorkspaceLocations objectUnderTest = new ReadWorkspaceLocations();

        File metadataProjectDirectory = new File( WORKSPACE_PROJECT_METADATA_DIRECTORY, "project-O" );
        File expectedProjectDirectory = new File( DYNAMIC_WORKSPACE_DIRECTORY, "project-O" );

        File projectLocation = objectUnderTest.getProjectLocation( WORKSPACE_DIRECTORY, metadataProjectDirectory );

        assertFileEquals( expectedProjectDirectory, projectLocation );
    }

    public void testReadDefinedServers_PrefsFileDoesNotExist()
        throws Exception
    {
        MockControl logControl = MockControl.createControl( Log.class );
        mm.add( logControl );

        Log logger = (Log) logControl.getMock();
        WorkspaceConfiguration workspaceConfiguration = new WorkspaceConfiguration();
        workspaceConfiguration.setWorkspaceDirectory( new File( "/does/not/exist" ) );

        mm.replayAll();

        ReadWorkspaceLocations objectUnderTest = new ReadWorkspaceLocations();
        HashMap servers = objectUnderTest.readDefinedServers( workspaceConfiguration, logger );

        mm.verifyAll();
        assertTrue( servers.isEmpty() );
    }

    public void testReadDefinedServers_PrefsFileExistsWithMissingRuntimes()
        throws Exception
    {
        MockControl logControl = MockControl.createControl( Log.class );
        mm.add( logControl );

        Log logger = (Log) logControl.getMock();
        WorkspaceConfiguration workspaceConfiguration = new WorkspaceConfiguration();
        File prefsFile =
            new File(
                      "target/test-classes/eclipse/dynamicWorkspace/workspace/.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.server.core.prefs" );
        workspaceConfiguration.setWorkspaceDirectory( prefsFile );
        mm.replayAll();

        ReadWorkspaceLocations objectUnderTest = new ReadWorkspaceLocations();
        HashMap servers = objectUnderTest.readDefinedServers( workspaceConfiguration, logger );

        mm.verifyAll();
        assertTrue( servers.isEmpty() );
    }

    /**
     * Assert that two files represent the same absolute file.
     * 
     * @param expectedFile
     * @param actualFile
     * @throws IOException
     */
    private void assertFileEquals( File expectedFile, File actualFile )
        throws IOException
    {
        assertEquals( expectedFile.getCanonicalFile(), actualFile.getCanonicalFile() );

    }

}
