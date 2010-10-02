package org.apache.maven.plugin.eclipse.it;

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
import java.util.List;
import java.util.Properties;

import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.test.plugin.BuildTool;
import org.codehaus.plexus.util.StringUtils;

/**
 * A build tool impl that's smart enough to respect maven.repo.local.
 * 
 * @plexus.component role="org.apache.maven.shared.test.plugin.BuildTool" role-hint="default"
 */
public class FixedBuildTool
    extends BuildTool
{

    public InvocationRequest createBasicInvocationRequest( File pom, Properties properties, List goals,
                                                           File buildLogFile )
    {
        InvocationRequest request = super.createBasicInvocationRequest( pom, properties, goals, buildLogFile );

        request.setLocalRepositoryDirectory( findLocalRepo() );

        return request;
    }

    private File findLocalRepo()
    {
        String basedir = System.getProperty( "maven.repo.local" );

        if ( StringUtils.isNotEmpty( basedir ) )
        {
            return new File( basedir );
        }

        return null;
    }

}
