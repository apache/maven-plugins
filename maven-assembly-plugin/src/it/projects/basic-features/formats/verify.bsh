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
    File assembly = new File( basedir, "target/assemblies/format-test-1.0" );
    result = result && assembly.exists();
    result = result && assembly.isDirectory();
    result = result && new File( assembly, "TODO.txt" ).exists();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.zip" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.tar" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.tar.gz" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.tgz" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.tar.bz2" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.tbz2" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.jar" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.war" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.tar.snappy" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.tar.xz" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

try
{
    File assembly = new File( basedir, "target/assemblies/format-test-1.0.txz" );
    result = result && assembly.exists();
    result = result && assembly.isFile();
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
