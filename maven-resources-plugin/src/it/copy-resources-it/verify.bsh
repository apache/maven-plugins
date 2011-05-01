
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
    File target = new File( basedir, "target" );
    if ( !target.exists() || !target.isDirectory() )
    {
        System.err.println( "target file is missing or a directory." );
        return false;
    }

    File resource = new File( target, "/extra-resources/configuration.properties" );
    if ( !resource.exists() || resource.isDirectory() )
    {
        System.err.println( "configuration.properties is missing or not a file." );
        return false;
    }

    String paramContent = FileUtils.fileRead( resource );

    int indexOf = paramContent.indexOf( "artifactId=maven-resources-plugin-copy-resources-it" );
    if ( indexOf < 0 )
    {
      System.err.println( "configuration.properties not contains artifactId=maven-resources-plugin-copy-resources-it" );
      return false;
    }

    int indexOf = paramContent.indexOf( "version=1.0.1-SNAPSHOT" );
    if ( indexOf < 0 )
    {
      System.err.println( "configuration.properties not contains version=1.0.1-SNAPSHOT" );
      return false;
    }
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
