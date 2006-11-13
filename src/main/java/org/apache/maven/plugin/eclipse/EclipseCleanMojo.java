package org.apache.maven.plugin.eclipse;

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
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Deletes the .project, .classpath, .wtpmodules files and .settings folder used by Eclipse.
 * 
 * @goal clean
 */
public class EclipseCleanMojo
    extends AbstractMojo
{

    /**
     * Definition file for Eclipse Web Tools project.
     */
    private static final String FILE_DOT_WTPMODULES = ".wtpmodules"; //$NON-NLS-1$

    /**
     * Classpath definition file for an Eclipse Java project.
     */
    private static final String FILE_DOT_CLASSPATH = ".classpath"; //$NON-NLS-1$

    /**
     * Project definition file for an Eclipse Project.
     */
    private static final String FILE_DOT_PROJECT = ".project"; //$NON-NLS-1$

    /**
     * Web Project definition file for Eclipse Web Tools Project (Release 1.0x).
     */
    private static final String DIR_DOT_SETTINGS = ".settings"; //$NON-NLS-1$

    /**
     * File name where the WTP component settings will be stored - WTP 1.0 name.
     */
    private static final String FILE_DOT_COMPONENT = ".settings/.component"; //$NON-NLS-1$

    /**
     * File name where the WTP component settings will be stored - WTP 1.5 name.
     */
    private static final String FILE_DOT_COMPONENT_15 = ".settings/org.eclipse.wst.common.component"; //$NON-NLS-1$

    /**
     * File name where Eclipse Project's Facet configuration will be stored.
     */
    private static final String FILE_FACET_CORE_XML = ".settings/org.eclipse.wst.common.project.facet.core.xml"; //$NON-NLS-1$

    /**
     * General project preferences.
     */
    private static final String FILE_ECLIPSE_JDT_CORE_PREFS = ".settings/org.eclipse.jdt.core.prefs"; //$NON-NLS-1$

    /**
     * Packaging for the current project.
     * @parameter expression="${project.packaging}"
     * @required
     * @readonly
     */
    private String packaging;

    /**
     * The root directory of the project
     *
     * @parameter expression="${basedir}"
     */
    private File basedir;

    /**
     * Is it an PDE project?
     * 
     * @parameter expression="${eclipse.pde}" default-value="false"
     */
    private boolean pde;

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {

        // since the eclipse plugin doesn't generate configuration for POM projects, it should neither delete it
        if ( "pom".equals( packaging ) ) //$NON-NLS-1$
        {
            getLog().info( Messages.getString( "EclipsePlugin.pompackaging" ) ); //$NON-NLS-1$
            return;
        }

        delete( new File( basedir, FILE_DOT_PROJECT ) );
        delete( new File( basedir, FILE_DOT_CLASSPATH ) );
        delete( new File( basedir, FILE_DOT_WTPMODULES ) );

        delete( new File( basedir, FILE_DOT_COMPONENT ) );
        delete( new File( basedir, FILE_DOT_COMPONENT_15 ) );
        delete( new File( basedir, FILE_FACET_CORE_XML ) );
        delete( new File( basedir, FILE_ECLIPSE_JDT_CORE_PREFS ) );

        File settingsDir = new File( basedir, DIR_DOT_SETTINGS );
        if ( settingsDir.exists() && settingsDir.isDirectory() && settingsDir.list().length == 0 )
        {
            delete( settingsDir );
        }
        
        cleanExtras();
    }

    protected void cleanExtras()
        throws MojoExecutionException
    {
        // extension point.
    }

    /**
     * Delete a file, handling log messages and exceptions
     * 
     * @param f File to be deleted
     * @throws MojoExecutionException only if a file exists and can't be deleted
     */
    protected void delete( File f )
        throws MojoExecutionException
    {
        if ( f.isDirectory() )
        {
            getLog().info( Messages.getString( "EclipseCleanMojo.deletingDirectory", f.getName() ) ); //$NON-NLS-1$
        }
        else
        {
            getLog().info( Messages.getString( "EclipseCleanMojo.deletingFile", f.getName() ) ); //$NON-NLS-1$
        }

        if ( f.exists() )
        {
            if ( !f.delete() )
            {
                try
                {
                    FileUtils.forceDelete( f );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( Messages.getString( "EclipseCleanMojo.failedtodelete", //$NON-NLS-1$
                                                                          new Object[] {
                                                                              f.getName(),
                                                                              f.getAbsolutePath() } ) );
                }
            }
        }
        else
        {
            getLog().debug( Messages.getString( "EclipseCleanMojo.nofilefound", f.getName() ) ); //$NON-NLS-1$
        }
    }

    /**
     * Getter for <code>basedir</code>.
     * @return Returns the basedir.
     */
    public File getBasedir()
    {
        return this.basedir;
    }

    /**
     * Setter for <code>basedir</code>.
     * @param basedir The basedir to set.
     */
    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

}
