package org.apache.maven.plugins.jar;

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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;

/**
 * Base class for creating a jar from project classes.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public abstract class AbstractJarMojo
    extends AbstractMojo
{

    private static final String[] DEFAULT_EXCLUDES = new String[] { "**/package.html" };

    private static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };

    /**
     * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     */
    @Parameter
    private String[] includes;

    /**
     * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     */
    @Parameter
    private String[] excludes;

    /**
     * Directory containing the generated JAR.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true )
    private File outputDirectory;

    /**
     * Name of the generated JAR.
     */
    @Parameter( defaultValue = "${project.build.finalName}", readonly = true )
    private String finalName;

    /**
     * The Jar archiver.
     */
    @Component( role = Archiver.class, hint = "jar" )
    private JarArchiver jarArchiver;

    /**
     * The {@link {MavenProject}.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * The {@link MavenSession}.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    /**
     * The archive configuration to use. See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
     * Archiver Reference</a>.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Using this property will fail your build cause it has been removed from the plugin configuration. See the
     * <a href="https://maven.apache.org/plugins/maven-jar-plugin/">Major Version Upgrade to version 3.0.0</a> for the
     * plugin.
     * 
     * @deprecated For version 3.0.0 this parameter is only defined here to break the build if you use it!
     */
    @Parameter( property = "jar.useDefaultManifestFile", defaultValue = "false" )
    private boolean useDefaultManifestFile;

    /**
     *
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Require the jar plugin to build a new JAR even if none of the contents appear to have changed. By default, this
     * plugin looks to see if the output jar exists and inputs have not changed. If these conditions are true, the
     * plugin skips creation of the jar. This does not work when other plugins, like the maven-shade-plugin, are
     * configured to post-process the jar. This plugin can not detect the post-processing, and so leaves the
     * post-processed jar in place. This can lead to failures when those plugins do not expect to find their own output
     * as an input. Set this parameter to <tt>true</tt> to avoid these problems by forcing this plugin to recreate the
     * jar every time.<br/>
     * Starting with <b>3.0.0</b> the property has been renamed from <code>jar.forceCreation</code> to
     * <code>maven.jar.forceCreation</code>.
     */
    @Parameter( property = "maven.jar.forceCreation", defaultValue = "false" )
    private boolean forceCreation;

    /**
     * Skip creating empty archives.
     */
    @Parameter( defaultValue = "false" )
    private boolean skipIfEmpty;

    /**
     * Return the specific output directory to serve as the root for the archive.
     * @return get classes directory.
     */
    protected abstract File getClassesDirectory();

    /**
     * @return the {@link #project}
     */
    protected final MavenProject getProject()
    {
        return project;
    }

    /**
     * Overload this to produce a jar with another classifier, for example a test-jar.
     * @return get the classifier.
     */
    protected abstract String getClassifier();

    /**
     * Overload this to produce a test-jar, for example.
     * @return return the type.
     */
    protected abstract String getType();

    /**
     * Returns the Jar file to generate, based on an optional classifier.
     *
     * @param basedir the output directory
     * @param resultFinalName the name of the ear file
     * @param classifier an optional classifier
     * @return the file to generate
     */
    protected File getJarFile( File basedir, String resultFinalName, String classifier )
    {
        if ( basedir == null )
        {
            throw new IllegalArgumentException( "basedir is not allowed to be null" );
        }
        if ( resultFinalName == null )
        {
            throw new IllegalArgumentException( "finalName is not allowed to be null" );
        }

        StringBuilder fileName = new StringBuilder( resultFinalName );

        if ( hasClassifier() )
        {
            fileName.append( "-" ).append( classifier );
        }

        fileName.append( ".jar" );

        return new File( basedir, fileName.toString() );
    }

    /**
     * Generates the JAR.
     * @return The instance of File for the created archive file.
     * @throws MojoExecutionException in case of an error.
     */
    public File createArchive()
        throws MojoExecutionException
    {
        File jarFile = getJarFile( outputDirectory, finalName, getClassifier() );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        archive.setForced( forceCreation );

        try
        {
            File contentDirectory = getClassesDirectory();
            if ( !contentDirectory.exists() )
            {
                getLog().warn( "JAR will be empty - no content was marked for inclusion!" );
            }
            else
            {
                archiver.getArchiver().addDirectory( contentDirectory, getIncludes(), getExcludes() );
            }

            archiver.createArchive( session, project, archive );

            return jarFile;
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error assembling JAR", e );
        }
    }

    /**
     * Generates the JAR.
     * @throws MojoExecutionException in case of an error.
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( useDefaultManifestFile )
        {
            throw new MojoExecutionException( "You are using 'useDefaultManifestFile' which has been removed"
                + " from the maven-jar-plugin. "
                + "Please see the >>Major Version Upgrade to version 3.0.0<< on the plugin site." );
        }

        if ( skipIfEmpty && ( !getClassesDirectory().exists() || getClassesDirectory().list().length < 1 ) )
        {
            getLog().info( "Skipping packaging of the " + getType() );
        }
        else
        {
            File jarFile = createArchive();

            if ( hasClassifier() )
            {
                projectHelper.attachArtifact( getProject(), getType(), getClassifier(), jarFile );
            }
            else
            {
                if ( projectHasAlreadySetAnArtifact() )
                {
                    throw new MojoExecutionException( "You have to use a classifier "
                        + "to attach supplemental artifacts to the project instead of replacing them." );
                }
                getProject().getArtifact().setFile( jarFile );
            }
        }
    }

    private boolean projectHasAlreadySetAnArtifact()
    {
        if ( getProject().getArtifact().getFile() != null )
        {
            return getProject().getArtifact().getFile().isFile();
        }
        else
        {
            return false;
        }
    }

    /**
     * @return true in case where the classifier is not {@code null} and contains something else than white spaces.
     */
    protected boolean hasClassifier()
    {
        boolean result = false;
        if ( getClassifier() != null && getClassifier().trim().length() > 0 )
        {
            result = true;
        }

        return result;
    }

    private String[] getIncludes()
    {
        if ( includes != null && includes.length > 0 )
        {
            return includes;
        }
        return DEFAULT_INCLUDES;
    }

    private String[] getExcludes()
    {
        if ( excludes != null && excludes.length > 0 )
        {
            return excludes;
        }
        return DEFAULT_EXCLUDES;
    }
}
