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
import java.util.jar.*;

File jarDir = new File( basedir, "target/classes/jars" );

File includedJarFile = new File( jarDir, "included.jar" );
System.out.println( "Checking for existence of " + includedJarFile );
if ( !includedJarFile.isFile() )
{
    throw new Exception( "missing " + includedJarFile );
}

JarFile includedJar = new JarFile( includedJarFile );
System.out.println( "Checking for existence of " + includedJarFile.getName() + "!/META-INF/TESTING.SF" );
if ( includedJar.getEntry( "META-INF/TESTING.SF" ) == null )
{
    throw new Exception( "missing " + includedJarFile.getName() + "!/META-INF/TESTING.SF" );
}
System.out.println( "Checking for existence of " + includedJarFile.getName() + "!/META-INF/TESTING.DSA" );
if ( includedJar.getEntry( "META-INF/TESTING.DSA" ) == null )
{
    throw new Exception( "missing " + includedJarFile.getName() + "!/META-INF/TESTING.DSA" );
}
includedJar.close();

File excludedJarFile = new File( jarDir, "excluded.jar" );
System.out.println( "Checking for existence of " + excludedJarFile );
if ( !excludedJarFile.isFile() )
{
    throw new Exception( "missing " + excludedJarFile );
}

JarFile excludedJar = new JarFile( excludedJarFile );
System.out.println( "Checking for absence of " + excludedJarFile.getName() + "!/META-INF/TESTING.SF" );
if ( excludedJar.getEntry( "META-INF/TESTING.SF" ) != null )
{
    throw new Exception( "present " + excludedJarFile.getName() + "!/META-INF/TESTING.SF" );
}
System.out.println( "Checking for absence of " + excludedJarFile.getName() + "!/META-INF/TESTING.DSA" );
if ( excludedJar.getEntry( "META-INF/TESTING.DSA" ) != null )
{
    throw new Exception( "present " + excludedJarFile.getName() + "!/META-INF/TESTING.DSA" );
}
excludedJar.close();

return true;
