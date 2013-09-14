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
import java.util.zip.*;

File attachedJarFile = new File( basedir, "excluded.jar" );
System.out.println( "Checking for existence of " + attachedJarFile );
if ( !attachedJarFile.isFile() )
{
    throw new Exception( "missing " + attachedJarFile );
}

ZipFile attachedJar = new ZipFile( attachedJarFile );
System.out.println( "Checking for absence of " + attachedJarFile.getName() + "!/META-INF/TESTING.SF" );
if ( attachedJar.getEntry( "META-INF/TESTING.SF" ) != null )
{
    throw new Exception( "present " + attachedJarFile.getName() + "!/META-INF/TESTING.SF" );
}
System.out.println( "Checking for absence of " + attachedJarFile.getName() + "!/META-INF/TESTING.DSA" );
if ( attachedJar.getEntry( "META-INF/TESTING.DSA" ) != null )
{
    throw new Exception( "present " + attachedJarFile.getName() + "!/META-INF/TESTING.DSA" );
}
attachedJar.close();

return true;
