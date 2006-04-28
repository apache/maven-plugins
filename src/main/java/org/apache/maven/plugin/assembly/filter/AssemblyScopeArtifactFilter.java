package org.apache.maven.plugin.assembly.filter;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

public class AssemblyScopeArtifactFilter
    implements ArtifactFilter
{
    private final boolean compileScope;

    private final boolean runtimeScope;

    private final boolean testScope;

    private final boolean providedScope;

    private final boolean systemScope;

    public AssemblyScopeArtifactFilter( String scope )
    {
        if ( DefaultArtifact.SCOPE_COMPILE.equals( scope ) )
        {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = false;
            testScope = false;
        }
        else if ( DefaultArtifact.SCOPE_RUNTIME.equals( scope ) )
        {
            systemScope = false;
            providedScope = false;
            compileScope = true;
            runtimeScope = true;
            testScope = false;
        }
        else if ( DefaultArtifact.SCOPE_TEST.equals( scope ) )
        {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = true;
            testScope = true;
        }
        else if ( DefaultArtifact.SCOPE_PROVIDED.equals( scope ) )
        {
            systemScope = false;
            providedScope = true;
            compileScope = false;
            runtimeScope = false;
            testScope = false;
        }
        else
        {
            systemScope = false;
            providedScope = false;
            compileScope = false;
            runtimeScope = false;
            testScope = false;
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
