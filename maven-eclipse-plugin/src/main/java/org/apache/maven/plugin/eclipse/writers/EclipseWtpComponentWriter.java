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
 * Creates a .settings folder for Eclipse WTP 1.x release and writes out the configuration under it.
 * 
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseWtpComponentWriter
    extends AbstractWtpResourceWriter
{

    private static final String ATTR_CONTEXT_ROOT = "context-root"; //$NON-NLS-1$

    /**
     * The .settings folder for Web Tools Project 1.x release.
     */
    private static final String DIR_WTP_SETTINGS = ".settings"; //$NON-NLS-1$

    /**
     * File name where the WTP component settings will be stored for our Eclipse Project.
     */
    private static final String FILE_DOT_COMPONENT = ".component"; //$NON-NLS-1$

    public EclipseWtpComponentWriter( Log log, File eclipseProjectDir, MavenProject project, IdeDependency[] deps )
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
        try
        {
            w = new FileWriter( new File( settingsDir, FILE_DOT_COMPONENT ) );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        // create a .component file and write out to it
        XMLWriter writer = new PrettyPrintXMLWriter( w );
        String packaging = getProject().getPackaging();
        writeModuleTypeComponent( writer, packaging, buildOutputDirectory, sourceDirs, localRepository );
        IOUtil.close( w );

    }

    /**
     * Writes out the module type settings for a Web Tools Project to a {@link #FILE_DOT_COMPONENT}.
     * 
     * @param writer
     * @param packaging
     * @param buildOutputDirectory
     * @param referencedReactorArtifacts
     * @param localRepository
     * @throws MojoExecutionException
     */
    private void writeModuleTypeComponent( XMLWriter writer, String packaging, File buildOutputDirectory,
                                           EclipseSourceDir[] sourceDirs, ArtifactRepository localRepository )
        throws MojoExecutionException
    {
        writer.startElement( ELT_PROJECT_MODULES );
        writer.addAttribute( ATTR_MODULE_ID, "moduleCoreId" ); //$NON-NLS-1$
        writer.startElement( ELT_WB_MODULE );

        writer.addAttribute( ATTR_DEPLOY_NAME, getProject().getArtifactId() );

        // deploy-path is "/" for utility and ejb projects, "/WEB-INF/classes" for webapps
        String target = "/"; //$NON-NLS-1$

        if ( "war".equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            target = "/WEB-INF/classes"; //$NON-NLS-1$

            String warSourceDirectory = IdeUtils.getPluginSetting( getProject(), ARTIFACT_MAVEN_WAR_PLUGIN,
                                                                   "warSourceDirectory", //$NON-NLS-1$
                                                                   "/src/main/webapp" ); //$NON-NLS-1$

            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_CONTEXT_ROOT, getProject().getArtifactId() );
            writer.endElement(); // property

            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_SOURCE_PATH, IdeUtils
                .toRelativeAndFixSeparator( getProject().getBasedir(), new File( getEclipseProjectDirectory(),
                                                                                 warSourceDirectory ), false ) );
            writer.endElement();

            // @todo is this really needed?
            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_NAME, "java-output-path" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_VALUE, "/" //$NON-NLS-1$
                + IdeUtils.toRelativeAndFixSeparator( getProject().getBasedir(), buildOutputDirectory, false ) );
            writer.endElement(); // property

        }
        else if ( "ear".equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/" ); //$NON-NLS-1$ 
            writer.addAttribute( ATTR_SOURCE_PATH, "/" ); //$NON-NLS-1$ 
            writer.endElement();
        }

        if ( "war".equalsIgnoreCase( packaging ) || "ear".equalsIgnoreCase( packaging ) ) //$NON-NLS-1$ //$NON-NLS-2$
        {
            // write out the dependencies.
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
    }

}
