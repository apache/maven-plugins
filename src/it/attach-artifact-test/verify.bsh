
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

import org.codehaus.plexus.util.*;

try
{
    String zipFilePath = "../../local-repo/antrun-plugin/test/antrun-plugin-attach-artifact-test/1.0-SNAPSHOT/antrun-plugin-attach-artifact-test-1.0-SNAPSHOT-foo.zip";
    File installedZipFile = new File( basedir, zipFilePath );

    if ( ! installedZipFile.exists() )
    {
        System.out.println( "Zip file not installed: " + installedZipFile );
        return false;
    }

    String zipWithouClassifierFilePath = "../../local-repo/antrun-plugin/test/antrun-plugin-attach-artifact-test/1.0-SNAPSHOT/antrun-plugin-attach-artifact-test-1.0-SNAPSHOT.zip";
    File installedZipWithouClassifierFile = new File( basedir, zipWithouClassifierFilePath );

    if ( ! installedZipWithouClassifierFile.exists() )
    {
        System.out.println( "Zip file (without classifier) not installed: " + installedZipWithouClassifierFile );
        return false;
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
