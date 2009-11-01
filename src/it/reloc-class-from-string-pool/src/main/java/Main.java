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

public class Main
{

    public static void main( String[] args )
        throws Exception
    {
        testClassWithSlashes();
        testClassWithDots();
        testArrayClassWithDots();
        testArrayClassWithDots();
    }

    private static void testClassWithSlashes()
        throws Exception
    {
        String typeName = "Lrelocated/RelocatedClass;";
        typeName = typeName.substring( 1, typeName.length() - 1 );
        typeName = typeName.replace( '/', '.' );
        Class type = Class.forName( typeName );
        System.out.println( type.getName() );
    }

    private static void testClassWithDots()
        throws Exception
    {
        String typeName = "Lrelocated.RelocatedClass;";
        typeName = typeName.substring( 1, typeName.length() - 1 );
        typeName = typeName.replace( '/', '.' );
        Class type = Class.forName( typeName );
        System.out.println( type.getName() );
    }

    private static void testArrayClassWithSlashes()
        throws Exception
    {
        String typeName = "[[[Lrelocated/RelocatedClass;";
        typeName = typeName.substring( 4, typeName.length() - 1 );
        typeName = typeName.replace( '/', '.' );
        Class type = Class.forName( typeName );
        System.out.println( type.getName() );
    }

    private static void testArrayClassWithDots()
        throws Exception
    {
        String typeName = "[[[[Lrelocated.RelocatedClass;";
        typeName = typeName.substring( 5, typeName.length() - 1 );
        typeName = typeName.replace( '/', '.' );
        Class type = Class.forName( typeName );
        System.out.println( type.getName() );
    }

}
