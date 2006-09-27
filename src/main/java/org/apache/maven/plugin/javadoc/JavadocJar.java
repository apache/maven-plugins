package org.apache.maven.plugin.javadoc;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Bundles the javadoc documentation in a jar so it can be deployed to the repo.
 *
 * @goal jar
 * @phase package
 */
public class JavadocJar
    extends AbstractJavadocMojo
{
    /**
     * Specifies the destination directory where javadoc saves the generated HTML files.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#d">d</a>.
     *
     * @parameter expression="${destDir}"
     * @deprecated
     */
    private File destDir;

    /**
     * Specifies the directory where the generated jar file will be put.
     *
     * @parameter expression="${project.build.directory}"
     */
    private String jarOutputDirectory;

    /**
     * Specifies the filename that will be used for the generated jar file. Please note that "-javadoc"
     * will be appended to the file name.
     *
     * @parameter expression="${project.build.finalName}"
     */
    private String finalName;

    /**
     * Used for attaching the artifact in the project
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Specifies whether to attach the generated artifact to the project helper.
     *
     * @parameter expression="${attach}" default-value="true"
     */
    private boolean attach;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        File destDir = this.destDir;

        if ( destDir == null )
        {
            destDir = outputDirectory;
        }

        // The JAR does not operate in aggregation mode - individual Javadoc JARs are always distributed.
        aggregate = false;

        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        if ( !"java".equals( artifactHandler.getLanguage() ) )
        {
            getLog().info( "Not executing Javadoc as the project is not a Java classpath-capable package" );
        }
        else
        {
            try
            {
                executeReport( Locale.getDefault() );

                if ( destDir.exists() )
                {
                    File outputFile = generateArchive( destDir, finalName + "-javadoc.jar" );

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
            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException( "Error while creating archive:" + e.getMessage(), e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error while creating archive:" + e.getMessage(), e );
            }
            catch ( MavenReportException e )
            {
                throw new MojoExecutionException( "Error while creating archive:" + e.getMessage(), e );
            }
        }
    }

    /**
     * Method that creates the jar file
     *
     * @param javadocFiles the directory where the generated jar file will be put
     * @param target       the filename of the generated jar file
     * @return a File object that contains the generated jar file
     * @throws ArchiverException
     * @throws IOException
     */
    private File generateArchive( File javadocFiles, String target )
        throws ArchiverException, IOException
    {
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
