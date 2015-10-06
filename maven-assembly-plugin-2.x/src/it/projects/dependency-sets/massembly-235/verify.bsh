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

File dir = new File( basedir, "target/massembly235-test-1-release" );

File goodJar = new File( dir, "xmlenc-0.52.jar" );
File badJar = new File( dir, "xmlenc-0.39.jar" );

System.out.println( "Good version of xmlenc exists? " + goodJar.exists() );
System.out.println( "Bad version of xmlenc doesn't exist? " + (!badJar.exists()) );

return goodJar.exists() && !badJar.exists();
