# How to run Integration Tests with JDK 9

If you like to run the integration tests of the Maven JMod Plugin you have to start
the Maven process like the following:
```
mvn clean verify -Prun-its -Dinvoker.javaHome=JDK9JAVAHOME
```
You have to give the home location for JDK 9 via parameter.
