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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jason van Zyl
 * @author Mauro Talevi
 */
public class SimpleRelocator
    implements Relocator
{
    private String pattern;

    private String shadedPattern;

    private List excludes;

    public SimpleRelocator(String patt, String shadedPattern, List excludes)
    {
        this.pattern = patt.replace('.', '/');

        if ( shadedPattern != null )
        {
            this.shadedPattern = shadedPattern.replace('.', '/');
        } else
        {
            this.shadedPattern = "hidden/" + this.pattern;
        }

        if (excludes != null)
        {
            this.excludes = new ArrayList();

            for (Iterator i = excludes.iterator(); i.hasNext();)
            {
                String e = (String) i.next();

                this.excludes.add(e.replace('.', '/'));
            }
        }
    }

    public boolean canRelocate( String clazz )
    {
        if ( excludes != null )
        {
            for ( Iterator i = excludes.iterator(); i.hasNext(); )
            {
                String exclude = (String) i.next();

                // Remember we have converted "." -> "/" in the constructor. So ".*" is really "/*"
                if ( exclude.endsWith( "/*" ) && clazz.startsWith( exclude.substring( 0, exclude.length() - 2 ) ) )
                {
                    return false;
                }
                else if ( clazz.equals( exclude ) )
                {
                    return false;
                }
            }
        }

        return clazz.startsWith( pattern );
    }

    public String relocate( String clazz )
    {
        return clazz.replaceFirst(pattern, shadedPattern);
    }

}
