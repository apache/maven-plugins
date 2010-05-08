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

import java.lang.reflect.*;

import relocated.*;

@MyAnno
public class Main
{

    @MyAnno
    public String field = "";

    @MyAnno
    public Main()
    {
    }

    @MyAnno
    public void method()
    {
    }

    public static void main( String[] args )
        throws Exception
    {
        Class<?> type = Main.class;
        Field field = type.getField( "field" );
        Method method = type.getMethod( "method" );
        Constructor constructor = type.getConstructor();

        AnnotatedElement[] elements = { type, method, constructor, field };
        for ( AnnotatedElement element : elements )
        {
            if ( !element.isAnnotationPresent( MyAnno.class ) )
            {
                throw new IllegalStateException( "annotation missing on " + element );
            }
        }
    }

}
