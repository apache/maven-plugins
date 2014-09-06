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
    File itRepoDir = new File( basedir, "target/it-repo" );
    if ( !itRepoDir.isDirectory() )
    {
        System.out.println( "IT local repository missing: " + itRepoDir );
        return false;
    }

    String[] files =
    {
        "org/apache/maven/plugins/maven-clean-plugin/2.4/maven-clean-plugin-2.4.jar",
        "org/apache/maven/plugins/maven-clean-plugin/2.4/maven-clean-plugin-2.4.pom",
        "org/apache/maven/plugins/maven-clean-plugin/2.4/maven-clean-plugin-2.4-javadoc.jar",
        "org/apache/maven/plugins/maven-plugins/16/maven-plugins-16.pom",
        "org/apache/maven/maven-parent/15/maven-parent-15.pom",
        "org/apache/apache/6/apache-6.pom",
        "org/codehaus/plexus/plexus-utils/1.5.6/plexus-utils-1.5.6.jar",
        "org/codehaus/plexus/plexus-utils/1.5.6/plexus-utils-1.5.6.pom",
        "junit/junit/3.8.2/junit-3.8.2.jar",
        "junit/junit/3.8.2/junit-3.8.2.pom",
        "jdom/jdom/1.1/jdom-1.1.pom",
        "org/jdom/jdom/1.1/jdom-1.1.pom",
        "org/jdom/jdom/1.1/jdom-1.1.jar",
    };
    for ( String file : files )
    {
        File stagedFile = new File( itRepoDir, file );
        System.out.println( "Checking for existence of: " + stagedFile );
        if ( !stagedFile.isFile() )
        {
            throw new IllegalStateException( "Missing: " + stagedFile );
        }
        if ( file.endsWith( "jdom-1.1.jar" ) && stagedFile.length() < 1024 * 10 )
        {
            throw new IllegalStateException( "Corrupt: " + stagedFile );
        }
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
