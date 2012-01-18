
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

File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()

// Module 1
assert 1 == buildLog.getText().count('[INFO] PMD Failure: test.MyClass:8 Rule:EmptyStatementNotInLoop Priority:3 An empty statement (semicolon) not part of a loop.')
assert 1 == buildLog.getText().count('[INFO] PMD Failure: test.MyClass:9 Rule:UnnecessaryReturn Priority:3 Avoid unnecessary return statements.')
assert 1 == buildLog.getText().count('[INFO] You have 2 PMD violations. For more details see:')

// Module 2
assert 1 == buildLog.getText().count('[INFO] PMD Failure: test.MyClass:8 Rule:EmptyStatementNotInLoop Priority:3 TEST: LOCAL-FILE-RULESET.')
assert 1 == buildLog.getText().count('[INFO] You have 1 PMD violation. For more details see:')

// Module 3
assert 1 == buildLog.getText().count('[INFO] You have 1 CPD duplication. For more details see:')

// Module 4
assert 1 == buildLog.getText().count('[INFO] You have 2 CPD duplications. For more details see:')