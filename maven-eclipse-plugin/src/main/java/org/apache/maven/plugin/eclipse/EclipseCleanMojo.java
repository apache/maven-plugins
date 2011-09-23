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
package org.apache.maven.plugin.eclipse;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.writers.workspace.EclipseWorkspaceWriter;
import org.apache.maven.plugin.ide.IdeUtils;

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
     * Packaging for the current project.
     * 
     * @parameter expression="${project.packaging}"
     */
    private String packaging;

    /**
     * The root directory of the project
     * 
     * @parameter expression="${basedir}"
     */
    private File basedir;

    /**
     * Skip the operation when true.
     * 
     * @parameter expression="${eclipse.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * additional generic configuration files for eclipse
     * 
     * @parameter
     */
    private EclipseConfigFile[] additionalConfig;

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            return;
        }

        if ( Constants.PROJECT_PACKAGING_POM.equals( this.packaging ) )
        {
            return;
        }

        delete( new File( basedir, FILE_DOT_PROJECT ) );
        delete( new File( basedir, FILE_DOT_CLASSPATH ) );
        delete( new File( basedir, FILE_DOT_WTPMODULES ) );

        File settingsDir = new File( basedir, EclipseWorkspaceWriter.DIR_DOT_SETTINGS );
        if ( settingsDir.exists() && settingsDir.isDirectory() && settingsDir.list().length == 0 )
        {
            delete( settingsDir );
        }

        if ( additionalConfig != null )
        {
            for ( int i = 0; i < additionalConfig.length; i++ )
            {
                delete( new File( basedir, additionalConfig[i].getName() ) );
            }
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
        IdeUtils.delete( f, getLog() );
    }

    /**
     * Getter for <code>basedir</code>.
     * 
     * @return Returns the basedir.
     */
    public File getBasedir()
    {
        return this.basedir;
    }

    /**
     * Setter for <code>basedir</code>.
     * 
     * @param basedir The basedir to set.
     */
    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    /**
     * @return the packaging
     */
    public String getPackaging()
    {
        return this.packaging;
    }

    /**
     * @param packaging the packaging to set
     */
    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

}
