package org.apache.maven.plugin.ear.util;

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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * An artifact repository used to resolve {@link org.apache.maven.plugin.ear.EarModule}.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class ArtifactRepository
{
    private final Set artifacts;

    private final String mainArtifactId;

    private final ArtifactTypeMappingService artifactTypeMappingService;

    /**
     * Creates a new repository wih the specified artifacts.
     *
     * @param artifacts                  the artifacts
     * @param mainArtifactId             the id to use for the main artifact (no classifier)
     * @param artifactTypeMappingService
     */
    public ArtifactRepository( Set artifacts, String mainArtifactId,
                               ArtifactTypeMappingService artifactTypeMappingService )
    {
        this.artifacts = artifacts;
        this.mainArtifactId = mainArtifactId;
        this.artifactTypeMappingService = artifactTypeMappingService;
    }

    /**
     * Returns the artifact with the specified parameters.
     * <p/>
     * If the artifact is classified and is the only one with the specified
     * groupI, artifactId and type, it will be returned.
     * <p/>
     * If the artifact is classified and is not the only one with the specified
     * groupI, artifactId and type, it returns null.
     * <p/>
     * If the artifact is not found, it returns null.
     *
     * @param groupId    the group id
     * @param artifactId the artifact id
     * @param type       the type
     * @param classifier the classifier
     * @return the artifact or null if no artifact were found
     */
    public Artifact getUniqueArtifact( String groupId, String artifactId, String type, String classifier )
    {
        final Set candidates = getArtifacts( groupId, artifactId, type );
        if ( candidates.size() == 0 )
        {
            return null;
        }
        else if ( candidates.size() == 1 && classifier == null )
        {
            return (Artifact) candidates.iterator().next();
        }
        else if ( classifier != null )
        {
            final Iterator it = candidates.iterator();
            while ( it.hasNext() )
            {
                Artifact a = (Artifact) it.next();
                if ( a.getClassifier() == null && classifier.equals( mainArtifactId ) )
                {
                    return a;
                }
                else if ( classifier.equals( a.getClassifier() ) )
                {
                    return a;
                }
            }
        }
        // All other cases, classifier is null and more than one candidate ; artifact not found
        return null;
    }

    /**
     * Returns the artifact with the specified parameters.
     * <p/>
     * If the artifact is classified and is the only one with the specified
     * groupI, artifactId and type, it will be returned.
     * <p/>
     * If the artifact is classified and is not the only one with the specified
     * groupI, artifactId and type, it returns null.
     * <p/>
     * If the artifact is not found, it returns null.
     *
     * @param groupId    the group id
     * @param artifactId the artifact id
     * @param type       the type
     * @return the artifact or null if no artifact were found
     */
    public Artifact getUniqueArtifact( String groupId, String artifactId, String type )
    {
        return getUniqueArtifact( groupId, artifactId, type, null );
    }

    /**
     * Returns the artifacts with the specified parameters.
     *
     * @param groupId    the group id
     * @param artifactId the artifact id
     * @param type       the type
     * @return the artifacts or an empty set if no artifact were found
     */
    public Set getArtifacts( String groupId, String artifactId, String type )
    {
        final Set result = new TreeSet();
        final Iterator it = artifacts.iterator();
        while ( it.hasNext() )
        {
            Artifact a = (Artifact) it.next();

            // If the groupId, the artifactId and if the
            // artifact's type is known, then we have found a candidate.
            if ( a.getGroupId().equals( groupId ) && a.getArtifactId().equals( artifactId ) &&
                artifactTypeMappingService.isMappedToType( type, a.getType() ) )
            {
                result.add( a );

            }
        }
        return result;
    }
}
