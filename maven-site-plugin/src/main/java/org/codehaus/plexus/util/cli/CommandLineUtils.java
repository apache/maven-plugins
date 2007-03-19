package org.codehaus.plexus.util.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l </a>
 * @version $Id$
 */
public abstract class CommandLineUtils
{
    private static Map processes = Collections.synchronizedMap( new HashMap() );

    static
    {
        Runtime.getRuntime().addShutdownHook( new Thread( "CommandlineUtil shutdown" )
        {
            public void run()
            {
                if ( ( processes != null ) && ( processes.size() > 0 ) )
                {
                    System.err.println( "Destroying " + processes.size() + " processes" );
                    for ( Iterator it = processes.values().iterator(); it.hasNext(); )
                    {
                        System.err.println( "Destroying process.." );
                        ( (Process) it.next() ).destroy();

                    }
                    System.err.println( "Destroyed " + processes.size() + " processes" );
                }
            }
        } );
    }

    public static class StringStreamConsumer
        implements StreamConsumer
    {
        private StringBuffer string = new StringBuffer();

        private String ls = System.getProperty( "line.separator" );

        public void consumeLine( String line )
        {
            string.append( line + ls );
        }

        public String getOutput()
        {
            return string.toString();
        }
    }

    public static int executeCommandLine( Commandline cl, StreamConsumer systemOut, StreamConsumer systemErr )
        throws CommandLineException
    {
        return executeCommandLine( cl, null, systemOut, systemErr );
    }

    public static int executeCommandLine( Commandline cl, InputStream systemIn, StreamConsumer systemOut,
                                          StreamConsumer systemErr )
        throws CommandLineException
    {
        if ( cl == null )
        {
            throw new IllegalArgumentException( "cl cannot be null." );
        }

        Process p;

        p = cl.execute();

        processes.put( new Long( cl.getPid() ), p );

        StreamFeeder inputFeeder = null;

        if ( systemIn != null )
        {
            inputFeeder = new StreamFeeder( systemIn, p.getOutputStream() );
        }

        StreamPumper outputPumper = new StreamPumper( p.getInputStream(), systemOut );

        StreamPumper errorPumper = new StreamPumper( p.getErrorStream(), systemErr );

        if ( inputFeeder != null )
        {
            inputFeeder.start();
        }

        outputPumper.start();

        errorPumper.start();

        try
        {
            int returnValue = p.waitFor();

            if ( inputFeeder != null )
            {
                synchronized ( inputFeeder )
                {
                    if ( !inputFeeder.isDone() )
                    {
                        inputFeeder.wait();
                    }
                }
            }

            synchronized ( outputPumper )
            {
                if ( !outputPumper.isDone() )
                {
                    outputPumper.wait();
                }
            }

            synchronized ( errorPumper )
            {
                if ( !errorPumper.isDone() )
                {
                    errorPumper.wait();
                }
            }

            processes.remove( new Long( cl.getPid() ) );

            return returnValue;
        }
        catch ( InterruptedException ex )
        {
            killProcess( cl.getPid() );
            throw new CommandLineException( "Error while executing external command, process killed.", ex );
        }
        finally
        {
            if ( inputFeeder != null )
            {
                inputFeeder.close();
            }

            outputPumper.close();

            errorPumper.close();
        }
    }

    public static Properties getSystemEnvVars()
        throws IOException
    {
        return getSystemEnvVars( true );
    }

    /**
     * Return the shell environment variables. If <code>caseSensitive == true</code>, then envar
     * keys will all be upper-case.
     *
     * @param caseSensitive Whether environment variable keys should be treated case-sensitively.
     * @return Properties object of (possibly modified) envar keys mapped to their values.
     * @throws IOException
     */
    public static Properties getSystemEnvVars( boolean caseSensitive )
        throws IOException
    {
        Process p = null;

        Properties envVars = new Properties();

        Runtime r = Runtime.getRuntime();

        String os = System.getProperty( "os.name" ).toLowerCase();

        //If this is windows set the shell to command.com or cmd.exe with correct arguments.
        if ( os.indexOf( "windows" ) != -1 )
        {
            if ( os.indexOf( "95" ) != -1 || os.indexOf( "98" ) != -1 || os.indexOf( "Me" ) != -1 )
            {
                p = r.exec( "command.com /c set" );
            }
            else
            {
                p = r.exec( "cmd.exe /c set" );
            }
        }
        else
        {
            p = r.exec( "env" );
        }

        BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );

        String line;

        String lastKey = null;
        String lastVal = null;

        while ( ( line = br.readLine() ) != null )
        {
            int idx = line.indexOf( '=' );

            if ( idx > 1 )
            {
                lastKey = line.substring( 0, idx );

                if ( !caseSensitive )
                {
                    lastKey = lastKey.toUpperCase();
                }

                lastVal = line.substring( idx + 1 );

                envVars.setProperty( lastKey, lastVal );
            }
            else if ( lastKey != null )
            {
                lastVal += "\n" + line;

                envVars.setProperty( lastKey, lastVal );
            }
        }

        return envVars;
    }

    /**
     * Kill a process launched by executeCommandLine methods
     * Doesn't work correctly on windows, only the cmd process will be destroy but not the sub process (<a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4770092">Bug ID 4770092</a>)
     *
     * @param pid The pid of command return by Commandline.getPid()
     */
    public static void killProcess( long pid )
    {
        Process p = (Process) processes.get( new Long( pid ) );

        if ( p != null )
        {
            p.destroy();
            System.out.println( "killed." );
            processes.remove( new Long( pid ) );
        }
        else
        {
            System.out.println( "don't exist." );
        }
    }

    public static boolean isAlive( long pid )
    {
        return ( processes.get( new Long( pid ) ) != null );
    }

}
