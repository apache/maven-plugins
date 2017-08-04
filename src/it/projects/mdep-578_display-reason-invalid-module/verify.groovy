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

File file = new File( basedir, "build.log" );
assert file.exists();

String buildLog = file.getText( "UTF-8" );

// Cause message is JDK specific and can change over time

// JDOMAbout$Author.class found in top-level directory (unnamed package not allowed in module)
assert buildLog.contains( "Can't get module name from jdom-1.0.jar: " );

// geronimo.servlet.2.4.spec: Invalid module name: '2' is not a Java identifier
assert buildLog.contains( "Can't get module name from geronimo-servlet_2.4_spec-1.1.1.jar: " );

// geronimo.jta.1.1.spec: Invalid module name: '1' is not a Java identifier
assert buildLog.contains( "Can't get module name from geronimo-jta_1.1_spec-1.1.jar: " );

return true;
