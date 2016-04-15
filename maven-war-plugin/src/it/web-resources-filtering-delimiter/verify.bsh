
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

import org.codehaus.plexus.util.*;

boolean result = true;

try
{
    // Load and check jetty-env.xml

    File target = new File( basedir, "web/target/example-web/WEB-INF" );
    if ( !target.exists() || !target.isDirectory() )
    {
        System.err.println( "web/target/example-web/WEB-INF is missing or is not a directory." );
        return false;
    }

    File jettyEnv = new File( target, "jetty-env.xml" );
    if ( !jettyEnv.exists() || jettyEnv.isDirectory() )
    {
        System.err.println( "jetty-env.xml is missing or is a directory." );
        return false;
    }


    FileInputStream fis = new FileInputStream ( jettyEnv );
    String paramContent = IOUtil.toString ( fis, "UTF-8" );

    System.out.println( "content='" + paramContent + "'" );


    int indexOf = paramContent.indexOf( "Characters that should be encoded in UTF-8: åäö" );
    if ( indexOf < 0 )
    {
      System.err.println( "Non-ascii characters changed encoding during filtering" );
      return false;
    }

    indexOf = paramContent.indexOf( "<Set name=\"URL\">jdbc:oracle:thin:@localhost:1521:orcl</Set>" );
    if ( indexOf < 0 )
    {
      System.err.println( "jdbc.url not filtered correctly" );
      return false;
    }

    indexOf = paramContent.indexOf( "<Set name=\"password\">@@jdbc.password@@</Set>" );
    if ( indexOf < 0 )
    {
      System.err.println( "jdbc.password has been filtered" );
      return false;
    }

    // Load and check my.properties

    target = new File( basedir, "web/target/example-web/WEB-INF/classes" );
    if ( !target.exists() || !target.isDirectory() )
    {
        System.err.println( "web/target/example-web/WEB-INF/classes is missing or is not a directory." );
        return false;
    }

    File myProperties = new File( target, "my.properties" );
    if ( !myProperties.exists() || myProperties.isDirectory() )
    {
        System.err.println( "my.properties is missing or is a directory." );
        return false;
    }

    Properties properties = new Properties();
    FileInputStream fis = new FileInputStream( myProperties );
    properties.load( fis );
    fis.close();

    String property = properties.get( "my.property" );
    System.out.println( "my.property='" + property + "'" );
    if ( !"Characters that should be encoded in ISO-8859-1: åäö".equals( property ) )
    {
        System.err.println( "Non-ascii characters has wrong encoding after filtering" );
        return false;
    }
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
