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

void checkFilePresence( String path )
{
    File depJar = new File( localRepositoryPath, path );
    if ( !depJar.exists() )
    {
        throw new Exception( "Dependency jar was not re-resolved: " + depJar );
    }
}

void checkFileAbsence( String path )
{
    File depJar = new File( localRepositoryPath, path );
    if ( depJar.exists() )
    {
        throw new Exception( "Dependency jar was not purged: " + depJar );
    }
}

checkFilePresence( "org/apache/maven/its/dependency/purge-local-repository/1.0/purge-local-repository-1.0.jar" );
checkFilePresence( "org/apache/maven/its/dependency/purge-local-repository/1.0/purge-local-repository-1.0.pom" );

checkFileAbsence( "org/apache/maven/its/dependency/purge-local-repository-2/1.0/purge-local-repository-2-1.0.jar" );
checkFileAbsence( "org/apache/maven/its/dependency/purge-local-repository-2/1.0/purge-local-repository-2-1.0.pom" );

String buildLog = new File( basedir, "build.log" ).getText( "UTF-8" );
assert !buildLog.contains( 'Purging artifact: org.apache.maven.its.dependency:purge-local-repository:jar:1.0' );
assert buildLog.contains( 'Purging artifact: org.apache.maven.its.dependency:purge-local-repository-2:jar:1.0' );

return true;
