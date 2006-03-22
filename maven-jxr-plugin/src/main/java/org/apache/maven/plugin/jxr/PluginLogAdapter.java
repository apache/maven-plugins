package org.apache.maven.plugin.jxr;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

    public PluginLogAdapter( Log log )
    {
        this.log = log;
    }

    public void info( String string )
    {
        log.info( string );
    }

    public void debug( String string )
    {
        log.debug( string );
    }

    public void warn( String string )
    {
        log.warn( string );
    }

    public void error( String string )
    {
        log.error( string );
    }
}
