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
File dependencies = new File( basedir, 'target/site/dependencies.html' )
def mavenModel = '''\
<tr class="a">
<td>maven-model-3.3.9.jar</td>
<td>163.98 kB</td>
<td>71</td>
<td>54</td>
<td>3</td>
<td>1.7</td>
<td>Yes</td></tr>
'''

def jacksonDataTypeJsr310 = '''\
<tr class="a">
<td>jackson-datatype-jsr310-2.6.4.jar</td>
<td>78.12 kB</td>
<td>69</td>
<td>51</td>
<td>5</td>
<td>1.8</td>
<td>Yes</td></tr>
'''

assert dependencies.text.contains( mavenModel.replaceAll( "\n", System.getProperty( "line.separator" ) ) )
assert dependencies.text.contains( jacksonDataTypeJsr310.replaceAll( "\n", System.getProperty( "line.separator" ) ) )
