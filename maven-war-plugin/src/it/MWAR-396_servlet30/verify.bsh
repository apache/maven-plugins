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
import java.util.jar.*;
import java.util.regex.*;

try
{
    File explodedDir = new File( basedir, "target/maven-it-mwar396_servlet30-1.0" );
    System.out.println( "Checking for existence of exploded directory " + explodedDir );
    if ( !explodedDir.exists() )
    {
        System.out.println( "FAILURE! The directory " + explodedDir + " does not exist." );
        return false;
    }
    
    File webInfFile = new File( explodedDir, "WEB-INF/web.xml" );
    if ( webInfFile.exists() )
    {
        System.err.println( "FAILURE! The file web.xml should not be present." );
        return false;
    }

}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
