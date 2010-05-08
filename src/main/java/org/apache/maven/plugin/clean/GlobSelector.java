package org.apache.maven.plugin.clean;

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

import java.io.File;
import java.util.Arrays;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.SelectorUtils;

/**
 * Selects paths based on Ant-like glob patterns.
 * 
 * @author Benjamin Bentmann
 */
class GlobSelector
    implements Selector
{

    private final String[] includes;

    private final String[] excludes;

    private final String str;

    public GlobSelector( String[] includes, String[] excludes )
    {
        this( includes, excludes, false );
    }

    public GlobSelector( String[] includes, String[] excludes, boolean useDefaultExcludes )
    {
        this.str = "includes = " + toString( includes ) + ", excludes = " + toString( excludes );
        this.includes = normalizePatterns( includes );
        this.excludes = normalizePatterns( addDefaultExcludes( excludes, useDefaultExcludes ) );
    }

    private static String toString( String[] patterns )
    {
        return ( patterns == null ) ? "[]" : Arrays.asList( patterns ).toString();
    }

    private static String[] addDefaultExcludes( String[] excludes, boolean useDefaultExcludes )
    {
        String[] defaults = DirectoryScanner.DEFAULTEXCLUDES;
        if ( !useDefaultExcludes )
        {
            return excludes;
        }
        else if ( excludes == null || excludes.length <= 0 )
        {
            return defaults;
        }
        else
        {
            String[] patterns = new String[excludes.length + defaults.length];
            System.arraycopy( excludes, 0, patterns, 0, excludes.length );
            System.arraycopy( defaults, 0, patterns, excludes.length, defaults.length );
            return patterns;
        }
    }

    private static String[] normalizePatterns( String[] patterns )
    {
        String[] normalized;

        if ( patterns != null )
        {
            normalized = new String[patterns.length];
            for ( int i = patterns.length - 1; i >= 0; i-- )
            {
                normalized[i] = normalizePattern( patterns[i] );
            }
        }
        else
        {
            normalized = new String[0];
        }

        return normalized;
    }

    private static String normalizePattern( String pattern )
    {
        if ( pattern == null )
        {
            return "";
        }

        String normalized = pattern.replace( ( File.separatorChar == '/' ) ? '\\' : '/', File.separatorChar );

        if ( normalized.endsWith( File.separator ) )
        {
            normalized += "**";
        }

        return normalized;
    }

    public boolean isSelected( String pathname )
    {
        return ( includes.length <= 0 || isMatched( pathname, includes ) )
            && ( excludes.length <= 0 || !isMatched( pathname, excludes ) );
    }

    private static boolean isMatched( String pathname, String[] patterns )
    {
        for ( int i = patterns.length - 1; i >= 0; i-- )
        {
            String pattern = patterns[i];
            if ( SelectorUtils.matchPath( pattern, pathname ) )
            {
                return true;
            }
        }
        return false;
    }

    public boolean couldHoldSelected( String pathname )
    {
        for ( int i = includes.length - 1; i >= 0; i-- )
        {
            String include = includes[i];
            if ( SelectorUtils.matchPatternStart( include, pathname ) )
            {
                return true;
            }
        }
        return includes.length <= 0;
    }

    public String toString()
    {
        return str;
    }

}
