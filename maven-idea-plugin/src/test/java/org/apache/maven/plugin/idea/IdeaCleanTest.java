package org.apache.maven.plugin.idea;

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

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.idea.stubs.TestCounter;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class IdeaCleanTest
    extends AbstractMojoTestCase
{
    public void testClean()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/clean-plugin-configs/min-plugin-config.xml" );

        File basedir = new File( getBasedir(), "target/test-harness/" + ( TestCounter.currentCount() + 1 ) );
        if ( basedir.exists() )
        {
            FileUtils.deleteDirectory( basedir );
        }
        assertTrue( "Prepare test base directory", basedir.mkdirs() );

        String artifactId = "plugin-test-" + ( TestCounter.currentCount() + 1 );

        File iprFile = new File( basedir, artifactId + ".ipr" );
        assertTrue( "Test creation of project files", iprFile.createNewFile() );

        File imlFile = new File( basedir, artifactId + ".iml" );
        assertTrue( "Test creation of project files", imlFile.createNewFile() );

        File iwsFile = new File( basedir, artifactId + ".iws" );
        assertTrue( "Test creation of project files", iwsFile.createNewFile() );

        Mojo mojo = lookupMojo( "clean", pluginXmlFile );

        mojo.execute();

        assertFalse( "Test idea project file was deleted", iprFile.exists() );

        assertFalse( "Test idea module file was deleted", imlFile.exists() );

        assertFalse( "Test idea workspace file was deleted", iwsFile.exists() );

        assertTrue( "Test project dir was not deleted", basedir.exists() );
    }
}
