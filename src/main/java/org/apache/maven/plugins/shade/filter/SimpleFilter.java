package org.apache.maven.plugins.shade.filter;

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

import org.codehaus.plexus.util.SelectorUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author David Blevins
 */
public class SimpleFilter
    implements Filter
{

    private Set jars;

    private Set includes;

    private Set excludes;

    public SimpleFilter( Set jars, Set includes, Set excludes )
    {
        this.jars = ( jars != null ) ? new HashSet( jars ) : new HashSet();
        this.includes = normalizePatterns( includes );
        this.excludes = normalizePatterns( excludes );
    }

    public boolean canFilter( File jar )
    {
        return jars.contains( jar );
    }

    public boolean isFiltered( String classFile )
    {
        String path = normalizePath( classFile );

        return !( isIncluded( path ) && !isExcluded( path ) );
    }

    private boolean isIncluded( String classFile )
    {
        if ( includes == null || includes.isEmpty() )
        {
            return true;
        }

        return matchPaths( includes, classFile );
    }

    private boolean isExcluded( String classFile )
    {
        if ( excludes == null || excludes.isEmpty() )
        {
            return false;
        }

        return matchPaths( excludes, classFile );
    }

    private boolean matchPaths( Set patterns, String classFile )
    {
        for ( Iterator iterator = patterns.iterator(); iterator.hasNext(); )
        {
            String pattern = (String) iterator.next();

            if ( SelectorUtils.matchPath( pattern, classFile ) )
            {
                return true;
            }
        }

        return false;
    }

    private String normalizePath( String path )
    {
        return ( path != null ) ? path.replace( File.separatorChar == '/' ? '\\' : '/', File.separatorChar ) : null;
    }

    private Set normalizePatterns( Set patterns )
    {
        Set result = new HashSet();

        if ( patterns != null )
        {
            for ( Iterator it = patterns.iterator(); it.hasNext(); )
            {
                String pattern = (String) it.next();

                pattern = normalizePath( pattern );

                if ( pattern.endsWith( File.separator ) )
                {
                    pattern += "**";
                }

                result.add( pattern );
            }
        }

        return result;
    }

    public void finished()
    {
    }
}
