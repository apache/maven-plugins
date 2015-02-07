package org.apache.maven.plugin.jdeps.consumers;

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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Consumes the output of the jdeps tool
 *  
 * @author Robert Scholte
 *
 */
public class JDepsConsumer
    extends CommandLineUtils.StringStreamConsumer
    implements StreamConsumer
{

    private static final Pattern JDKINTERNALAPI = Pattern.compile( "\\s+->\\s([a-z\\.]+)\\s+(\\S.*)" );

    private Map<String, String> offendingPackages = new HashMap<String, String>();

    public void consumeLine( String line )
    {
        super.consumeLine( line );
        
        Matcher matcher = JDKINTERNALAPI.matcher( line );
        if ( matcher.matches() )
        {
            offendingPackages.put( matcher.group( 1 ), matcher.group( 2 ) );
        }
    }

    public Map<String, String> getOffendingPackages()
    {
        return offendingPackages;
    }
}
