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

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseAjdtWriter
    extends AbstractEclipseWriter
{

    /**
     * 
     */
    private static final String LIBRARY = "LIBRARY";

    /**
     * 
     */
    private static final String BINARY = "BINARY";

    /**
     * 
     */
    private static final String CONTENT_KIND = ".contentKind";

    /**
     * 
     */
    private static final String ENTRY_KIND = ".entryKind";

    private static final String FILE_AJDT_PREFS = "org.eclipse.ajdt.ui.prefs"; //$NON-NLS-1$

    private static final String PROP_ECLIPSE_PREFERENCES_VERSION = "eclipse.preferences.version"; //$NON-NLS-1$

    private static final String DIR_DOT_SETTINGS = ".settings"; //$NON-NLS-1$

    private static final String AJDT_PROP_PREFIX = "org.eclipse.ajdt.ui."; //$NON-NLS-1$

    private static final String ASPECT_DEP_PROP = "aspectPath";

    private static final String WEAVE_DEP_PROP = "inPath";

    /**
     * @see org.apache.maven.plugin.eclipse.writers.EclipseWriter#write()
     */
    public void write()
        throws MojoExecutionException
    {

        // check if it's necessary to create project specific settings
        Properties ajdtSettings = new Properties();

        IdeDependency[] deps = config.getDeps();
        int ajdtDepCount = 0;
        int ajdtWeaveDepCount = 0;
        for ( int i = 0; i < deps.length; i++ )
        {
            if ( deps[i].isAjdtDependency() )
            {
                addDependency( ajdtSettings, deps[i], ASPECT_DEP_PROP, ++ajdtDepCount );
            }

            if ( deps[i].isAjdtWeaveDependency() )
            {
                addDependency( ajdtSettings, deps[i], WEAVE_DEP_PROP, ++ajdtWeaveDepCount );
            }
        }

        // write the settings, if needed
        if ( !ajdtSettings.isEmpty() )
        {
            File settingsDir = new File( config.getEclipseProjectDirectory(), DIR_DOT_SETTINGS ); //$NON-NLS-1$

            settingsDir.mkdirs();

            ajdtSettings.put( PROP_ECLIPSE_PREFERENCES_VERSION, "1" ); //$NON-NLS-1$ 

            try
            {
                File oldAjdtSettingsFile;

                File ajdtSettingsFile = new File( settingsDir, FILE_AJDT_PREFS );

                if ( ajdtSettingsFile.exists() )
                {
                    oldAjdtSettingsFile = ajdtSettingsFile;

                    Properties oldsettings = new Properties();
                    oldsettings.load( new FileInputStream( oldAjdtSettingsFile ) );

                    Properties newsettings = (Properties) oldsettings.clone();
                    newsettings.putAll( ajdtSettings );

                    if ( !oldsettings.equals( newsettings ) )
                    {
                        newsettings.store( new FileOutputStream( ajdtSettingsFile ), null );
                    }
                }
                else
                {
                    ajdtSettings.store( new FileOutputStream( ajdtSettingsFile ), null );

                    log.info( Messages.getString( "EclipseSettingsWriter.wrotesettings", //$NON-NLS-1$
                                                  ajdtSettingsFile.getCanonicalPath() ) );
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
    }

    private void addDependency( Properties ajdtSettings, IdeDependency dep, String propName, int index )
        throws MojoExecutionException
    {

        String path;

        if ( dep.isReferencedProject() && !config.isPde() )
        {
            path = "/" + dep.getEclipseProjectName(); //$NON-NLS-1$
        }
        else if ( dep.isReferencedProject() && config.isPde() )
        {
            // don't do anything, referenced projects are automatically handled by eclipse in PDE builds
            return;
        }
        else
        {
            File artifactPath = dep.getFile();

            if ( artifactPath == null )
            {
                log.error( Messages.getString( "EclipsePlugin.artifactpathisnull", dep.getId() ) ); //$NON-NLS-1$
                return;
            }

            if ( dep.isSystemScoped() )
            {
                path = IdeUtils.toRelativeAndFixSeparator( config.getEclipseProjectDirectory(), artifactPath, false );

                if ( log.isDebugEnabled() )
                {
                    log.debug( Messages.getString( "EclipsePlugin.artifactissystemscoped", //$NON-NLS-1$
                                                   new Object[] { dep.getArtifactId(), path } ) );
                }
            }
            else
            {
                File localRepositoryFile = new File( config.getLocalRepository().getBasedir() );

                // if the dependency is not provided and the plugin runs in "pde mode", the dependency is
                // added to the Bundle-Classpath:
                if ( config.isPde() && ( dep.isProvided() || dep.isOsgiBundle() ) )
                {
                    return;
                }
                else if ( config.isPde() && !dep.isProvided() && !dep.isTestDependency() )
                {
                    // path for link created in .project, not to the actual file
                    path = dep.getFile().getName();
                }
                // running in PDE mode and the dependency is provided means, that it is provided by
                // the target platform. This case is covered by adding the plugin container
                else
                {
                    String fullPath = artifactPath.getPath();
                    String relativePath =
                        IdeUtils.toRelativeAndFixSeparator( localRepositoryFile, new File( fullPath ), false );

                    if ( !new File( relativePath ).isAbsolute() )
                    {
                        path = EclipseClasspathWriter.M2_REPO + "/" //$NON-NLS-1$
                            + relativePath;
                    }
                    else
                    {
                        path = relativePath;
                    }
                }
            }
        }

        ajdtSettings.setProperty( AJDT_PROP_PREFIX + propName + CONTENT_KIND + index, BINARY );
        ajdtSettings.setProperty( AJDT_PROP_PREFIX + propName + ENTRY_KIND + index, LIBRARY );
        ajdtSettings.setProperty( AJDT_PROP_PREFIX + propName + index, path );
    }
}
