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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Adds the classpath variable MAVEN_REPO to Eclipse.
 * 
 * @goal add-maven-repo
 * @requiresProject false
 */
public class AddMavenRepoMojo
    extends AbstractMojo
{

    /**
     * Path under Eclipse workspace where Eclipse Plugin metadata/config is
     * stored.
     */
    public static final String DIR_ECLIPSE_PLUGINS_METADATA = ".metadata/.plugins"; //$NON-NLS-1$

    /**
     * Path under {@value #DIR_ECLIPSE_PLUGINS_METADATA } folder where Eclipse
     * Workspace Runtime settings are stored.
     */
    public static final String DIR_ECLIPSE_CORE_RUNTIME_SETTINGS = DIR_ECLIPSE_PLUGINS_METADATA
        + "/org.eclipse.core.runtime/.settings"; //$NON-NLS-1$

    /**
     * File that stores the Eclipse JDT Core preferences.
     */
    public static final String FILE_ECLIPSE_JDT_CORE_PREFS = "org.eclipse.jdt.core.prefs"; //$NON-NLS-1$

    /**
     * Property constant under which Variable 'M2_REPO' is setup.
     */
    public static final String CLASSPATH_VARIABLE_M2_REPO = "org.eclipse.jdt.core.classpathVariable.M2_REPO"; //$NON-NLS-1$

    /**
     * Location of the <code>Eclipse</code> workspace that holds your
     * configuration and source. On Windows, this will be the
     * <code>workspace</code> directory under your eclipse installation. For
     * example, if you installed eclipse into <code>c:\eclipse</code>, the
     * workspace is <code>c:\eclipse\workspace</code>.
     * 
     * @parameter expression="${eclipse.workspace}"
     * @required
     */
    private String workspace;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    public void execute()
        throws MojoExecutionException
    {

        File workDir = new File( workspace, DIR_ECLIPSE_CORE_RUNTIME_SETTINGS );
        workDir.mkdirs();

        Properties props = new Properties();

        File f = new File( workDir, FILE_ECLIPSE_JDT_CORE_PREFS );

        // preserve old settings
        if ( f.exists() )
        {
            try
            {
                props.load( new FileInputStream( f ) );
            }
            catch ( FileNotFoundException e )
            {
                throw new MojoExecutionException( Messages
                    .getString( "EclipsePlugin.cantreadfile", f.getAbsolutePath() ), e ); //$NON-NLS-1$
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( Messages
                    .getString( "EclipsePlugin.cantreadfile", f.getAbsolutePath() ), e ); //$NON-NLS-1$
            }
        }

        props.put( CLASSPATH_VARIABLE_M2_REPO, localRepository.getBasedir() ); //$NON-NLS-1$  //$NON-NLS-2$

        try
        {
            OutputStream os = new FileOutputStream( f );
            props.store( os, null );
            os.close();
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantwritetofile", //$NON-NLS-1$
                                                                  f.getAbsolutePath() ) );
        }
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public String getWorkspace()
    {
        return workspace;
    }

    public void setWorkspace( String workspace )
    {
        this.workspace = workspace;
    }
}
