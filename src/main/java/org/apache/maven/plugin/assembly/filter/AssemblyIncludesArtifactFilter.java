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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.codehaus.plexus.util.StringUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TODO: include in maven-artifact in future
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class AssemblyIncludesArtifactFilter
    implements ArtifactFilter
{
    private final List patterns;
    private final boolean actTransitively;

    public AssemblyIncludesArtifactFilter( List patterns )
    {
        this.actTransitively = false;
        this.patterns = Collections.unmodifiableList( patterns );
    }

    public AssemblyIncludesArtifactFilter( List patterns, boolean actTransitively )
    {
        this.actTransitively = actTransitively;
        this.patterns = Collections.unmodifiableList( patterns );
    }

    public boolean include( Artifact artifact )
    {
        String shortId = artifact.getGroupId() + ":" + artifact.getArtifactId();
        String id = artifact.getDependencyConflictId();

        boolean matched = false;
        for ( Iterator i = patterns.iterator(); i.hasNext() && !matched; )
        {
            // TODO: what about wildcards? Just specifying groups? versions?
            String pattern = (String) i.next();
            if ( id.equals( pattern ) )
            {
                matched = true;
            }
            else if ( shortId.equals( pattern ) )
            {
                matched = true;
            }
        }
        
        if ( !matched && actTransitively )
        {
            List depTrail = artifact.getDependencyTrail();
            if ( depTrail != null && !depTrail.isEmpty() )
            {
                String trailStr = StringUtils.join( depTrail.iterator(), "," );
                
                for ( Iterator it = patterns.iterator(); it.hasNext(); )
                {
                    String pattern = (String) it.next();
                    
                    if ( trailStr.indexOf( pattern ) > -1 )
                    {
                        matched = true;
                        break;
                    }
                }
            }
        }
        
        return matched;
    }
}
