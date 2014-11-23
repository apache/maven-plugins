/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

def latin1File = new File( basedir, "latin-1/target/application.xml" )
assert latin1File.exists()
println "latin-1/target/application.xml: " + latin1File.getText( 'ISO-8859-1' )
def latin1Chars = new XmlParser().parse( latin1File ).description.text()
println "Latin-1: " + latin1Chars
assert "TEST-CHARS: \u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF".equals( latin1Chars )

def utf8File = new File( basedir, "utf-8/target/application.xml" )
assert utf8File.exists()
println "utf-8/target/application.xml: " + utf8File.getText( 'UTF-8' )
def utf8Chars = new XmlParser().parse( utf8File ).description.text()
println "UTF-8: " + utf8Chars
assert "TEST-CHARS: \u00C4\u00D6\u00DC\u00E4\u00F6\u00FC\u00DF".equals( utf8Chars )

return true;
