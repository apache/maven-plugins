package org.apache.maven.doxia.docrenderer;

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

import org.codehaus.plexus.util.ReaderFactory;

/**
 * Context when processing Velocity files using a {@link java.util.HashMap} for data storage.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: DocumentRendererContext.java 1185508 2011-10-18 06:58:50Z ltheussl $
 * @since 1.1.2
 */
public class DocumentRendererContext
{
    private String inputEncoding = ReaderFactory.UTF_8;

    /**
     *  Storage for key/value pairs.
     */
    private final Map<String, Object> context;

    /**
     *  Default constructor.
     */
    public DocumentRendererContext()
    {
        context = new HashMap<String, Object>();
    }

    /**
     * @return The input encoding when processing files.
     */
    public String getInputEncoding()
    {
        return inputEncoding;
    }

    /**
     * @param inputEncoding new input encoding value when processing files.
     */
    public void setInputEncoding( String inputEncoding )
    {
        this.inputEncoding = inputEncoding;
    }

    /**
     * Adds a name/value pair to the context.
     *
     * @param key The name to key the provided value with.
     * @param value The corresponding value.
     * @return Object that was replaced in the the Context if applicable or null if not.
     */
    public Object put( String key, Object value )
    {
        if ( key == null )
        {
            return null;
        }

        return context.put( key, value );
    }

    /**
     *  Gets the value corresponding to the provided key from the context.
     *
     *  @param key The name of the desired value.
     *  @return The value corresponding to the provided key or null if the key param is null.
     */
    public Object get( String key )
    {
        if ( key == null )
        {
            return null;
        }

        return context.get( key );
    }

    /**
     *  Indicates whether the specified key is in the context.
     *
     * @param key The key to look for.
     * @return true if the key is in the context, false if not.
     */
    public boolean containsKey( Object key )
    {
        if ( !( key instanceof String ) ) // this includes null check
        {
            return false;
        }

        return context.containsKey( key.toString() );
    }

    /**
     *  Get all the keys for the values in the context
     *
     *  @return Object[] of keys in the Context.
     */
    public Object[] getKeys()
    {
        return context.keySet().toArray();
    }

    /**
     * Removes the value associated with the specified key from the context.
     *
     * @param key The name of the value to remove.
     * @return The value that the key was mapped to, or <code>null</code> if unmapped.
     */
    public Object remove( Object key )
    {
        if ( !( key instanceof String ) ) // this includes null check
        {
            return null;
        }

        return context.remove( key.toString() );
    }
}
