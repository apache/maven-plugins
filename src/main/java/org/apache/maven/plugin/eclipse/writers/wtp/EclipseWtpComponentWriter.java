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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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

    /**
     * Context root attribute.
     */
    public static final String ATTR_CONTEXT_ROOT = "context-root"; //$NON-NLS-1$

    /**
     * The .settings folder for Web Tools Project 1.x release.
     */
    public static final String DIR_WTP_SETTINGS = ".settings"; //$NON-NLS-1$

    /**
     * File name where the WTP component settings will be stored for our Eclipse Project.
     * 
     * @return <code>.component</code>
     */
    protected String getComponentFileName()
    {
        return ".component"; //$NON-NLS-1$
    }

    /**
     * Version number added to component configuration.
     * 
     * @return <code>1.0</code>
     */
    protected String getProjectVersion()
    {
        return null;
    }

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
        try
        {
            w =
                new OutputStreamWriter( new FileOutputStream( new File( settingsDir, getComponentFileName() ) ),
                                        "UTF-8" );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        // create a .component file and write out to it
        XMLWriter writer = new PrettyPrintXMLWriter( w, "UTF-8", null );

        writeModuleTypeComponent( writer, config.getPackaging(), config.getBuildOutputDirectory(),
                                  config.getSourceDirs(), config.getLocalRepository() );

        IOUtil.close( w );
    }

    /**
     * Writes out the module type settings for a Web Tools Project to a component file.
     * 
     * @param writer
     * @param packaging
     * @param buildOutputDirectory
     * @param sourceDirs
     * @param localRepository
     * @throws MojoExecutionException
     */
    private void writeModuleTypeComponent( XMLWriter writer, String packaging, File buildOutputDirectory,
                                           EclipseSourceDir[] sourceDirs, ArtifactRepository localRepository )
        throws MojoExecutionException
    {
        writer.startElement( ELT_PROJECT_MODULES );
        writer.addAttribute( ATTR_MODULE_ID, "moduleCoreId" ); //$NON-NLS-1$
        if ( getProjectVersion() != null )
        {
            writer.addAttribute( ATTR_PROJECT_VERSION, getProjectVersion() );
        }
        writer.startElement( ELT_WB_MODULE );

        // we should use the eclipse project name as the deploy name.
        writer.addAttribute( ATTR_DEPLOY_NAME, this.config.getEclipseProjectName() );

        // deploy-path is "/" for utility and ejb projects, "/WEB-INF/classes" for webapps
        String target = "/"; //$NON-NLS-1$

        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {
            target = "/WEB-INF/classes"; //$NON-NLS-1$

            File warSourceDirectory =
                new File( IdeUtils.getPluginSetting( config.getProject(), JeeUtils.ARTIFACT_MAVEN_WAR_PLUGIN,
                                                     "warSourceDirectory", //$NON-NLS-1$
                                                     config.getProject().getBasedir() + "/src/main/webapp" ) ); //$NON-NLS-1$

            writeContextRoot( writer );

            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_SOURCE_PATH,
                                 IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(),
                                                                     warSourceDirectory, false ) );
            writer.endElement();

            // add web resources over the top of the war source directory
            Xpp3Dom[] webResources =
                IdeUtils.getPluginConfigurationDom( config.getProject(), JeeUtils.ARTIFACT_MAVEN_WAR_PLUGIN,
                                                    new String[] { "webResources", "resource" } );
            for ( int index = 0; index < webResources.length; index++ )
            {
                File webResourceDirectory = new File( webResources[index].getChild( "directory" ).getValue() );
                writer.startElement( ELT_WB_RESOURCE );
                writer.addAttribute( ATTR_DEPLOY_PATH, "/" ); //$NON-NLS-1$
                writer.addAttribute( ATTR_SOURCE_PATH,
                                     IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(),
                                                                         webResourceDirectory, false ) );
                writer.endElement();
            }

            // @todo is this really needed?
            writer.startElement( ELT_PROPERTY );
            writer.addAttribute( ATTR_NAME, "java-output-path" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_VALUE, "/" //$NON-NLS-1$
                + IdeUtils.toRelativeAndFixSeparator( config.getProject().getBasedir(), buildOutputDirectory, false ) );
            writer.endElement(); // property

        }
        else if ( Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) ) //$NON-NLS-1$
        {

            String defaultApplicationXML =
                config.getWtpapplicationxml() ? "/target/eclipseEar" : "/src/main/application";

            String earSourceDirectory =
                IdeUtils.getPluginSetting( config.getProject(), JeeUtils.ARTIFACT_MAVEN_EAR_PLUGIN,
                                           "earSourceDirectory", //$NON-NLS-1$
                                           config.getProject().getBasedir() + defaultApplicationXML ); //$NON-NLS-1$
            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/" ); //$NON-NLS-1$
            writer.addAttribute( ATTR_SOURCE_PATH,
                                 IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(),
                                                                     new File( earSourceDirectory ), false ) );
            writer.endElement();
        }

        if ( Constants.PROJECT_PACKAGING_WAR.equalsIgnoreCase( packaging )
            || Constants.PROJECT_PACKAGING_EAR.equalsIgnoreCase( packaging ) ) //$NON-NLS-1$ //$NON-NLS-2$
        {
            // write out the dependencies.
            writeWarOrEarResources( writer, config.getProject(), localRepository );

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

    /**
     * @param writer
     */
    protected void writeContextRoot( XMLWriter writer )
    {
        writer.startElement( ELT_PROPERTY );
        writer.addAttribute( ATTR_CONTEXT_ROOT, config.getContextName() );
        writer.endElement(); // property
    }

}
