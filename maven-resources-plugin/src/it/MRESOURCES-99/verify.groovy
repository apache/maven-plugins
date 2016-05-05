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

import java.util.List;
import java.util.Collections;
import java.util.Arrays;

import org.codehaus.plexus.util.*;

boolean result = true;

File target = new File( basedir, "target" );
if ( !target.exists() || !target.isDirectory() )
{
    System.err.println( "target folder is missing or not a directory." );
    return false;
}

File someResource = new File( target, "/classes/SomeResource.txt" );
if ( !someResource.exists() || someResource.isDirectory() )
{
    System.err.println( "SomeResource.txt is missing or not a file." );
    return false;
}

Properties props = new Properties();

FileInputStream fis = null;
try
{
    fis = new FileInputStream( someResource );
    props.load( fis );
    fis.close();
    fis = null;
}
catch ( IOException ex )
{
  System.err.println( "Failure during reading the properties " + someResource.getAbsolutePath() );
  return false;
}
finally
{
    IOUtil.close( fis );
}

def keysWhichHaveToExist = [
    "timestamp", 
    "at.timestamp", 
    "build.timestamp",
    "project.build.timestamp", 
    "baseUri", 
    "project.baseUri",
    "pom.baseUri", 
    "at.baseUri", 
    "groupId", 
    "basedir",
    "project.basedir",
]

 
keysWhichHaveToExist.each { key ->
  if (!props.containsKey(key)) {
      println "Missing the key '" + key + "'"
      return false
  }
}

currentTimestamp = props.get("timestamp") 
currentAtTimeStamp = props.get("at.timestamp")
if (  !currentTimestamp.equals (currentAtTimeStamp) 
    && !currentTimestamp.equals('${maven.build.timestamp}') 
    && !currentAtTimeStamp.equals('@maven.build.timestamp@') ) {
  println 'The ${maven.build.timestamp} has not correctly being replaced.'
  return false
} 

buildTimeStamp = props.get("build.timestamp") 
if ( !buildTimeStamp.equals ('${build.timestamp}') ) {
  println 'The ${build.timestamp} has been replaced.'
  return false
}

currentProjectBuildTimeStamp = props.get('project.build.timestamp')
if ( !currentProjectBuildTimeStamp.equals ('${project.build.timestamp}') ) {
  println 'The ${project.build.timestamp} has been replaced.'
  return false
}

currentBaseUri = props.get('baseUri')
if ( !currentBaseUri.equals ('${baseUri}') ) {
  println 'The ${baseUri} has been replaced.'
  return false
}

currentProjectBaseUri = props.get('project.baseUri')
if ( currentProjectBaseUri.equals ('${project.baseUri}') ) {
  println 'The ${project.baseUri} has not been replaced.'
  return false
}

currentPomBaseUri = props.get('pom.baseUri')
if ( !currentPomBaseUri.equals ('${pom.baseUri}') ) {
  println 'The ${pom.baseUri} has been replaced.'
  return false
}

currentAtBaseUri = props.get('at.baseUri')
if ( !currentAtBaseUri.equals ('@baseUri@') ) {
  println 'The @baseUri@ has been replaced.'
  return false
}

currentGroupId = props.get('project.groupId')
if ( currentGroupId.equals ('${project.groupId}') ) {
  println 'The ${project.groupId} has not been replaced.'
  return false
}
currentBaseDir = props.get('basedir')
if ( currentBaseDir.equals ('${basedir}') ) {
  println 'The ${basedir} has not been replaced.'
  return false
}
currentProjectBaseDir = props.get('project.basedir')
if ( currentBaseDir.equals ('${project.basedir}') ) {
  println 'The ${project.basedir} has not been replaced.'
  return false
}

return result;
