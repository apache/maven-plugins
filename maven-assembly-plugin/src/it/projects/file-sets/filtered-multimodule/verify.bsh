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

File f = new File( basedir, "child/target/child-1-src/test.txt" );

if ( !f.exists() )
{
    System.out.println( "Filtered file from file-set: " + f + " is missing." );
    return false;
}

String line = null;

BufferedReader reader = new BufferedReader( new FileReader( f ) );
line = reader.readLine();
reader.close();

System.out.println( "First line of test.txt: '" + line + "' should equal the project version: '1'." );

return "1".equals( line.trim() );
