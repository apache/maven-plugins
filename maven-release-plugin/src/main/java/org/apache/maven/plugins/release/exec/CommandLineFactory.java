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

import org.codehaus.plexus.util.cli.Commandline;

/**
 * Create a command line for execution. Componentised to allow mocking.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface CommandLineFactory
{
    /**
     * Plexus role.
     */
    String ROLE = CommandLineFactory.class.getName();

    /**
     * Create a command line object with default environment for the given executable.
     *
     * @param executable the executable
     * @return the command line
     * @throws MavenExecutorException if there was a problem creating the command line
     */
    Commandline createCommandLine( String executable )
        throws MavenExecutorException;
}
