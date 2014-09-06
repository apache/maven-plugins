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
import java.net.*;
import java.util.jar.*;

try
{
    File file = new File( basedir, "target/filters-defined-in-build-1/file-1.properties");
    // This is a properties file encoded using ISO-8859-1
    BufferedReader r = new BufferedReader( new InputStreamReader( new FileInputStream( file ), "ISO-8859-1" ) );
    String s = r.readLine();
    if(s.contains("filter.build"))
    {
        System.out.println("file-1.properties was not filtered");
        return false;
    }
    s = r.readLine();
    if(!s.contains("escapedString=${project.artifactId}"))
    {
        System.out.println("file-1.properties did not escape filtering");
        return false;
    }
    s = r.readLine();
    if(!s.contains("åäö"))
    {
        System.out.println("file-1.properties has corrupted non ascii characters");
        return false;
    }

    file = new File( basedir, "target/filters-defined-in-build-1/file-2.properties");
    r = new BufferedReader(new FileReader(file));
    s = r.readLine();
    if(s.contains("filter.build"))
    {
        System.out.println("file-2.properties was not filtered");
        return false;
    }

    return true;
}
catch( IOException e )
{
    e.printStackTrace();
    return false;
}
