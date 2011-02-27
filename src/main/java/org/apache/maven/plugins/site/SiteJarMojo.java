package org.apache.maven.plugins.site;

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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Bundles the site output into a JAR so that it can be deployed to a repository.
 *
 * @author <a href="mailto:mbeerman@yahoo.com">Matthew Beermann</a>
 * @version $Id$
 * @goal jar
 * @phase package
 * @since 2.0-beta-6
 */
public class SiteJarMojo
    extends SiteMojo
{
    /**
     * Specifies the directory where the generated jar file will be put.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String jarOutputDirectory;

    /**
     * Specifies the filename that will be used for the generated jar file.
     * Please note that "-site" will be appended to the file name.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * Used for attaching the artifact in the project.
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Specifies whether to attach the generated artifact to the project.
     *
     * @parameter expression="${site.attach}" default-value="true"
     */
    private boolean attach;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !outputDirectory.exists() )
        {
            super.execute();
        }

        try
        {
            File outputFile = createArchive( outputDirectory,
                                             finalName + "-" + getClassifier() + "." + getArtifactType() );

            if ( attach )
            {
                projectHelper.attachArtifact( project, getArtifactType(), getClassifier(), outputFile );
            }
            else
            {
                getLog().info( "NOT adding site jar to the list of attached artifacts." );
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

    protected String getArtifactType()
    {
        return "jar";
    }

    protected String getClassifier()
    {
        return "site";
    }

    /**
     * Method that creates the jar file.
     *
     * @param siteDirectory the directory where the site files are located
     * @param jarFilename   the filename of the created jar file
     * @return a File object that contains the created jar file
     * @throws ArchiverException
     * @throws IOException
     */
    private File createArchive( File siteDirectory, String jarFilename )
        throws ArchiverException, IOException
    {
        File siteJar = new File( jarOutputDirectory, jarFilename );

        if ( siteJar.exists() )
        {
            siteJar.delete();
        }

        JarArchiver archiver = new JarArchiver();
        archiver.addDirectory( siteDirectory );
        archiver.setDestFile( siteJar );
        archiver.createArchive();

        return siteJar;
    }
}
