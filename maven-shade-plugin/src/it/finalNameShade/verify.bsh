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
    // check for primary artifact, which is the unshaded one!
    File file = new File( basedir, "target/finalNameShade-1.0.jar" );
    System.out.println( "file with finalName : " + file );
    if ( !file.exists() )
    {
        System.out.println( "file with finalName does not exist: " + file );
        return false;
    }

    // check for the shaded jar
    File shaded = new File( basedir, "target/MyFinalShadeName.jar" );
    System.out.println( "shaded file with name : " + shaded );
    if ( !shaded.exists() )
    {
        System.out.println( "shaded file does not exist: " + shaded );
        return false;
    }

    // check for the artifact in the repo
    File rfile = new File( localRepositoryPath
                         ,  "org/apache/maven/its/shade/fns/finalNameShade/1.0/finalNameShade-1.0.jar" );
    System.out.println( "Checking for existence in repo: " + rfile );
    if ( !rfile.isFile() )
    {
        throw new FileNotFoundException( "Missing: " + rfile.getAbsolutePath() );
    }

}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
