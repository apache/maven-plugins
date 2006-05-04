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

/**
 * @author Edwin Punzalan
 */
public class AssemblyMojoTest
    extends AbstractMojoTestCase
{
    public void testMinConfiguration()
        throws Exception
    {
        AssemblyMojo mojo = getMojo( "min-plugin-config.xml" );

        mojo.execute();
    }

    private AssemblyMojo getMojo( String pluginXml )
        throws Exception
    {
        return (AssemblyMojo) lookupMojo( "assembly", PlexusTestCase.getBasedir() +
                                        "/src/test/plugin-configs/assembly/" + pluginXml );
    }

    private AssemblyMojo executeMojo( String pluginXml )
        throws Exception
    {
        AssemblyMojo mojo = getMojo( pluginXml );

        mojo.execute();

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "executedProject" );

        return mojo;
    }
}
