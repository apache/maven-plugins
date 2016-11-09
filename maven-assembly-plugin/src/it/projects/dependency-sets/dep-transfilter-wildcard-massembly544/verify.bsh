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

File dir = new File( basedir, "target/dep-transfilter-wildcard-massembly544-1-bin/maven-libs" );
File[] files = {
  new File( dir, "wagon-webdav-jackrabbit-1.0-beta-4.jar" ),
  new File( dir, "xercesMinimal-1.9.6.2.jar" ),
  new File( dir, "maven-reporting-api-2.0.4.jar" ),
  new File( dir, "doxia-sink-api-1.0-alpha-7.jar" )
};

boolean result = true;
for( int i = 0; i<files.length; i++ )
{
    if ( !files[i].exists() )
    {
        System.out.println( "Cannot find jar: " + files[i] + " (should be included via transitive pattern)." );
        result = false;
    }
}

return result;
