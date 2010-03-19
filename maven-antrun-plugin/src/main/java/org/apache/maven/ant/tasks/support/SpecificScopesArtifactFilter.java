package org.apache.maven.ant.tasks.support;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

/**
 * Filter to only retain objects in the given scope(s).
 *
 * @author pgier
 * @version $Id$
 */
public class SpecificScopesArtifactFilter
    implements ArtifactFilter
{
    private boolean compileScope;

    private boolean runtimeScope;

    private boolean testScope;

    private boolean providedScope;

    private boolean systemScope;

    /**
     * Takes a comma separated list of scopes to include.
     * 
     * @param scopes A comma separated list of scopes
     */
    public SpecificScopesArtifactFilter( String scopes )
    {
        String [] scopeList = scopes.split( "," );
        
        for ( int i=0; i<scopeList.length; ++i )
        {
            if ( scopeList[i].trim().equals( DefaultArtifact.SCOPE_COMPILE) )
            {
                compileScope = true;
            }
            else if ( scopeList[i].trim().equals( DefaultArtifact.SCOPE_PROVIDED) )
            {
                providedScope = true;
            }
            else if ( scopeList[i].trim().equals( DefaultArtifact.SCOPE_RUNTIME) )
            {
                runtimeScope = true;
            }
            else if ( scopeList[i].trim().equals( DefaultArtifact.SCOPE_SYSTEM) )
            {
                systemScope = true;
            }
            else if ( scopeList[i].trim().equals( DefaultArtifact.SCOPE_TEST) )
            {
                testScope = true;
            }
        }
    }

    public boolean include( Artifact artifact )
    {
        if ( Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) )
        {
            return compileScope;
        }
        else if ( Artifact.SCOPE_RUNTIME.equals( artifact.getScope() ) )
        {
            return runtimeScope;
        }
        else if ( Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
        {
            return testScope;
        }
        else if ( Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) )
        {
            return providedScope;
        }
        else if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            return systemScope;
        }
        else
        {
            return true;
        }
    }
}
