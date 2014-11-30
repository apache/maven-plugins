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
File licensePage = new File( basedir, 'target/site/license.html' )
assert licensePage.exists()
// assert licensePage.text.contains( 'Свобода всему коду!' )
// Raw UTF-8 bytes written by Doxia and converted from ISO-8859-5
assert licensePage.text.contains( '&#x421;&#x432;&#x43e;&#x431;&#x43e;&#x434;&#x430; &#x432;&#x441;&#x435;&#x43c;&#x443; &#x43a;&#x43e;&#x434;&#x443;!' )
