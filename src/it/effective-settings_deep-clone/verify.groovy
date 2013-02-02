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
def buildLog = new File(basedir, 'build.log'); 
assert buildLog.exists()

// for effective-settings call
// maven.version (3.0,3.0.4] are missing the proxies?! > assert = 2
// maven.version (,2.2.1) correct                      > assert = 3
// assert 3 == buildLog.text.count('***')
assert (2..3).contains( buildLog.text.count('***') ) 

// for evaluate calls
assert buildLog.text.contains('proxy-password')
assert buildLog.text.contains('server-password')
assert buildLog.text.contains('server-passphrase')
