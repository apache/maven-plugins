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

import java.io.*
import java.util.*

/**
 * This will filter out the version of the
 * maven-ejb-plugin which is configured
 * within the integration test.
 * @return Version information.
 */
def getProjectVersion() {
    def pom = new XmlSlurper().parse(new File(basedir, 'pom.xml'))

    def allPlugins = pom.build.pluginManagement.plugins.plugin

    def mavenejb = allPlugins.find { item ->
        item.groupId.equals("org.apache.maven.plugins") && item.artifactId.equals("maven-ejb-plugin")
    }

    return mavenejb.version;
}

def projectVersion = getProjectVersion();

println "ProjectVersion:" + projectVersion


def jarFile = new File( basedir, "target/maven-it-mejb93-1.0.jar" )
if ( !jarFile.isFile() )
{
    println ( "FAILURE!" )
    return false
}

def buildLog = new File( basedir, "build.log" ).getText('UTF-8')

if (!buildLog.contains ('[INFO] --- maven-ejb-plugin:' + projectVersion + ':ejb (default-ejb) @ maven-it-mejb93 ---')) {
  println ( "default executions did not happen.")
  return false
}
if (!buildLog.contains ('[INFO] --- maven-ejb-plugin:' + projectVersion + ':ejb (second-execution) @ maven-it-mejb93 ---')) {
  println ( "second executions did not happen.")
  return false
}
if (!buildLog.contains ('[ERROR] Failed to execute goal org.apache.maven.plugins:maven-ejb-plugin:' + projectVersion 
    + ':ejb (second-execution) on project maven-it-mejb93: ' 
    + 'You have to use a classifier to attach supplemental artifacts to the ' 
    + 'project instead of replacing them. -> [Help 1]')) {
  println ( "exception message does not exists or the expected content does not exist.")
  return false
}

return true;
