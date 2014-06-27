<?xml version="1.0" encoding="UTF-8"?>

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<faqs xmlns="http://maven.apache.org/FML/1.0.1"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/FML/1.0.1 http://maven.apache.org/xsd/fml-1.0.1.xsd"
  id="FAQ" title="Frequently Asked Questions">
 <part id="General">
   <faq id="question1">
     <question>Why do I need to use this plugin?</question>
     <answer>
       <p>
         It is essential that you provide some form of integration testing for your projects and the Invoker Plugin
         tries to make is easy for you to create integration tests for your Maven Plugins, new Lifecycles, or any other
         type of Maven component that you've created. Currently the Invoker Plugin forks Maven to execute the specified
         projects, but it is hoped that soon we will integrate the Maven Embedder into the mix to allow you run your
         projects in process for great speed.
       </p>
     </answer>
   </faq>
   <faq id="question2">
     <question>How can I assert that the build of an IT project fails?</question>
     <answer>
       <p>
         Sometimes you might want to test that error conditions are properly dealt with, i.e. fail a build. To assert
         a failure for a particular IT project, put the following setting into the properties file denoted by the
         plugin's <a href="run-mojo.html#invokerPropertiesFile"><code>invokerPropertiesFile</code></a> parameter:
         <i>invoker.buildResult=failure</i>.
         Now, the failure of the IT build will be interpreted as a test success. Likewise, a successful IT build will
         be considered a test failure.
       </p>
     </answer>
   </faq>
   <faq id="question3">
     <question>How can I share common code between the pre-/post-build scripts?</question>
     <answer>
       <p>
         If you want to avoid copy&amp;paste of lengthy code snippets within the hook scripts, you can move this code
         into a regular Java class. More precisely, you would place this utility code somewhere in your test source
         tree and set the plugin parameter <a href="run-mojo.html#addTestClassPath"><code>addTestClassPath</code></a>
         to <code>true</code>. For more details, please see the example
         <a href="examples/access-test-classes.html">Accessing Test Classes</a>.
       </p>
     </answer>
   </faq>
   <faq id="question4">
     <question>How can I invoke multiple Maven builds on the same IT project?</question>
     <answer>
       <p>
         This is not supported in the plugin configuration but rather on a per project basis by means of the invoker
         properties. This way, you use properties like <code>invoker.goals.2</code> to configure the goals for a second
         invocation of Maven. Have a look at the example about using
         <a href="examples/invoker-properties.html">Invoker Properties</a>.
       </p>
     </answer>
   </faq>
 </part>
</faqs>
