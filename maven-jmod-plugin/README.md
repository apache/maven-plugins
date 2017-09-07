# How to run Integration Tests with JDK 9

If you running your Maven installation with a different JDK than JDK 9 you have give
the JDK 9 path via the command line:

```
mvn clean verify -Prun-its -Dinvoker.javaHome=JDK9JAVAHOME
```

The reason behind this is simply cause Maven JMod Plugin will call `jmod` tool
of the JDK which is needed to be found. Otherwise none of the 
integration tests can work.
