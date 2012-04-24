 ------
 Selector Conditions
 ------
 Benjamin Bentmann
 ------
 2009-09-19
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

Selector Conditions
 
  Starting with plugin version 1.4, some keys of the {{{./invoker-properties.html}Invoker Properties}} can be used to
  skip individual projects based on the current JRE version or OS family as illustrated in the example below:

+------------------
# build project if JRE version is 1.4 or higher, but exclude version 1.4.1
invoker.java.version = 1.4+, !1.4.1

# build project if OS family is not Windows
invoker.os.family = !windows
+------------------

  In addition, with plugin version 1.5, there is the ability to skip individual projects based on the current Maven 
  version as illustrated in the example below:
  
+------------------
# build project if Maven version is 2.0.10 or higher, but exclude versions 2.1.0 and 2.2.0
invoker.maven.version = 2.0.10+, !2.1.0, !2.2.0
+------------------

  The values of these keys are comma separated tokens. A token prefixed with "!" denotes an exclusion, otherwise it
  denotes an inclusion. A project is build if no exclusion matches and if no inclusions are given or at least one of
  them matches the current environment.

  The tokens to describe versions can be suffixed with "+" or "-". The table below shows how the different styles
  of version tokens translate to a version range:

*----------------+----------------------------+
|| Version Token || Equivalent Version Range  |
*----------------+----------------------------+
| 1.5            | [1.5,1.6)                  |
*----------------+----------------------------+
| 1.5+           | [1.5,)                     |
*----------------+----------------------------+
| 1.5-           | (,1.5)                     |
*----------------+----------------------------+

  For the OS family, the tokens "windows", "unix" and "mac" are supported.
