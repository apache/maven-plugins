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

File child1Codec = new File( basedir, "child-3/target/child-3-1-bin/child-1/commons-codec-1.3.jar" );
File child2Codec = new File( basedir, "child-3/target/child-3-1-bin/child-2/commons-codec-1.3.jar" );

if ( !child1Codec.exists() )
{
    System.out.println( "commons-codec dependency in child-1 module is either missing or is the wrong version. File missing: " + child1Codec );
}

if ( !child2Codec.exists() )
{
    System.out.println( "commons-codec dependency in child-2 module is either missing or is the wrong version. File missing: " + child2Codec );
}

return child1Codec.exists() && child2Codec.exists();
