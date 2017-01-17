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

import org.codehaus.plexus.util.*;

String[] wanted =
{
    "org/apache/maven/plugins/shade/its/one/AppOne.class",
    "org/apache/maven/plugins/shade/its/one/App.class",
    "org/apache/maven/plugins/shade/its/two/App.class"
};


JarFile jarFile = new JarFile( new File( basedir, "two/target/two-1.0-SNAPSHOT.jar" ) );

for ( String path : wanted )
{
    if ( jarFile.getEntry( path ) == null )
    {
        throw new IllegalStateException( "wanted path is missing: " + path );
    }
}

jarFile.close();

// MSHADE-225 Writing output only once
File logFile = new File( basedir, "build.log" );
String log = FileUtils.fileRead( logFile );

int index = log.indexOf( "[INFO] Dependency-reduced POM written at: " );
if ( log.indexOf( "[INFO] Dependency-reduced POM written at: ", index+1 ) >= 0 )
{
  throw new IllegalStateException( "'[INFO] Dependency-reduced POM written at: ' written more than once" + path );
}
