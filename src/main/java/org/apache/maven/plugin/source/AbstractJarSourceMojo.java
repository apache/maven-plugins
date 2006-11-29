package org.apache.maven.plugin.source;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractJarSourceMojo
    extends AbstractMojo
{
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/*",};

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.packaging}"
     * @readonly
     * @required
     */
    protected String packaging;

    /**
     * The project where the plugin is currently being executed.
     * The default value is populated from maven.
     *
     * @parameter expression="${executedProject}"
     * @required
     */
    private MavenProject executedProject;

    /**
     * Specifies whether or not to attach the artifact to the project
     *
     * @parameter expression="${attach}" default-value="true"
     */
    private boolean attach = true;

    /** @component */
    private MavenProjectHelper projectHelper;

    /**
     * The directory where the generated archive file will be put.
     * Defaults to ${project.build.directory} specified in the pom or
     * inherited from the super pom.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * The filename to be used for the generated archive file.
     * For the source:jar goal, "-sources" is appended to this filename.
     * For the source:test-jar goal, "-test-sources" is appended.
     * Defaults to ${project.build.finalName} specified in the pom
     * or inherited from the super pom.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    protected String finalName;

    /** @see org.apache.maven.plugin.AbstractMojo#execute() */
    public abstract void execute()
        throws MojoExecutionException;

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public MavenProject getExecutedProject()
    {
        return executedProject;
    }

    public void setExecutedProject( MavenProject executedProject )
    {
        this.executedProject = executedProject;
    }

    /**
     * Add the compile source directories and resource directories that will be included in the jar file
     *
     * @param compileSourceRoots
     * @param resources
     * @param sourceDirectories
     * @return an array of File objects that contains the directories that will be included in the jar file
     */
    protected File[] addDirectories( List compileSourceRoots,
                                     List resources,
                                     File[] sourceDirectories )
    {
        int count = 0;
        for ( Iterator i = compileSourceRoots.iterator(); i.hasNext(); )
        {
            sourceDirectories[count] = new File( (String) i.next() );
            count++;
        }

        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();
            sourceDirectories[count] = new File( resource.getDirectory() );
            count++;
        }

        return sourceDirectories;
    }

    /**
     * Get the test sources that will be included in the test sources jar file
     *
     * @return an array of File objects that contains the test source directories
     */
    protected File[] getTestSources()
    {
        List testCompileSourceRoots = executedProject.getTestCompileSourceRoots();
        List testResources = executedProject.getTestResources();

        File[] testSourceDirectories = new File[testCompileSourceRoots.size() + testResources.size()];
        testSourceDirectories = addDirectories( testCompileSourceRoots, testResources, testSourceDirectories );

        return testSourceDirectories;
    }

    /**
     * Get the main sources that will be included in the jar file
     *
     * @return an array of File objects that contains the source directories
     */
    protected File[] getDefaultSources()
    {
        List compileSourceRoots = executedProject.getCompileSourceRoots();
        List resources = executedProject.getResources();

        File[] sourceDirectories = new File[compileSourceRoots.size() + resources.size()];
        sourceDirectories = addDirectories( compileSourceRoots, resources, sourceDirectories );

        return sourceDirectories;
    }

    /**
     * Create jar file that contains the specified source directories
     *
     * @param outputFile        the file name of the jar
     * @param sourceDirectories the source directories that will be included in the jar file
     */
    protected void createJar( File outputFile,
                              File[] sourceDirectories,
                              Archiver archiver )
        throws IOException, ArchiverException
    {
        makeSourceBundle( outputFile, sourceDirectories, archiver );
    }

    /**
     * Method to attach generated artifact to artifact list
     *
     * @param outputFile the artifact file to be attached
     * @param classifier
     */
    protected void attachArtifact( File outputFile,
                                   String classifier )
    {
        if ( !attach )
        {
            getLog().info( "NOT adding java-sources to attached artifacts list." );
        }
        else
        {
            // TODO: these introduced dependencies on the project are going to become problematic - can we export it
            //  through metadata instead?
            projectHelper.attachArtifact( project, "java-source", classifier, outputFile );
        }
    }

    /**
     * Method to create an archive of the specified files
     *
     * @param outputFile        the destination file of the generated archive
     * @param sourceDirectories the directory where the files to be archived are located
     * @param archiver          the archiver object that will create the archive
     * @throws ArchiverException
     * @throws IOException
     */
    protected void makeSourceBundle( File outputFile,
                                     File[] sourceDirectories,
                                     Archiver archiver )
        throws ArchiverException, IOException
    {
        String[] includes = DEFAULT_INCLUDES;

        for ( int i = 0; i < sourceDirectories.length; i++ )
        {
            if ( sourceDirectories[i].exists() )
            {
                archiver.addDirectory( sourceDirectories[i], includes, FileUtils.getDefaultExcludes() );
            }
        }

        archiver.setDestFile( outputFile );

        archiver.createArchive();
    }

    protected Archiver createArchiver()
        throws ArchiverException
    {
        Archiver archiver = new JarArchiver();

        if ( project.getBuild() != null )
        {
            List resources = project.getBuild().getResources();

            for ( Iterator i = resources.iterator(); i.hasNext(); )
            {
                Resource r = (Resource) i.next();

                if ( r.getDirectory().endsWith( "maven-shared-archive-resources" ) )
                {
                    archiver.addDirectory( new File( r.getDirectory() ) );
                }
            }

            //archiver.setDotFileDirectory( new File( project.getBuild().getDirectory() ) );
        }

        return archiver;
    }
}
