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

import java.io.*;

boolean result = true;

// verify that transitive deps of the POM dep were included.
result = result && new File( basedir, "target/including-sar-dependency-1.0-SNAPSHOT-bin/lib/xmlrpc" ).exists();
result = result && new File( basedir, "target/including-sar-dependency-1.0-SNAPSHOT-bin/lib/plexus-xmlrpc" ).exists();

result = result && new File( basedir, "target/including-sar-dependency-1.0-SNAPSHOT-bin/lib/plexus-appserver-service-xmlrpc/lib/commons-codec-1.3.jar" ).exists();

return result;
