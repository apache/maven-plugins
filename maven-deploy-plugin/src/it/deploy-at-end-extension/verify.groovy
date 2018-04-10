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

assert new File( basedir, "target/repo/org/apache/maven/its/deploy/dae/extension/dae/1.0/dae-1.0.pom" ).exists()
assert new File( basedir, "module1/target/repo/org/apache/maven/its/deploy/dae/extension/module1/1.0/module1-1.0.pom" ).exists()

File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()
assert buildLog.text.contains( "[INFO] Deploying org.apache.maven.its.deploy.dae.extension:dae:1.0 at end" )

