package org.apache.maven.plugins.site;

import org.apache.maven.doxia.site.decoration.inheritance.URIPathDescriptor;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

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
     * The String "staging/".
     */
    protected static final String DEFAULT_STAGING_DIRECTORY = "staging/";

    @Override
    protected boolean isDeploy()
    {
        return false;
    }

    /**
     * By default, staging mojos will get their top distribution management site url by getting top parent
     * with the same site.
     */
    @Override
    protected String determineTopDistributionManagementSiteUrl()
        throws MojoExecutionException
    {
        return getSite( getTopLevelProject( project ) ).getUrl();
    }

    /**
     * Extract the distributionManagement site of the top level parent of the given MavenProject.
     * This climbs up the project hierarchy and returns the site of the last project
     * for which {@link #getSite(org.apache.maven.project.MavenProject)} returns a site that resides in the
     * same site. Notice that it doesn't take into account if the parent is in the reactor or not.
     *
     * @param project the MavenProject. Not <code>null</code>.
     * @return the top level site. Not <code>null</code>.
     *         Also site.getUrl() and site.getId() are guaranteed to be not <code>null</code>.
     * @throws MojoExecutionException if no site info is found in the tree.
     * @see URIPathDescriptor#sameSite(java.net.URI)
     */
    private MavenProject getTopLevelProject( MavenProject project )
        throws MojoExecutionException
    {
        Site site = getSite( project );

        MavenProject parent = project;

        while ( parent.getParent() != null )
        {
            MavenProject oldProject = parent;
            // MSITE-585, MNG-1943
            parent = siteTool.getParentProject( parent, reactorProjects, localRepository );

            Site oldSite = site;

            try
            {
                site = getSite( parent );
            }
            catch ( MojoExecutionException e )
            {
                return oldProject;
            }

            // MSITE-600
            URIPathDescriptor siteURI = new URIPathDescriptor( URIEncoder.encodeURI( site.getUrl() ), "" );
            URIPathDescriptor oldSiteURI = new URIPathDescriptor( URIEncoder.encodeURI( oldSite.getUrl() ), "" );

            if ( !siteURI.sameSite( oldSiteURI.getBaseURI() ) )
            {
                return oldProject;
            }
        }

        return parent;
    }

    private static class URIEncoder
    {
        private static final String MARK = "-_.!~*'()";
        private static final String RESERVED = ";/?:@&=+$,";

        public static String encodeURI( final String uriString )
        {
            final char[] chars = uriString.toCharArray();
            final StringBuilder uri = new StringBuilder( chars.length );

            for ( char c : chars )
            {
                if ( ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'z' ) || ( c >= 'A' && c <= 'Z' )
                        || MARK.indexOf( c ) != -1  || RESERVED.indexOf( c ) != -1 )
                {
                    uri.append( c );
                }
                else
                {
                    uri.append( '%' );
                    uri.append( Integer.toHexString( (int) c ) );
                }
            }
            return uri.toString();
        }
    }
}
