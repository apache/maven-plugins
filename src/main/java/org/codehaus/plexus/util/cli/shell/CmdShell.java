package org.codehaus.plexus.util.cli.shell;

import java.util.Arrays;
import java.util.List;

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

/**
 * <p>
 * Implementation to call the CMD Shell present on Windows NT, 2000 and XP
 * </p>
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @since 1.2
 */
public class CmdShell
    extends Shell
{
    public CmdShell()
    {
        setShellCommand( "cmd.exe" );
        setShellArgs( new String[]{"/X", "/C"} );
    }

    /**
     * Specific implementation that quotes the all the command line
     */
    public List getCommandLine( String executable, String[] arguments )
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "\"" );
        sb.append( super.getCommandLine( executable, arguments ).get( 0 ) );
        sb.append( "\"" );

        return Arrays.asList( new String[]{sb.toString()} );
    }

}
