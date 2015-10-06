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

// verify that the ear is created
File dirWithMapping = new File( basedir, "target/project-1-bin/some_directory/junit.jar/" );

if ( dirWithMapping.exists() )
{
    System.out.println( "<outputFileNameMapping> should NOT be used when unpack == true." );
    return false;
}

File dir = new File( basedir, "target/project-1-bin/some_directory/junit/framework/TestCase.class" );

if ( !dir.exists() )
{
    System.out.println( "Expected unpacked class does not exist in the appropriate outputDirectory within the assembly." );
    return false;
}

return true;
