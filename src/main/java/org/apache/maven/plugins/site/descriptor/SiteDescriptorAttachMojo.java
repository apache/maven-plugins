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
import java.io.IOException;

import java.util.Locale;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.utils.PathTool;
import org.codehaus.plexus.util.FileUtils;

/**
 * Adds the site descriptor (<code>site.xml</code>) to the list of files to be installed/deployed.<br/>
 * For Maven-2.x this is enabled by default only when the project has <code>pom</code> packaging since it will be used
 * by modules inheriting, but this can be enabled for other projects packaging if needed.<br/>
 * This default execution has been removed from the built-in lifecycle of Maven 3.x for <code>pom</code>-projects.
 * Users that actually use those projects to provide a common site descriptor for sub modules will need to explicitly
 * define this goal execution to restore the intended behavior.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "attach-descriptor", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true )
public class SiteDescriptorAttachMojo
    extends AbstractSiteDescriptorMojo
{
    /**
     */
    @Parameter( property = "basedir", required = true, readonly = true )
    private File basedir;

    /**
     * Maven ProjectHelper.
     *
     * @since 2.1.1
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Attach site descriptor only if packaging is pom.
     * @since 3.0
     */
    @Parameter( defaultValue = "true" )
    private boolean pomPackagingOnly;

    public void execute()
        throws MojoExecutionException
    {
        if ( pomPackagingOnly && !"pom".equals( project.getPackaging() ) )
        {
            // https://issues.apache.org/jira/browse/MSITE-597
            getLog().info( "Skipping because packaging '" + project.getPackaging() + "' is not pom." );
            return;
        }

        boolean attachedSiteDescriptor = false;
        for ( Locale locale : getLocales() )
        {
            File descriptorFile = siteTool.getSiteDescriptor( siteDirectory, locale );

            if ( descriptorFile.exists() )
            {
                attachedSiteDescriptor = true;

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
                    getLog().info( "Attaching '"
                        + PathTool.getRelativeFilePath( basedir.getAbsolutePath(), descriptorFile.getAbsolutePath() )
                        + "' site descriptor with classifier '" + classifier + "'." );
                    projectHelper.attachArtifact( project, "xml", classifier, targetDescriptorFile );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to copy site descriptor", e );
                }
            }
        }

        if ( !attachedSiteDescriptor )
        {
            getLog().info( "No site descriptor found: nothing to attach." );
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
