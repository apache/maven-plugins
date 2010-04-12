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

package org.apache.maven.plugin.javadoc;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.javadoc.options.JavadocOptions;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import java.io.File;
import java.io.IOException;

/**
 * Bundle {@link AbstractJavadocMojo#javadocDirectory}, along with javadoc configuration options such
 * as taglet, doclet, and link information into a deployable artifact. This artifact can then be consumed
 * by the javadoc plugin mojos when used by the <code>includeDependencySources</code> option, to generate
 * javadocs that are somewhat consistent with those generated in the original project itself.
 *  
 * @goal resource-bundle
 * @phase package
 * @since 2.6.2
 */
public class ResourcesBundleMojo
    extends AbstractJavadocMojo
{
    
    public static final String BUNDLE_OPTIONS_PATH = "META-INF/maven/javadoc-options.xml";

    public static final String RESOURCES_DIR_PATH = "resources";

    /**
     * Base name of artifacts produced by this project. This will be combined with 
     * {@link ResourcesBundleMojo#getAttachmentClassifier()} to produce the name for this bundle 
     * jar.
     * 
     * @parameter default-value="${project.build.finalName}"
     * @readonly
     */
    private String finalName;
    
    /**
     * Helper component to provide an easy mechanism for attaching an artifact to the project for 
     * installation/deployment.
     * 
     * @component
     */
    private MavenProjectHelper projectHelper;
    
    /**
     * Archiver manager, used to manage jar builder.
     *
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * Assemble a new {@link JavadocOptions} instance that contains the configuration options in this
     * mojo, which are a subset of those provided in derivatives of the {@link AbstractJavadocMojo}
     * class (most of the javadoc mojos, in other words). Then, bundle the contents of the 
     * <code>javadocDirectory</code> along with the assembled JavadocOptions instance (serialized to
     * META-INF/maven/javadoc-options.xml) into a project attachment for installation/deployment.
     * 
     * {@inheritDoc}
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            buildJavadocOptions();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to generate javadoc-options file: " + e.getMessage(), e );
        }
        
        Archiver archiver;
        try
        {
            archiver = archiverManager.getArchiver( "jar" );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "Failed to retrieve jar archiver component from manager.", e );
        }
        
        File optionsFile = getJavadocOptionsFile();
        File bundleFile = new File( getProject().getBuild().getDirectory(), finalName + "-" + getAttachmentClassifier() + ".jar" );
        try
        {
            archiver.addFile( optionsFile, BUNDLE_OPTIONS_PATH );
            
            File javadocDir = getJavadocDirectory();
            if ( javadocDir.exists() && javadocDir.isDirectory() )
            {
                archiver.addDirectory( javadocDir, RESOURCES_DIR_PATH );
            }
            
            archiver.setDestFile( bundleFile );
            archiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Failed to assemble javadoc-resources bundle archive. Reason: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to assemble javadoc-resources bundle archive. Reason: " + e.getMessage(), e );
        }
        
        projectHelper.attachArtifact( getProject(), bundleFile, getAttachmentClassifier() );
    }
}
