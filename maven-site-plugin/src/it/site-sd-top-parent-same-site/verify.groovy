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

// deploy
deployDirectory = new File( basedir, 'deploy' );
assert deployDirectory.isDirectory();
assert new File( deployDirectory, 'module1' ).isDirectory();

// stage
stageDirectory = new File( target, 'staging' );
assert stageDirectory.isDirectory();
assert new File( stageDirectory, 'module1' ).isDirectory();

// stage deploy
stageDeployDirectory = new File( deployDirectory, 'staging' );
assert stageDeployDirectory.isDirectory();
assert new File( stageDeployDirectory, 'module1' ).isDirectory();

return true;