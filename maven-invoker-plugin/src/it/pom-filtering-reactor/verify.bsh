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

boolean check( String project, boolean filtered )
{
    File pomFile = new File( basedir, "target/it/" + project + "/pom.xml" );
    System.out.println( "Checking for existence of " + ( filtered ? "" : "un" ) + "filtered POM: " + pomFile );
    if ( !pomFile.exists() )
    {
        System.out.println( "FAILED!" );
        return false;
    }

    String xml = FileUtils.fileRead( pomFile, "UTF-8" );

    String[] values = {
            "<prop0>pom-filtering-reactor</prop0>",
        };
    for ( String value : values )
    {
        System.out.println( "Checking for " + ( filtered ? "occurrence" : "absence" ) + " of: " + value );
        if ( ( filtered && xml.indexOf( value ) < 0 ) || ( !filtered && xml.indexOf( value ) >= 0 ) )
        {
            System.out.println( "FAILED!" );
            return false;
        }
    }
    return true;
}

try
{
    return check( "mod2-parent", true ) && check( "mod2-parent/mod2", true ) && check( "mod2-parent/mod1-parent", true )
            && check( "mod2-parent/mod1", true ) && check( "mod2-parent/mod3", true)
            && check( "mod2-parent/mod1/src/it", false );
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
