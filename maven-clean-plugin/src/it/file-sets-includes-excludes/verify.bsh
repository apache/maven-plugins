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
import java.util.jar.*;
import java.util.regex.*;

try
{
    String[] expected = {
        "dir0/excluded.txt",
        "dir0/sub0/excluded.txt",
        "dir0/sub1",
        "dir0/sub2/excluded.txt",

        "dir1/excluded.txt",
        "dir1/sub0/excluded.txt",
        "dir1/sub1",
        "dir1/sub2/excluded.txt",

        "dir2/excluded.txt",
        "dir2/sub0/excluded.txt",
        "dir2/sub2/excluded.txt",

        "dir3/excluded.txt",
        "dir3/sub0/excluded.txt",
        "dir3/sub2/excluded.txt",

        "dir4/excluded/file.txt",
    };
    for ( String path : expected )
    {
        File file = new File( new File( basedir, "dirs" ), path );
        System.out.println( "Checking for existence of " + file );
        if ( !file.exists() )
        {
            System.out.println( "FAILURE!" );
            return false;
        }
    }

    String[] unexpected = {
        "dir0/included.txt",
        "dir0/sub0/included.txt",
        "dir0/sub1/included.txt",

        "dir1/included.txt",
        "dir1/sub0/included.txt",
        "dir1/sub1/included.txt",

        "dir2/included.txt",
        "dir2/sub0/included.txt",
        "dir2/sub1",

        "dir3/included.txt",
        "dir3/sub0/included.txt",
        "dir3/sub1",

        "dir4/file.txt",
        "dir4/included/file.txt",
    };
    for ( String path : unexpected )
    {
        File file = new File( new File( basedir, "dirs" ), path );
        System.out.println( "Checking for absence of " + file );
        if ( file.exists() )
        {
            System.out.println( "FAILURE!" );
            return false;
        }
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
