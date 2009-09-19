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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

/**
 * Provides utility methods for selecting build jobs based on environmental conditions.
 * 
 * @author Benjamin Bentmann
 */
class SelectorUtils
{

    static void parseList( String list, Collection includes, Collection excludes )
    {
        String[] tokens = ( list != null ) ? StringUtils.split( list, "," ) : new String[0];

        for ( int i = 0; i < tokens.length; i++ )
        {
            String token = tokens[i].trim();

            if ( token.startsWith( "!" ) )
            {
                excludes.add( token.substring( 1 ) );
            }
            else
            {
                includes.add( token );
            }
        }
    }

    static boolean isOsFamily( String osSpec )
    {
        List includes = new ArrayList();
        List excludes = new ArrayList();
        parseList( osSpec, includes, excludes );

        return isOsFamily( includes, true ) && !isOsFamily( excludes, false );
    }

    static boolean isOsFamily( List families, boolean defaultMatch )
    {
        if ( families != null && !families.isEmpty() )
        {
            for ( Iterator it = families.iterator(); it.hasNext(); )
            {
                String family = (String) it.next();

                if ( Os.isFamily( family ) )
                {
                    return true;
                }
            }

            return false;
        }
        else
        {
            return defaultMatch;
        }
    }

    static boolean isJreVersion( String jreSpec )
    {
        List includes = new ArrayList();
        List excludes = new ArrayList();
        parseList( jreSpec, includes, excludes );

        List jreVersion = parseVersion( System.getProperty( "java.version", "" ) );

        return isJreVersion( jreVersion, includes, true ) && !isJreVersion( jreVersion, excludes, false );
    }

    static boolean isJreVersion( List jreVersion, List versionPatterns, boolean defaultMatch )
    {
        if ( versionPatterns != null && !versionPatterns.isEmpty() )
        {
            for ( Iterator it = versionPatterns.iterator(); it.hasNext(); )
            {
                String versionPattern = (String) it.next();

                if ( isJreVersion( jreVersion, versionPattern ) )
                {
                    return true;
                }
            }

            return false;
        }
        else
        {
            return defaultMatch;
        }
    }

    static boolean isJreVersion( List jreVersion, String versionPattern )
    {
        List checkVersion = parseVersion( versionPattern );

        if ( versionPattern.endsWith( "+" ) )
        {
            // 1.5+ <=> [1.5,)
            return compareVersions( jreVersion, checkVersion ) >= 0;
        }
        else if ( versionPattern.endsWith( "-" ) )
        {
            // 1.5- <=> (,1.5)
            return compareVersions( jreVersion, checkVersion ) < 0;
        }
        else
        {
            // 1.5 <=> [1.5,1.6)
            return checkVersion.size() <= jreVersion.size()
                && checkVersion.equals( jreVersion.subList( 0, checkVersion.size() ) );
        }
    }

    static List parseVersion( String version )
    {
        version = version.replaceAll( "[^0-9]", "." );

        String[] tokens = StringUtils.split( version, "." );

        List numbers = new ArrayList();

        for ( int i = 0; i < tokens.length; i++ )
        {
            numbers.add( Integer.valueOf( tokens[i] ) );
        }

        return numbers;
    }

    static int compareVersions( List version1, List version2 )
    {
        for ( Iterator it1 = version1.iterator(), it2 = version2.iterator();; )
        {
            if ( !it1.hasNext() )
            {
                return it2.hasNext() ? -1 : 0;
            }
            if ( !it2.hasNext() )
            {
                return it1.hasNext() ? 1 : 0;
            }

            Integer num1 = (Integer) it1.next();
            Integer num2 = (Integer) it2.next();

            int rel = num1.compareTo( num2 );
            if ( rel != 0 )
            {
                return rel;
            }
        }
    }

}
