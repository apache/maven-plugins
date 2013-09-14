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

File jarFile = new File( basedir, "sub/target/sub-1.0.jar" );
System.out.println( "Checking for existence of " + jarFile );
if ( !jarFile.isFile() )
{
    throw new Exception( "Missing " + jarFile );
}

JarFile jar = new JarFile( jarFile );

String[] includedEntries = {
    "META-INF/ejb-jar.xml",
    "org/apache/maven/Person.class",
};
for ( String included : includedEntries )
{
    System.out.println( "Checking for existence of " + included );
    if ( jar.getEntry( included ) == null )
    {
        throw new Exception( "Missing " + included );
    }
}

jar.close();

return true;
