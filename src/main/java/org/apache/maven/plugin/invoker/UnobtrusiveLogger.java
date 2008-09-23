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
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

/**
 * A Plexus logger around an existing mojo logger used to change INFO messages into DEBUG messages. This is intended to
 * reduce the logging noise of other components.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class UnobtrusiveLogger
    extends AbstractLogger
{

    /**
     * The original logger being wrapped by this instance, never <code>null</code>.
     */
    private Log logger;

    /**
     * Creates a new wrapper for the specified logger.
     * 
     * @param logger The logger to wrap, must not be <code>null</code>.
     */
    public UnobtrusiveLogger( Log logger )
    {
        super( getThreshold( logger ), "" );
        this.logger = logger;
    }

    /**
     * Derives the log threshold for the specified logger.
     * 
     * @param logger The logger whose threshold should be determined, must not be <code>null</code>.
     * @return The logger's threshold.
     */
    private static int getThreshold( Log logger )
    {
        if ( logger == null )
        {
            throw new IllegalArgumentException( "no logger specified" );
        }
        if ( logger.isDebugEnabled() )
        {
            return Logger.LEVEL_DEBUG;
        }
        else if ( logger.isInfoEnabled() )
        {
            return Logger.LEVEL_INFO;
        }
        else if ( logger.isWarnEnabled() )
        {
            return Logger.LEVEL_WARN;
        }
        else if ( logger.isErrorEnabled() )
        {
            return Logger.LEVEL_ERROR;
        }
        return Logger.LEVEL_FATAL;
    }

    /**
     * Replaces the current logger of the specified Plexus component with a new instance of
     * <code>UnobtrusiveLogger</code> by delegating output to the given mojo logger.
     * 
     * @param component The Plexus component whose logger should be setup, may be <code>null</code>.
     * @param logger The mojo logger to use for log delegation, must not be <code>null</code>.
     */
    public static void setupLogger( Object component, Log logger )
    {
        if ( component instanceof LogEnabled )
        {
            LogEnabled logEnabled = (LogEnabled) component;
            logEnabled.enableLogging( new UnobtrusiveLogger( logger ) );
        }
    }

    /**
     * {@inheritDoc}
     */
    public Logger getChildLogger( String name )
    {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void debug( String message, Throwable cause )
    {
        logger.debug( message, cause );
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInfoEnabled()
    {
        // map info to debug
        return isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void info( String message, Throwable cause )
    {
        // map info to debug
        debug( message, cause );
    }

    /**
     * {@inheritDoc}
     */
    public void warn( String message, Throwable cause )
    {
        logger.warn( message, cause );
    }

    /**
     * {@inheritDoc}
     */
    public void error( String message, Throwable cause )
    {
        logger.error( message, cause );
    }

    /**
     * {@inheritDoc}
     */
    public void fatalError( String message, Throwable cause )
    {
        logger.error( message, cause );
    }

}
