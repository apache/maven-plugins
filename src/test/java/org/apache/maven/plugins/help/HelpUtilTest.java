package org.apache.maven.plugins.help;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 *
 */
public class HelpUtilTest
    extends AbstractMojoTestCase
{
    /**
     * Test method for {@link org.apache.maven.plugins.help.HelpUtil#getMojoDescriptor(java.lang.String, org.apache.maven.execution.MavenSession, org.apache.maven.project.MavenProject,
     * java.lang.String, boolean, boolean)}.
     *
     * @throws Exception if any
     */
    public void testGetMojoDescriptor()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        EvaluateMojo describe = (EvaluateMojo) lookupMojo( "evaluate", testPom );

        MavenSession session =
            new MavenSession(
                              container,
                              describe.settings, // Settings settings,
                              describe.localRepository, // ArtifactRepository localRepository,
                              null, // EventDispatcher eventDispatcher,
                              null, // ReactorManager reactorManager,
                              Arrays.asList( new String[] { "evaluate" } ),
                              describe.project.getBasedir().toString(), new Properties(),
                              Calendar.getInstance().getTime() );
        HelpUtil.getMojoDescriptor( "help:evaluate", session, describe.project, "help:evaluate", true, false );
    }
}
