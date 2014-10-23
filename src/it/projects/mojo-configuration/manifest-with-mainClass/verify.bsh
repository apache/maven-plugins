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
import java.util.*;
import java.net.*;

File file = new File( basedir, "target/manifest-with-mainClass-1-jar-with-dependencies.jar" );

System.out.println( "file: " + file + " exists? " + file.exists() );

JarFile jarFile = new JarFile( file );

Manifest mf = jarFile.getManifest();

Attributes attrs = mf.getMainAttributes();

String mainClass = (String) attrs.get( Attributes.Name.MAIN_CLASS );

System.out.println( "Got Main-Class: " + mainClass );

return "test.App".equals( mainClass );
