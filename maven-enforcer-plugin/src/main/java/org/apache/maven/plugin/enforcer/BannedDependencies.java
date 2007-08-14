package org.apache.maven.plugin.enforcer;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.enforcer.util.EnforcerUtils;
import org.apache.maven.shared.enforcer.rule.api.EnforcerRuleException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that lists of dependencies are not
 * included.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 * 
 */
public class BannedDependencies
    extends AbstractBanDependencies
{

    /**
     * Specify the banned dependencies. This can be a list
     * of artifacts in the format
     * groupId[:artifactId][:version] Any of the sections
     * can be a wildcard by using '*' (ie group:*:1.0)
     * 
     * @parameter
     * @required
     */
    public ArrayList excludes = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.enforcer.AbstractBanDependencies#checkDependencies(java.util.Set)
     */
    protected Set checkDependencies( Set theDependencies )
        throws EnforcerRuleException
    {
        return checkDependencies( theDependencies, excludes );
    }

    /**
     * Checks the set of dependencies against the list of
     * excludes
     * 
     * @param dependencies
     * @return
     * @throws EnforcerRuleException
     */
    private Set checkDependencies( Set dependencies, List theExcludes )
        throws EnforcerRuleException
    {
        Set foundExcludes = new HashSet();

        Iterator iter = theExcludes.iterator();
        while ( iter.hasNext() )
        {
            String exclude = (String) iter.next();

            String[] subStrings = exclude.split( ":" );
            subStrings = StringUtils.stripAll( subStrings );

            Iterator DependencyIter = dependencies.iterator();
            while ( DependencyIter.hasNext() )
            {
                Artifact artifact = (Artifact) DependencyIter.next();

                if ( compareDependency( subStrings, artifact ) )
                {
                    foundExcludes.add( artifact );
                }
            }
        }
        return foundExcludes;
    }

    /**
     * Compares the parsed array of substrings against the
     * artifact
     * 
     * @param exclude
     * @param artifact
     * @return
     * @throws EnforcerRuleException
     */
    protected boolean compareDependency( String[] exclude, Artifact artifact )
        throws EnforcerRuleException
    {

        boolean result = false;
        if ( exclude.length > 0 )
        {
            result = exclude[0].equals( "*" ) || artifact.getGroupId().equals( exclude[0] );
        }

        if ( result && exclude.length > 1 )
        {
            result = exclude[1].equals( "*" ) || artifact.getArtifactId().equals( exclude[1] );
        }

        if ( result && exclude.length > 2 )
        {
            // short circuit if the versions are exactly the
            // same
            if ( exclude[2].equals( "*" ) || artifact.getVersion().equals( exclude[2] ) )
            {
                result = true;
            }
            else
            {
                try
                {
                    result = EnforcerUtils.containsVersion( VersionRange.createFromVersionSpec( exclude[2] ),
                                                            new DefaultArtifactVersion( artifact.getVersion() ) );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new EnforcerRuleException( "Invalid Version Range: ", e );
                }
            }
        }

        return result;

    }

    /**
     * @return the excludes
     */
    public ArrayList getExcludes()
    {
        return this.excludes;
    }

    /**
     * @param theExcludes the excludes to set
     */
    public void setExcludes( ArrayList theExcludes )
    {
        this.excludes = theExcludes;
    }

}
