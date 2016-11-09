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
  JarFile file = new JarFile( new File( basedir, "child2/target/child2-1.0-SNAPSHOT-bin.jar" ) );
  JarEntry handlerEntry = file.getEntry( "META-INF/spring.handlers" );
  if ( handlerEntry == null )
  {
    return false;
  }

  BufferedReader br = new BufferedReader( new InputStreamReader( file.getInputStream( handlerEntry ) ) );

  boolean aopFound = false;
  boolean ctxFound = false;

  String line = null;
  while( ( line = br.readLine() ) != null )
  {
    if ( line.endsWith( "AopNamespaceHandler1" ) )
    {
      aopFound = true;
    }
    else if ( line.endsWith( "ContextNamespaceHandler1" ) )
    {
      ctxFound = true;
    }

    if ( aopFound && ctxFound )
    {
      break;
    }
  }

  if ( !aopFound )
  {
    System.out.println( "Cannot find entry 'AopNamespaceHandler1' in: " + handlerEntry.getName() );
    return false;
  }

  br.close();
  
  if ( !ctxFound )
  {
    System.out.println( "Cannot find entry 'ContextNamespaceHandler1' in: " + handlerEntry.getName() );
    return false;
  }
  
  JarEntry schemaEntry = file.getEntry( "META-INF/spring.schemas" );
  if ( schemaEntry == null )
  {
    return false;
  }

  br = new BufferedReader( new InputStreamReader( file.getInputStream( schemaEntry ) ) );

  boolean found30 = false;
  boolean found40 = false;

  String line = null;
  while( ( line = br.readLine() ) != null )
  {
    if ( line.endsWith( "spring-aop-3.0.xsd" ) )
    {
      found30 = true;
    }
    else if ( line.endsWith( "spring-aop-4.0.xsd" ) )
    {
      found40 = true;
    }

    if ( found30 && found40 )
    {
      break;
    }
  }

  br.close();
  
  if ( !found30 )
  {
    System.out.println( "Cannot find entry 'spring-aop-3.0.xsd' in: " + schemaEntry.getName() );
    return false;
  }

  if ( !found40 )
  {
    System.out.println( "Cannot find entry 'spring-aop-4.0.xsd' in: " + schemaEntry.getName() );
    return false;
  }
  
  return true;
}
catch( IOException e )
{
    e.printStackTrace();
}

return false;
