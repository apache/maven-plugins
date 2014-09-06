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
    File pomFile = new File( basedir, "target/it/project/pom.xml" );
    System.out.println( "Checking for existence of original IT POM: " + pomFile );
    if ( !pomFile.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    pomFile = new File( basedir, "target/it/project/target/classes/pom.xml" );
    System.out.println( "Checking for existence of interpolated IT POM: " + pomFile );
    if ( !pomFile.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    String xml = FileUtils.fileRead( pomFile, "UTF-8" );

    String[] values = {
            "<prop0>${project.version}</prop0>",
            "<prop1>1.0-SNAPSHOT</prop1>",
            "<prop2>PROPERTY-FROM-PLUGIN-CONFIG</prop2>",
            "<prop3>PROPERTY-FROM-PROPERTIES-SECTION</prop3>",
            "<prop4>PASSED</prop4>",
            "<prop5>PASSED</prop5>",
            "<prop6>PASSED</prop6>",
            "<prop7>PASSED</prop7>",
            "<prop8>PASSED</prop8>",
            "<prop10>file:///",
            "<prop12>file:///",
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
            "<prop9>@basedir@</prop9>",
            "<prop9>FAILED</prop9>",
            "<prop10>@baseurl@</prop10>",
            "<prop10>FAILED</prop10>",
            "<prop11>@localRepository@</prop11>",
            "<prop12>@localRepositoryUrl@</prop12>",
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
