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

// NOTE: We deliberately use JarInputStream and not JarFile here!
JarInputStream jarStream = new JarInputStream( new FileInputStream( new File( basedir, "target/test-1.0.jar" ) ) );
Manifest mf = jarStream.getManifest();
jarStream.close();

if ( mf == null )
{
    throw new IllegalStateException( "META-INF/MANIFEST.MF is missing" );
}

if ( !"PASSED".equals( mf.getMainAttributes().getValue( "Test-Entry" ) ) )
{
    throw new IllegalStateException( "META-INF/MANIFEST.MF is incomplete" );
}

if ( !"PASSED".equals( mf.getMainAttributes().getValue( "Original-Entry" ) ) )
{
    throw new IllegalStateException( "META-INF/MANIFEST.MF is incomplete" );
}

if ( !"org.apache.maven.Shade".equals( mf.getMainAttributes().getValue( "Main-Class" ) ) )
{
    throw new IllegalStateException( "META-INF/MANIFEST.MF is incomplete" );
}
