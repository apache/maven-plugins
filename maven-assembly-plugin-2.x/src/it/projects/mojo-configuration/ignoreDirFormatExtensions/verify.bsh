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

File src = new File( basedir, "target/ignoreDirFormatExtensions-1-src/pom.xml" );
File src2 = new File( basedir, "target/ignoreDirFormatExtensions-1-src2.dir/pom.xml" );

{
    File f = src;

    if ( !f.exists() )
    {
        System.out.println( "Project assembly directory is missing. Check-file was: " + f );
    }
}

{
    File f = src2;

    if ( !f.exists() )
    {
        System.out.println( "Project assembly directory is missing. Check-file was: " + f );
    }
}

return src.exists() && src2.exists();
