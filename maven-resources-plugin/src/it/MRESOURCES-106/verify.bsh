
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

boolean result = true;

try
{
    File target = new File( basedir, "target" );
    if ( !target.exists() || !target.isDirectory() )
    {
        System.err.println( "target file is missing or a directory." );
        return false;
    }

    File someResource = new File( target, "/classes/filter.properties" );
    if ( !someResource.exists() || !someResource.isFile() )
    {
        System.err.println( "file.txt is missing or not a file." );
        return false;
    }

    FileInputStream fis = new FileInputStream( someResource );
    Properties p = new Properties();
    p.load( fis );

    File f = new File( p.getProperty( "location" ) );
    if ( !f.exists() )
    {
        System.err.println( "Property was not correctly filtered: " + f + "\n" + f.getAbsolutePath() );
        result = false;
    }

    File basedir = new File( p.getProperty( "basedir" ) );
    if ( !basedir.exists() )
    {
        System.err.println( "Property was not correctly filtered: " + basedir + "\n" + basedir.getAbsolutePath() );
        result = false;
    }

    File expectedLocation = new File( basedir, "src/main/resources/file.txt" );
    if ( !expectedLocation.equals( f ) )
    {
        System.err.println( "Files doesn't match:\n" + basedir + "\n" + expectedLocation );
        result = false;
    }
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
