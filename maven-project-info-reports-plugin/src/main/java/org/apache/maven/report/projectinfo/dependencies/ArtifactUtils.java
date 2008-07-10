package org.apache.maven.report.projectinfo.dependencies;

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

import java.util.List;

import org.apache.commons.validator.UrlValidator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utility methods around the <code>Artifact</code>.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.1
 */
public class ArtifactUtils
{
    private static final UrlValidator URL_VALIDATOR = new UrlValidator( new String[] { "http", "https" } );

    /**
     * @param artifact not null
     * @param mavenProjectBuilder not null
     * @param remoteRepositories not null
     * @param localRepository not null
     * @return the artifact url or null if an error occurred.
     */
    public static String getArtifactUrl( Artifact artifact, MavenProjectBuilder mavenProjectBuilder,
                                         List remoteRepositories, ArtifactRepository localRepository )
    {
        try
        {
            MavenProject pluginProject = mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories,
                                                                                  localRepository );

            if ( URL_VALIDATOR.isValid( pluginProject.getUrl() ) )
            {
                return pluginProject.getUrl();
            }

            return null;
        }
        catch ( ProjectBuildingException e )
        {
            return null;
        }
    }

    /**
     * @param artifactId not null
     * @param link could be null
     * @return the artifactId cell with or without a link pattern
     * @see {@link AbstractMavenReportRenderer#linkPatternedText(String)}
     */
    public static String getArtifactIdCell( String artifactId, String link )
    {
        if ( StringUtils.isEmpty( link ) )
        {
            return artifactId;
        }

        return "{" + artifactId + "," + link + "}";
    }
}
