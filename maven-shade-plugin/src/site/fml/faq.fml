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
  id="FAQ" 
  title="Frequently Asked Questions">
 <part id="general">
   <faq id="two-executions">
     <question>Why Does My Second Shade Include The Results Of The First Execution?</question>
     <answer>
       <p>
       By default, shade replaces with original jar with the result of shading.
       So, when a <code>pom.xml</code> includes two shades, 
       the second shade execution will (by default) start from the result of the 
       first shade execution.
       </p><p>
       If you're looking for two independent shades then read 
       in <a href='shade-mojo.html'>shade:shade</a> about
       ways choose a different name for your first shade.
       </p>
     </answer>
   </faq>
 </part>
</faqs>