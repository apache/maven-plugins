package org.apache.maven.plugins.deploy;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Cast or copy instance across ClassLoaders
 * 
 * @version $Id:  $
 */
class CastHelper
{
    private static final String GET = "get";
    private static final int GET_LEN = GET.length();

    private static final String IS = "is";
    private static final int IS_LEN = IS.length();

    /**
     * Cast an instance, or copy if the instance is in a different ClassLoader
     * @param dstType The class of the returned instance
     * @param src The source instance.
     * @return The original instance if it was of the proper type; otherwise a copy in the correct ClassLoader
     * @throws MojoFailureException
     */
    static <T> T castToSameClassLoader( Class<T> dstType, Object src ) throws MojoFailureException
    {
        try
        {
            return dstType.cast( src );
        }
        catch ( ClassCastException cce )
        {
            // copy contents to a across ClassLoaders.
            try
            {
                return copyAttributes( dstType.newInstance(), src );
            }
            catch ( Exception e )
            {
                throw new MojoFailureException( e.getMessage(), e );
            }
        }
    }

    /**
     * Simplistic copy of fields from src instance to dst instance.  All fields must match.
     */
    private static <T> T copyAttributes( T dst, Object src )
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        for ( Method getter : src.getClass().getMethods() )
        {
            if ( getter.getParameterTypes().length != 0 )
            {
                continue;
            }
            String capitalName = extractAttributeNameFromGetter( getter.getName() );
            if ( capitalName == null )
            {
                continue;
            }
            try
            {
                Method setter = dst.getClass().getMethod( "set" + capitalName, getter.getReturnType() );
                Object attribute = getter.invoke( src );
                setter.invoke( dst, attribute );
            }
            catch ( NoSuchMethodException ignore )
            {
                continue;
            }
        }
        return dst;
    }

    private static String extractAttributeNameFromGetter( String name )
    {
        if ( name.startsWith( GET ) )
        {
            return name.substring( GET_LEN );
        }
        if ( name.startsWith( IS ) )
        {
            return name.substring( IS_LEN );
        }
        return null;
    }
}
