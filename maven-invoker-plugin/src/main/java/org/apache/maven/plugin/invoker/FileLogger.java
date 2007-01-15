package org.apache.maven.plugin.invoker;

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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class FileLogger
    implements InvocationOutputHandler
{
    
    private PrintStream stream;
    
    private boolean shouldFinalize = true;

    private final Log log;
    
    public FileLogger( File outputFile )
        throws IOException
    {
        this( outputFile, null );
    }
    
    public FileLogger( File outputFile, Log log )
        throws IOException
    {
        this.log = log;
        stream = new PrintStream( new FileOutputStream( outputFile  ) );
        
        Runnable finalizer = new Runnable()
        {
            public void run()
            {
                try
                {
                    finalize();
                }
                catch ( Throwable e )
                {
                }
            }
        };
        
        Runtime.getRuntime().addShutdownHook( new Thread( finalizer ) );
    }

    public PrintStream getPrintStream()
    {
        return stream;
    }

    public void consumeLine( String line )
    {
        stream.println( line );
        stream.flush();
        
        if ( log != null )
        {
            log.info( line );
        }
    }
    
    public void close()
    {
        if ( stream != null )
        {
            stream.flush();
        }
        
        IOUtil.close( stream );
    }
    
    public void finalize()
    {
        if ( shouldFinalize )
        {
            close();
        }
    }
}
