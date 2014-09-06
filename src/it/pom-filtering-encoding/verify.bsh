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

boolean check( String project, String encoding )
{
    File pomFile = new File( basedir, "target/it/" + project + "/pom.xml" );
    System.out.println( "Checking for existence of original IT POM: " + pomFile );
    if ( !pomFile.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    pomFile = new File( basedir, "target/it/" + project + "/target/classes/pom.xml" );
    System.out.println( "Checking for existence of interpolated IT POM: " + pomFile );
    if ( !pomFile.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    String xml = FileUtils.fileRead( pomFile, encoding );

    String[] values = {
            "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>",
            "<prop0>\u00A9\u00AE\u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF</prop0>",
            "<prop1>\u00A9\u00AE\u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF</prop1>",
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
    return true;
}

try
{
    return check( "latin-1", "ISO-8859-1" ) && check( "utf-8", "UTF-8" );
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
