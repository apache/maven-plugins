package org.apache.maven.plugin.assembly.utils;

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

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Kristian Rosenvold
 */
class LinuxLineFeedInputStream
    extends InputStream
{

    private boolean slashNSeen = false;

    private boolean eofSeen = false;

    private final InputStream target;

    private final boolean ensureLineFeedAtEndOfFile;

    public LinuxLineFeedInputStream( InputStream in, boolean ensureLineFeedAtEndOfFile )
    {
        this.target = in;
        this.ensureLineFeedAtEndOfFile = ensureLineFeedAtEndOfFile;
    }

    private int readWithUpdate()
        throws IOException
    {
        final int target = this.target.read();
        eofSeen = target == -1;
        if ( eofSeen )
        {
            return target;
        }
        slashNSeen = target == '\n';
        return target;
    }

    @Override
    public int read()
        throws IOException
    {
        if ( eofSeen )
        {
            return eofGame();
        }
        else
        {
            int target = readWithUpdate();
            if ( eofSeen )
            {
                return eofGame();
            }
            if ( target == '\r' )
            {
                target = readWithUpdate();
            }
            return target;
        }
    }

    private int eofGame()
    {
        if ( !ensureLineFeedAtEndOfFile )
        {
            return -1;
        }
        if ( !slashNSeen )
        {
            slashNSeen = true;
            return '\n';
        }
        else
        {
            return -1;
        }
    }

    @Override
    public void close()
        throws IOException
    {
        super.close();
        target.close();
    }

    @Override
    public synchronized void mark( int readlimit )
    {
        throw new UnsupportedOperationException( "Mark not implemented yet" );
    }
}
