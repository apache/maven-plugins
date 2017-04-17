~~ Licensed to the Apache Software Foundation (ASF) under one
~~ or more contributor license agreements.  See the NOTICE file
~~ distributed with this work for additional information
~~ regarding copyright ownership.  The ASF licenses this file
~~ to you under the Apache License, Version 2.0 (the
~~ "License"); you may not use this file except in compliance
~~ with the License.  You may obtain a copy of the License at
~~
~~ http://www.apache.org/licenses/LICENSE-2.0
~~
~~ Unless required by applicable law or agreed to in writing,
~~ software distributed under the License is distributed on an
~~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~~ KIND, either express or implied.  See the License for the
~~ specific language governing permissions and limitations
~~ under the License.    
 
  ------
  Filtering the dependency tree
  ------
  Mark Hobson
  ------
  2007-09-01
  ------

Filtering the dependency tree

  A project's dependency tree can be filtered to locate specific dependencies.  For example, to find out why Velocity
  is being used by the Maven Dependency Plugin, we can execute the following in the project's directory:

+---+
mvn dependency:tree -Dincludes=velocity:velocity
+---+

  Which outputs:

+---+
[INFO] [dependency:tree]
[INFO] org.apache.maven.plugins:maven-dependency-plugin:maven-plugin:2.0-alpha-5-SNAPSHOT
[INFO] \- org.apache.maven.doxia:doxia-site-renderer:jar:1.0-alpha-8:compile
[INFO]    \- org.codehaus.plexus:plexus-velocity:jar:1.1.3:compile
[INFO]       \- velocity:velocity:jar:1.4:compile
+---+

  Thus we can see that Velocity is being brought in by Plexus Velocity, which in turn is being brought in by a direct
  dependency on Doxia Site Renderer.

* Filter pattern syntax

  The syntax for filter patterns is as follows:
	
+---+
[groupId]:[artifactId]:[type]:[version]
+---+

  Where each pattern segment is optional and supports full and partial <<<*>>> wildcards.  An empty pattern segment is
  treated as an implicit wildcard.
	
  For example, <<<org.apache.*>>> would match all artifacts whose group id started with <<<org.apache.>>>, and
  <<<:::*-SNAPSHOT>>> would match all snapshot artifacts.

* Excluding dependencies from the tree

  The dependency tree can also be filtered to remove specific dependencies.  For example, to exclude Plexus
  dependencies from the tree, we can execute the following:
	
+---+
mvn dependency:tree -Dexcludes=org.codehaus.plexus
+---+

* Specifying multiple patterns

  Multiple patterns can be specified when filtering the dependency tree by separating the patterns with commas.  For
  example, to exclude Maven and Plexus dependencies from the tree, we can execute the following:
	
+---+
mvn dependency:tree -Dexcludes=org.apache.maven*,org.codehaus.plexus
+---+

* Including and excluding dependencies from the tree

  Both include and exclude patterns and be specified together to filter the dependency tree.  For example, to locate
  all non-snapshot Plexus dependencies in the tree, we can execute the following:
	
+---+
mvn dependency:tree -Dincludes=org.codehaus.plexus -Dexcludes=:::*-SNAPSHOT
+---+
