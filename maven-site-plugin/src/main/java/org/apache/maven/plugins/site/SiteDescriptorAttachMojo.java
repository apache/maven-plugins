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

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Locale;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;

import org.codehaus.plexus.util.FileUtils;

/**
 * Adds the site descriptor (<code>site.xml</code>) to the list of files to be installed/deployed.
 * By default, this is enabled only when the project has pom packaging since it will be used by modules inheriting,
 * but this can be enabled for other projects packaging if needed.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal attach-descriptor
 * @phase package
 */
public class SiteDescriptorAttachMojo
    extends AbstractSiteMojo
{
    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * Maven ProjectHelper.
     *
     * @component
     * @readonly
     * @since 2.1.1
     */
    private MavenProjectHelper projectHelper;

    /**
     * @parameter default-value="true"
     */
    private boolean pomPackagingOnly;

    public void execute()
        throws MojoExecutionException
    {
        if ( pomPackagingOnly && !"pom".equals( project.getPackaging() ) )
        {
            // http://jira.codehaus.org/browse/MSITE-597
            return;
        }

        List<Locale> localesList = siteTool.getAvailableLocales( locales );

        for ( Locale locale : localesList )
        {
            File descriptorFile = siteTool.getSiteDescriptorFromBasedir(
                siteTool.getRelativePath( siteDirectory.getAbsolutePath(), project.getBasedir().getAbsolutePath() ),
                                                                         basedir, locale );

            if ( descriptorFile.exists() )
            {
                // Calculate the classifier to use
                String classifier = getClassifier( descriptorFile );
                // Prepare a file for the interpolated site descriptor
                String filename = project.getArtifactId() + "-" + project.getVersion() + "-" + descriptorFile.getName();
                File targetDescriptorFile = new File( project.getBuild().getDirectory(), filename );

                try
                {
                    // Copy the site descriptor to a file
                    FileUtils.copyFile( descriptorFile, targetDescriptorFile );
                    // Attach the site descriptor
                    getLog().debug( "Attaching the site descriptor '" + targetDescriptorFile.getAbsolutePath()
                        + "' with classifier '" + classifier + "' to the project." );
                    projectHelper.attachArtifact( project, "xml", classifier, targetDescriptorFile );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to copy site descriptor", e );
                }
            }
        }
    }

    private static String getClassifier( final File descriptorFile )
        throws MojoExecutionException
    {
        final int index = descriptorFile.getName().lastIndexOf( '.' );

        if ( index > 0 )
        {
            return descriptorFile.getName().substring( 0, index );
        }
        else
        {
            throw new MojoExecutionException( "Unable to determine the classifier to use" );
        }
    }
}
