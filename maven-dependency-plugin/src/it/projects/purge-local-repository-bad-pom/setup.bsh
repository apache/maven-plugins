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

File localRepoDir = new File( localRepositoryPath, "org/apache/maven/its/dependency/purge-local-repository/1.0/" );
localRepoDir.mkdirs();

File badPomSrc = new File( basedir, "bad-pom.xml" );
File badPomDest = new File( localRepoDir, "purge-local-repository-1.0.pom" );

System.out.println( "Moving bad pom: " + badPomSrc );
if ( ! badPomSrc.renameTo( badPomDest ) )
{
    System.out.println( "Unable to move file: " + badPomSrc );
}
System.out.println( "Moved to: " + badPomDest ); 

return true;
