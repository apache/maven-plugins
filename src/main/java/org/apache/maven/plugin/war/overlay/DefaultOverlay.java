package org.apache.maven.plugin.war.overlay;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.war.Overlay;

/**
 * A default overlay implementation based on an {@link Artifact}.
 *
 * @author Stephane Nicoll
 * 
 * @version $Id$
 */
public class DefaultOverlay
    extends Overlay
{

    /**
     * Creates an overlay for the specified artifact.
     *
     * @param a the artifact
     */
    public DefaultOverlay( Artifact a )
    {
        super();
        setGroupId( a.getGroupId() );
        setArtifactId( a.getArtifactId() );
        setClassifier( a.getClassifier() );
        setArtifact( a );
        setType( a.getType() );
    }

    /**
     * Creates an overlay for the specified artifact.
     *
     * @param a        the artifact
     * @param includes the includes to use
     * @param excludes the excludes to use
     */
    public DefaultOverlay( Artifact a, String includes, String excludes )
    {
        this( a );
        setIncludes( includes );
        setExcludes( excludes );
    }
}
