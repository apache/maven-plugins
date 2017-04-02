
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
 
// since maven-site-plugin 3.7, this IT is not expected to be successful any more: back to reporting section
return true;

assert !new File( basedir, 'target/surefire-reports' ).exists();
assert !new File( basedir, 'target/surefire-reports/org.apache.maven.plugins.site.its.AppTest.txt' ).exists();

assert !new File( basedir, 'target/site/surefire-report.html' ).exists();
assert new File( basedir, 'target/site/index.html' ).exists();
assert !new File( basedir, 'target/site/checkstyle.html' ).exists();
assert !new File( basedir, 'target/site/cpd.html' ).exists();
assert new File( basedir, 'target/site/apidocs/index.html' ).exists();
assert !new File( basedir, 'target/site/apidocs/org/apache/maven/plugins/site/its/App.html' ).exists();
assert !new File( basedir, 'target/site/cobertura/index.html' ).exists();
assert !new File( basedir, 'target/site/xref/index.html' ).exists();
assert !new File( basedir, 'target/site/xref-test/index.html' ).exists();

assert !new File( basedir, 'target/site/taglist.html' ).exists();
assert !new File( basedir, 'target/site/team-list.html' ).exists();

assert !new File( basedir, 'target/site/dependencies.html' ).exists();

return true;