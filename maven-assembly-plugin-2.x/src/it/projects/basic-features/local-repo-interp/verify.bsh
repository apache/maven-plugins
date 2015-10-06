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

File f = new File( basedir, "target/test-1-bin/unprefixed/maven-assembly-plugin.jar" );
File f2 = new File( basedir, "target/test-1-bin/prefixed/maven-assembly-plugin.jar" );

if ( !f.exists() || !f.isFile() )
{
    System.out.println( "File: " + f + " doesn't exist, or isn't a file." );
    return false;
}

if ( !f2.exists() || !f2.isFile() )
{
    System.out.println( "File: " + f2 + " doesn't exist, or isn't a file." );
    return false;
}

return true;
