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

plugins = new File( basedir, 'target/site/plugins.html' ).text;

// version of maven-invoker-plugin is defined in pluginManagement
assert plugins.contains( '<td>1.invoker-pluginManagement</td>' );

// version of maven-javadoc-plugin is defined in plugins (overriding pluginManagement)
assert plugins.contains( '<td>2.javadoc-plugin</td>' );

// version of maven-checkstyle-plugin is defined in plugins
assert plugins.contains( '<td>2.checkstyle-plugin</td>' );

// version of maven-changes-plugin is defined in reporting (overriding pluginManagement and plugins)
assert plugins.contains( '<td>3.changes-reporting</td>' );

return true;