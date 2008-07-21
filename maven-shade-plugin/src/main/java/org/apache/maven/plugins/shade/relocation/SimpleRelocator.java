package org.apache.maven.plugins.shade.relocation;

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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.SelectorUtils;

/**
 * @author Jason van Zyl
 * @author Mauro Talevi
 */
public class SimpleRelocator
    implements Relocator
{
    private String pattern;

    private String pathPattern;

    private String shadedPattern;

    private String shadedPathPattern;

    private Set excludes;

    public SimpleRelocator( String patt, String shadedPattern, List excludes )
    {
        this.pattern = patt.replace( '/', '.' );
        this.pathPattern = patt.replace( '.', '/' );

        if ( shadedPattern != null )
        {
            this.shadedPattern = shadedPattern.replace( '/', '.' );
            this.shadedPathPattern = shadedPattern.replace( '.', '/' );
        }
        else
        {
            this.shadedPattern = "hidden." + this.pattern;
            this.shadedPathPattern = "hidden/" + this.pathPattern;
        }

        if ( excludes != null && !excludes.isEmpty() )
        {
            this.excludes = new LinkedHashSet();

            for ( Iterator i = excludes.iterator(); i.hasNext(); )
            {
                String e = (String) i.next();

                String classExclude = e.replace( '.', '/' );
                this.excludes.add( classExclude );

                if ( classExclude.endsWith( "/*" ) )
                {
                    String packageExclude = classExclude.substring( 0, classExclude.lastIndexOf( '/' ) );
                    this.excludes.add( packageExclude );
                }
            }
        }
    }

    public boolean canRelocatePath( String path )
    {
        if ( path.endsWith( ".class" ) )
        {
            path = path.substring( 0, path.length() - 6 );
        }
        if ( excludes != null )
        {
            for ( Iterator i = excludes.iterator(); i.hasNext(); )
            {
                String exclude = (String) i.next();

                if ( SelectorUtils.matchPath( exclude, path, true ) )
                {
                    return false;
                }
            }
        }

        return path.startsWith( pathPattern );
    }

    public boolean canRelocateClass( String clazz )
    {
        return canRelocatePath( clazz.replace( '.', '/' ) );
    }

    public String relocatePath( String path )
    {
        return path.replaceFirst( pathPattern, shadedPathPattern );
    }

    public String relocateClass( String clazz )
    {
        return clazz.replaceFirst( pattern, shadedPattern );
    }
}
