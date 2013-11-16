/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
 def service1 = new File( basedir, "app/target/app-1.0-SNAPSHOT/service1.jar" ).toURL().toString();
 assert new URL( "jar:${service1}!/com/example/A.class" ).content != null
 try
 {
   new URL( "jar:${service1}!/com/example/B.class" ).content
   assert false : "service1.jar should not contain com.example.B.class" 
 }
 catch( FileNotFoundException e)
 {
 }
 
 def service2 = new File( basedir, "app/target/app-1.0-SNAPSHOT/service2.jar" ).toURL().toString();
 assert new URL( "jar:${service2}!/com/example/B.class").content != null
  try
 {
   new URL( "jar:${service2}!/com/example/A.class").content
   assert false : "service2.jar should not contain com.example.A.class" 
 }
 catch( FileNotFoundException e)
 {
 }
 