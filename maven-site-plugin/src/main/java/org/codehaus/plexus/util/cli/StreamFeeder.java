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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Read from an InputStream and write the output to an OutputStream.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class StreamFeeder
    extends Thread
{
    private InputStream input;

    private OutputStream output;

    private boolean done;

    /**
     * Create a new StreamFeeder
     *
     * @param input  Stream to read from
     * @param output Stream to write to
     */
    public StreamFeeder( InputStream input, OutputStream output )
    {
        this.input = input;

        this.output = output;
    }

    // ----------------------------------------------------------------------
    // Runnable implementation
    // ----------------------------------------------------------------------

    public void run()
    {
        try
        {
            feed();
        }
        catch ( Throwable ex )
        {
            // Catched everything so the streams will be closed and flagged as done.
        }
        finally
        {
            close();

            done = true;

            synchronized ( this )
            {
                this.notifyAll();
            }
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void close()
    {
        if ( input != null )
        {
            synchronized ( input )
            {
                try
                {
                    input.close();
                }
                catch ( IOException ex )
                {
                    // ignore
                }

                input = null;
            }
        }

        if ( output != null )
        {
            synchronized ( output )
            {
                try
                {
                    output.close();
                }
                catch ( IOException ex )
                {
                    // ignore
                }

                output = null;
            }
        }
    }

    public boolean isDone()
    {
        return done;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void feed()
        throws IOException
    {
        int data = input.read();

        while ( !done && data != -1 )
        {
            synchronized ( output )
            {
                output.write( data );

                data = input.read();
            }
        }
    }
}
