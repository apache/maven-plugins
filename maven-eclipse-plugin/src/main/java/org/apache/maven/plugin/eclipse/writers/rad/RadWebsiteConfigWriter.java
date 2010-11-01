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
package org.apache.maven.plugin.eclipse.writers.rad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.eclipse.writers.wtp.AbstractWtpResourceWriter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Creates a .settings folder for Eclipse WTP 1.x release and writes out the configuration under it.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven</a>
 */
public class RadWebsiteConfigWriter
    extends AbstractEclipseWriter
{

    private static final String WEBSITE_CONFIG_FILENAME = ".website-config";

    private static final String WEBSITE_CONFIG_STRUCTURE = "structure";

    private static final String WEBSITE_CONFIG_VERSION = "version";

    private static final String WEBSITE_CONFIG_WEBSITE = "website";

    /**
     * write the website-config file for RAD6 if needed.
     * 
     * @see AbstractWtpResourceWriter#write(EclipseSourceDir[], ArtifactRepository, File)
     * @param sourceDirs all eclipse source directorys
     * @param localRepository the local reposetory
     * @param buildOutputDirectory build output directory (target)
     * @throws MojoExecutionException when writing the config files was not possible
     */
    public void write()
        throws MojoExecutionException
    {
        Writer w;
        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( config.getPackaging() ) )
        {
            try
            {
                w =
                    new OutputStreamWriter( new FileOutputStream( new File( config.getEclipseProjectDirectory(),
                                                                            WEBSITE_CONFIG_FILENAME ) ), "UTF-8" );
            }
            catch ( IOException ex )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
            }
            XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );
            writeModuleTypeFacetCore( writer );
            IOUtil.close( w );
        }
    }

    /**
     * write the website-config file.
     * 
     * @param writer wher to write to
     */
    private void writeModuleTypeFacetCore( XMLWriter writer )
    {
        writer.startElement( WEBSITE_CONFIG_WEBSITE );
        writer.addAttribute( WEBSITE_CONFIG_VERSION, "600" );
        writer.startElement( WEBSITE_CONFIG_STRUCTURE );
        writer.endElement();
        writer.endElement();
    }
}
