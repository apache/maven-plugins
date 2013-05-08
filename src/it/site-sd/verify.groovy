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

target = new File( basedir, 'target' );
assert target.isDirectory();

// stage
stagingDirectory = new File( target, 'staging' );
assert stagingDirectory.isDirectory();
assert new File( stagingDirectory, 'MSITE-304-child' ).isDirectory(); // notice: artifactId, not module name

// stage-deploy
deployDirectory = new File( target, 'www.example.com/parent' );
stageDeployDirectory = new File( deployDirectory, 'staging' );
assert stageDeployDirectory.isDirectory();
assert new File( stageDeployDirectory, 'MSITE-304-child' ).isDirectory();
assert !new File( deployDirectory, 'MSITE-304-child/staging' ).exists();

return true;