package org.apache.maven.plugins.mavenone;

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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * Package a Maven 1 plugin.
 *
 * @goal maven-one-plugin
 * @phase package
 */
public class MavenOnePluginMojo
    extends AbstractMojo
{

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/package.html"};

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    /**
     * Base directory.
     *
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * Directory containing the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File targetDirectory;

    /**
     * Name of the generated JAR.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Used for attaching the artifact in the project.
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Directory that contains the compiled classes to include in the jar.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File contentDirectory;

    /**
     * Generates the JAR.
     *
     * @todo Add license files in META-INF directory.
     */
    public File createArchive()
        throws MojoExecutionException
    {
        File jarFile = new File( targetDirectory, finalName + ".jar" );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        try
        {
            if ( contentDirectory.exists() )
            {
                archiver.getArchiver().addDirectory( contentDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES );
            }

            addFile( archiver, new File( basedir, "plugin.jelly" ) );
            addFile( archiver, new File( basedir, "plugin.properties" ) );
            addFile( archiver, new File( basedir, "project.properties" ) );
            addFile( archiver, new File( basedir, "build.properties" ) );
            addFile( archiver, new File( basedir, "project.xml" ) );
            addDirectory( archiver, new File( basedir, "src/plugin-resources" ) );

            archiver.createArchive( project, new MavenArchiveConfiguration() );

            return jarFile;
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error assembling JAR", e );
        }
    }

    private static void addDirectory( MavenArchiver archiver, File file )
        throws ArchiverException
    {
        if ( file.exists() )
        {
            archiver.getArchiver().addDirectory( file, file.getName() + "/", DEFAULT_INCLUDES,
                                                 FileUtils.getDefaultExcludes() );
        }
    }

    private static void addFile( MavenArchiver archiver, File file )
        throws ArchiverException
    {
        if ( file.exists() )
        {
            archiver.getArchiver().addFile( file, file.getName() );
        }
    }

    /**
     * Generates the JAR.
     *
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws MojoExecutionException
    {
        File jarFile = createArchive();

        project.getArtifact().setFile( jarFile );
    }
}
