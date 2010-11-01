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
package org.apache.maven.plugin.eclipse.writers.wtp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeUtils;
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

    private static final String FACET_COM_IBM_WEBSPHERE_COEXISTENCE_EAR = "com.ibm.websphere.coexistence.ear"; //$NON-NLS-1$

    private static final String FACET_COM_IBM_WEBSPHERE_EXTENDED_EAR = "com.ibm.websphere.extended.ear"; //$NON-NLS-1$

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

    /**
     * @see org.apache.maven.plugin.eclipse.writers.EclipseWriter#write()
     */
    public void write()
        throws MojoExecutionException
    {

        // create a .settings directory (if not existing)
        File settingsDir = new File( config.getEclipseProjectDirectory(), DIR_WTP_SETTINGS );
        settingsDir.mkdirs();

        Writer w;

        String packaging = config.getPackaging();

        // Write out facet core xml
        try
        {
            w = new OutputStreamWriter( new FileOutputStream( new File( settingsDir, FILE_FACET_CORE_XML ) ), "UTF-8" );
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
        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            writeFacetFixedElement( writer, FACET_JST_JAVA ); // fixed
            writeFacetFixedElement( writer, FACET_JST_WEB ); // fixed
            writeFacetInstalledElement( writer, FACET_JST_WEB, JeeUtils.resolveServletVersion( config.getProject() ) ); // installed
            writeFacetInstalledElement( writer, FACET_JST_JAVA, IdeUtils.resolveJavaVersion( config.getProject() ) ); // installed
        }
        else if ( Constants.PROJECT_PACKAGING_EJB.equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            writeFacetFixedElement( writer, FACET_JST_JAVA ); // fixed
            writeFacetFixedElement( writer, FACET_JST_EJB ); // fixed
            writeFacetInstalledElement( writer, FACET_JST_EJB, JeeUtils.resolveEjbVersion( config.getProject() ) ); // installed
            writeFacetInstalledElement( writer, FACET_JST_JAVA, IdeUtils.resolveJavaVersion( config.getProject() ) ); // installed
        }
        else if ( Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            if ( this.config.getWorkspaceConfiguration().getWebsphereVersion() != null )
            {
                writer.startElement( "runtime" );
                writer.addAttribute( "name", config.getWorkspaceConfiguration().getDefaultDeployServerName() );
                writer.endElement(); // runtime

                writeFacetInstalledElement( writer, FACET_COM_IBM_WEBSPHERE_EXTENDED_EAR,
                                            this.config.getWorkspaceConfiguration().getWebsphereVersion() ); // installed
                writeFacetInstalledElement( writer, FACET_COM_IBM_WEBSPHERE_COEXISTENCE_EAR,
                                            this.config.getWorkspaceConfiguration().getWebsphereVersion() ); // installed

            }
            writeFacetFixedElement( writer, FACET_JST_EAR ); // fixed
            writeFacetInstalledElement( writer, FACET_JST_EAR, JeeUtils.resolveJeeVersion( config.getProject() ) ); // installed

        }
        else if ( Constants.PROJECT_PACKAGING_JAR.equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            writeFacetFixedElement( writer, FACET_JST_JAVA ); // fixed
            writeFacetFixedElement( writer, FACET_JST_UTILITY ); // fixed
            writeFacetInstalledElement( writer, FACET_JST_UTILITY, "1.0" ); //$NON-NLS-1$
            writeFacetInstalledElement( writer, FACET_JST_JAVA, IdeUtils.resolveJavaVersion( config.getProject() ) ); // installed
            // installed
        }

        writeAdditionalProjectFacets( writer );

        writer.endElement(); // faceted-project
    }

    /**
     * Writes facet <code>fixed</code> element with attribute <code>facet</code> set to the value of argument
     * <code>facetName</code>.
     * 
     * @param writer
     * @param facetName
     */
    private void writeFacetFixedElement( XMLWriter writer, String facetName )
    {
        writer.startElement( ELT_FIXED );
        writer.addAttribute( ATTR_FACET, facetName );
        writer.endElement();
    }

    /**
     * Writes a facet <code>installed</code> element with attribute <code>facet</code> set to the value of argument
     * <code>facetName</code>, and attribute <code>version</code> set to the value of argument <code>facetVersion</code>
     * .
     * 
     * @param writer
     * @param facetName
     * @param facetVersion
     */
    private void writeFacetInstalledElement( XMLWriter writer, String facetName, String facetVersion )
    {
        writer.startElement( ELT_INSTALLED );
        writer.addAttribute( ATTR_FACET, facetName );
        writer.addAttribute( ATTR_VERSION, facetVersion );
        writer.endElement();
    }

    /**
     * Writes out any additional project facets specified in the plugin configuration
     * 
     * @param writer
     * @param packaging
     */
    private void writeAdditionalProjectFacets( XMLWriter writer )
    {
        if ( config.getProjectFacets() == null )
        {
            return;
        }

        Iterator facetIterator = config.getProjectFacets().entrySet().iterator();
        while ( facetIterator.hasNext() )
        {
            Entry facetEntry = (Entry) facetIterator.next();

            writer.startElement( ELT_INSTALLED );
            writer.addAttribute( ATTR_FACET, (String) facetEntry.getKey() );
            writer.addAttribute( ATTR_VERSION, (String) facetEntry.getValue() );
            writer.endElement(); // installed
        }
    }

}
