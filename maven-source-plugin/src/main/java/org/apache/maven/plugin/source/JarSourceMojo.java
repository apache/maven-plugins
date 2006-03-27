package org.apache.maven.plugin.source;

/*
 * Copyright 2005 The Apache Software Foundation.
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
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This plugin bundles all the generated sources into a jar archive.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 * @goal jar
 * @phase package
 * @execute phase="generate-sources"
 */
public class JarSourceMojo
    extends AbstractMojo
{
    /**
     * @deprecated ICK! This needs to be generalized OUTSIDE of this mojo!
     */
    private static final List BANNED_PACKAGINGS;

    static
    {
        List banned = new ArrayList();

        banned.add( "pom" );

        BANNED_PACKAGINGS = banned;
    }

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}
     */
    private MavenProjectHelper projectHelper;

    /**
     * @parameter expression="${project.packaging}"
     * @readonly
     * @required
     */
    private String packaging;

    /**
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * @parameter expression="${attach}" default-value="true"
     */
    private boolean attach = true;

    /**
     * @parameter expression="${executedProject}"
     * @required
     */
    private MavenProject executedProject;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter expression="${includeTestSources}" default="false"
     */
    private boolean includeTestSources;

    private Archiver archiver;

    private static SourceBundler sourceBundler;

    public void execute()
        throws MojoExecutionException
    {
        if ( BANNED_PACKAGINGS.contains( packaging ) )
        {
            getLog().info( "NOT adding java-sources to attached artifacts for packaging: \'" + packaging + "\'." );

            return;
        }
        // TODO: use a component lookup?
        archiver = new JarArchiver();
        sourceBundler = new SourceBundler();

        File outputFile = new File( outputDirectory, finalName + "-sources.jar" );
        List compileSourceRoots = executedProject.getCompileSourceRoots();
        List resources = executedProject.getResources();
        File[] sourceDirectories = new File[compileSourceRoots.size() + resources.size()];

        sourceDirectories = addDirectories( compileSourceRoots, resources, sourceDirectories );

        try
        {
            createJar( outputFile, sourceDirectories );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error building source JAR", e );
        }

        if ( includeTestSources )
        {
            try
            {
                createTestSourcesJar();
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Error building source JAR", e );
            }
        }

        if ( !attach )
        {
            getLog().info( "NOT adding java-sources to attached artifacts list." );
        }
        else
        {
            // TODO: these introduced dependencies on the project are going to become problematic - can we export it
            //  through metadata instead?
            projectHelper.attachArtifact( project, "java-source", "sources", outputFile );
        }
    }

    /**
     * Add the compile source directories and resource directories that will be included in the jar file
     *
     * @param compileSourceRoots
     * @param resources
     * @param sourceDirectories
     * @return an array of File objects that contains the directories that will be included in the jar file
     */
    private File[] addDirectories( List compileSourceRoots, List resources, File[] sourceDirectories )
    {
        int count = 0;
        for ( Iterator i = compileSourceRoots.iterator(); i.hasNext(); count++ )
        {
            sourceDirectories[count] = new File( (String) i.next() );
        }

        for ( Iterator i = resources.iterator(); i.hasNext(); count++ )
        {
            Resource resource = (Resource) i.next();
            sourceDirectories[count] = new File( resource.getDirectory() );
        }

        return sourceDirectories;
    }

    /**
     * Create jar file that will contain the test sources
     *
     * @throws Exception
     */
    private void createTestSourcesJar()
        throws Exception
    {
        File outputFile = new File( outputDirectory, finalName + "-test-sources.jar" );
        List testCompileSourceRoots = executedProject.getTestCompileSourceRoots();
        List testResources = executedProject.getTestResources();

        File[] testSourceDirectories = new File[testCompileSourceRoots.size() + testResources.size()];
        testSourceDirectories = addDirectories( testCompileSourceRoots, testResources, testSourceDirectories );
        archiver = new JarArchiver();

        createJar( outputFile, testSourceDirectories );
    }

    /**
     * Create jar file that contains the specified source directories
     *
     * @param outputFile        the file name of the jar
     * @param sourceDirectories the source directories that will be included in the jar file
     * @throws Exception
     */
    private void createJar( File outputFile, File[] sourceDirectories )
        throws Exception
    {
        sourceBundler.makeSourceBundle( outputFile, sourceDirectories, archiver );
    }

}
