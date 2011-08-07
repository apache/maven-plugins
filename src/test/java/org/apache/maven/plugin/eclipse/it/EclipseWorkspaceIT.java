/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.maven.plugin.eclipse.it;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.maven.plugin.eclipse.writers.workspace.EclipseWorkspaceWriter;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * @version $Id$
 */
public class EclipseWorkspaceIT
    extends AbstractEclipsePluginIT
{

    private static final String ECLIPSE_JDT_CORE_PREFS_PATH =
        EclipseWorkspaceWriter.ECLIPSE_CORE_RUNTIME_SETTINGS_DIR + "/"
            + EclipseWorkspaceWriter.ECLIPSE_JDT_CORE_PREFS_FILE;

    private static final String ECLIPSE_JDT_UI_PREFS_PATH =
        EclipseWorkspaceWriter.ECLIPSE_CORE_RUNTIME_SETTINGS_DIR + "/"
            + EclipseWorkspaceWriter.ECLIPSE_JDT_UI_PREFS_FILE;

    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void testWorkspace01()
        throws Exception
    {
        String projectName = "workspace-01";

        FileUtils.deleteDirectory( this.getTestWorkspaceWorkDirectory( "add-maven-repo" ) );
        testWorkspace( projectName, "add-maven-repo" );

        this.validateM2REPOVar( projectName );

        File eclipseJDTUIPrefsFile = new File( this.getOutputDirectory( projectName ), ECLIPSE_JDT_UI_PREFS_PATH );

        assertFalse( eclipseJDTUIPrefsFile.exists() );
    }

    public void testWorkspace02()
        throws Exception
    {
        // In this test we purposely do not include
        // expected/.metatdata/.plugins/org.eclipse.core.runtime.settings/org.eclipse.jdt.core.prefs
        // The content of that file is heavily depended on the location of the test

        String projectName = "workspace-02";

        FileUtils.deleteDirectory( this.getTestWorkspaceWorkDirectory( projectName ) );
        testWorkspace( projectName );

        this.validateM2REPOVar( projectName );

        File eclipseJDTUIPrefsFile = new File( this.getOutputDirectory( projectName ), ECLIPSE_JDT_UI_PREFS_PATH );

        assertTrue( eclipseJDTUIPrefsFile.exists() );

    }

    private void validateM2REPOVar( String projectName )
        throws Exception
    {
        File eclipseJDTCorePrefsFile = new File( this.getOutputDirectory( projectName ), ECLIPSE_JDT_CORE_PREFS_PATH );

        assertTrue( "Test if workspace properties exists", eclipseJDTCorePrefsFile.exists() );

        Properties props = new Properties();
        props.load( new FileInputStream( eclipseJDTCorePrefsFile ) );

        String M2_REPO = props.getProperty( EclipseWorkspaceWriter.CLASSPATH_VARIABLE_M2_REPO );

        assertNotNull( "Test M2_REPO has a value", M2_REPO );

        String localRepo = new File( PlexusTestCase.getBasedir(), "target/test-classes/m2repo" ).getCanonicalPath();

        // comparing repo's all in lower case because windows is case insensitive and settings.xml may have
        // a repository specified with different case
        assertEquals( "Test M2_REPO value", localRepo.replace( '\\', '/' ).toLowerCase(),
                      M2_REPO.replace( '\\', '/' ).toLowerCase() );

    }

}
