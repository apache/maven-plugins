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
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adds the site descriptor to the list of files to be installed/deployed.
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
        List localesList = getAvailableLocales();

        for ( Iterator iterator = localesList.iterator(); iterator.hasNext(); )
        {
            Locale locale = (Locale) iterator.next();

            File descriptorFile = getSiteDescriptorFile( basedir, locale );

            if ( descriptorFile.exists() )
            {
                Map props = new HashMap();
                props.put( "reports", "<menu ref=\"reports\"/>" );
                props.put( "modules", "<menu ref=\"modules\"/>" );

                DecorationModel decoration;
                try
                {
                    String siteDescriptorContent = FileUtils.fileRead( descriptorFile );

                    siteDescriptorContent =
                        getInterpolatedSiteDescriptorContent( props, project, siteDescriptorContent );

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

                MavenProject parentProject = getParentProject( project );
                if ( parentProject != null && project.getUrl() != null && parentProject.getUrl() != null )
                {
                    populateProjectParentMenu( decoration, locale, parentProject, true );
                }
                populateModules( decoration, locale, true );

                artifact.addMetadata( new SiteDescriptorArtifactMetadata( artifact, decoration, descriptorFile ) );
            }
        }
    }
}
