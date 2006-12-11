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
package org.apache.maven.plugin.ide;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
class Messages
{

    /**
     * bundle filename.
     */
    private static final String BUNDLE_NAME = "org.apache.maven.plugin.ide.messages"; //$NON-NLS-1$

    /**
     * Static resource bundle.
     */
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle( BUNDLE_NAME );

    /**
     * Don't instantiate.
     *
     */
    private Messages()
    {
    }

    /**
     * Returns a string from the bundle.
     * @param key message key
     * @return message value or <code>!key!</code> if key is not found
     */
    public static String getString( String key )
    {
        try
        {
            return RESOURCE_BUNDLE.getString( key );
        }
        catch ( MissingResourceException e )
        {
            return '!' + key + '!';
        }
    }

    /**
     * Returns a string from the bundle, formatting it using provided params.
     * @param key message key
     * @param params MessageFormat arguments
     * @return message value or <code>!key!</code> if key is not found
     */
    public static String getString( String key, Object[] params )
    {
        try
        {
            return MessageFormat.format( RESOURCE_BUNDLE.getString( key ), params );
        }
        catch ( MissingResourceException e )
        {
            return '!' + key + '!';
        }
    }

    /**
     * Returns a string from the bundle, formatting it using provided param.
     * @param key message key
     * @param param MessageFormat arguments
     * @return message value or <code>!key!</code> if key is not found
     */
    public static String getString( String key, Object param )
    {
        return getString( key, new Object[] { param } );
    }
}
