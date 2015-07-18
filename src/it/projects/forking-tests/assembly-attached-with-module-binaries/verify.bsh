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

try
{
    File zipFile = new File( basedir, "child2/target/child2-1.0-SNAPSHOT-bin.zip" );
    
    if ( !zipFile.exists() || zipFile.isDirectory() )
    {
        System.err.println( "zip-file is missing or a directory." );
        result = false;
    }
    
    ZipFile zf = new ZipFile( zipFile );
    
    if ( zf.getEntry( "child2-1.0-SNAPSHOT/modules/child1.jar" ) == null )
    {
        System.err.println( "child1 entry is missing." );
        result = false;
    }
    
    if ( zf.getEntry( "child2-1.0-SNAPSHOT/modules/child2.jar" ) == null )
    {
        System.err.println( "child2 entry is missing." );
        result = false;
    }
    
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
