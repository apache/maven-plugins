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

boolean result = true;

try
{
    File file = new File( basedir, "target/parent-1.0-filtered-assembly/file.txt");
    if(result) {
        BufferedReader r = new BufferedReader(new FileReader(file));
        String s = r.readLine();
        result = s.equals("foo");
        if(result) {
            s = r.readLine();
            if(!s.contains("\\"))
            {
                System.out.println("file.txt escaped filtering");
                return false;
            }
        }
    }
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
