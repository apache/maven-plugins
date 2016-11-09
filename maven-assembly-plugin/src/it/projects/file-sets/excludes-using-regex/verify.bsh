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

import java.io.*;
import java.net.*;
import java.util.jar.*;

boolean result = true;

try
{
    File f = new File( basedir, "target/parent-1.0-src.jar" );
    if ( !f.exists() )
    {
        System.out.println( "jar is missing" );
        return false;
    }

    JarFile jf = new JarFile( f );
    JarEntry je = jf.getEntry( "parent-1.0/src/main/java/org/test/target/file.properties" );

    if ( je == null )
    {
        System.out.println( "sources for org.test.target package in parent project are missing from jar" );
        return false;
    }

    je = jf.getEntry( "parent-1.0/child/src/main/resources/target/file.txt" );

    if ( je == null )
    {
        System.out.println( "target/file.txt in child resources is missing from the jar." );
        return false;
    }

    je = jf.getEntry( "parent-1.0/target/omit.txt" );

    if ( je != null )
    {
        System.out.println( "target/omit.txt in parent project was included the jar." );
        return false;
    }

    je = jf.getEntry( "parent-1.0/child/target/omit.txt" );

    if ( je != null )
    {
        System.out.println( "target/omit.txt in child project was included the jar." );
        return false;
    }

    je = jf.getEntry( "parent-1.0/target/classes/src/omit.txt" );

    if ( je != null )
    {
        System.out.println( "target/classes/src/omit.txt in parent project was included the jar." );
        return false;
    }

    je = jf.getEntry( "parent-1.0/child/target/classes/src/omit.txt" );

    if ( je != null )
    {
        System.out.println( "target/classes/src/omit.txt in child project was included the jar." );
        return false;
    }
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
