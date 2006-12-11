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
package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author Edwin Punzalan
 */
public class AddMavenRepoMojoTest
    extends AbstractMojoTestCase
{
    public void testMinConfiguration()
        throws Exception
    {
        Mojo mojo = executeMojo( "min-plugin-config.xml" );
    }

    private AddMavenRepoMojo executeMojo( String pomXml )
        throws Exception
    {
        AddMavenRepoMojo mojo = getConfiguredMojo( pomXml );

        mojo.execute();

        File workDir = new File( mojo.getWorkspace(), mojo.DIR_ECLIPSE_CORE_RUNTIME_SETTINGS );

        File eclipseJDTCorePrefsFile = new File( workDir, mojo.FILE_ECLIPSE_JDT_CORE_PREFS );

        assertTrue( "Test if workspace properties exists", eclipseJDTCorePrefsFile.exists() );

        Properties props = new Properties();
        props.load( new FileInputStream( eclipseJDTCorePrefsFile ) );

        String M2_REPO = props.getProperty( mojo.CLASSPATH_VARIABLE_M2_REPO );

        assertNotNull( "Test M2_REPO has a value", M2_REPO );

        String localRepo = PlexusTestCase.getBasedir() + "/target/local-repo";

        assertEquals( "Test M2_REPO value", localRepo.replace( '\\', '/' ), M2_REPO.replace( '\\', '/' ) );

        return mojo;
    }

    private AddMavenRepoMojo getConfiguredMojo( String pomXml )
        throws Exception
    {
        File pomXmlFile = new File( PlexusTestCase.getBasedir(), "target/test-classes/test-harness/add-maven-repo/"
            + pomXml );

        AddMavenRepoMojo mojo = (AddMavenRepoMojo) lookupMojo( "add-maven-repo", pomXmlFile );

        return mojo;
    }
}
