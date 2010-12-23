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

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

/**
 * A map-like source to interpolate expressions.
 * 
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 22 nov. 07
 * @version $Id$
 */
class CompositeMap
    implements Map<String, Object>
{

    /**
     * The Maven project from which to extract interpolated values, never <code>null</code>.
     */
    private MavenProject mavenProject;

    /**
     * The set of additional properties from which to extract interpolated values, never <code>null</code>.
     */
    private Map<String, Object> properties;

    /**
     * Creates a new interpolation source backed by the specified Maven project and some user-specified properties.
     * 
     * @param mavenProject The Maven project from which to extract interpolated values, must not be <code>null</code>.
     * @param properties The set of additional properties from which to extract interpolated values, may be
     *            <code>null</code>.
     */
    protected CompositeMap( MavenProject mavenProject, Map<String, Object> properties )
    {
        if ( mavenProject == null )
        {
            throw new IllegalArgumentException( "no project specified" );
        }
        this.mavenProject = mavenProject;
        this.properties = properties == null ? (Map) new Properties() : properties;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#clear()
     */
    public void clear()
    {
        // nothing here
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey( Object key )
    {
        if ( !( key instanceof String ) )
        {
            return false;
        }

        String expression = (String) key;
        if ( expression.startsWith( "project." ) || expression.startsWith( "pom." ) )
        {
            try
            {
                Object evaluated = ReflectionValueExtractor.evaluate( expression, this.mavenProject );
                if ( evaluated != null )
                {
                    return true;
                }
            }
            catch ( Exception e )
            {
                // uhm do we have to throw a RuntimeException here ?
            }
        }

        return properties.containsKey( key ) || mavenProject.getProperties().containsKey( key );
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue( Object value )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#entrySet()
     */
    public Set<Entry<String, Object>> entrySet()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get( Object key )
    {
        if ( !( key instanceof String ) )
        {
            return null;
        }

        String expression = (String) key;
        if ( expression.startsWith( "project." ) || expression.startsWith( "pom." ) )
        {
            try
            {
                Object evaluated = ReflectionValueExtractor.evaluate( expression, this.mavenProject );
                if ( evaluated != null )
                {
                    return evaluated;
                }
            }
            catch ( Exception e )
            {
                // uhm do we have to throw a RuntimeException here ?
            }
        }

        Object value = properties.get( key );

        return ( value != null ? value : this.mavenProject.getProperties().get( key ) );

    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty()
    {
        return this.mavenProject == null && this.mavenProject.getProperties().isEmpty() && this.properties.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#keySet()
     */
    public Set<String> keySet()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put( String key, Object value )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll( Map<? extends String, ? extends Object> t )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove( Object key )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#size()
     */
    public int size()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Map#values()
     */
    public Collection<Object> values()
    {
        throw new UnsupportedOperationException();
    }
}
