package org.apache.maven.plugins.release.exec;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Test the command line factory.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CommandLineFactoryTest
    extends PlexusTestCase
{
    private CommandLineFactory factory;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        factory = (CommandLineFactory) lookup( CommandLineFactory.ROLE );
    }

    public void testCreation()
        throws Exception
    {
        Commandline cl = factory.createCommandLine( "exec" );

        assertEquals( "Check executable", "exec", cl.getExecutable() );
        assertNotNull( "Check environment", cl.getEnvironments() );
        assertFalse( "Check environment", cl.getEnvironments().length == 0 );
    }
}
