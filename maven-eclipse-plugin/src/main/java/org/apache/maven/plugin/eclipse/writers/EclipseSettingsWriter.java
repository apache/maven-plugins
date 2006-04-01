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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseSettingsWriter
    extends AbstractEclipseResourceWriter
{

    /**
     * 'target' property for maven-compiler-plugin.
     */
    private static final String PROPERTY_TARGET = "target"; //$NON-NLS-1$

    /**
     * 'source' property for maven-compiler-plugin.
     */
    private static final String PROPERTY_SOURCE = "source"; //$NON-NLS-1$

    private static final String JDK_1_2_SOURCES = "1.2"; //$NON-NLS-1$

    private static final String FILE_ECLIPSE_JDT_CORE_PREFS = "org.eclipse.jdt.core.prefs"; //$NON-NLS-1$

    private static final String PROP_ECLIPSE_PREFERENCES_VERSION = "eclipse.preferences.version"; //$NON-NLS-1$

    private static final String DIR_DOT_SETTINGS = ".settings"; //$NON-NLS-1$

    private static final String PROP_JDT_CORE_COMPILER_COMPLIANCE = "org.eclipse.jdt.core.compiler.compliance"; //$NON-NLS-1$

    private static final String PROP_JDT_CORE_COMPILER_SOURCE = "org.eclipse.jdt.core.compiler.source"; //$NON-NLS-1$

    private static final String ARTIFACT_MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin"; //$NON-NLS-1$

    public EclipseSettingsWriter( Log log, File eclipseProjectDir, MavenProject project, IdeDependency[] deps )
    {
        super( log, eclipseProjectDir, project, deps );
    }

    public void write()
        throws MojoExecutionException
    {

        // check if it's necessary to create project specific settings
        Properties coreSettings = new Properties();

        String source = IdeUtils.getPluginSetting( getProject(), ARTIFACT_MAVEN_COMPILER_PLUGIN, PROPERTY_SOURCE, null );
        String target = IdeUtils.getPluginSetting( getProject(), ARTIFACT_MAVEN_COMPILER_PLUGIN, PROPERTY_TARGET, null );

        if ( source != null )
        {
            coreSettings.put( PROP_JDT_CORE_COMPILER_SOURCE, source );
            coreSettings.put( PROP_JDT_CORE_COMPILER_COMPLIANCE, source );
        }

        if ( target != null && !JDK_1_2_SOURCES.equals( target ) )
        {
            coreSettings.put( "org.eclipse.jdt.core.compiler.codegen.targetPlatform", target ); //$NON-NLS-1$
        }

        // write the settings, if needed
        if ( !coreSettings.isEmpty() )
        {
            File settingsDir = new File( getEclipseProjectDirectory(), DIR_DOT_SETTINGS ); //$NON-NLS-1$

            settingsDir.mkdirs();

            coreSettings.put( PROP_ECLIPSE_PREFERENCES_VERSION, "1" ); //$NON-NLS-1$ 

            try
            {
                File oldCoreSettingsFile;

                File coreSettingsFile = new File( settingsDir, FILE_ECLIPSE_JDT_CORE_PREFS );

                if ( coreSettingsFile.exists() )
                {
                    oldCoreSettingsFile = coreSettingsFile;

                    Properties oldsettings = new Properties();
                    oldsettings.load( new FileInputStream( oldCoreSettingsFile ) );

                    Properties newsettings = (Properties) oldsettings.clone();
                    newsettings.putAll( coreSettings );

                    if ( !oldsettings.equals( newsettings ) )
                    {
                        newsettings.store( new FileOutputStream( coreSettingsFile ), null );
                    }
                }
                else
                {
                    coreSettings.store( new FileOutputStream( coreSettingsFile ), null );

                    getLog().info( Messages.getString( "EclipseSettingsWriter.wrotesettings", //$NON-NLS-1$
                                                       coreSettingsFile.getCanonicalPath() ) );
                }
            }
            catch ( FileNotFoundException e )
            {
                throw new MojoExecutionException( Messages.getString( "EclipseSettingsWriter.cannotcreatesettings" ), e ); //$NON-NLS-1$
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( Messages.getString( "EclipseSettingsWriter.errorwritingsettings" ), e ); //$NON-NLS-1$
            }
        }
        else
        {
            getLog().info( Messages.getString( "EclipseSettingsWriter.usingdefaults" ) ); //$NON-NLS-1$
        }
    }
}
