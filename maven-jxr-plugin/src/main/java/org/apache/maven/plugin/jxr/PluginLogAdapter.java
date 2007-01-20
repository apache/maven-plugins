package org.apache.maven.plugin.jxr;

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

import org.apache.maven.plugin.logging.Log;

/**
 * Logging adapter.
 *
 * @author <a href="mailto:brett.NO-SPAM@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class PluginLogAdapter
    implements org.apache.maven.jxr.log.Log
{
    private final Log log;

    /**
     * Class constructor
     *
     * @param log   the Log object to be used
     */
    public PluginLogAdapter( Log log )
    {
        this.log = log;
    }

    /**
     * @see org.apache.maven.jxr.log.Log#info(String)
     */
    public void info( String string )
    {
        log.info( string );
    }

    /**
     * @see org.apache.maven.jxr.log.Log#debug(String)
     */
    public void debug( String string )
    {
        log.debug( string );
    }

    /**
     * @see org.apache.maven.jxr.log.Log#warn(String)
     */
    public void warn( String string )
    {
        log.warn( string );
    }

    /**
     * @see org.apache.maven.jxr.log.Log#error(String)  
     */
    public void error( String string )
    {
        log.error( string );
    }
}
