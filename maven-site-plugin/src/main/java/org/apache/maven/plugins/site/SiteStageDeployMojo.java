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


import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Deploys the generated site to a staging or mock directory to the site URL
 * specified in the <code>&lt;distributionManagement&gt;</code> section of the
 * POM. It supports <code>scp</code> and <code>file</code> protocols for
 * deployment.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal stage-deploy
 * @requiresDependencyResolution test
 */
public class SiteStageDeployMojo
    extends AbstractDeployMojo
{
    /**
     * The staged site will be deployed to this URL.
     *
     * If you don't specify this, the default-value will be
     * "${project.distributionManagement.site.url}/staging", where "project" is
     * either the current project or, in a reactor build, the top level project
     * in the reactor.
     * <p>
     * Note that even if you specify this plugin parameter you still need to indicate
     * ${project.distributionManagement.site.url} at least in your top level project
     * in order for relative links between modules to be resolved correctly.
     * </p>
     *
     * @parameter expression="${stagingSiteURL}"
     * @see <a href="http://maven.apache.org/maven-model/maven.html#class_site">MavenModel#class_site</a>
     */
    private String stagingSiteURL;

    /**
     * The identifier of the repository where the staging site will be deployed. This id will be used to lookup a
     * corresponding <code>&lt;server&gt;</code> entry from the <code>settings.xml</code>. If a matching
     * <code>&lt;server&gt;</code> entry is found, its configured credentials will be used for authentication.
     *
     * If this is not specified, then the corresponding value of <code>distributionManagement.site.id</code>
     * will be taken as default, unless this is not defined either then the String
     * <code>"stagingSite"</code> is used. (<strong>Note</strong>:
     * until v. 2.3 and 3.0-beta-3 the String <code>"stagingSite"</code> is always used.)
     *
     * @parameter expression="${stagingRepositoryId}"
     *
     * @since 2.0.1
     */
    private String stagingRepositoryId;

    @Override
    /**
     * Find the relative path between the distribution URLs of the parent that
     * supplied the staging deploy URL and the current project.
     *
     * @return the relative path or "./" if the two URLs are the same.
     *
     * @throws MojoExecutionException
     */
    protected String getDeployModuleDirectory()
        throws MojoExecutionException
    {
        // MSITE-602: If the user specified an explicit stagingSiteURL, use a special relative path
        if( StringUtils.isNotEmpty( stagingSiteURL ) )
        {
            // We need to calculate the relative path between this project and
            // the first one that supplied a stagingSiteURL
            String relative = siteTool.getRelativePath( getSite( project ).getUrl(),
                getSiteForFirstParentWithStagingSiteURL( project ).getUrl() );

            // SiteTool.getRelativePath() uses File.separatorChar,
            // so we need to convert '\' to '/' in order for the URL to be valid for Windows users
            relative = relative.replace( '\\', '/' );

            getLog().debug( "The stagingSiteURL is configured, using special way to calculate relative path." );
            return ( "".equals( relative ) ) ? "./" : relative;
        }
        else
        {
            getLog().debug( "No stagingSiteURL is configured, using standard way to calculate relative path." );
            return super.getDeployModuleDirectory();
        }
    }

    @Override
    protected String getDeployRepositoryID()
        throws MojoExecutionException
    {
        stagingRepositoryId =  stagingRepoId ( stagingRepositoryId );

        getLog().info( "Using this server ID for stage deploy: " + stagingRepositoryId );

        return stagingRepositoryId;
    }

    @Override
    protected String getDeployRepositoryURL()
        throws MojoExecutionException
    {
        String stagingURL = determineStagingSiteURL( stagingSiteURL );

        getLog().info( "Using this base URL for stage deploy: " + stagingURL );

        return stagingURL;
    }

    /**
     * Extract the distributionManagement.site of the first project up the
     * hierarchy that specifies a stagingSiteURL, starting at the given
     * MavenProject.
     * <p/>
     * This climbs up the project hierarchy and returns the site of the first
     * project for which
     * {@link #getStagingSiteURL(org.apache.maven.project.MavenProject)} returns
     * a URL.
     *
     * @param project the MavenProject. Not null.
     * @return the site for the first project that has a stagingSiteURL. Not null.
     */
    protected Site getSiteForFirstParentWithStagingSiteURL( MavenProject project )
    {
        Site site = project.getDistributionManagement().getSite();

        MavenProject parent = project;

        // @todo Should we check that the stagingSiteURL equals the one in this project instead of being non-empty?
        while ( parent != null
                && StringUtils.isNotEmpty( getStagingSiteURL( parent ) ) )
        {
            site = parent.getDistributionManagement().getSite();

            // MSITE-585, MNG-1943
            parent = siteTool.getParentProject( parent, reactorProjects, localRepository );
        }

        return site;
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

        final Plugin sitePlugin = build.getPluginsAsMap().get( sitePluginKey );
        if( sitePlugin == null )
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
     * @param usersStagingSiteURL The staging site URL as suggested by the user's configuration
     * 
     * @return the site URL for staging
     */
    private String determineStagingSiteURL( final String usersStagingSiteURL )
        throws MojoExecutionException
    {
        String topLevelURL = null;

        if ( usersStagingSiteURL != null )
        {
            // the user has specified a stagingSiteURL - use it
            getLog().debug( "stagingSiteURL specified by the user: " + usersStagingSiteURL );
            topLevelURL = usersStagingSiteURL;
        }
        else
        {
            // The user didn't specify a URL, use the top level site distribution URL and add "[/]staging/" to it
            topLevelURL = appendSlash( getRootSite( project ).getUrl() )
                + DEFAULT_STAGING_DIRECTORY;
            getLog().debug( "stagingSiteURL NOT specified, using the top level project: " + topLevelURL );
        }

        // Return either
        //   usersURL
        // or
        //   topLevelProjectURL + "staging"
        return topLevelURL;
    }

    private String stagingRepoId( final String stagingRepoId )
    {
        if ( stagingRepoId != null )
        {
            return stagingRepoId;
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
