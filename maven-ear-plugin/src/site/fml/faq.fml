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
   <faq id="what-is-ear">
     <question>What is an EAR archive?</question>
     <answer>
       <p>
         An EAR archive is used to deploy standalone EJBs, usually separated from the web application. Thus, there is no
         need for a web application to access these EJBs. The EJBs are still accessible though using EJB clients.
       </p>
     </answer>
   </faq>
   <faq id="har-files">
     <question>
       The EAR Plugin throws an exception when encountering artifact types it is unfamiliar with. Is this a bug?
     </question>
     <answer>
       <p>
         The exception can be prevented by adding your custom artifact type to the artifactTypeMappings configuration.
         There is a mini-guide on how to do that in the <a href="modules.html#Custom Artifact Types">modules
         configuration</a> section.
       </p>
     </answer>
   </faq>
   <faq id="avoid-display-name">
     <question>
       How can I avoid to generate a display-name entry in the generated application.xml?
     </question>
     <answer>
       <p>
         By default, the plugin will always generate a display-name with the id of the project if a custom one
         is not provided through configuration. If for some reason you don't want any display-name at all, just
         use the ${null} value instead.
       </p>
     </answer>
   </faq>
   <faq id="when-should-one-use-the-modules">
     <question>When should one use the modules configuration?</question>
     <answer>
       <p>
         By default, the EAR plugin generates sensible defaults for all the JavaEE dependencies it finds in the
         current project. The <em>modules</em> configuration is useful if you need to customize something such as the
         bundle location, context root, etc.
       </p>
       <p>
         Adding a module entry with only the groupId and artifactId of a dependency is therefore useless.
       </p>
     </answer>
   </faq>
 </part>
</faqs>

