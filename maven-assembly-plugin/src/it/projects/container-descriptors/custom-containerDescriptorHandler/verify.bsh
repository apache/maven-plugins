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

boolean result = true;

try
{
    File a = new File( basedir, "assembly/target/assembly-1-bin/file.txt");
    File b = new File( basedir, "assembly/target/assembly-1-bin/b/file.txt");
    
    if(result && !a.exists() ) {
        System.out.println( "File: " + a + " should have been generated, but was not." );
        result = false;
    }
    
    if(result) {
        BufferedReader r = new BufferedReader(new FileReader(a));
        String s = null;
        
        boolean foundA = false;
        boolean foundB = false;
        while( ( s = r.readLine() ) != null )
        {
            if ( s.equals( "file A" ) ) {
                foundA = true;
            }
            else if ( s.equals( "file B" ) ) {
                foundB = true;
            }
            
            if ( foundA && foundB )
            {
                result = true;
                break;
            }
        }
        
        if ( !result )
        {
            System.out.println( "File: " + a + " should contain lines:\nfile A\nfile B\n\n ...but didn't." );
        }
    }
    
    if(result && b.exists() ) {
        System.out.println( "File: " + b + " should not exist, but does." );
        result = false;
    }
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
