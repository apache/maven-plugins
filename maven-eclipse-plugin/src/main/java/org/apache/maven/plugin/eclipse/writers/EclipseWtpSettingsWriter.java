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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.EclipseUtils;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Creates a .settings folder for Eclipse WTP 1.xRCx release and writes out the
 * configuration under it.
 * 
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @version $Id$
 */
public class EclipseWtpSettingsWriter
    extends AbstractWtpResourceWriter
{

    private static final String FACET_JST_EAR = "jst.ear";

    private static final String ATTR_CONTEXT_ROOT = "context-root";

    private static final String ATTR_VERSION = "version";

    private static final String ELT_INSTALLED = "installed";

    private static final String FACET_JST_EJB = "jst.ejb";

    private static final String FACET_JST_WEB = "jst.web";

    private static final String FACET_JST_JAVA = "jst.java";

    private static final String ATTR_FACET = "facet";

    private static final String ELT_FIXED = "fixed";

    private static final String ELT_FACETED_PROJECT = "faceted-project";

    /**
     * The .settings folder for Web Tools Project 1.xRCx release.
     */
    private static final String DIR_WTP_SETTINGS = ".settings";

    /**
     * File name where the WTP component settings will be stored for our Eclipse
     * Project.
     */
    private static final String FILE_DOT_COMPONENT = ".component";

    /**
     * File name where Eclipse Project's Facet configuration will be stored.
     */
    private static final String FILE_FACET_CORE_XML = "org.eclipse.wst.common.project.facet.core.xml";

    public EclipseWtpSettingsWriter( Log log, File eclipseProjectDir, MavenProject project, Collection artifacts )
    {
        super( log, eclipseProjectDir, project, artifacts );
    }

    public void write( List referencedReactorArtifacts, EclipseSourceDir[] sourceDirs,
                      ArtifactRepository localRepository, File buildOutputDirectory )
        throws MojoExecutionException
    {
        // delete the .settings directory (if exists)
        File settingsDir = new File( DIR_WTP_SETTINGS );
        if ( settingsDir.isDirectory() && !settingsDir.delete() )
        {
            // force delete
            try
            {
                FileUtils.forceDelete( settingsDir );
            }
            catch ( IOException e )
            {
                if ( getLog().isErrorEnabled() )
                    getLog().error( "Unable to delete directory " + DIR_WTP_SETTINGS );
            }
        }

        // create a .settings directory
        FileUtils.mkdir( DIR_WTP_SETTINGS );
        FileWriter w;
        try
        {
            w = new FileWriter( new File( getEclipseProjectDirectory() + "/" + DIR_WTP_SETTINGS, FILE_DOT_COMPONENT ) ); //$NON-NLS-1$
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        // create a .component file and write out to it
        XMLWriter writer = new PrettyPrintXMLWriter( w );
        String packaging = getProject().getPackaging();
        writeModuleTypeComponent( writer, packaging, buildOutputDirectory, referencedReactorArtifacts, localRepository );
        IOUtil.close( w );

        // Write out facet core xml
        try
        {
            w = new FileWriter( new File( getEclipseProjectDirectory() + "/" + DIR_WTP_SETTINGS, FILE_FACET_CORE_XML ) ); //$NON-NLS-1$
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }
        writer = new PrettyPrintXMLWriter( w );
        writeModuleTypeFacetCore( writer, packaging );
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
                                          List referencedReactorArtifacts, ArtifactRepository localRepository )
        throws MojoExecutionException
    {
        writer.startElement( ELT_PROJECT_MODULES );
        writer.addAttribute( ATTR_MODULE_ID, "moduleCoreId" );
        writer.startElement( ELT_WB_MODULE );
        writer.addAttribute( ATTR_DEPLOY_NAME, getProject().getArtifactId() );
        if ( "war".equalsIgnoreCase( packaging ) )
        {
            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/WEB-INF/classes" );
            writer.addAttribute( ATTR_SOURCE_PATH, EclipseUtils.toRelativeAndFixSeparator( getProject().getBasedir(),
                                                                                           new File( getProject()
                                                                                               .getBuild()
                                                                                               .getSourceDirectory() ),
                                                                                           false ) );
            writer.endElement();
            String warSourceDirectory = EclipseUtils.getPluginSetting( getProject(), ARTIFACT_MAVEN_WAR_PLUGIN,
                                                                       "warSourceDirectory", "/src/main/webapp" );
            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/" );
            writer.addAttribute( ATTR_SOURCE_PATH, EclipseUtils
                .toRelativeAndFixSeparator( getProject().getBasedir(), new File( getEclipseProjectDirectory(),
                                                                                 warSourceDirectory ), false ) );
            writer.endElement();
        }
        else if ( "ear".equalsIgnoreCase( packaging ) )
        {
            writer.startElement( ELT_WB_RESOURCE );
            writer.addAttribute( ATTR_DEPLOY_PATH, "/ejbmodule" );
            writer.endElement();
        }
        // write out the dependencies.
        writeWarOrEarResources( writer, getProject(), referencedReactorArtifacts, localRepository );
        // write out properties.
        writer.startElement( ELT_PROPERTY );
        writer.addAttribute( ATTR_NAME, "java-output-path" );
        // writer.addAttribute (ATTR_VALUE, "/" +
        // EclipseUtils.toRelativeAndFixSeparator (getProject ().getBasedir (),
        // buildOutputDirectory, false));
        writer.addAttribute( ATTR_VALUE, "/build/classes/" );
        // close elements
        writer.endElement(); // property
        writer.startElement( ELT_PROPERTY );
        writer.addAttribute( ATTR_CONTEXT_ROOT, getProject().getArtifactId() );
        writer.endElement(); // property
        writer.endElement(); // wb-module
        writer.endElement(); // project-modules
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
        if ( "war".equalsIgnoreCase( packaging ) )
        {
            writer.startElement( ELT_FIXED );
            writer.addAttribute( ATTR_FACET, FACET_JST_WEB );
            writer.endElement(); // fixed
            writer.startElement( ELT_INSTALLED );
            writer.addAttribute( ATTR_FACET, FACET_JST_WEB );
            writer.addAttribute( ATTR_VERSION, "2.4" );
            writer.endElement(); // installed
        }
        else if ( "ejb".equalsIgnoreCase( packaging ) )
        {
            writer.startElement( ELT_FIXED );
            writer.addAttribute( ATTR_FACET, FACET_JST_EJB );
            writer.endElement(); // fixed
            writer.startElement( ELT_INSTALLED );
            writer.addAttribute( ATTR_FACET, FACET_JST_EJB );
            writer.addAttribute( ATTR_VERSION, "2.1" );
            writer.endElement(); // installed
        }
        else if ( "ear".equalsIgnoreCase( packaging ) )
        {
            writer.startElement( ELT_FIXED );
            writer.addAttribute( ATTR_FACET, FACET_JST_EAR );
            writer.endElement(); // fixed
            writer.startElement( ELT_INSTALLED );
            writer.addAttribute( ATTR_FACET, FACET_JST_EAR );
            writer.addAttribute( ATTR_VERSION, "1.4" );
            writer.endElement(); // installed
        }
        // common installed element
        writer.startElement( ELT_INSTALLED );
        writer.addAttribute( ATTR_FACET, FACET_JST_JAVA );
        writer.addAttribute( ATTR_VERSION, "1.4" );
        writer.endElement(); // installed
        writer.endElement(); // faceted-project
    }

    /**
     * Patch that overrides the default implementation for super{@link #writeWarOrEarResources(XMLWriter, MavenProject, List, ArtifactRepository)}
     * to patch issue of referring to external libs by WTP.<br>
     * See <a
     * href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=116783">https://bugs.eclipse.org/bugs/show_bug.cgi?id=116783</a>
     * <br>
     * TODO: Remove this method definition the issue is addressed in WTP.
     */
    protected void writeWarOrEarResources( XMLWriter writer, MavenProject project, List referencedReactorArtifacts,
                                          ArtifactRepository localRepository )
        throws MojoExecutionException
    {
        ScopeArtifactFilter scopeFilter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
        String warSourceDirectory = EclipseUtils.getPluginSetting( getProject(), ARTIFACT_MAVEN_WAR_PLUGIN,
                                                                   "warSourceDirectory", "/src/main/webapp/" );
        String webInfLibDirectory = getEclipseProjectDirectory() + "/" + warSourceDirectory + "/WEB-INF/lib";

        if ( getLog().isWarnEnabled() )
        {
            getLog().warn( "----------------------------------------------------------------------------" );
            getLog().warn( "Copying over dependencies for WTP1.0 Project to directory: " + webInfLibDirectory );
            getLog()
                .warn(
                       "Please NOTE that this is a patch to allow publishing external dependencies for a WTP1.0 project." );
            getLog().warn( "----------------------------------------------------------------------------" );
        }

        // dependencies
        for ( Iterator it = getDependencies().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            String type = artifact.getType();

            if ( ( scopeFilter.include( artifact ) || Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
                && ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type ) || "war".equals( type ) ) )
            {
                // we want this bit container independent, so copy over
                // everything to /WEB-INF/lib under our eclipse
                // warSourceDirectory
                // and add a deploy-path so that resources get published.
                try
                {

                    getLog().info( "Copying dependency: " + artifact.getFile().getName() + "..." );
                    FileUtils.copyFileToDirectory( artifact.getFile(), new File( webInfLibDirectory ) );
                }
                catch ( IOException e )
                {
                    // we log the error and still go ahead with the wtp project
                    // creation.

                    getLog().error(
                                    "Unable to copy dependency: " + artifact.getFile().getAbsolutePath()
                                        + " over to web app lib directory : " + webInfLibDirectory );
                }
            }
        }
        if ( getLog().isWarnEnabled() )
        {
            getLog().warn( "----------------------------------------------------------------------------" );
            getLog().warn( "WTP1.0 Project dependencies copied!" );
            getLog().warn( "----------------------------------------------------------------------------" );
        }
        writer.startElement( ELT_WB_RESOURCE );
        writer.addAttribute( ATTR_DEPLOY_PATH, "/WEB-INF/lib" );
        writer.addAttribute( ATTR_SOURCE_PATH, webInfLibDirectory );
        writer.endElement();
    }

}
