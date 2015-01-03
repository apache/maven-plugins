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

assert new File( basedir, "target/repo/org/apache/maven/plugins/deploy/its/mdeploy178/1.0/mdeploy178-1.0.jar" ).exists()
File deployedPom = new File( basedir, "target/repo/org/apache/maven/plugins/deploy/its/mdeploy178/1.0/mdeploy178-1.0.pom" )
assert deployedPom.exists()

File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()
assert buildLog.text.contains( "[DEBUG] Using META-INF/maven/org.apache.maven.plugins.deploy.its/mdeploy178/pom.xml as pomFile" )

def pomProject = new XmlSlurper().parse( deployedPom )
assert "https://jira.codehaus.org/browse/MDEPLOY-178".equals( pomProject.url.text() )