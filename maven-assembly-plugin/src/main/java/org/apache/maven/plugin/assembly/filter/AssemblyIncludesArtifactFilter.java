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
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * TODO: include in maven-artifact in future
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class AssemblyIncludesArtifactFilter
    implements ArtifactFilter, StatisticsReportingFilter
{
    private final List positivePatterns;
    private final List negativePatterns;
    private final boolean actTransitively;

    private Set patternsTriggered = new HashSet();
    private List filteredArtifactIds = new ArrayList();

    public AssemblyIncludesArtifactFilter( List patterns )
    {
        this( patterns, false );
    }

    public AssemblyIncludesArtifactFilter( List patterns, boolean actTransitively )
    {
        this.actTransitively = actTransitively;
        List pos = new ArrayList();
        List neg = new ArrayList();
        if ( patterns != null && !patterns.isEmpty() )
        {
            for ( Iterator it = patterns.iterator(); it.hasNext(); )
            {
                String pattern = (String) it.next();
                
                if ( pattern.startsWith( "!" ) )
                {
                    neg.add( pattern.substring( 1 ) );
                }
                else
                {
                    pos.add( pattern );
                }
            }
        }
        
        this.positivePatterns = pos;
        this.negativePatterns = neg;
    }

    public boolean include( Artifact artifact )
    {
        boolean shouldInclude = patternMatches( artifact );

        if ( !shouldInclude )
        {
            addFilteredArtifactId( artifact.getId() );
        }

        return shouldInclude;
    }

    protected boolean patternMatches( Artifact artifact )
    {
        return positiveMatch( artifact ) && !negativeMatch( artifact );
    }

    protected void addFilteredArtifactId( String artifactId )
    {
        filteredArtifactIds.add( artifactId );
    }

    private boolean negativeMatch( Artifact artifact )
    {
        return match( artifact, negativePatterns );
    }

    protected boolean positiveMatch( Artifact artifact )
    {
        return match( artifact, positivePatterns );
    }

    private boolean match( Artifact artifact, List patterns )
    {
        String shortId = ArtifactUtils.versionlessKey( artifact );
        String id = artifact.getDependencyConflictId();
        
        if ( matchAgainst( id, patterns, false ) )
        {
            return true;
        }
        
        if ( matchAgainst( shortId, patterns, false ) )
        {
            return true;
        }

        if ( actTransitively )
        {
            List depTrail = artifact.getDependencyTrail();

            if ( depTrail != null && !depTrail.isEmpty() )
            {
                String trailStr = "," + StringUtils.join( depTrail.iterator(), "," );

                return matchAgainst( trailStr, patterns, true );
            }
        }

        return false;
    }

    private boolean matchAgainst( String value, List patterns, boolean regionMatch )
    {
        for ( Iterator i = patterns.iterator(); i.hasNext(); )
        {
            // TODO: what about wildcards? Just specifying groups? versions?
            String pattern = (String) i.next();

            if ( regionMatch )
            {
                if ( value.indexOf( pattern ) > -1 )
                {
                    patternsTriggered.add( pattern );
                    return true;
                }
            }
            else
            {
                if ( value.equals( pattern ) )
                {
                    patternsTriggered.add( pattern );
                    return true;
                }
            }
            
            if ( pattern.indexOf( '*' ) > -1 )
            {
                String[] subPatterns = pattern.split( "\\*" );
                int[] idxes = new int[subPatterns.length];
                
                for ( int j = 0; j < subPatterns.length; j++ )
                {
                    String subPattern = subPatterns[j];
                    
                    int lastIdx = j == 0 ? 0 : idxes[j-1];
                    
                    idxes[j] = value.indexOf( subPattern, lastIdx );
                    
                    if ( idxes[j] < 0 )
                    {
                        return false;
                    }
                }
                
                patternsTriggered.add( pattern );
                return true;
            }
        }
        
        return false;
    }

    public void reportMissedCriteria( Logger logger )
    {
        // if there are no patterns, there is nothing to report.
        if ( !positivePatterns.isEmpty() )
        {
            List missed = new ArrayList( positivePatterns );
            missed.removeAll( patternsTriggered );

            if ( !missed.isEmpty() && logger.isWarnEnabled() )
            {
                StringBuffer buffer = new StringBuffer();

                buffer.append( "The following patterns were never triggered in this " );
                buffer.append( getFilterDescription() );
                buffer.append( ':' );

                for ( Iterator it = missed.iterator(); it.hasNext(); )
                {
                    String pattern = (String) it.next();

                    buffer.append( "\no  \'" ).append( pattern ).append( "\'" );
                }

                buffer.append( "\n" );

                logger.warn( buffer.toString() );
            }
        }
    }

    public String toString()
    {
        return "Includes filter:" + getPatternsAsString();
    }

    protected String getPatternsAsString()
    {
        StringBuffer buffer = new StringBuffer();
        for ( Iterator it = positivePatterns.iterator(); it.hasNext(); )
        {
            String pattern = ( String ) it.next();

            buffer.append( "\no \'" ).append( pattern ).append( "\'" );
        }

        return buffer.toString();
    }

    protected String getFilterDescription()
    {
        return "artifact inclusion filter";
    }

    public void reportFilteredArtifacts( Logger logger )
    {
        if ( !filteredArtifactIds.isEmpty() && logger.isDebugEnabled() )
        {
            StringBuffer buffer = new StringBuffer( "The following artifacts were removed by this " + getFilterDescription() + ": " );

            for ( Iterator it = filteredArtifactIds.iterator(); it.hasNext(); )
            {
                String artifactId = ( String ) it.next();

                buffer.append( '\n' ).append( artifactId );
            }

            logger.debug( buffer.toString() );
        }
    }

}
