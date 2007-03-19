package org.codehaus.plexus.util.interpolation;

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

import org.codehaus.plexus.util.cli.CommandLineUtils;

import java.io.IOException;
import java.util.Properties;

public class EnvarBasedValueSource
    implements ValueSource
{
    
    private Properties envars;
    private final boolean caseSensitive;

    /**
     * Create a new value source for interpolation based on shell environment variables. In this
     * case, envar keys ARE CASE SENSITIVE.
     * 
     * @throws IOException
     */
    public EnvarBasedValueSource() throws IOException
    {
        this( true );
    }

    /**
     * Create a new value source for interpolation based on shell environment variables.
     * 
     * @param caseSensitive Whether the environment variable key should be treated in a 
     *                      case-sensitive manner for lookups
     * @throws IOException
     */
    public EnvarBasedValueSource( boolean caseSensitive ) throws IOException
    {
        this.caseSensitive = caseSensitive;
        
        envars = CommandLineUtils.getSystemEnvVars( caseSensitive );
    }

    public Object getValue( String expression )
    {
        String expr = expression;
        
        if ( expr.startsWith( "env." ) )
        {
            expr = expr.substring( "env.".length() );
        }
        
        if ( !caseSensitive )
        {
            expr = expr.toUpperCase();
        }
        
        return envars.getProperty( expr );
    }
}
