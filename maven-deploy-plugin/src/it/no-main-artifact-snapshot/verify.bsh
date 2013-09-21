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

File dir = new File( new File( basedir, "target/repo" ), "org/apache/maven/its/deploy/nmas/test/1.0-SNAPSHOT" );

String[] files = dir.list();

if ( files == null )
{
    throw new FileNotFoundException( "Missing directory: " + dir );
}

String pom = null, jar = null;

for ( String file : files )
{
    if ( file.matches( "test-1\\.0-[0-9]{8}\\.[0-9]{6}-([0-9]+)\\.pom" ) )
    {
        pom = file.substring( 0, file.length() - 4 );
    }
    if ( file.matches( "test-1\\.0-[0-9]{8}\\.[0-9]{6}-([0-9]+)-it\\.jar" ) )
    {
        jar = file.substring( 0, file.length() - 7 );
    }
}

if ( pom == null )
{
    throw new FileNotFoundException( "Missing timestamped POM" );
}

if ( jar == null )
{
    throw new FileNotFoundException( "Missing timestamped JAR" );
}

if ( !jar.equals( pom ) )
{
    throw new IllegalStateException( "Timestamp mismatch " + jar + " vs " + pom );
}

return true;
