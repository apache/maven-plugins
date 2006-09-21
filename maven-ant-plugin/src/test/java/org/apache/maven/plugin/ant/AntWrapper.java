package org.apache.maven.plugin.ant;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitException;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.util.optional.NoExitSecurityManager;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.util.StringUtils;

/**
 * Wrap <code>Ant</code> call.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AntWrapper
{
    /**
     * Invoke <code>Ant</code> for the default target of a given build file.
     *
     * @param antBuild an <code>Ant build</code> file
     * @throws IllegalArgumentException if any
     * @throws BuildException if any
     */
    public static void invoke( File antBuild )
        throws BuildException, IllegalArgumentException
    {
        if ( !antBuild.exists() )
        {
            throw new IllegalArgumentException( "antBuild should exist" );
        }
        if ( !antBuild.isFile() )
        {
            throw new IllegalArgumentException( "antBuild should be a file" );
        }

        // ----------------------------------------------------------------------
        // NB: By using org.apache.tools.ant.launch.Launcher, we have:
        // java.lang.ClassCastException
        //     at org.apache.tools.ant.launch.Launcher.run(Launcher.java:245)
        // So, using org.apache.tools.ant.Main#main()
        // ----------------------------------------------------------------------

        Properties oldSystemProperties = System.getProperties();

        System.setProperty( "basedir", antBuild.getParentFile().getAbsolutePath() );

        SecurityManager oldSm = System.getSecurityManager();
        System.setSecurityManager( new NoExitSecurityManager() );

        PrintStream oldErr = System.err;
        OutputStream errOS = new StringOutputStream();
        PrintStream err = new PrintStream( errOS );
        System.setErr( err );

        PrintStream oldOut = System.out;
        OutputStream outOS = new StringOutputStream();
        PrintStream out = new PrintStream( outOS );
        System.setOut( out );

        // ----------------------------------------------------------------------
        // To prevent Javac exception i.e. "Unable to find a javac compiler"
        // Ant can use the same command line arguments as the javac of the current VM
        // ----------------------------------------------------------------------
        System.setProperty( "build.compiler", "extJavac" );

        try
        {
            Main.main( new String[] { "-f", antBuild.getAbsolutePath() } );
        }
        catch ( ExitException e )
        {
            if ( StringUtils.isNotEmpty( errOS.toString() ) )
            {
                throw new BuildException( "Error in the Ant build file. \n= Ant output =\n" + outOS.toString() + "\n"
                    + errOS.toString() );
            }
        }
        finally
        {
            System.setSecurityManager( oldSm );
            System.setErr( oldErr );
            System.setOut( oldOut );
            System.setProperties( oldSystemProperties );
        }
    }
}
