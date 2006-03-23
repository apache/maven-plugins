package org.apache.maven.plugin.javadoc;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;
import java.io.IOException;

/**
 * Bundles the javadoc documentation in a jar so it can be deployed to the repo.
 *
 * @goal jar
 * @phase package
 * @execute goal="javadoc"
 */
public class JavadocJar
    extends AbstractMojo
{
    /**
     * Specifies the destination directory where javadoc saves the generated HTML files.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#d">d</a>.
     *
     * @parameter expression="${destDir}" default-value="${project.build.directory}/site/apidocs"
     * @required
     */
    private File destDir;

    /**
     * @parameter expression="${project.build.directory}"
     */
    private String jarOutputDirectory;

    /**
     * @parameter expression="${project.build.finalName}"
     */
    private String finalName;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}
     */
    private MavenProjectHelper projectHelper;

    /**
     * @parameter expression="${attach}" default-value="true"
     */
    private boolean attach = true;

    public void execute()
        throws MojoExecutionException
    {
        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        if ( !"java".equals( artifactHandler.getLanguage() ) )
        {
            getLog().info( "Not executing Javadoc as the project is not a Java classpath-capable package" );
        }
        else if ( destDir.exists() )
        {
            try
            {
                File outputFile = generateArchive( destDir.getAbsolutePath(), finalName + "-javadoc.jar" );

                if ( !attach )
                {
                    getLog().info( "NOT adding javadoc to attached artifacts list." );

                }
                else
                {
                    // TODO: these introduced dependencies on the project are going to become problematic - can we export it
                    //  through metadata instead?
                    projectHelper.attachArtifact( project, "javadoc", "javadoc", outputFile );
                }
            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException( "Error while creating archive.", e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error while creating archive.", e );
            }
        }
    }

    private File generateArchive( String source, String target )
        throws MojoExecutionException, ArchiverException, IOException
    {
        File javadocFiles = new File( source );

        if ( !javadocFiles.exists() )
        {
            throw new MojoExecutionException( "javadoc files not found." );
        }

        File javadocJar = new File( jarOutputDirectory, target );

        if ( javadocJar.exists() )
        {
            javadocJar.delete();
        }

        JarArchiver archiver = new JarArchiver();

        archiver.addDirectory( javadocFiles );

        archiver.setDestFile( javadocJar );

        archiver.createArchive();

        return javadocJar;
    }
}
