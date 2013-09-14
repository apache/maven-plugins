 ------
 Sample Verifications
 ------
 Denis Cabasson
 ------
 2010-01-18
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

Sample Verifications

  Here is an example of what a <<<verifications.xml>>> file can look like:

+--------
<verifications xmlns="http://maven.apache.org/verifications/1.0.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://maven.apache.org/verifications/1.0.0 http://maven.apache.org/xsd/verifications-1.0.0.xsd">
  <files>
    <file>
      <location>src/main/resources/file1.txt</location>
    </file>
    <file>
      <location>src/main/resources/file2.txt</location>
      <contains>aaaabbbb</contains>
    </file>
    <file>
      <location>src/main/resources/file3.txt</location>
      <exists>false</exists>
    </file>
  </files>
</verifications>
+---------

  This file:

  * checks that the file <<<src/main/resources/file1.txt>>> is present.

  * checks that the file <<<src/main/resources/file2.txt>>> is present and matches the regular expression <aaaabbbb>.

  * checks that the file <<<src/main/resources/file3.txt>>> is <<not>> present.
