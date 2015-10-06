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

try
{
  boolean fooFound = false;
  boolean barFound = false;
  
  JarFile file = new JarFile( new File( basedir, "child2/target/child2-1.0-SNAPSHOT-bin.jar" ) );
  JarEntry entry = file.getEntry( "META-INF/services/test.Test" );
  if ( entry == null )
  {
    return false;
  }
  
  BufferedReader br = new BufferedReader( new InputStreamReader( file.getInputStream( entry ) ) );
  
  String line = null;
  while( ( line = br.readLine() ) != null )
  {
    if ( line.equals( "test.Foo" ) )
    {
      fooFound = true;
    }
    else if ( line.equals( "test.Bar" ) )
    {
      barFound = true;
    }
    
    if ( fooFound && barFound )
    {
      break;
    }
  }
  
  if ( !fooFound )
  {
    System.out.println( "Cannot find entry 'test.Foo' in: " + entry.getName() );
  }
  
  if ( !barFound )
  {
    System.out.println( "Cannot find entry 'test.Bar' in: " + entry.getName() );
  }
  
  return fooFound && barFound;
}
catch( IOException e )
{
    e.printStackTrace();
}

return false;
