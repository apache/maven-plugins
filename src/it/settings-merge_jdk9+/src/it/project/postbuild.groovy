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

File effectiveSettings = new File( basedir, "effective-settings.xml" )
assert effectiveSettings.isFile()

def settings = new XmlSlurper().parse( effectiveSettings )
def server = settings.servers.server.find{ it.id.equals('dummy-it-settings-merge') }

if( !server.password.equals('overridden') )
  throw new org.apache.maven.plugin.MojoExecutionException("Incorrect server password - specified settings.xml not merged/dominant " + server.password)
