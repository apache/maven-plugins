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
import java.util.*;
import java.util.regex.*;

import org.codehaus.plexus.util.*;

try
{
    File extensionsFile = new File( basedir, "target/it/project/.mvn/extensions.xml" );
    System.out.println( "Checking for existence of filtered extensions.xml" );
    if ( !extensionsFile.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    String xml = FileUtils.fileRead( extensionsFile, "UTF-8" );
  

    String[] values = {
      "<groupId>org.apache.maven.plugins.invoker</groupId>",
      "<artifactId>pom-filtering</artifactId>",
      "<version>1.0-SNAPSHOT</version>",
    };
    for ( String value : values )
    {
        System.out.println( "Checking for occurrence of: " + value );
        if ( xml.indexOf( value ) < 0 )
        {
            System.out.println( "FAILED!" );
            return false;
        }
    }

    String[] badValues = {
        "<groupId>@project.groupId@</groupId>",
        "<artifactId>@project.artifactId@</artifactId>",
        "<version>@project.version@</version>",
    };
    for ( String value : badValues )
    {
        System.out.println( "Checking for absence of: " + value );
        if ( xml.indexOf( value ) >= 0 )
        {
            System.out.println( "FAILED!" );
            return false;
        }
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
