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

try
{
  String javaVersion = System.getProperty( "java.version" );
  File a = new File( basedir, "target/parent-1.0-bin/a/filtered.txt");
  
  BufferedReader r = new BufferedReader(new FileReader(a));
  String line = r.readLine();
  if ( ! line.endsWith( javaVersion ) )
  {
    System.out.println( "Expected java version: " + javaVersion + "\nLine was: " + line );
    return false;
  }
}
catch( IOException e )
{
    e.printStackTrace();
}

return true;
