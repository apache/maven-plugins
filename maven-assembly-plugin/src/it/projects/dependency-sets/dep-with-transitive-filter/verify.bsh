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

File wagonJackrabbitJar = new File( basedir, "target/project-1-bin/maven-libs/wagon-webdav-jackrabbit-1.0-beta-4.jar" );
File xercesMinimalJar = new File( basedir, "target/project-1-bin/maven-libs/xercesMinimal-1.9.6.2.jar" );

if ( !wagonJackrabbitJar.exists() )
{
    System.out.println( "Wagon provider jar is missing (should be included via include wildcard pattern)." );
}

if ( !xercesMinimalJar.exists() )
{
    System.out.println( "Xerces minimal jar (from nekohtml, brought in by wagon jackrabbit provider) is missing (should be included via include wildcard pattern)." );
}

return wagonJackrabbitJar.exists() && xercesMinimalJar.exists();
