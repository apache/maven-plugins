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

File f = new File( basedir, "target/massembly-256-pomPropertiesInterp-1-myassembly/file/path/some.file" );
File f2 = new File( basedir, "target/massembly-256-pomPropertiesInterp-1-myassembly/file2/path/some.file" );
File f3 = new File( basedir, "target/massembly-256-pomPropertiesInterp-1-myassembly/file3/path/some.file" );

boolean fOK = f.exists() && f.isFile();
if ( !fOK )
{
    System.out.println( "File: " + f + " doesn't exist, or isn't a file." );
}

boolean f2OK = f2.exists() && f2.isFile();
if ( !f2OK )
{
    System.out.println( "File: " + f2 + " doesn't exist, or isn't a file." );
}

boolean f3OK = f3.exists() && f3.isFile();
if ( !f3OK )
{
    System.out.println( "File: " + f3 + " doesn't exist, or isn't a file." );
}

return fOK && f2OK;
