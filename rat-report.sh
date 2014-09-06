#!/bin/bash
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
