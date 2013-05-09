package org.apache.maven.plugins.site;

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

import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Deploys the generated site to a staging or mock URL to the site URL
 * specified in the <code>&lt;distributionManagement&gt;</code> section of the
 * POM, using <a href="/wagon/">wagon supported protocols</a>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
@Mojo( name = "stage-deploy", requiresDependencyResolution = ResolutionScope.TEST )
public class SiteStageDeployMojo
    extends AbstractStagingMojo
{
    /**
     * The staged site will be deployed to this URL.
     * <p/>
     * If you don't specify this, the default-value will be
     * "${project.distributionManagement.site.url}/staging", where "project" is
     * either the current project or, in a reactor build, the top level project
     * in the reactor.
     * <p>
     * Note that even if you specify this plugin parameter, you still need to indicate
     * ${project.distributionManagement.site.url} at least in your top level project
     * in order for relative links between modules to be resolved correctly.
     * </p>
     *
     * @see <a href="http://maven.apache.org/maven-model/maven.html#class_site">MavenModel#class_site</a>
     */
    @Parameter( property = "stagingSiteURL" )
    private String stagingSiteURL;

    /**
     * The identifier of the repository where the staging site will be deployed. This id will be used to lookup a
     * corresponding <code>&lt;server&gt;</code> entry from the <code>settings.xml</code>. If a matching
     * <code>&lt;server&gt;</code> entry is found, its configured credentials will be used for authentication.
     * <p/>
     * If this is not specified, then the corresponding value of <code>distributionManagement.site.id</code>
     * will be taken as default, unless this is not defined either then the String
     * <code>"stagingSite"</code> is used. (<strong>Note</strong>:
     * until v. 2.3 and 3.0-beta-3 the String <code>"stagingSite"</code> is always used.)
     *
     * @since 2.0.1
     */
    @Parameter( property = "stagingRepositoryId" )
    private String stagingRepositoryId;

    /**
     * If <code>stagingSiteURL</code> is configured, top most parent with same staging site url
     * will be used.
     */
    @Override
    protected String determineTopDistributionManagementSiteUrl()
        throws MojoExecutionException
    {
        if ( StringUtils.isNotEmpty( topSiteURL ) )
        {
            return topSiteURL;
        }

        if ( StringUtils.isNotEmpty( stagingSiteURL ) )
        {
            // We need to calculate the first project that supplied same stagingSiteURL
            return getSite( getTopMostParentWithSameStagingSiteURL( project ) ).getUrl();
        }

        return super.determineTopDistributionManagementSiteUrl();
    }

    @Override
    protected Site determineDeploySite()
        throws MojoExecutionException
    {
        Site top = new Site();

        top.setId( stagingRepoId() );
        getLog().info( "Using this server ID for stage deploy: " + top.getId() );

        String stagingURL = determineStageDeploySiteURL();
        getLog().info( "Using this base URL for stage deploy: " + stagingURL );

        top.setUrl( stagingURL );

        return top;
    }

    /**
     * Extract the distributionManagement.site of the top most project in the
     * hierarchy that specifies a stagingSiteURL, starting at the given
     * MavenProject.
     * <p/>
     * This climbs up the project hierarchy and returns the site of the top most
     * project for which
     * {@link #getStagingSiteURL(org.apache.maven.project.MavenProject)} returns
     * same URL as actual.
     *
     * @param project the MavenProject. Not null.
     * @return the site for the top most project that has a stagingSiteURL. Not null.
     */
    private MavenProject getTopMostParentWithSameStagingSiteURL( MavenProject project )
    {
        String actualStagingSiteURL = getStagingSiteURL( project );

        MavenProject parent = project;

        while ( parent != null
                && actualStagingSiteURL.equals( getStagingSiteURL( parent ) ) )
        {
            project = parent;
 
            // MSITE-585, MNG-1943
            parent = siteTool.getParentProject( parent, reactorProjects, localRepository );
        }

        return project;
    }

    /**
     * Extract the value of the stagingSiteURL configuration parameter of
     * maven-site-plugin for the given project.
     *
     * @param project The MavenProject, not null
     * @return The stagingSiteURL for the project, or null if it doesn't have one
     */
    private String getStagingSiteURL( MavenProject project )
    {
        final String sitePluginKey = "org.apache.maven.plugins:maven-site-plugin";

        if ( project == null )
        {
            return null;
        }

        final Build build = project.getBuild();
        if ( build == null )
        {
            return null;
        }

        Map<String, Plugin> plugins = build.getPluginsAsMap();

        Plugin sitePlugin = plugins.get( sitePluginKey );
        if ( sitePlugin == null )
        {
            final PluginManagement buildPluginManagement = build.getPluginManagement();
            if ( buildPluginManagement == null )
            {
                return null;
            }

            plugins = buildPluginManagement.getPluginsAsMap();
            sitePlugin = plugins.get( sitePluginKey );
        }

        if ( sitePlugin == null )
        {
            return null;
        }

        final Xpp3Dom sitePluginConfiguration = (Xpp3Dom) sitePlugin.getConfiguration();
        if ( sitePluginConfiguration == null )
        {
            return null;
        }

        final Xpp3Dom child = sitePluginConfiguration.getChild( "stagingSiteURL" );
        if ( child == null )
        {
            return null;
        }
        else
        {
            return child.getValue();
        }
    }

    /**
     * Find the URL where staging will take place.
     *
     * @return the site URL for staging
     */
    private String determineStageDeploySiteURL()
        throws MojoExecutionException
    {
        if ( stagingSiteURL != null )
        {
            // the user has specified a stagingSiteURL - use it
            getLog().debug( "stagingSiteURL specified by the user: " + stagingSiteURL );
            return stagingSiteURL;
        }

        // The user didn't specify a URL, use the top level site distribution URL and add "[/]staging/" to it
        String defaultStagingSiteURL = appendSlash( getTopDistributionManagementSiteUrl() ) + DEFAULT_STAGING_DIRECTORY;
        getLog().debug( "stagingSiteURL NOT specified, using the top level project: " + defaultStagingSiteURL );

        return defaultStagingSiteURL;
    }

    private String stagingRepoId()
    {
        if ( stagingRepositoryId != null )
        {
            return stagingRepositoryId;
        }

        try
        {
            return getSite( project ).getId();
        }
        catch ( MojoExecutionException ex )
        {
            return "stagingSite";
        }
    }
}
