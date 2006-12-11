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
package org.apache.maven.plugin.eclipse.writers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Writes eclipse .wtpmodules file.
 *
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseWtpmodulesWriter
    extends AbstractWtpResourceWriter
{

    protected static final String FILE_DOT_WTPMODULES = ".wtpmodules"; //$NON-NLS-1$

    /**
     * @see org.apache.maven.plugin.eclipse.writers.EclipseWriter#write()
     */
    public void write()
        throws MojoExecutionException
    {
        FileWriter w;

        try
        {
            w = new FileWriter( new File( config.getEclipseProjectDirectory(), FILE_DOT_WTPMODULES ) );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );
        writer.startElement( ELT_PROJECT_MODULES );
        writer.addAttribute( ATTR_MODULE_ID, "moduleCoreId" ); //$NON-NLS-1$

        writer.startElement( ELT_WB_MODULE );
        writer.addAttribute( ATTR_DEPLOY_NAME, config.getProject().getArtifactId() );

        writer.startElement( ELT_MODULE_TYPE );
        writeModuleTypeAccordingToPackaging( config.getProject(), writer, config.getBuildOutputDirectory() );
        writer.endElement(); // module-type

        // source and resource paths.
        // deploy-path is "/" for utility and ejb projects, "/WEB-INF/classes" for webapps

        String target = "/"; //$NON-NLS-1$
        if ( "war".equals( config.getProject().getPackaging() ) ) //$NON-NLS-1$
        {
            String warSourceDirectory = IdeUtils.getPluginSetting( config.getProject(), ARTIFACT_MAVEN_WAR_PLUGIN,
                                                                   "warSourceDirectory", //$NON-NLS-1$
                                                                   "/src/main/webapp" ); //$NON-NLS-1$

            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_SOURCE_PATH, "/" //$NON-NLS-1$
                + IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(), new File( config
                    .getEclipseProjectDirectory(), warSourceDirectory ), false ) );
            writer.endElement();

            writeWarOrEarResources( writer, config.getProject(), config.getLocalRepository() );

            target = "/WEB-INF/classes"; //$NON-NLS-1$
        }
        else if ( "ear".equals( config.getProject().getPackaging() ) ) //$NON-NLS-1$
        {
            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_SOURCE_PATH, "/" ); //$NON-NLS-1$
            writer.endElement();

            writeWarOrEarResources( writer, config.getProject(), config.getLocalRepository() );
        }

        for ( int j = 0; j < config.getSourceDirs().length; j++ )
        {
            EclipseSourceDir dir = config.getSourceDirs()[j];
            // test src/resources are not added to wtpmodules
            if ( !dir.isTest() )
            {
                // <wb-resource deploy-path="/" source-path="/src/java" />
                writer.startElement( ELT_WB_RESOURCE );
                writer.addAttribute( ATTR_DEPLOY_PATH, target );
                writer.addAttribute( ATTR_SOURCE_PATH, dir.getPath() );
                writer.endElement();
            }
        }

        writer.endElement(); // wb-module
        writer.endElement(); // project-modules

        IOUtil.close( w );
    }

}
