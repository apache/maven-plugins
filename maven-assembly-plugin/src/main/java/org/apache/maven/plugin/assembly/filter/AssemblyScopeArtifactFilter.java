package org.apache.maven.plugin.assembly.filter;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import org.codehaus.plexus.logging.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TODO: include in maven-artifact in future
 */
public class AssemblyScopeArtifactFilter
    implements ArtifactFilter, StatisticsReportingFilter
{
    private final boolean compileScope;

    private final boolean runtimeScope;

    private final boolean testScope;

    private final boolean providedScope;

    private final boolean systemScope;

    private boolean compileScopeHit = false;

    private boolean runtimeScopeHit = false;

    private boolean testScopeHit = false;

    private boolean providedScopeHit = false;

    private boolean systemScopeHit = false;

    private List filteredArtifactIds = new ArrayList();

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
        else if ( DefaultArtifact.SCOPE_SYSTEM.equals( scope ) )
        {
            systemScope = true;
            providedScope = false;
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
        boolean result = true;

        if ( Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) )
        {
            compileScopeHit = true;
            result = compileScope;
        }
        else if ( Artifact.SCOPE_RUNTIME.equals( artifact.getScope() ) )
        {
            runtimeScopeHit = true;
            result = runtimeScope;
        }
        else if ( Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
        {
            testScopeHit = true;
            result = testScope;
        }
        else if ( Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) )
        {
            providedScopeHit = true;
            result = providedScope;
        }
        else if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            systemScopeHit = true;
            result = systemScope;
        }

        if ( !result )
        {
            filteredArtifactIds.add( artifact.getId() );
        }

        return result;
    }

    public String toString()
    {
        return "Scope filter [compile=" + compileScope + ", runtime=" + runtimeScope + ", test=" + testScope
                        + ", provided=" + providedScope + ", system=" + systemScope + "]";
    }

    public void reportFilteredArtifacts( Logger logger )
    {
        if ( !filteredArtifactIds.isEmpty() && logger.isDebugEnabled() )
        {
            StringBuffer buffer = new StringBuffer( "The following artifacts were removed by this filter: " );

            for ( Iterator it = filteredArtifactIds.iterator(); it.hasNext(); )
            {
                String artifactId = ( String ) it.next();

                buffer.append( '\n' ).append( artifactId );
            }

            logger.debug( buffer.toString() );
        }
    }

    public void reportMissedCriteria( Logger logger )
    {
        if ( logger.isDebugEnabled() )
        {
            StringBuffer buffer = new StringBuffer();

            boolean report = false;
            if ( !compileScopeHit )
            {
                buffer.append( "\no Compile" );
                report = true;
            }
            if ( !runtimeScopeHit )
            {
                buffer.append( "\no Runtime" );
                report = true;
            }
            if ( !testScopeHit )
            {
                buffer.append( "\no Test" );
                report = true;
            }
            if ( !providedScopeHit )
            {
                buffer.append( "\no Provided" );
                report = true;
            }
            if ( !systemScopeHit )
            {
                buffer.append( "\no System" );
                report = true;
            }

            if ( report )
            {
                logger.debug( "The following scope filters were not used: " + buffer.toString() );
            }
        }
    }

    public boolean hasMissedCriteria()
    {
        boolean report = false;
        
        if ( !compileScopeHit )
        {
            report = true;
        }
        if ( !runtimeScopeHit )
        {
            report = true;
        }
        if ( !testScopeHit )
        {
            report = true;
        }
        if ( !providedScopeHit )
        {
            report = true;
        }
        if ( !systemScopeHit )
        {
            report = true;
        }

        return report;
    }
}
