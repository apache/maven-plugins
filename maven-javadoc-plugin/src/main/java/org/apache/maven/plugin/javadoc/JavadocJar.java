package org.apache.maven.plugin.javadoc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.model.Resource;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Bundles the Javadoc documentation for <code>main Java code</code> in an <b>NON aggregator</b> project into
 * a jar using the standard <a href="http://java.sun.com/j2se/javadoc/">Javadoc Tool</a>.
 *
 * @version $Id$
 * @since 2.0
 * @goal jar
 * @phase package
 */
public class JavadocJar
    extends AbstractJavadocMojo
{
    /** Includes all generated Javadoc files */
    private static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };

    /**
     * Excludes all processing files.
     *
     * @see AbstractJavadocMojo#DEBUG_JAVADOC_SCRIPT_NAME
     * @see AbstractJavadocMojo#OPTIONS_FILE_NAME
     * @see AbstractJavadocMojo#PACKAGES_FILE_NAME
     * @see AbstractJavadocMojo#ARGFILE_FILE_NAME
     * @see AbstractJavadocMojo#FILES_FILE_NAME
     */
    private static final String[] DEFAULT_EXCLUDES =
        new String[] { DEBUG_JAVADOC_SCRIPT_NAME, OPTIONS_FILE_NAME, PACKAGES_FILE_NAME, ARGFILE_FILE_NAME,
            FILES_FILE_NAME };

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Used for attaching the artifact in the project.
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     * @since 2.5
     */
    private JarArchiver jarArchiver;

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

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
     * Specifies the filename that will be used for the generated jar file. Please note that <code>-javadoc</code>
     * or <code>-test-javadoc</code> will be appended to the file name.
     *
     * @parameter expression="${project.build.finalName}"
     */
    private String finalName;

    /**
     * Specifies whether to attach the generated artifact to the project helper.
     * <br/>
     *
     * @parameter expression="${attach}" default-value="true"
     */
    private boolean attach;

    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     *
     * @parameter
     * @since 2.5
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Path to the default MANIFEST file to use. It will be used if
     * <code>useDefaultManifestFile</code> is set to <code>true</code>.
     *
     * @parameter expression="${project.build.outputDirectory}/META-INF/MANIFEST.MF"
     * @required
     * @readonly
     * @since 2.5
     */
    private File defaultManifestFile;

    /**
     * Set this to <code>true</code> to enable the use of the <code>defaultManifestFile</code>.
     * <br/>
     *
     * @parameter default-value="false"
     * @since 2.5
     */
    private boolean useDefaultManifestFile;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Skipping javadoc generation" );
            return;
        }

        File innerDestDir = this.destDir;
        if ( innerDestDir == null )
        {
            innerDestDir = new File( getOutputDirectory() );
        }

        if ( !( "pom".equals( project.getPackaging().toLowerCase( Locale.ENGLISH ) ) && isAggregator() ) )
        {
            ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
            if ( !"java".equals( artifactHandler.getLanguage() ) )
            {
                if ( getLog().isInfoEnabled() )
                {
                    getLog().info( "Not executing Javadoc as the project is not a Java classpath-capable package" );
                }

                return;
            }
        }

        try
        {
            executeReport( Locale.getDefault() );

            if ( innerDestDir.exists() )
            {
                File outputFile = generateArchive( innerDestDir, finalName + "-" + getClassifier() + ".jar" );

                if ( !attach )
                {
                    if ( getLog().isInfoEnabled() )
                    {
                        getLog().info( "NOT adding javadoc to attached artifacts list." );
                    }
                }
                else
                {
                    // TODO: these introduced dependencies on the project are going to become problematic - can we export it
                    //  through metadata instead?
                    projectHelper.attachArtifact( project, "javadoc", getClassifier(), outputFile );
                }
            }
        }
        catch ( ArchiverException e )
        {
            if ( failOnError )
            {
                throw new MojoExecutionException( "ArchiverException: Error while creating archive:"
                    + e.getMessage(), e );
            }

            getLog().error( "ArchiverException: Error while creating archive:" + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            if ( failOnError )
            {
                throw new MojoExecutionException( "IOException: Error while creating archive:" + e.getMessage(), e );
            }

            getLog().error( "IOException: Error while creating archive:" + e.getMessage(), e );
        }
        catch ( MavenReportException e )
        {
            if ( failOnError )
            {
                throw new MojoExecutionException( "MavenReportException: Error while creating archive:"
                                                  + e.getMessage(), e );
            }

            getLog().error( "MavenReportException: Error while creating archive:" + e.getMessage(), e );
        }
        catch ( RuntimeException e )
        {
            if ( failOnError )
            {
                throw e;
            }

            getLog().error( e.getMessage(), e );
        }
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    /**
     * @return the wanted classifier, i.e. <code>javadoc</code> or <code>test-javadoc</code>
     */
    protected String getClassifier()
    {
        return "javadoc";
    }

    // ----------------------------------------------------------------------
    // private methods
    // ----------------------------------------------------------------------

    /**
     * Method that creates the jar file
     *
     * @param javadocFiles the directory where the generated jar file will be put
     * @param jarFileName the filename of the generated jar file
     * @return a File object that contains the generated jar file
     * @throws ArchiverException if any
     * @throws IOException if any
     */
    private File generateArchive( File javadocFiles, String jarFileName )
        throws ArchiverException, IOException
    {
        File javadocJar = new File( jarOutputDirectory, jarFileName );

        if ( javadocJar.exists() )
        {
            javadocJar.delete();
        }

        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver( jarArchiver );
        archiver.setOutputFile( javadocJar );

        File contentDirectory = javadocFiles;
        if ( !contentDirectory.exists() )
        {
            getLog().warn( "JAR will be empty - no content was marked for inclusion!" );
        }
        else
        {
            archiver.getArchiver().addDirectory( contentDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES );
        }

        List<Resource> resources = project.getBuild().getResources();

        for ( Resource r : resources )
        {
            if ( r.getDirectory().endsWith( "maven-shared-archive-resources" ) )
            {
                archiver.getArchiver().addDirectory( new File( r.getDirectory() ) );
            }
        }

        if ( useDefaultManifestFile && defaultManifestFile.exists() && archive.getManifestFile() == null )
        {
            getLog().info( "Adding existing MANIFEST to archive. Found under: " + defaultManifestFile.getPath() );
            archive.setManifestFile( defaultManifestFile );
        }

        try
        {
            // we dont want Maven stuff
            archive.setAddMavenDescriptor( false );
            archiver.createArchive( project, archive );
        }
        catch ( ManifestException e )
        {
            throw new ArchiverException( "ManifestException: " + e.getMessage(), e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new ArchiverException( "DependencyResolutionRequiredException: " + e.getMessage(), e );
        }

        return javadocJar;
    }
}
