package org.apache.maven.plugins.jdeprscan.consumers;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Consumes output of jdeprscan tool
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
public class JDeprScanConsumer
    extends CommandLineUtils.StringStreamConsumer
    implements StreamConsumer
{

    private Map<String, Set<String>> deprecatedClasses = new HashMap<>();

    private Map<String, Set<String>> deprecatedMethods = new HashMap<>();

    public static final Pattern DEPRECATED_CLASS = Pattern.compile( "^class (\\S+) uses deprecated class (\\S+)" );

    public static final Pattern DEPRECATED_METHOD = Pattern.compile( "^class (\\S+) uses deprecated method (\\S+)" );

    public Map<String, Set<String>> getDeprecatedClasses()
    {
        return deprecatedClasses;
    }
    
    public Map<String, Set<String>> getDeprecatedMethods()
    {
        return deprecatedMethods;
    }
    
    @Override
    public void consumeLine( String line )
    {
        super.consumeLine( line );

        Matcher matcher;
        
        matcher = DEPRECATED_CLASS.matcher( line );
        if ( matcher.find() )
        {
            Set<String> dc = deprecatedClasses.get( matcher.group( 1 ) );
            if ( dc == null )
            {
                dc = new HashSet<>();
                deprecatedClasses.put( matcher.group( 1 ), dc );
            }
            dc.add( matcher.group( 2 ) );
            return;
        }
        
        matcher = DEPRECATED_METHOD.matcher( line );
        if ( matcher.find() )
        {
            Set<String> dm = deprecatedMethods.get( matcher.group( 1 ) );
            if ( dm == null )
            {
                dm = new HashSet<>();
                deprecatedMethods.put( matcher.group( 1 ), dm );
            }
            dm.add( matcher.group( 2 ) );
            return;
        }
    }
}
