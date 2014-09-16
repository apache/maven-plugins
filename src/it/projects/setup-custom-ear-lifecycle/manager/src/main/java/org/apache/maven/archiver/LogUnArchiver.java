package org.apache.maven.archiver;

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


import java.io.File;

import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.ArchiverException;

public class LogUnArchiver extends AbstractUnArchiver
{
 
    @Override
    protected void execute()
        throws ArchiverException
    {
        getLogger().info( "LogUnArchiver.execute()" );
    }
    
    @Override
    protected void execute( String path, File outputDirectory )
        throws ArchiverException
    {
        getLogger().info( "LogUnArchiver.execute( String path, File outputDirectory )" );
        getLogger().info( "  path = " + path );
        getLogger().info( "  outputDirectory = " + outputDirectory );
    }
}
