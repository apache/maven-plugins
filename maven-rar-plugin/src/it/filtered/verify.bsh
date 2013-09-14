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
import org.codehaus.plexus.util.*;

try
{
    File jarFile = new File( basedir, "target/maven-it-non-filtered-1.0-SNAPSHOT.rar" );
    System.out.println( "Checking for existence of " + jarFile );
    if ( !jarFile.isFile() )
    {
        System.out.println( "FAILURE! rar file not found" );
        return false;
    }

    JarFile jar = new JarFile( jarFile );

    String[] includedEntries = {
        "META-INF/ra.xml",
        "SomeResource.txt",
        "ext/wine.txt",
        "ext-filtered/wine.txt"
    };
    for ( String included : includedEntries )
    {
        System.out.println( "Checking for existence of " + included );
        if ( jar.getEntry( included ) == null )
        {
            System.out.println( "FAILURE! entry not in rar file" + included );
            return false;
        }
    }

    InputStream stream = jar.getInputStream( jar.getEntry("SomeResource.txt") );

    String content = new String(IOUtil.toByteArray( stream ));

    int idx = content.indexOf("1.0-SNAPSHOT");

    if (idx<1) {
      System.out.println("SomeResource.txt not filtered");
      return false;
    }

    stream = jar.getInputStream( jar.getEntry("ext/wine.txt") );

    content = new String(IOUtil.toByteArray( stream ));

    idx = content.indexOf("${bestwine}");

    if (idx<1) {
      System.out.println("ext/wine.txt filtered:"+content);
      return false;
    }

    stream = jar.getInputStream( jar.getEntry("ext-filtered/wine.txt") );

    content = new String(IOUtil.toByteArray( stream ));

    idx = content.indexOf("bordeaux");

    if (idx<1) {
      System.out.println("ext-filtered/wine.txt filtered:"+content);
      return false;
    }

    jar.close();
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
