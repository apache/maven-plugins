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
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
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

    public void execute()
        throws MojoExecutionException
    {
        List<Locale> localesList = siteTool.getAvailableLocales( locales );

        for ( Locale locale : localesList )
        {
            File descriptorFile = siteTool.getSiteDescriptorFromBasedir( toRelative( project.getBasedir(),
                                                                                     siteDirectory.getAbsolutePath() ),
                                                                         basedir, locale );

            if ( descriptorFile.exists() )
            {
                Map<String, String> props = new HashMap<String, String>();
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
                    throw new MojoExecutionException( "Error when interpoling site descriptor", e );
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

                artifact.addMetadata( new SiteDescriptorArtifactMetadata( artifact, decoration, descriptorFile ) );
            }
        }
    }
}
