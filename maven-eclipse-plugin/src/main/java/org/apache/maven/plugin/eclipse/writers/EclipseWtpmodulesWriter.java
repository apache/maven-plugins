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
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
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

    public EclipseWtpmodulesWriter( Log log, File eclipseProjectDir, MavenProject project, IdeDependency[] deps )
    {
        super( log, eclipseProjectDir, project, deps );
    }

    public void write( EclipseSourceDir[] sourceDirs, ArtifactRepository localRepository, File buildOutputDirectory )
        throws MojoExecutionException
    {
        FileWriter w;

        try
        {
            w = new FileWriter( new File( getEclipseProjectDirectory(), FILE_DOT_WTPMODULES ) );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );
        writer.startElement( ELT_PROJECT_MODULES );
        writer.addAttribute( ATTR_MODULE_ID, "moduleCoreId" ); //$NON-NLS-1$ 

        writer.startElement( ELT_WB_MODULE );
        writer.addAttribute( ATTR_DEPLOY_NAME, getProject().getArtifactId() );

        String packaging = getProject().getPackaging();

        writer.startElement( ELT_MODULE_TYPE );
        writeModuleTypeAccordingToPackaging( getProject(), writer, packaging, buildOutputDirectory );
        writer.endElement(); // module-type

        // source and resource paths.
        // deploy-path is "/" for utility and ejb projects, "/WEB-INF/classes" for webapps

        String target = "/"; //$NON-NLS-1$
        if ( "war".equals( getProject().getPackaging() ) ) //$NON-NLS-1$
        {
            String warSourceDirectory = IdeUtils.getPluginSetting( getProject(), ARTIFACT_MAVEN_WAR_PLUGIN,
                                                                   "warSourceDirectory", //$NON-NLS-1$
                                                                   "/src/main/webapp" ); //$NON-NLS-1$

            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/" ); //$NON-NLS-1$ 
            writer.addAttribute( ATTR_SOURCE_PATH, "/" //$NON-NLS-1$
                + IdeUtils.toRelativeAndFixSeparator( getEclipseProjectDirectory(),
                                                      new File( getEclipseProjectDirectory(), warSourceDirectory ),
                                                      false ) );
            writer.endElement();

            writeWarOrEarResources( writer, getProject(), localRepository );

            target = "/WEB-INF/classes"; //$NON-NLS-1$
        }
        else if ( "ear".equals( getProject().getPackaging() ) ) //$NON-NLS-1$
        {
            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/" ); //$NON-NLS-1$ 
            writer.addAttribute( ATTR_SOURCE_PATH, "/" ); //$NON-NLS-1$ 
            writer.endElement();

            writeWarOrEarResources( writer, getProject(), localRepository );
        }

        for ( int j = 0; j < sourceDirs.length; j++ )
        {
            EclipseSourceDir dir = sourceDirs[j];
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
