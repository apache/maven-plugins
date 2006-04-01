package org.apache.maven.plugin.eclipse.writers;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Creates a .settings folder for Eclipse WTP 1.x release and writes out the configuration under it.
 * 
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseWtpFacetsWriter
    extends AbstractWtpResourceWriter
{

    private static final String FACET_JST_EAR = "jst.ear"; //$NON-NLS-1$

    private static final String FACET_JST_UTILITY = "jst.utility"; //$NON-NLS-1$

    private static final String FACET_JST_EJB = "jst.ejb"; //$NON-NLS-1$

    private static final String FACET_JST_WEB = "jst.web"; //$NON-NLS-1$

    private static final String FACET_JST_JAVA = "jst.java"; //$NON-NLS-1$

    private static final String ATTR_VERSION = "version"; //$NON-NLS-1$

    private static final String ELT_INSTALLED = "installed"; //$NON-NLS-1$

    private static final String ATTR_FACET = "facet"; //$NON-NLS-1$

    private static final String ELT_FIXED = "fixed"; //$NON-NLS-1$

    private static final String ELT_FACETED_PROJECT = "faceted-project"; //$NON-NLS-1$

    /**
     * The .settings folder for Web Tools Project 1.x release.
     */
    private static final String DIR_WTP_SETTINGS = ".settings"; //$NON-NLS-1$

    /**
     * File name where Eclipse Project's Facet configuration will be stored.
     */
    private static final String FILE_FACET_CORE_XML = "org.eclipse.wst.common.project.facet.core.xml"; //$NON-NLS-1$

    public EclipseWtpFacetsWriter( Log log, File eclipseProjectDir, MavenProject project, IdeDependency[] deps )
    {
        super( log, eclipseProjectDir, project, deps );
    }

    public void write( EclipseSourceDir[] sourceDirs, ArtifactRepository localRepository, File buildOutputDirectory )
        throws MojoExecutionException
    {

        // create a .settings directory (if not existing)
        File settingsDir = new File( getEclipseProjectDirectory(), DIR_WTP_SETTINGS );
        settingsDir.mkdirs();

        FileWriter w;

        String packaging = getProject().getPackaging();

        // Write out facet core xml
        try
        {
            w = new FileWriter( new File( settingsDir, FILE_FACET_CORE_XML ) );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }
        XMLWriter writer = new PrettyPrintXMLWriter( w );
        writeModuleTypeFacetCore( writer, packaging );
        IOUtil.close( w );
    }

    /**
     * Writes out the facet info for a faceted-project based on the packaging.
     * 
     * @param writer
     * @param packaging
     */
    private void writeModuleTypeFacetCore( XMLWriter writer, String packaging )
    {
        writer.startElement( ELT_FACETED_PROJECT );
        // common facet
        writer.startElement( ELT_FIXED );
        writer.addAttribute( ATTR_FACET, FACET_JST_JAVA );
        writer.endElement(); // element fixed
        if ( "war".equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            writer.startElement( ELT_FIXED );
            writer.addAttribute( ATTR_FACET, FACET_JST_WEB );
            writer.endElement(); // fixed
            writer.startElement( ELT_INSTALLED );
            writer.addAttribute( ATTR_FACET, FACET_JST_WEB );
            writer.addAttribute( ATTR_VERSION, resolveServletVersion() );
            writer.endElement(); // installed
        }
        else if ( "ejb".equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            writer.startElement( ELT_FIXED );
            writer.addAttribute( ATTR_FACET, FACET_JST_EJB );
            writer.endElement(); // fixed
            writer.startElement( ELT_INSTALLED );
            writer.addAttribute( ATTR_FACET, FACET_JST_EJB );
            writer.addAttribute( ATTR_VERSION, resolveEjbVersion() );
            writer.endElement(); // installed
        }
        else if ( "ear".equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            writer.startElement( ELT_FIXED );
            writer.addAttribute( ATTR_FACET, FACET_JST_EAR );
            writer.endElement(); // fixed
            writer.startElement( ELT_INSTALLED );
            writer.addAttribute( ATTR_FACET, FACET_JST_EAR );
            writer.addAttribute( ATTR_VERSION, resolveJ2eeVersion() );
            writer.endElement(); // installed
        }
        else if ( "jar".equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            writer.startElement( ELT_FIXED );
            writer.addAttribute( ATTR_FACET, FACET_JST_UTILITY );
            writer.endElement(); // fixed
            writer.startElement( ELT_INSTALLED );
            writer.addAttribute( ATTR_FACET, FACET_JST_UTILITY );
            writer.addAttribute( ATTR_VERSION, "1.0" ); //$NON-NLS-1$
            writer.endElement(); // installed
        }

        // common installed element
        writer.startElement( ELT_INSTALLED );
        writer.addAttribute( ATTR_FACET, FACET_JST_JAVA );
        writer.addAttribute( ATTR_VERSION, resolveJavaVersion() );
        writer.endElement(); // installed
        writer.endElement(); // faceted-project
    }

}
