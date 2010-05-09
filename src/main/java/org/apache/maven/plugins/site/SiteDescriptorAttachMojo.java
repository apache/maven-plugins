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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Writer;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adds the site descriptor (<code>site.xml</code>) to the list of files to be installed/deployed.
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
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

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

    public void execute()
        throws MojoExecutionException
    {
        List localesList = siteTool.getAvailableLocales( locales );

        for ( Iterator iterator = localesList.iterator(); iterator.hasNext(); )
        {
            Locale locale = (Locale) iterator.next();

            File descriptorFile = siteTool.getSiteDescriptorFromBasedir( toRelative( project.getBasedir(),
                                                                                     siteDirectory.getAbsolutePath() ),
                                                                         basedir, locale );

            if ( descriptorFile.exists() )
            {
                Map props = new HashMap();
                props.put( "reports", "<menu ref=\"reports\"/>" );
                props.put( "modules", "<menu ref=\"modules\"/>" );

                DecorationModel decoration;
                try
                {
                    String siteDescriptorContent = IOUtil.toString( ReaderFactory.newXmlReader( descriptorFile ) );

                    siteDescriptorContent =
                        siteTool.getInterpolatedSiteDescriptorContent( props, project, siteDescriptorContent,
                                                                       getInputEncoding(), getOutputEncoding() );

                    decoration = new DecorationXpp3Reader().read( new StringReader( siteDescriptorContent ) );
                }
                catch ( XmlPullParserException e )
                {
                    throw new MojoExecutionException( "Error parsing site descriptor", e );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Error reading site descriptor", e );
                }
                catch ( SiteToolException e )
                {
                    throw new MojoExecutionException( "Error when interpolating site descriptor", e );
                }

                MavenProject parentProject = siteTool.getParentProject( project, reactorProjects, localRepository );
                if ( parentProject != null && project.getUrl() != null && parentProject.getUrl() != null )
                {
                    siteTool.populateParentMenu( decoration, locale, project, parentProject, true );
                }
                try
                {
                    siteTool.populateModulesMenu( project, reactorProjects, localRepository, decoration, locale, true );
                }
                catch ( SiteToolException e )
                {
                    throw new MojoExecutionException( "Error when populating modules", e );
                }

                // Calculate the classifier to use
                String classifier = null;
                int index = descriptorFile.getName().lastIndexOf( '.' );
                if ( index > 0 )
                {
                    classifier = descriptorFile.getName().substring( 0, index );
                }
                else
                {
                    throw new MojoExecutionException( "Unable to determine the classifier to use" );
                }

                // Prepare a file for the interpolated site descriptor
                String filename = project.getArtifactId() + "-" + project.getVersion() + "-" + descriptorFile.getName();
                File interpolatedDescriptorFile = new File( project.getBuild().getDirectory(), filename );
                interpolatedDescriptorFile.getParentFile().mkdirs();

                try
                {
                    // Write the interpolated site descriptor to a file
                    Writer writer = WriterFactory.newXmlWriter( interpolatedDescriptorFile );
                    new DecorationXpp3Writer().write( writer, decoration );
                    // Attach the interpolated site descriptor
                    getLog().debug( "Attaching the site descriptor '" + interpolatedDescriptorFile.getAbsolutePath()
                        + "' with classifier '" + classifier + "' to the project." );
                    projectHelper.attachArtifact( project, "xml", classifier, interpolatedDescriptorFile );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to store interpolated site descriptor", e );
                }
            }
        }
    }
}
