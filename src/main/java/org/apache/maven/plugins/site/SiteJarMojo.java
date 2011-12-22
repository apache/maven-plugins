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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

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
    private static final String[] DEFAULT_ARCHIVE_EXCLUDES = new String[]{};

    private static final String[] DEFAULT_ARCHIVE_INCLUDES = new String[]{"**/**"};

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
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     * @since 3.1
     */
    private JarArchiver jarArchiver;

    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     *
     * @parameter
     * @since 3.1
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * List of files to include. Specified as file set patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     *
     * @parameter
     * @since 3.1
     */
    private String[] archiveIncludes;

    /**
     * List of files to exclude. Specified as file set patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     *
     * @parameter
     * @since 3.1
     */
    private String[] archiveExcludes;

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
        catch ( ManifestException e )
        {
            throw new MojoExecutionException( "Error while creating archive.", e );
        }
        catch ( DependencyResolutionRequiredException e )
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
     * @throws ManifestException
     * @throws DependencyResolutionRequiredException
     */
    private File createArchive( File siteDirectory, String jarFilename )
        throws ArchiverException, IOException, ManifestException, DependencyResolutionRequiredException
    {
        File siteJar = new File( jarOutputDirectory, jarFilename );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( this.jarArchiver );

        archiver.setOutputFile( siteJar );

        if ( !siteDirectory.isDirectory() )
        {
            getLog().warn( "JAR will be empty - no content was marked for inclusion !" );
        }
        else
        {
            archiver.getArchiver().addDirectory( siteDirectory, getArchiveIncludes(), getArchiveExcludes() );
        }

        archiver.createArchive( project, archive );

        return siteJar;
    }

    private String[] getArchiveIncludes()
    {
        if ( this.archiveIncludes != null && this.archiveIncludes.length > 0 )
        {
            return this.archiveIncludes;
        }

        return DEFAULT_ARCHIVE_INCLUDES;
    }

    private String[] getArchiveExcludes()
    {
        if ( this.archiveExcludes != null && this.archiveExcludes.length > 0 )
        {
            return this.archiveExcludes;
        }
        return DEFAULT_ARCHIVE_EXCLUDES;
    }
}
