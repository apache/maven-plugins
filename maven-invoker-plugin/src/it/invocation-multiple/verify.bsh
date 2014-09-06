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
import java.util.*;
import java.util.regex.*;

import org.codehaus.plexus.util.*;

try
{
    File test0File = new File( basedir, "target/it/project/target/test0.txt" );
    System.out.println( "Checking for existence of first test file: " + test0File );
    if ( !test0File.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    String test0 = FileUtils.fileRead( test0File, "UTF-8" ).trim();
    System.out.println( "Checking contents of first test file: " + test0 );
    if ( !"ISO-8859-1".equals( test0 ) )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    File test1File = new File( basedir, "target/it/project/target/test1.txt" );
    System.out.println( "Checking for existence of second test file: " + test1File );
    if ( !test1File.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    String test1 = FileUtils.fileRead( test1File, "UTF-8" ).trim();
    System.out.println( "Checking contents of second test file: " + test1 );
    if ( !"UTF-8".equals( test1 ) )
    {
        System.out.println( "FAILED!" );
        return false;
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
