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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractJarSourceMojo
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
     * @parameter expression="${project.packaging}"
     * @readonly
     * @required
     */
    private String packaging;

    /**
     * @parameter expression="${executedProject}"
     * @required
     */
    private MavenProject executedProject;

    /**
     * @parameter expression="${attach}" default-value="true"
     */
    private boolean attach = true;

    /**
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}
     */
    private MavenProjectHelper projectHelper;

    private static SourceBundler sourceBundler;

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

    protected void validatePackaging()
    {
        if ( BANNED_PACKAGINGS.contains( packaging ) )
        {
            getLog().info( "NOT adding java-sources to attached artifacts for packaging: \'" + packaging + "\'." );

            return;
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
    protected File[] addDirectories( List compileSourceRoots, List resources, File[] sourceDirectories )
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
     * Get the test sources that will be included in the test sources jar file
     *
     * @return an array of File objects that contains the test source directories
     */
    protected File[] getTestSources()
    {
        List testCompileSourceRoots = getExecutedProject().getTestCompileSourceRoots();
        List testResources = getExecutedProject().getTestResources();

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
     * @throws Exception
     */
    protected void createJar( File outputFile, File[] sourceDirectories, Archiver archiver )
        throws Exception
    {
        if ( sourceBundler == null )
        {
            sourceBundler = new SourceBundler();
        }
        sourceBundler.makeSourceBundle( outputFile, sourceDirectories, archiver );
    }

    /**
     * Method to attach generated artifact to artifact list
     *
     * @param outputFile the artifact file to be attached
     */
    protected void attachArtifact( File outputFile )
    {
        if ( !attach )
        {
            getLog().info( "NOT adding java-sources to attached artifacts list." );
        }
        else
        {
            // TODO: these introduced dependencies on the project are going to become problematic - can we export it
            //  through metadata instead?
            projectHelper.attachArtifact( getProject(), "java-source", "sources", outputFile );
        }
    }

}
