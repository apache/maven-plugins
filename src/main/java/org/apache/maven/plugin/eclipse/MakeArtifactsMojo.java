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
package org.apache.maven.plugin.eclipse;

import java.util.StringTokenizer;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.StringUtils;

/**
 * Add eclipse artifacts from an eclipse installation to the local repo. This mojo automatically analize the eclipse
 * directory, copy plugins jars to the local maven repo, and generates appropriate poms.
 * 
 * Use <code>eclipse:to-maven</code> for the latest naming conventions in place, 
 * <code>groupId</code>.<code>artifactId</code>.
 * 
 * @author Fabrizio Giustina
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @goal make-artifacts
 * @requiresProject false
 * @deprecated use {@link EclipseToMavenMojo} for the latest naming conventions
 */
public class MakeArtifactsMojo
    extends EclipseToMavenMojo
    implements Contextualizable
{

    /**
     * Get the group id as the three first tokens in artifacts Id
     * e.g. <code>org.eclipse.jdt</code> -> <code>org.eclipse.jdt</code>
     * @param bundleName bundle name
     * @return group id
     */
    protected String createGroupId( String bundleName )
    {
        if ( StringUtils.countMatches( bundleName, "." ) > 1 )
        {
            StringTokenizer st = new StringTokenizer( bundleName, "." );
            int i = 0;
            String groupId = "";
            while ( st.hasMoreTokens() && ( i < 3 ) )
            {
                groupId += "." + st.nextToken();
                i++;
            }
            return groupId.substring( 1 );
        }
        return bundleName;
    }

    /**
     * Get the artifact id equal to the bundleName
     * e.g. <code>org.eclipse.jdt</code> -> <code>org.eclipse.jdt</code>
     * @param bundleName bundle name
     * @return artifact id
     */
    protected String createArtifactId( String bundleName )
    {
        return bundleName;
    }
}
