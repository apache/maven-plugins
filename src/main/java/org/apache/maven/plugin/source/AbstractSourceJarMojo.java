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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
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
    private static final String[] DEFAULT_INCLUDES = new String[] { "**/*" };

    private static final String[] DEFAULT_EXCLUDES = new String[] {};

    /**
     * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     * 
     * @parameter
     * @since 2.1
     */
    private String[] includes;

    /**
     * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     * 
     * @parameter
     * @since 2.1
     */
    private String[] excludes;

    /**
     * Exclude commonly excluded files such as SCM configuration. These are defined in the plexus
     * FileUtils.getDefaultExcludes()
     * 
     * @parameter default-value="true"
     * @since 2.1
     */
    private boolean useDefaultExcludes;

    /**
     * The Maven Project Object
     * 
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * The archive configuration to use. See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
     * Archiver Reference</a>.
     * 
     * @parameter
     * @since 2.1
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Path to the default MANIFEST file to use. It will be used if <code>useDefaultManifestFile</code> is set to
     * <code>true</code>.
     *
     * @parameter default-value="${project.build.outputDirectory}/META-INF/MANIFEST.MF"
     * @required
     * @readonly
     * @since 2.1
     */
    private File defaultManifestFile;

    /**
     * Set this to <code>true</code> to enable the use of the <code>defaultManifestFile</code>. <br/>
     *
     * @parameter default-value="false"
     * @since 2.1
     */
    private boolean useDefaultManifestFile;

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
     *
     * @parameter default-value="${project.build.directory}"
     */
    protected File outputDirectory;

    /**
     * The filename to be used for the generated archive file.
     * For the source:jar goal, "-sources" is appended to this filename.
     * For the source:test-jar goal, "-test-sources" is appended.
     *
     * @parameter default-value="${project.build.finalName}"
     */
    protected String finalName;

    /**
     * Contains the full list of projects in the reactor.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    protected List reactorProjects;

    /**
     * Whether creating the archive should be forced.  If set to true, the jar will
     * always be created.  If set to false, the jar will only be created when the
     * sources are newer than the jar.
     *
     * @parameter expression="${source.forceCreation}" default-value="false"
     * @since 2.1
     */
    private boolean forceCreation;

    /**
     * A flag used to disable the source procedure. This is primarily intended for usage from the command line to
     * occasionally adjust the build.
     * 
     * @parameter expression="${source.skip}" default-value="false"
     * @since 2.2
     */
    private boolean skipSource;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( skipSource )
        {
            getLog().info( "Skipping source per configuration." );
            return;
        }

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
            packageSources( Arrays.asList( new Object[] { p } ) );
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

        MavenArchiver archiver = createArchiver();

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject subProject = getProject( (MavenProject) i.next() );

            if ( "pom".equals( subProject.getPackaging() ) )
            {
                continue;
            }

            archiveProjectContent( subProject, archiver.getArchiver() );
        }

        if(!archiver.getArchiver().getFiles().isEmpty()){
        
            if ( useDefaultManifestFile && defaultManifestFile.exists() && archive.getManifestFile() == null )
            {
                getLog().info( "Adding existing MANIFEST to archive. Found under: " + defaultManifestFile.getPath() );
                archive.setManifestFile( defaultManifestFile );
            }
    
            File outputFile = new File( outputDirectory, finalName + "-" + getClassifier() + getExtension() );
            
            try
            {
                archiver.setOutputFile( outputFile );
    
                archive.setAddMavenDescriptor( false );
                archive.setForced( forceCreation );
    
                archiver.createArchive( project, archive );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error creating source archive: " + e.getMessage(), e );
            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException( "Error creating source archive: " + e.getMessage(), e );
            }
            catch ( DependencyResolutionRequiredException e )
            {
                throw new MojoExecutionException( "Error creating source archive: " + e.getMessage(), e );
            }
            catch ( ManifestException e )
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
        else
        {
            getLog().info( "No sources in project. Archive not created." );
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
                addDirectory( archiver, sourceDirectory, getCombinedIncludes( null ), getCombinedExcludes( null ) );
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

            String[] combinedIncludes = getCombinedIncludes( resourceIncludes );

            List resourceExcludes = resource.getExcludes();

            String[] combinedExcludes = getCombinedExcludes( resourceExcludes );

            String targetPath = resource.getTargetPath();
            if ( targetPath != null )
            {
                if ( !targetPath.trim().endsWith( "/" ) )
                {
                    targetPath += "/";
                }
                addDirectory( archiver, sourceDirectory, targetPath, combinedIncludes, combinedExcludes );
            }
            else
            {
                addDirectory( archiver, sourceDirectory, combinedIncludes, combinedExcludes );
            }
        }
    }

    protected MavenArchiver createArchiver()
        throws MojoExecutionException
    {
        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver( jarArchiver );

        if ( project.getBuild() != null )
        {
            List resources = project.getBuild().getResources();

            for ( Iterator i = resources.iterator(); i.hasNext(); )
            {
                Resource r = (Resource) i.next();

                if ( r.getDirectory().endsWith( "maven-shared-archive-resources" ) )
                {
                    addDirectory( archiver.getArchiver(), new File( r.getDirectory() ), getCombinedIncludes( null ),
                                  getCombinedExcludes( null ) );
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

    /**
     * Combines the includes parameter and additional includes. Defaults to {@link #DEFAULT_INCLUDES} If the
     * additionalIncludes parameter is null, it is not added to the combined includes.
     * 
     * @param additionalIncludes The includes specified in the pom resources section
     * @return The combined array of includes.
     */
    private String[] getCombinedIncludes( List additionalIncludes )
    {
        ArrayList combinedIncludes = new ArrayList();

        if ( includes != null && includes.length > 0 )
        {
            combinedIncludes.addAll( Arrays.asList( includes ) );
        }

        if ( additionalIncludes != null && additionalIncludes.size() > 0 )
        {
            combinedIncludes.addAll( additionalIncludes );
        }

        // If there are no other includes, use the default.
        if ( combinedIncludes.size() == 0 )
        {
            combinedIncludes.addAll( Arrays.asList( DEFAULT_INCLUDES ) );
        }

        return (String[]) combinedIncludes.toArray( new String[combinedIncludes.size()] );
    }

    /**
     * Combines the user parameter {@link #excludes}, the default excludes from plexus FileUtils,
     * and the contents of the parameter addionalExcludes.
     * 
     * @param additionalExcludes Additional excludes to add to the array
     * @return The combined list of excludes.
     */

    private String[] getCombinedExcludes( List additionalExcludes )
    {
        ArrayList combinedExcludes = new ArrayList();

        if ( useDefaultExcludes )
        {
            combinedExcludes.addAll( FileUtils.getDefaultExcludesAsList() );
        }

        if ( excludes != null && excludes.length > 0 )
        {
            combinedExcludes.addAll( Arrays.asList( excludes ) );
        }

        if ( additionalExcludes != null && additionalExcludes.size() > 0 )
        {
            combinedExcludes.addAll( additionalExcludes );
        }

        if ( combinedExcludes.size() == 0 )
        {
            combinedExcludes.addAll( Arrays.asList( DEFAULT_EXCLUDES ) );
        }

        return (String[]) combinedExcludes.toArray( new String[combinedExcludes.size()] );
    }
}
