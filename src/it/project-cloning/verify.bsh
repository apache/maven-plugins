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

try
{
    File itDir = new File( basedir, "target/it" );
    if ( !itDir.isDirectory() )
    {
        System.out.println( "IT directory not existent: " + itDir );
        return false;
    }

    // NOTE: It is part of the test design that "module" is a prefix of "module-1"
    String[] paths = {
            "module",
            "module/pom.xml",
            "module-1",
            "module-1/pom.xml",
            "module-1/empty-dir",
            "module-1/sub-module",
            "module-1/sub-module/pom.xml",
    };
    for ( String path : paths )
    {
        File file = new File( itDir, path );
        if ( !file.exists() )
        {
            System.out.println( "Cloned file/directory not existent: " + file );
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
