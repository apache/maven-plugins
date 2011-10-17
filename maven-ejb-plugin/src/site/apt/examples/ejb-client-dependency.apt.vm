 ------
 Using the ejb-client as a dependency
 ------
 Pete Marvin King
 ------
 2009-04-07
 ------

 ~~ Licensed to the Apache Software Foundation (ASF) under one
 ~~ or more contributor license agreements.  See the NOTICE file
 ~~ distributed with this work for additional information
 ~~ regarding copyright ownership.  The ASF licenses this file
 ~~ to you under the Apache License, Version 2.0 (the
 ~~ "License"); you may not use this file except in compliance
 ~~ with the License.  You may obtain a copy of the License at
 ~~
 ~~   http://www.apache.org/licenses/LICENSE-2.0
 ~~
 ~~ Unless required by applicable law or agreed to in writing,
 ~~ software distributed under the License is distributed on an
 ~~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~~ KIND, either express or implied.  See the License for the
 ~~ specific language governing permissions and limitations
 ~~ under the License.

 ~~ NOTE: For help with the syntax of this file, see:
 ~~ http://maven.apache.org/doxia/references/apt-format.html


Using the ejb-client as a dependency

 The EJB Plugin is capable of generating another artifact aside from the primary one which is EJB. To choose the EJB client
 as the dependency just specify the <<<type>>> as <<<ejb-client>>>.

* Normal way of adding an EJB dependency

 The following dependency declaration would include the primary EJB artifact <<<ejb-project-1.0-SNAPSHOT.jar>>> in your
 project's package.

+------------+
<project>
  [...]
  <dependencies>
    <dependency>
      <groupId>com.example</groupId>
      <artifactId>ejb-project</artifactId>
      <version>1.0-SNAPSHOT</version>
      <type>ejb</type>
    </dependency>
  </dependencies>
  [...]
</project>
+------------+

* Using the ejb-client

 Using the following dependency declaration would instead use the ejb-client artifact
 <<<ejb-project-1.0-SNAPSHOT-client.jar>>> in your project's package.

+------------+
<project>
  [...]
  <dependencies>
    <dependency>
      <groupId>com.example</groupId>
      <artifactId>ejb-project</artifactId>
      <version>1.0-SNAPSHOT</version>
      <type>ejb-client</type>
    </dependency>
  </dependencies>
  [...]
</project>
+------------+


 Read more about {{{./generating-ejb-client.html}Generating the EJB client}}.
