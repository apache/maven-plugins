package org.codehaus.plexus.util.introspection;

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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.codehaus.plexus.util.StringUtils;

/**
 * Using simple dotted expressions extract the values from a MavenProject
 * instance, For example we might want to extract a value like:
 * project.build.sourceDirectory
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class ReflectionValueExtractor
{
    private static Class[] args = new Class[ 0 ];

    private static Object[] params = new Object[ 0 ];

    private static ClassMap classMap;

    private static Map classMaps = new HashMap();

    private ReflectionValueExtractor()
    {
    }
    
    public static Object evaluate( String expression, Object root )
        throws Exception
    {
        return evaluate( expression, root, true );
    }

    // TODO: don't throw Exception
    public static Object evaluate( String expression, Object root, boolean trimRootToken )
        throws Exception
    {
        // if the root token refers to the supplied root object parameter, remove it.
        if ( trimRootToken )
        {
            expression = expression.substring( expression.indexOf( '.' ) + 1 );
        }

        Object value = root;

        // ----------------------------------------------------------------------
        // Walk the dots and retrieve the ultimate value desired from the
        // MavenProject instance.
        // ----------------------------------------------------------------------

        StringTokenizer parser = new StringTokenizer( expression, "." );

        while ( parser.hasMoreTokens() )
        {
            String token = parser.nextToken();

            if ( value == null )
            {
                return null;
            }

            classMap = getClassMap( value.getClass() );

            String methodBase = StringUtils.capitalizeFirstLetter( token );
            
            String methodName = "get" + methodBase;

            Method method = classMap.findMethod( methodName, args );
            
            if ( method == null )
            {
                // perhaps this is a boolean property??
                methodName = "is" + methodBase;
                
                method = classMap.findMethod( methodName, args );
            }

            if ( method == null )
            {
                return null;
            }

            value = method.invoke( value, params );
        }

        return value;
    }

    private static ClassMap getClassMap( Class clazz )
    {
        classMap = (ClassMap) classMaps.get( clazz );

        if ( classMap == null )
        {
            classMap = new ClassMap( clazz );

            classMaps.put( clazz, classMap );
        }

        return classMap;
    }
}
