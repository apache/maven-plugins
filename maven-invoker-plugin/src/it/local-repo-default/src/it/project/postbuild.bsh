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
import java.util.*;
import java.util.regex.*;

try
{
    // just dump our local repo path for verification by the parent build
    File textFile = new File( basedir, "target/local-repo-path.txt" );
    textFile.getParentFile().mkdirs();
    Writer writer = new OutputStreamWriter( new FileOutputStream( textFile ), "UTF-8" );
    writer.write( localRepositoryPath.getAbsolutePath() );
    writer.close();
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
