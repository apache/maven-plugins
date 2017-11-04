
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

try
{
    File target = new File( basedir, "module1/target" );
    if ( !target.exists() || !target.isDirectory() )
    {
        System.err.println( "module1/target file is missing or a directory." );
        return false;
    }

    File apidocs = new File( basedir, "module1/target/site/apidocs" );
    if ( !apidocs.exists() || !apidocs.isDirectory() )
    {
        System.err.println( "module1/target/site/apidocs file is missing or a directory." );
        return false;
    }

    File options = new File( apidocs, "options" );
    if ( !options.exists() || !options.isFile() )
    {
        System.err.println( "module1/target/site/apidocs/options file is missing or not a file." );
        return false;
    }

    File index = new File( apidocs, "index.html" );
    if ( !index.exists() || !index.isFile() )
    {
        System.err.println( "module1/target/site/apidocs/index.html file is missing or not a file." );
        return false;
    }
    
    File reference = new File( apidocs, "reference" );
    if ( !reference.exists() || reference.isFile() )
    {
        System.err.println( "module1/target/site/apidocs/reference file is missing or a file." );
        return false;
    }    
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
