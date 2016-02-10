package org.apache.maven.plugins.site.deploy;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Abstract base class for staging mojos.
 *
 * @author hboutemy
 * @since 3.3
 */
public abstract class AbstractStagingMojo
    extends AbstractDeployMojo
{
    /**
     * Top distribution management site url, for manual configuration when auto-calculated value
     * doesn't match expectations. Relative module directory will be calculated from this url.
     *
     * @since 3.3
     */
    @Parameter( property = "topSiteURL" )
    protected String topSiteURL;

    /**
     * The String "staging/".
     */
    protected static final String DEFAULT_STAGING_DIRECTORY = "staging/";

    /**
     * By default, staging mojos will get their top distribution management site url by getting top parent
     * with the same site, which is a good heuristics. But in case the default value doesn't match
     * expectations, <code>topSiteURL</code> can be configured: it will be used instead.
     */
    @Override
    protected String determineTopDistributionManagementSiteUrl()
        throws MojoExecutionException
    {
        if ( StringUtils.isEmpty( topSiteURL ) )
        {
            MavenProject topProject = getTopLevelProject( project );
            String url = getSite( topProject ).getUrl();

            getLog().debug( "staging top distributionManagement.site.url found in " + topProject.getId()
                + " with value: " + url );

            return url;
        }

        getLog().debug( "staging top distributionManagement.site.url configured with topSiteURL parameter: "
            + topSiteURL );
        return topSiteURL;
    }
}
