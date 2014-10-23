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
import java.util.jar.*;

try
{
    File f = new File( basedir, "target/massembly-375-1-test.jar" );

    System.out.println( "Examining assembly file: " + f );
    System.out.flush();

    if ( !f.isFile() )
    {
        System.out.println( "Target file: " + f + " doesn't exist!" );
        System.out.flush();
        return false;
    }

    JarFile jf = new JarFile( f );

    JarEntry je = jf.getEntry( "/test.txt" );
    if ( je != null )
    {
        System.out.println( "Entry: /test.txt should NOT exist." );
        System.out.flush();
        return false;
    }

    je = jf.getEntry( "test.txt" );
    if ( je == null )
    {
        System.out.println( "Entry: test.txt SHOULD exist." );
        System.out.flush();
        return false;
    }

    System.out.println( "VERIFICATION PASSED." );
    System.out.flush();
    return true;
}
catch ( Throwable t )
{
    t.printStackTrace( System.out );
    System.out.flush();
    return false;
}
