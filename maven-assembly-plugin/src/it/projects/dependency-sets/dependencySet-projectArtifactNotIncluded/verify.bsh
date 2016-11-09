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

if ( basedir == void )
{
  basedir = new File( "." );
}

File depFile = new File( basedir, "target/dependencySet-projectArtifactNotIncluded-1.0-SNAPSHOT-bin/lib/commons-logging-1.0.4.jar" );

File projectArtifactFile = new File( basedir, "target/dependencySet-projectArtifactNotIncluded-1.0-SNAPSHOT-bin/lib/dependencySet-projectArtifactNotIncluded-1.0-SNAPSHOT.war" );

System.out.println( "result: " + ( depFile.exists() && !projectArtifactFile.exists() ) );
return depFile.exists() && !projectArtifactFile.exists();
