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
import java.util.zip.ZipEntry;
import org.codehaus.plexus.util.IOUtil;

try
{
    File file = new File( basedir, "target/mejb-34-not-filtered-1.0.jar" );
    System.out.println( "Checking for existence of " + file );
    if ( !file.isFile() )
    {
        System.out.println( "FAILURE! Missing JAR file" );
        return false;
    }

    JarFile jarFile = new JarFile( file );

    ZipEntry zipEntry = jarFile.getEntry( "META-INF/ejb-jar.xml" );
    if ( zipEntry == null )
    {
        System.out.println( "FAILURE! Missing META-INF/ejb-jar.xml in the JAR file" );
        return false;
    }
    else
    {
        InputStream is = jarFile.getInputStream( zipEntry );
        String contents = IOUtil.toString ( is, "UTF-8" );
        int index = contents.indexOf( "myKey" );
        if ( index < 0 )
        {
            System.out.println( "FAILURE! The key 'myKey' has been replaced, but filtering is turned off" );
            return false;
        }
        index = contents.indexOf( "myValue" );
        if ( index >= 0 )
        {
            System.out.println( "FAILURE! The value 'myValue' has been injected, but filtering is turned off" );
            return false;
        }
        is.close();
    }

    jarFile.close();
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
