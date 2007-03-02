package org.apache.maven.plugin.source;

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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractSourceJarMojo
    extends AbstractMojo
{
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/*",};

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * Specifies whether or not to attach the artifact to the project
     *
     * @parameter expression="${attach}" default-value="true"
     */
    private boolean attach;

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

    /** @parameter expression="${reactorProjects}" */
    protected List reactorProjects;

    protected abstract String getClassifier();

    protected abstract List getSources( MavenProject project );

    protected abstract List getResources( MavenProject project );

    /** @see org.apache.maven.plugin.AbstractMojo#execute() */
    public void execute()
        throws MojoExecutionException
    {
        packageSources( project );
    }

    protected void packageSources( MavenProject project )
        throws MojoExecutionException
    {
        if ( !"pom".equals( project.getPackaging() ) )
        {
            packageSources( Arrays.asList( new Object[]{project} ) );
        }
    }

    protected void packageSources( List projects )
        throws MojoExecutionException
    {
        if ( project.getArtifact().getClassifier() != null )
        {
            getLog().warn( "NOT adding sources to artifacts with classifier as Maven only supports one classifier " +
                "per artifact. Current artifact [" + project.getArtifact().getId() + "] has a [" +
                project.getArtifact().getClassifier() + "] classifier." );
        }
        else
        {
            Archiver archiver = createArchiver();

            for ( Iterator i = projects.iterator(); i.hasNext(); )
            {
                MavenProject project = getProject( (MavenProject) i.next() );

                if ( "pom".equals( project.getPackaging() ) )
                {
                    continue;
                }

                archiveProjectContent( project, archiver );
            }

            File outputFile = new File( outputDirectory, finalName + "-" + getClassifier() + ".jar" );

            try
            {
                archiver.setDestFile( outputFile );

                archiver.createArchive();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error creating source archive: " + e.getMessage(), e );
            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException( "Error creating source archive: " + e.getMessage(), e );
            }

            if ( attach )
            {
                projectHelper.attachArtifact( project, "java-source", getClassifier(), outputFile );
            }
            else
            {
                getLog().info( "NOT adding java-sources to attached artifacts list." );
            }
        }
    }

    protected void archiveProjectContent( MavenProject project,
                                          Archiver archiver )
        throws MojoExecutionException
    {
        for ( Iterator i = getSources( project ).iterator(); i.hasNext(); )
        {
            String s = (String) i.next();

            File sourceDirectory = new File( s );

            if ( sourceDirectory.exists() )
            {
                addDirectory( archiver, sourceDirectory, DEFAULT_INCLUDES, FileUtils.getDefaultExcludes() );
            }
        }

        //MAPI: this should be taken from the resources plugin
        for ( Iterator i = getResources( project ).iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();

            File sourceDirectory = new File( resource.getDirectory() );

            if ( sourceDirectory.exists() )
            {
                List resourceIncludes = resource.getIncludes();

                String includes[];

                if ( resourceIncludes == null || resourceIncludes.size() == 0 )
                {
                    includes = DEFAULT_INCLUDES;
                }
                else
                {
                    includes = (String[]) resourceIncludes.toArray( new String[resourceIncludes.size()] );
                }

                List resourceExcludes = resource.getExcludes();

                String[] excludes;

                if ( resourceExcludes == null || resourceExcludes.size() == 0 )
                {
                    excludes = FileUtils.getDefaultExcludes();
                }
                else
                {
                    excludes = (String[]) resourceExcludes.toArray( new String[resourceExcludes.size()] );
                }

                addDirectory( archiver, sourceDirectory, includes, excludes );
            }
        }
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
    }

    protected Archiver createArchiver()
        throws MojoExecutionException
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
                    addDirectory( archiver, new File( r.getDirectory() ), new String[]{}, new String[]{} );
                }
            }
        }

        return archiver;
    }

    protected void addDirectory( Archiver archiver,
                                 File sourceDirectory,
                                 String[] includes,
                                 String[] excludes )
        throws MojoExecutionException
    {
        try
        {
            archiver.addDirectory( sourceDirectory, includes, excludes );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error adding directory to source archive.", e );
        }
    }

    protected MavenProject getProject( MavenProject project )
    {
        if ( project.getExecutionProject() != null )
        {
            return project.getExecutionProject();
        }
        else
        {
            return project;
        }
    }
}
