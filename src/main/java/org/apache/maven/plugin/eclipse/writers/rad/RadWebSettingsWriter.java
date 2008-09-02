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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.ide.JeeUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Creates a .settings folder for Eclipse WTP 1.x release and writes out the configuration under it.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven </a>
 */
public class RadWebSettingsWriter
    extends AbstractEclipseWriter
{

    private static final String COM_IBM_ETOOLS_SITEEDIT_WIZARDS_PROJECTFEATURE_WEB_SITE_FEATURE =
        "com.ibm.etools.siteedit.wizards.projectfeature.WebSiteFeature";

    private static final String WEBSETTINGS_CONTEXT_ROOT = "context-root";

    private static final String WEBSETTINGS_FEATURE = "feature";

    private static final String WEBSETTINGS_FEATURE_ID = "feature-id";

    private static final String WEBSETTINGS_FEATURES = "features";

    private static final String WEBSETTINGS_FILENAME = ".websettings";

    private static final String WEBSETTINGS_JSP_LEVEL = "jsp-level";

    private static final String WEBSETTINGS_PROJECT_TYPE = "project-type";

    private static final String WEBSETTINGS_TEMPLATEFEATURE = "templatefeature";

    private static final String WEBSETTINGS_VERSION = "version";

    private static final String WEBSETTINGS_WEBCONTENT = "webcontent";

    private static final String WEBSETTINGS_WEBSETTINGS = "websettings";

    private static final String WEBSETTINGS_LIBMODULES = "lib-modules";

    private static final String WEBSETTINGS_LIBMODULE = "lib-module";

    private static final String WEBSETTINGS_LM_JAR = "jar";

    private static final String WEBSETTINGS_LM_PROJECT = "project";

    /**
     * the context root to use for this project
     */
    private String warContextRoot;

    /**
     * required default constructor.
     * 
     * @param warContextRoot the context root to use for this project
     */
    public RadWebSettingsWriter( String warContextRoot )
    {
        this.warContextRoot = warContextRoot;
    }

    /**
     * write the websettings file for RAD6 if needed.
     * 
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
                                                                            WEBSETTINGS_FILENAME ) ), "UTF-8" );
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
     * write the websettings file for RAD6.
     * 
     * @param writer where to write to
     * @throws MojoExecutionException
     */
    private void writeModuleTypeFacetCore( XMLWriter writer )
        throws MojoExecutionException
    {
        writer.startElement( WEBSETTINGS_WEBSETTINGS );
        writer.addAttribute( WEBSETTINGS_VERSION, "600" );
        writer.startElement( WEBSETTINGS_WEBCONTENT );

        // Generating web content settings based on war plug-in warSourceDirectory property
        File warSourceDirectory =
            new File( IdeUtils.getPluginSetting( config.getProject(), JeeUtils.ARTIFACT_MAVEN_WAR_PLUGIN,
                                                 "warSourceDirectory", //$NON-NLS-1$
                                                 config.getProject().getBasedir() + "/src/main/webapp" ) ); //$NON-NLS-1$
        String webContentDir =
            IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(), warSourceDirectory, false );

        writer.writeText( webContentDir );

        writer.endElement();
        writer.startElement( WEBSETTINGS_CONTEXT_ROOT );
        writer.writeText( getContextRoot( warContextRoot ) );
        writer.endElement();
        writer.startElement( WEBSETTINGS_PROJECT_TYPE );
        writer.writeText( "J2EE" );
        writer.endElement();
        writer.startElement( WEBSETTINGS_JSP_LEVEL );
        writer.writeText( JeeUtils.resolveJspVersion( config.getProject() ) );
        writer.endElement();
        writer.startElement( WEBSETTINGS_FEATURES );
        writer.startElement( WEBSETTINGS_FEATURE );
        writer.startElement( WEBSETTINGS_FEATURE_ID );
        writer.writeText( WEBSETTINGS_TEMPLATEFEATURE );
        writer.endElement();
        writer.endElement();
        writer.startElement( WEBSETTINGS_FEATURE );
        writer.startElement( WEBSETTINGS_FEATURE_ID );
        writer.writeText( COM_IBM_ETOOLS_SITEEDIT_WIZARDS_PROJECTFEATURE_WEB_SITE_FEATURE );
        writer.endElement();
        writer.endElement();
        writer.endElement();

        // library modules
        writer.startElement( WEBSETTINGS_LIBMODULES );

        // iterate relevant dependencies (non-test, non-provided, project)
        IdeDependency[] deps = config.getDepsOrdered();
        if ( deps != null )
        {
            for ( int i = 0; i < deps.length; i++ )
            {
                final IdeDependency dependency = deps[i];
                log.debug( "RadWebSettingsWriter: checking dependency " + dependency.toString() );

                if ( dependency.isReferencedProject() && !dependency.isTestDependency() && !dependency.isProvided() )
                {
                    log.debug( "RadWebSettingsWriter: dependency " + dependency.toString()
                        + " selected for inclusion as lib-module" );

                    String depName = dependency.getEclipseProjectName();
                    String depJar = dependency.getArtifactId() + ".jar";

                    writer.startElement( WEBSETTINGS_LIBMODULE );

                    writer.startElement( WEBSETTINGS_LM_JAR );
                    writer.writeText( depJar );
                    writer.endElement(); // jar

                    writer.startElement( WEBSETTINGS_LM_PROJECT );
                    writer.writeText( depName );
                    writer.endElement(); // project

                    writer.endElement(); // libmodule
                }
            }
        }

        writer.endElement(); // libmodules
        writer.endElement(); // websettings

    }

    /**
     * Create the ContextRoot for this project, the default is the artifact id
     * 
     * @param warContextRoot set as a configuration property.
     * @return the context root to use
     */
    private String getContextRoot( String warContextRoot )
    {
        if ( warContextRoot == null || warContextRoot.length() == 0 )
        {
            return config.getProject().getArtifactId();
        }
        else
        {
            return warContextRoot;
        }
    }

}
