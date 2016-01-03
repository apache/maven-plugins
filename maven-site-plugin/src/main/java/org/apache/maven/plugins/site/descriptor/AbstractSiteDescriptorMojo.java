package org.apache.maven.plugins.site.descriptor;

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

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.inheritance.DecorationModelInheritanceAssembler;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.site.AbstractSiteMojo;

/**
 * Abstract class to compute effective site decoration model for site descriptors.
 *
 * @since 3.5
 */
public abstract class AbstractSiteDescriptorMojo
    extends AbstractSiteMojo
{
    /**
     * The component for assembling site decoration model inheritance.
     */
    @Component
    private DecorationModelInheritanceAssembler assembler;

    /**
     * Remote repositories used for the project.
     *
     * @todo this is used for site descriptor resolution - it should relate to the actual project but for some reason
     *       they are not always filled in
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true )
    protected List<ArtifactRepository> repositories;

    /**
     * Directory containing the <code>site.xml</code> file and the source for hand written docs (one directory
     * per Doxia-source-supported markup types):
     * see <a href="/doxia/references/index.html">Doxia Markup Languages References</a>).
     *
     * @since 2.3
     */
    @Parameter( defaultValue = "${basedir}/src/site" )
    protected File siteDirectory;

    /**
     * Make links in the site descriptor relative to the project URL.
     * By default, any absolute links that appear in the site descriptor,
     * e.g. banner hrefs, breadcrumbs, menu links, etc., will be made relative to project.url.
     * <p/>
     * Links will not be changed if this is set to false, or if the project has no URL defined.
     *
     * @since 2.3
     */
    @Parameter( property = "relativizeDecorationLinks", defaultValue = "true" )
    private boolean relativizeDecorationLinks;

    protected DecorationModel prepareDecorationModel( Locale locale )
        throws MojoExecutionException
    {
        DecorationModel decorationModel;
        try
        {
            decorationModel = siteTool.getDecorationModel( siteDirectory, locale, project, reactorProjects,
                                                           localRepository, repositories );
        }
        catch ( SiteToolException e )
        {
            throw new MojoExecutionException( "SiteToolException: " + e.getMessage(), e );
        }

        if ( relativizeDecorationLinks )
        {
            final String url = project.getUrl();

            if ( url == null )
            {
                getLog().warn( "No project URL defined - decoration links will not be relativized!" );
            }
            else
            {
                List<Locale> localesList = getLocales();

                // Default is first in the list
                Locale defaultLocale = localesList.get( 0 );

                // MSITE-658
                final String localeUrl = locale.equals( defaultLocale ) ? url : append( url, locale.getLanguage() );

                getLog().info( "Relativizing decoration links with respect to localized project URL: " + localeUrl );
                assembler.resolvePaths( decorationModel, localeUrl );
            }
        }
        return decorationModel;
    }

    private String append( String url, String path )
    {
        return url.endsWith( "/" ) ? ( url + path ) : ( url + '/' + path );
    }
}
