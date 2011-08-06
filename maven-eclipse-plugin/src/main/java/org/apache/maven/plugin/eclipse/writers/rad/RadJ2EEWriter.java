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
import org.apache.maven.plugin.ide.JeeUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Creates the .j2ee file for RAD6 for now write hardcoded: EJB version 2.1 WAR version 2.4 EAR version 1.4 future
 * releases could make these varriable.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven</a>
 */
public class RadJ2EEWriter
    extends AbstractEclipseWriter
{

    private static final String J2EE_FILENAME = ".j2ee";

    private static final String J2EE_J2EESETTINGS = "j2eesettings";

    private static final String J2EE_MODULEVERSION = "moduleversion";

    private static final String J2EE_VERSION = "version";

    /**
     * write the .j2ee file to the project root directory.
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
        String packaging = config.getPackaging();

        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging )
            || Constants.PROJECT_PACKAGING_EJB.equalsIgnoreCase( packaging )
            || Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) )
        {
            try
            {
                w =
                    new OutputStreamWriter( new FileOutputStream( new File( config.getEclipseProjectDirectory(),
                                                                            J2EE_FILENAME ) ), "UTF-8" );
            }
            catch ( IOException ex )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
            }

            XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );
            writeModuleTypeFacetCore( writer, packaging );
            IOUtil.close( w );
        }
    }

    /**
     * Writes out the facet info for a faceted-project based on the packaging.
     * 
     * @param writer where to write to
     * @param packaging packaging type
     */
    private void writeModuleTypeFacetCore( XMLWriter writer, String packaging )
    {
        writer.startElement( J2EE_J2EESETTINGS );
        writer.addAttribute( J2EE_VERSION, "600" );
        writer.startElement( J2EE_MODULEVERSION );
        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging ) )
        {
            // In format X.X
            String servletVersion;
            if ( config.getJeeVersion() != null )
            {
                servletVersion = JeeUtils.getJeeDescriptorFromJeeVersion( config.getJeeVersion() ).getServletVersion();
            }
            else
            {
                servletVersion = JeeUtils.resolveServletVersion( config.getProject() );
            }
            writer.writeText( "" + servletVersion.charAt( 0 ) + servletVersion.charAt( 2 ) );
        }
        else if ( Constants.PROJECT_PACKAGING_EJB.equalsIgnoreCase( packaging ) )
        {
            // In format X.X
            String ejbVersion;
            if ( config.getJeeVersion() != null )
            {
                ejbVersion = JeeUtils.getJeeDescriptorFromJeeVersion( config.getJeeVersion() ).getEjbVersion();
            }
            else
            {
                ejbVersion = JeeUtils.resolveEjbVersion( config.getProject() );
            }
            writer.writeText( "" + ejbVersion.charAt( 0 ) + ejbVersion.charAt( 2 ) );
        }
        else if ( Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) )
        {
            // In format X.X
            String jeeVersion;
            if ( config.getJeeVersion() != null )
            {
                jeeVersion = JeeUtils.getJeeDescriptorFromJeeVersion( config.getJeeVersion() ).getJeeVersion();
            }
            else
            {
                jeeVersion = JeeUtils.resolveJeeVersion( config.getProject() );
            }
            writer.writeText( "" + jeeVersion.charAt( 0 ) + jeeVersion.charAt( 2 ) );
        }
        writer.endElement();
        writer.endElement();
    }

}
