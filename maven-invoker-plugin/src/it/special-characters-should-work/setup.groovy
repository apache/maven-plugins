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

import java.io.*
import java.util.*
import java.util.regex.*

File srcIt = new File (basedir, "src/it");
File originalFolder = new File (srcIt, "minvoker-test");
File destinationFolder = new File(srcIt, "test-\u00c9\u00e9\u00ea-more-\u00c9\u00e9\u00ea-test");
// rename old one into new one with special characters.
if (!originalFolder.renameTo(destinationFolder)) {
  throw new IOException("Rename didn't work.")
}
