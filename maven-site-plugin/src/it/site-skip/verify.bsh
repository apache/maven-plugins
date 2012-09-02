
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
    final File deployDirectory = new File( basedir, "deployed" );
    final File stageDirectory = new File( basedir, "target/staging" );
    final File stageDeployDirectory = new File( deployDirectory, "staging" );

    // top

    File siteDir = new File( basedir, "target/site" );
    if ( !siteDir.exists() || !siteDir.isDirectory() )
    {
        System.err.println( "top site is missing or not a directory." );
        result = false;
    }

    File stageDir = new File( stageDirectory, "." );
    if ( !stageDir.exists() || !stageDir.isDirectory() )
    {
        System.err.println( "top stage site is missing or not a directory." );
        result = false;
    }

    File deployDir = new File( deployDirectory, "." );
    if ( !deployDir.exists() || !deployDir.isDirectory() )
    {
        System.err.println( "top deploy site is missing or not a directory." );
        result = false;
    }

    File stageDeployDir = new File( stageDeployDirectory, "." );
    if ( !stageDeployDir.exists() || !stageDeployDir.isDirectory() )
    {
        System.err.println( "top stage-deploy site is missing or not a directory." );
        result = false;
    }

    // site

    siteDir = new File( basedir, "site/target/site" );
    if ( !siteDir.exists() || !siteDir.isDirectory() )
    {
        System.err.println( "site site is missing or not a directory." );
        result = false;
    }

    stageDir = new File( stageDirectory, "site" );
    if ( !stageDir.exists() || !stageDir.isDirectory() )
    {
        System.err.println( "site stage site is missing or not a directory." );
        result = false;
    }

    deployDir = new File( deployDirectory, "site" );
    if ( !deployDir.exists() || !deployDir.isDirectory() )
    {
        System.err.println( "site deploy site is missing or not a directory." );
        result = false;
    }

    stageDeployDir = new File( stageDeployDirectory, "site" );
    if ( !stageDeployDir.exists() || !stageDeployDir.isDirectory() )
    {
        System.err.println( "site stage-deploy site is missing or not a directory." );
        result = false;
    }

    // skip-site

    siteDir = new File( basedir, "skip-site/target/site" );
    if ( siteDir.exists() )
    {
        System.err.println( "skip-site site exists." );
        result = false;
    }

    stageDir = new File( stageDirectory, "skip-site" );
    if ( stageDir.exists() )
    {
        System.err.println( "skip-site stage site exists." );
        result = false;
    }

    deployDir = new File( deployDirectory, "skip-site" );
    if ( deployDir.exists() )
    {
        System.err.println( "skip-site deploy site exists." );
        result = false;
    }

    stageDeployDir = new File( stageDeployDirectory, "skip-site" );
    if ( stageDeployDir.exists() )
    {
        System.err.println( "skip-site stage-deploy site exists." );
        result = false;
    }

    // skip-site-deploy

    siteDir = new File( basedir, "skip-site-deploy/target/site" );
    if ( !siteDir.exists() || !siteDir.isDirectory() )
    {
        System.err.println( "skip-site-deploy site is missing or not a directory." );
        result = false;
    }

    stageDir = new File( stageDirectory, "skip-site-deploy" );
    if ( !stageDir.exists() )
    {
        System.err.println( "skip-site-deploy stage site should still exist." );
        result = false;
    }

    deployDir = new File( deployDirectory, "skip-site-deploy" );
    if ( deployDir.exists() )
    {
        System.err.println( "skip-site-deploy deploy site exists." );
        result = false;
    }

    stageDeployDir = new File( stageDeployDirectory, "skip-site-deploy" );
    if ( stageDeployDir.exists() )
    {
        System.err.println( "skip-site-deploy stage-deploy site exists." );
        result = false;
    }

}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
