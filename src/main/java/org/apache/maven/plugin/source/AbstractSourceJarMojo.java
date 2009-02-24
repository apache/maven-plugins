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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for bundling sources into a jar archive.
 *
 * @version $Id$
 * @since 2.0.3
 */
public abstract class AbstractSourceJarMojo
    extends AbstractMojo
{
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/*"};

    /**
     * The Maven Project Object
     *
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

    /**
     * Specifies whether or not to exclude resources from the sources-jar. This
     * can be convenient if your project includes large resources, such as
     * images, and you don't want to include them in the sources-jar.
     *
     * @parameter expression="${source.excludeResources}" default-value="false"
     * @since 2.0.4
     */
    protected boolean excludeResources;

    /**
     * Specifies whether or not to include the POM file in the sources-jar.
     *
     * @parameter expression="${source.includePom}" default-value="false"
     * @since 2.1
     */
    protected boolean includePom;

    /**
     * Used for attaching the source jar to the project.
     *
     * @component
     */
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

    /**
     * Contains the full list of projects in the reactor.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    protected List reactorProjects;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        packageSources( project );
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    /**
     * @return the wanted classifier, ie <code>sources</code> or <code>test-sources</code>
     */
    protected abstract String getClassifier();

    /**
     * @param p not null
     * @return the compile or test sources
     */
    protected abstract List getSources( MavenProject p )
        throws MojoExecutionException;

    /**
     * @param p not null
     * @return the compile or test resources
     */
    protected abstract List getResources( MavenProject p )
        throws MojoExecutionException;

    protected void packageSources( MavenProject p )
        throws MojoExecutionException
    {
        if ( !"pom".equals( p.getPackaging() ) )
        {
            packageSources( Arrays.asList( new Object[]{p} ) );
        }
    }

    protected void packageSources( List projects )
        throws MojoExecutionException
    {
        if ( project.getArtifact().getClassifier() != null )
        {
            getLog().warn( "NOT adding sources to artifacts with classifier as Maven only supports one classifier "
                + "per artifact. Current artifact [" + project.getArtifact().getId() + "] has a ["
                + project.getArtifact().getClassifier() + "] classifier." );

            return;
        }

        Archiver archiver = createArchiver();

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject subProject = getProject( (MavenProject) i.next() );

            if ( "pom".equals( subProject.getPackaging() ) )
            {
                continue;
            }

            archiveProjectContent( subProject, archiver );
        }

        File outputFile = new File( outputDirectory, finalName + "-" + getClassifier() + getExtension() );
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
            projectHelper.attachArtifact( project, getType(), getClassifier(), outputFile );
        }
        else
        {
            getLog().info( "NOT adding java-sources to attached artifacts list." );
        }
    }

    protected void archiveProjectContent( MavenProject p, Archiver archiver )
        throws MojoExecutionException
    {
        if ( includePom )
        {
            try
            {
                archiver.addFile( p.getFile(), p.getFile().getName() );
            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException( "Error adding POM file to target jar file.", e );
            }
        }

        for ( Iterator i = getSources( p ).iterator(); i.hasNext(); )
        {
            String s = (String) i.next();

            File sourceDirectory = new File( s );

            if ( sourceDirectory.exists() )
            {
                addDirectory( archiver, sourceDirectory, DEFAULT_INCLUDES, FileUtils.getDefaultExcludes() );
            }
        }

        //MAPI: this should be taken from the resources plugin
        for ( Iterator i = getResources( p ).iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();

            File sourceDirectory = new File( resource.getDirectory() );

            if ( !sourceDirectory.exists() )
            {
                continue;
            }

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
                List allExcludes = new ArrayList();
                allExcludes.addAll( FileUtils.getDefaultExcludesAsList() );
                allExcludes.addAll( resourceExcludes );
                excludes = (String[]) allExcludes.toArray( new String[allExcludes.size()] );
            }

            String targetPath = resource.getTargetPath();
            if ( targetPath != null )
            {
                if ( !targetPath.trim().endsWith( "/" ) )
                {
                    targetPath += "/";
                }
                addDirectory( archiver, sourceDirectory, targetPath, includes, excludes );
            }
            else
            {
                addDirectory( archiver, sourceDirectory, includes, excludes );
            }
        }
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
                    addDirectory( archiver, new File( r.getDirectory() ), DEFAULT_INCLUDES, new String[]{} );
                }
            }
        }

        return archiver;
    }

    protected void addDirectory( Archiver archiver, File sourceDirectory, String[] includes, String[] excludes )
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

    protected void addDirectory( Archiver archiver, File sourceDirectory, String prefix, String[] includes,
                                 String[] excludes )
        throws MojoExecutionException
    {
        try
        {
            archiver.addDirectory( sourceDirectory, prefix, includes, excludes );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error adding directory to source archive.", e );
        }
    }

    protected String getExtension()
    {
        return ".jar";
    }

    protected MavenProject getProject( MavenProject p )
    {
        if ( p.getExecutionProject() != null )
        {
            return p.getExecutionProject();
        }

        return p;
    }

    protected String getType()
    {
        return "java-source";
    }
}
