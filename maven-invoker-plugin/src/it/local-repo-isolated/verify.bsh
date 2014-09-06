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
    File badRepoDir = new File( basedir, "target/bad-repo" );
    if ( badRepoDir.exists() )
    {
        System.out.println( "IT used wrong local repository: " + badRepoDir );
        return false;
    }

    File itRepoDir = new File( basedir, "target/it-repo" );
    if ( !itRepoDir.isDirectory() )
    {
        System.out.println( "IT local repository missing: " + itRepoDir );
        return false;
    }

    File installedFile = new File( itRepoDir, "test/local-repo-isolated/0.1-SNAPSHOT/local-repo-isolated-0.1-SNAPSHOT.pom" );
    if ( !installedFile.isFile() )
    {
        System.out.println( "Installed file missing in local repo: " + installedFile );
        return false;
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
