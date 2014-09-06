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
    File testFile = new File( basedir, "target/it projects/project/target/classes/test.txt" );
    System.out.println( "Checking for existence of test file: " + testFile );
    if ( !testFile.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    String[] values = {
        "value with spaces from settings",
        "value with spaces from cli",
    };

    BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( testFile ), "UTF-8" ) );
    try
    {
        for ( String value : values )
        {
            String line = reader.readLine();
            System.out.println( "Checking for occurrence of: " + value );
            if ( !value.equals( line ) )
            {
                System.out.println( "FAILED!" );
                return false;
            }
        }
    }
    finally
    {
        reader.close();
    }

    File installedFile = new File( basedir, "target/it repo/test/spacy-pom/0.1-SNAPSHOT/spacy-pom-0.1-SNAPSHOT.jar" );
    System.out.println( "Checking for existence of installed file: " + installedFile );
    if ( !installedFile.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
