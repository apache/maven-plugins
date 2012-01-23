package org.apache.maven.plugin.war.packaging;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.plugin.war.util.ClassesPackager;
import org.apache.maven.plugin.war.util.PathSet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;

import java.io.File;
import java.io.IOException;

/**
 * Handles the classes directory that needs to be packaged in the web application.
 * <p/>
 * Based on the {@link WarPackagingContext#archiveClasses()} flag the resources
 * either copied into to <tt>WEB-INF/classes</tt> directory or archived in a jar
 * within the <tt>WEB-INF/lib</tt> directory.
 *
 * @author Stephane Nicoll
 *
 * @version $Id$
 */
public class ClassesPackagingTask
    extends AbstractWarPackagingTask
{
    private final Overlay currentProjectOverlay;

    public ClassesPackagingTask( Overlay currentProjectOverlay )
    {
        this.currentProjectOverlay = currentProjectOverlay;
    }

    public void performPackaging( WarPackagingContext context )
        throws MojoExecutionException
    {
        final File webappClassesDirectory = new File( context.getWebappDirectory(), CLASSES_PATH );
        if ( !webappClassesDirectory.exists() )
        {
            webappClassesDirectory.mkdirs();
        }

        if ( context.getClassesDirectory().exists() && !context.getClassesDirectory().equals( webappClassesDirectory ) )
        {
            if ( context.archiveClasses() )
            {
                generateJarArchive( context );
            }
            else
            {
                final PathSet sources = getFilesToIncludes( context.getClassesDirectory(), null, null );
                try
                {
                    copyFiles( currentProjectOverlay.getId(), context, context.getClassesDirectory(),
                               sources, CLASSES_PATH, false );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException(
                        "Could not copy webapp classes [" + context.getClassesDirectory().getAbsolutePath() + "]", e );
                }
            }
        }
    }

    protected void generateJarArchive( WarPackagingContext context )
        throws MojoExecutionException
    {
        MavenProject project = context.getProject();
        ArtifactFactory factory = context.getArtifactFactory();
        Artifact artifact = factory.createBuildArtifact( project.getGroupId(), project.getArtifactId(),
                                                         project.getVersion(), "jar" );
        String archiveName = null;
        try
        {
            archiveName = getArtifactFinalName( context, artifact );
        }
        catch ( InterpolationException e )
        {
            throw new MojoExecutionException(
                "Could not get the final name of the artifact [" + artifact.getGroupId() + ":" + artifact.getArtifactId()
                    + ":" + artifact.getVersion() + "]", e );
        }
        final String targetFilename = LIB_PATH + archiveName;

        if ( context.getWebappStructure().registerFile( currentProjectOverlay.getId(), targetFilename ) )
        {
            final File libDirectory = new File( context.getWebappDirectory(), LIB_PATH );
            final File jarFile = new File( libDirectory, archiveName );
            final ClassesPackager packager = new ClassesPackager();
            packager.packageClasses( context.getClassesDirectory(), jarFile, context.getJarArchiver(),
                                     context.getSession(), project, context.getArchive() );
        }
        else
        {
            context.getLog().warn(
                "Could not generate archive classes file [" + targetFilename + "] has already been copied." );
        }
    }
}
