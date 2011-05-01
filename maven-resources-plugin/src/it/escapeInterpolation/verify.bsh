
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

import org.codehaus.plexus.util.*;

boolean result = true;

try
{
    File target = new File( basedir, "target" );
    if ( !target.exists() || !target.isDirectory() )
    {
        System.err.println( "target file is missing or a directory." );
        return false;
    }

    File someResource = new File( target, "/classes/SomeResource.txt" );
    if ( !someResource.exists() || someResource.isDirectory() )
    {
        System.err.println( "SomeResource.txt is missing or not a file." );
        return false;
    }

    String paramContent = FileUtils.fileRead( someResource );

    //test:direct resolution project.version=1.0-SNAPSHOT
    int indexOf = paramContent.indexOf( "test:direct resolution project.version=1.0-SNAPSHOT" );
    if ( indexOf < 0 )
    {
      System.err.println( "/classes/SomeResource.txt not contains test:direct resolution project.version=1.0-SNAPSHOT" );
      return false;
    }


    //test:filter resolution project.version=1.0-SNAPSHOT
    indexOf = paramContent.indexOf( "test:filter resolution project.version=1.0-SNAPSHOT" );
    if ( indexOf < 0 )
    {
      System.err.println( "/classes/SomeResource.txt not contains test:filter resolution project.version=1.0-SNAPSHOT" );
      return false;
    }

    //test:filter direct projectProperty=foo-projectProperty-bar
    indexOf = paramContent.indexOf( "test:filter direct projectProperty=foo-projectProperty-bar" );
    if ( indexOf < 0 )
    {
      System.err.println( "/classes/SomeResource.txt not contains test:filter direct projectProperty=foo-projectProperty-bar" );
      return false;
    }

    //test:filter resolution projectProperty=foo-projectProperty-bar
    indexOf = paramContent.indexOf( "test:filter resolution projectProperty=foo-projectProperty-bar" );
    if ( indexOf < 0 )
    {
      System.err.println( "/classes/SomeResource.txt not contains test:filter resolution projectProperty=foo-projectProperty-bar" );
      return false;
    }

    //test:filter direct profileProperty=foo-profileProperty-bar
    indexOf = paramContent.indexOf( "test:filter direct profileProperty=foo-profileProperty-bar" );
    if ( indexOf < 0 )
    {
      System.err.println( "/classes/SomeResource.txt not contains test:filter direct profileProperty=foo-profileProperty-bar" );
      return false;
    }

    //test:filter resolution profileProperty=foo-profileProperty-bar
    indexOf = paramContent.indexOf( "test:filter resolution profileProperty=foo-profileProperty-bar" );
    if ( indexOf < 0 )
    {
      System.err.println( "/classes/SomeResource.txt not contains test:filter resolution profileProperty=foo-profileProperty-bar" );
      return false;
    }

    //test:filter syspropsExecutionPropsWins=fromExecProps
    indexOf = paramContent.indexOf( "test:filter syspropsExecutionPropsWins=fromExecProps" );
    if ( indexOf < 0 )
    {
      System.err.println( "/classes/SomeResource.txt not contains test:filter syspropsExecutionPropsWins=fromExecProps" );
      return false;
    }

    //newToken=foo
    indexOf = paramContent.indexOf( "newToken=foo" );
    if ( indexOf < 0 )
    {
      System.err.println( "/classes/SomeResource.txt not contains newToken=foo" );
      return false;
    }

    //escapeString=${project.version}
    indexOf = paramContent.indexOf( "escapeString=${project.artifactId}" );
    if ( indexOf < 0 )
    {
      System.err.println( "/classes/SomeResource.txt not contains escapeString=${project.version}" );
      System.out.println (" content " + paramContent );
      return false;
    }

    // -------------------------
    // assert on test resources
    // -------------------------


    someResource = new File( target, "/test-classes/SomeResource.txt" );
    if ( !someResource.exists() || someResource.isDirectory() )
    {
        System.err.println( "/test-classes/SomeResource.txt is missing or not a file." );
        return false;
    }

    paramContent = FileUtils.fileRead( someResource );

    //test:direct resolution project.version=1.0-SNAPSHOT
    int indexOf = paramContent.indexOf( "test:direct resolution project.version=1.0-SNAPSHOT" );
    if ( indexOf < 0 )
    {
      System.err.println( "/test-classes/SomeResource.txt not contains test:direct resolution project.version=1.0-SNAPSHOT" );
      return false;
    }


    //test:filter resolution project.version=1.0-SNAPSHOT
    indexOf = paramContent.indexOf( "test:filter resolution project.version=1.0-SNAPSHOT" );
    if ( indexOf < 0 )
    {
      System.err.println( "/test-classes/SomeResource.txt not contains test:filter resolution project.version=1.0-SNAPSHOT" );
      return false;
    }

    //test:filter direct projectProperty=foo-projectProperty-bar
    indexOf = paramContent.indexOf( "test:filter direct projectProperty=foo-projectProperty-bar" );
    if ( indexOf < 0 )
    {
      System.err.println( "/test-classes/SomeResource.txt not contains test:filter direct projectProperty=foo-projectProperty-bar" );
      return false;
    }

    //test:filter resolution projectProperty=foo-projectProperty-bar
    indexOf = paramContent.indexOf( "test:filter resolution projectProperty=foo-projectProperty-bar" );
    if ( indexOf < 0 )
    {
      System.err.println( "/test-classes/SomeResource.txt not contains test:filter resolution projectProperty=foo-projectProperty-bar" );
      return false;
    }

    //test:filter direct profileProperty=foo-profileProperty-bar
    indexOf = paramContent.indexOf( "test:filter direct profileProperty=foo-profileProperty-bar" );
    if ( indexOf < 0 )
    {
      System.err.println( "/test-classes/SomeResource.txt not contains test:filter direct profileProperty=foo-profileProperty-bar" );
      return false;
    }

    //test:filter resolution profileProperty=foo-profileProperty-bar
    indexOf = paramContent.indexOf( "test:filter resolution profileProperty=foo-profileProperty-bar" );
    if ( indexOf < 0 )
    {
      System.err.println( "/test-classes/SomeResource.txt not contains test:filter resolution profileProperty=foo-profileProperty-bar" );
      return false;
    }

    //escapeString=${project.version}
    indexOf = paramContent.indexOf( "escapeString=${project.version}" );
    if ( indexOf < 0 )
    {
      System.err.println( "/test-classes/SomeResource.txt not contains escapeString=${project.version}" );
      return false;
    }

}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
