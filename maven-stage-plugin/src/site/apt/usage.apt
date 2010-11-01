 ------
 Usage
 ------
 Dennis Lundberg
 ------
 2007-08-25
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


Usage

 The plugin is used from the command line. In the following example we will
 copy artifacts from a staging repository located at
 <<<http://people.apache.org/~snicoll/maven-stage-repo/>>>
 that is configured in the user settings file with an <<<apache.staging>>> id 
 to the Apache scp repository located at
 <<<scp://people.apache.org/www/people.apache.org/repo/m2-ibiblio-rsync-repository>>>
 that is configured in the user settings file with an <<<apache.releases>>> id.

 If no special configuration is required for the source or target repositories
 the <<<sourceRepositoryId>>> or <<<targetRepositoryId>>> parameters
 respectively can be omitted.

-------------------
mvn stage:copy -Dsource="http://people.apache.org/~snicoll/maven-stage-repo/" \
               -Dtarget="scp://people.apache.org/www/people.apache.org/repo/m2-ibiblio-rsync-repository" \
               -DsourceRepositoryId=apache.staging \
               -DtargetRepositoryId=apache.releases \
               -Dversion=2.0.3
-------------------

 <<Note:>> Although it looks like we are only copying version <<<2.0.3>>>, we
 are in fact copying <<everything>> from the source URL to the target. This is
 due to a bug and will change in the future.

* What is happening under the hood?

 The following tasks will be performed by the plugin:

 * Download the files from the source repository

 * Download any metadata that is already present in the target repository

 * Merge source and target metadata

 * Create a rename script

 * Create a zip file containing the source files, the merged metadata and the rename script

 * Upload the zip file to the target repository

 * Unpack the zip file on the target machine

 * Delete the zip file from the target repository

 * Run the rename script on the target machine

 * Delete the rename script from the target repository
 