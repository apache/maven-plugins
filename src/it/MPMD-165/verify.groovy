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

File pmdHtml = new File( basedir, "target/site/pmd.html" );
assert pmdHtml.exists()

// Groovy's getText() automatically detects the correct encoding, so that UTF-16 works out of the box
// see also http://groovy.codehaus.org/api/groovy/util/CharsetToolkit.html
def html = pmdHtml.text;
assert html.contains( '<meta http-equiv="Content-Type" content="text/html; charset=UTF-16" />' );
