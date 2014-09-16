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

File projectProperties = new File( basedir, "target/project.properties" );

Properties props = new Properties();
props.load( new FileInputStream( projectProperties ) );

String junitJarPath = props.getProperty( "junit:junit:jar" );
if ( junitJarPath == null )
{
    throw new Exception( "junit:junit:jar is null" );
}
if ( ! (new File( junitJarPath )).isFile() )
{
    throw new Exception( "junit jar is not a file: " + junitJarPath );
}

String mavenArtifactPath = props.getProperty( "org.apache.maven:maven-artifact:jar" );
if ( mavenArtifactPath == null )
{
    throw new Exception( "org.apache.maven:maven-artifact:jar is null" );
}
if ( ! (new File( mavenArtifactPath )).isFile() )
{
    throw new Exception( "maven-artifact jar is not a file: " + mavenArtifactPath );
}

return true;
