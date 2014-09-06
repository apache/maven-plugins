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
import java.util.zip.*;

boolean result = true;

String basename = "sources-copied-1.0-SNAPSHOT";

try
{
    File zipFile = new File( basedir, "target/" + basename + "-project.zip" );
    
    System.out.println( "Checking for existence and file-ishness of: " + zipFile );
    
    if ( !zipFile.exists() || zipFile.isDirectory() )
    {
        System.err.println( "zip file is missing or a directory." );
        return false;
    }
    
    System.out.println( "Creating zipFile instance." );
    
    ZipFile zf = new ZipFile( zipFile );
    
    System.out.println( "Looking for project source file." );
    
    ZipEntry je = zf.getEntry( basename + "/src/main/java/test/App.java" ); 

    if ( je == null )
    {
        System.out.println( "project source file missing." );
        result = false;
    }
   
    System.out.println( "Looking for project test-source file." );
    
    je = null;
    je = zf.getEntry( basename + "/src/test/java/test/AppTest.java" ); 

    if ( je == null )
    {
        System.out.println( "project test-source file missing." );
        result = false;
    }
   
    System.out.println( "Looking for pom.xml file." );
    
    je = null;
    je = zf.getEntry( basename + "/pom.xml" ); 

    if ( je == null )
    {
        System.out.println( "project POM missing." );
        result = false;
    }
   
}
catch( IOException e )
{
    e.printStackTrace( System.out );
    result = false;
}

return result;
