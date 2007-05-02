#!/bin/sh

mvn stage:copy -Dsource="http://people.apache.org/~snicoll/maven-staging/repo" -Dtarget="scp://people.apache.org/www/people.apache.org/repo/m2-ibiblio-rsync-repository" -Dversion=2.0.3
