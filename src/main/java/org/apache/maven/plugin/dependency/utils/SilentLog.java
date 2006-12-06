package org.apache.maven.plugin.dependency.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

public class SilentLog
    implements Log, Logger
{

    public boolean isDebugEnabled()
    {
        return false;
    }

    public void debug( CharSequence content )
    {
    }

    public void debug( CharSequence content, Throwable error )
    {
    }

    public void debug( Throwable error )
    {
    }

    public boolean isInfoEnabled()
    {
        return false;
    }

    public void info( CharSequence content )
    {
    }

    public void info( CharSequence content, Throwable error )
    {
    }

    public void info( Throwable error )
    {
    }

    public boolean isWarnEnabled()
    {
        return false;
    }

    public void warn( CharSequence content )
    {
    }

    public void warn( CharSequence content, Throwable error )
    {
    }

    public void warn( Throwable error )
    {
    }

    public boolean isErrorEnabled()
    {
        return false;
    }

    public void error( CharSequence content )
    {
    }

    public void error( CharSequence content, Throwable error )
    {
    }

    public void error( Throwable error )
    {
    }

    public void debug( String message )
    {
        // TODO Auto-generated method stub

    }

    public void debug( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub

    }

    public void info( String message )
    {
        // TODO Auto-generated method stub

    }

    public void info( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub

    }

    public void warn( String message )
    {
        // TODO Auto-generated method stub

    }

    public void warn( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub

    }

    public void error( String message )
    {
        // TODO Auto-generated method stub

    }

    public void error( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub

    }

    public void fatalError( String message )
    {
        // TODO Auto-generated method stub

    }

    public void fatalError( String message, Throwable throwable )
    {
        // TODO Auto-generated method stub

    }

    public boolean isFatalErrorEnabled()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Logger getChildLogger( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getThreshold()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
