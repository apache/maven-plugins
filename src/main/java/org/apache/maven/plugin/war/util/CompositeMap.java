package org.apache.maven.plugin.war.util;

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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * @version $Id$
 * @todo merge with resources/assembly plugin
 */
public class CompositeMap
    extends AbstractMap
{
    private final Map[] maps;

    /**
     * Creates a new instance, which is composed from all the given maps.
     *
     * @param maps the map
     */
    public CompositeMap( Map[] maps )
    {
        this.maps = maps;
    }

    public synchronized Object get( Object key )
    {
        for ( int i = 0; i < maps.length; i++ )
        {
            Object value = maps[i].get( key );
            if ( value != null )
            {
                return value;
            }
        }
        return null;
    }

    public Set entrySet()
    {
        throw new UnsupportedOperationException( "Cannot enumerate properties in a composite map" );
    }
}
