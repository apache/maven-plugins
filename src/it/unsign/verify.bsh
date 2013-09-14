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

File targetDir = new File( basedir, "target" );

File mainJarFile = new File( targetDir, "test-1.0.jar" );
System.out.println( "Checking for existence of " + mainJarFile );
if ( !mainJarFile.isFile() )
{
    throw new Exception( "missing " + mainJarFile );
}

JarFile mainJar = new JarFile( mainJarFile );

System.out.println( "Checking for existence of " + mainJarFile.getName() + "!/META-INF/TESTING.SF" );
if ( mainJar.getEntry( "META-INF/TESTING.SF" ) == null )
{
    throw new Exception( "missing " + mainJarFile.getName() + "!/META-INF/TESTING.SF" );
}

System.out.println( "Checking for existence of " + mainJarFile.getName() + "!/META-INF/TESTING.DSA" );
if ( mainJar.getEntry( "META-INF/TESTING.DSA" ) == null )
{
    throw new Exception( "missing " + mainJarFile.getName() + "!/META-INF/TESTING.DSA" );
}

System.out.println( "Checking for absence of " + mainJarFile.getName() + "!/META-INF/UNSIGNED.SF" );
if ( mainJar.getEntry( "META-INF/UNSIGNED.SF" ) != null )
{
    throw new Exception( "present " + mainJarFile.getName() + "!/META-INF/UNSIGNED.SF" );
}

System.out.println( "Checking for absence of " + mainJarFile.getName() + "!/META-INF/UNSIGNED.DSA" );
if ( mainJar.getEntry( "META-INF/UNSIGNED.DSA" ) != null )
{
    throw new Exception( "present " + mainJarFile.getName() + "!/META-INF/UNSIGNED.DSA" );
}

System.out.println( "Checking for absence of " + mainJarFile.getName() + "!/META-INF/UNSIGNED.RSA" );
if ( mainJar.getEntry( "META-INF/UNSIGNED.RSA" ) != null )
{
    throw new Exception( "present " + mainJarFile.getName() + "!/META-INF/UNSIGNED.RSA" );
}

mainJar.close();

return true;
