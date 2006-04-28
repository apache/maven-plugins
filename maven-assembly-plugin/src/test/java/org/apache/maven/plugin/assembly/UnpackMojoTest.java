package org.apache.maven.plugin.assembly;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class UnpackMojoTest
    extends AbstractMojoTestCase
{
    public void testMinConfiguration()
        throws Exception
    {
        String pluginXml = "min-plugin-config.xml";

        UnpackMojo mojo = (UnpackMojo) lookupMojo( "unpack", PlexusTestCase.getBasedir() +
                                                   "/src/test/plugin-configs/unpack/" + pluginXml );

        mojo.execute();

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        String filename = project.getArtifact().getFile().getName();
        String dir = filename.substring( 0, filename.lastIndexOf( '.' ) );
        File workDir = (File) getVariableValueFromObject( mojo, "workDirectory" );
        File unpackDir = new File( workDir, dir );
        File unpacked = new File( unpackDir, filename + ".extracted" );

        assertTrue( "Test extracted project artifact", unpacked.exists() );
    }
}
