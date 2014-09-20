#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
FOLDERS=`find . -maxdepth 1 -type d -not -path "./.svn" -and -not -path "."`
MVN="/home/build/tools/maven/apache-maven-3.1.1/bin/mvn"
RAT_REPORT="${MVN} org.apache.rat:apache-rat-plugin:0.11:check -Drat.ignoreErrors=true -Drat.excludeSubProjects=false"
for i in $FOLDERS; do
  echo "Checking $i"
  cd $i
  $RAT_REPORT >../$i.log
  cd ..
done;
# Printout the reports only first 19 lines which contain the usefull information.
head -n19 `find . -maxdepth 3 -type f -name "rat.txt"`
# Removing report logs.
#for i in $FOLDERS; do
#  rm $i.log
#done;
