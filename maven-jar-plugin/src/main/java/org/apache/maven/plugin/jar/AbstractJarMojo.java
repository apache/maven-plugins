package org.apache.maven.plugin.jar;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
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

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/package.html"};

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    /**
     * Directory containing the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Name of the generated JAR.
     *
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * The Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
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
     * The maven archiver to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Get the directory where the classes to be jarred are (the root of the archive)
     * 
     * @return the directory
     */
    protected abstract File getClassesDirectory();

    /**
     * Set the directory where the classes to be jarred are
     * 
     * @param classesDirectory the directory
     */
    protected abstract void setClassesDirectory( File classesDirectory );

    /**
     * Set {@link JarArchiver} used to create the archive
     * 
     * @param jarArchiver
     */
    protected void setJarArchiver( JarArchiver jarArchiver )
    {
        this.jarArchiver = jarArchiver;
    }

    /**
     * Get {@link JarArchiver} used to create the archive
     * 
     * @return the archiver
     */
    protected JarArchiver getJarArchiver()
    {
        return jarArchiver;
    }

    /**
     * Get the {@link MavenProject} that will be used to gather the data needed
     * 
     * @return the maven project
     */
    protected final MavenProject getProject()
    {
        return project;
    }

    /**
     * Set the {@link MavenProject} whose data will be used in the archive
     * 
     * @param project
     */
    protected void setMavenProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * Overload this to produce a test-jar, for example.
     */
    protected abstract String getClassifier();

    protected static File getJarFile( File basedir, String finalName, String classifier )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + ".jar" );
    }

    /**
     * Generates the JAR. If contentDirectory does not exist the JAR won't be created.
     *
     * @todo Add license files in META-INF directory.
     * @return the JAR file or <code>null</code> if contentDirectory does not exist
     */
    public File createArchive()
        throws MojoExecutionException
    {
        File jarFile = getJarFile( outputDirectory, finalName, getClassifier() );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( getJarArchiver() );

        archiver.setOutputFile( jarFile );

        try
        {
            File contentDirectory = getClassesDirectory();

            if ( !contentDirectory.exists() )
            {
                getLog().warn( "Directory " + contentDirectory + " does not exist, not creating JAR file." );

                return null;
            }

            archiver.getArchiver().addDirectory( contentDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES );

            archiver.createArchive( project, archive );

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
     *
     * @todo Add license files in META-INF directory.
     */
    public void execute()
        throws MojoExecutionException
    {
        File jarFile = createArchive();

        if ( jarFile != null )
        {
            String classifier = getClassifier();
            if ( classifier != null )
            {
                projectHelper.attachArtifact( getProject(), "jar", classifier, jarFile );
            }
            else
            {
                getProject().getArtifact().setFile( jarFile );
            }
        }
    }
}
