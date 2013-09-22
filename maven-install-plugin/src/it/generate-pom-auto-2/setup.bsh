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
import java.util.*;

import org.codehaus.plexus.util.*;

File groupDir = new File( localRepositoryPath, "org/apache/maven/its/install/gpa2" );
System.out.println( "Deleting " + groupDir );
FileUtils.deleteDirectory( groupDir );

File pomFile = new File( localRepositoryPath, "org/apache/maven/its/install/gpa2/test/0.1/test-0.1.pom" );
System.out.println( "Writing " + pomFile );
pomFile.getParentFile().mkdirs();
FileUtils.fileWrite( pomFile.getPath(), "UTF-8", "test" );

return true;
