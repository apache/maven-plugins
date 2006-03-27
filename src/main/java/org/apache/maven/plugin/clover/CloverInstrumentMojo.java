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
package org.apache.maven.plugin.clover;

import com.cenqua.clover.CloverInstr;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Instrument source roots.
 *
 * Note: Do not call this MOJO directly. It is meant to be called in a forked lifecycle by the other MOJOs.
 *
 * @goal instrument
 * @phase generate-sources
 * @requiresDependencyResolution test
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverInstrumentMojo extends AbstractCloverMojo
{
    /**
     * @parameter
     * @required
     */
    private String cloverOutputDirectory;

    /**
     * The location of the <a href="http://cenqua.com/clover/doc/adv/database.html">Clover database</a>.
     * 
     * @parameter
     * @required
     */
    private String cloverDatabase;

    /**
     * @parameter expression="${plugin.artifacts}"
     * @required
     */
    private List pluginArtifacts;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory factory;

    private String cloverOutputSourceDirectory;

    public void execute()
        throws MojoExecutionException
    {
        if ( shouldExecute() )
        {
            new File( this.cloverOutputDirectory ).mkdirs();
            this.cloverOutputSourceDirectory = new File( this.cloverOutputDirectory, "src" ).getPath();

            super.execute();

            instrumentSources();
            addCloverDependencyToCompileClasspath();
            redirectSourceDirectories();
            redirectOutputDirectories();
        }
    }

    private boolean shouldExecute()
    {
        boolean shouldExecute = true;
        
        // Only execute reports for java projects
        ArtifactHandler artifactHandler = this.project.getArtifact().getArtifactHandler();
        File srcDir = new File(this.project.getBuild().getSourceDirectory());

        if ( !"java".equals( artifactHandler.getLanguage() ) )
        {
            getLog().debug( "Not executing Clover as this is not a Java project." );
            shouldExecute = false;
        }
        else if ( !srcDir.exists() )
        {
            getLog().debug("No sources found - No Clover instrumentation done");
            shouldExecute = false;
        }

        return shouldExecute;
    }
    
    private void instrumentSources() throws MojoExecutionException
    {
        int result = CloverInstr.mainImpl( createCliArgs() );
        if ( result != 0 )
        {
            throw new MojoExecutionException( "Clover has failed to instrument the source files" );
        }
    }

    private void redirectOutputDirectories()
    {
        // Explicitely set the output directory to be the Clover one so that all other plugins executing
        // thereafter output files in the Clover output directory and not in the main output directory.
        this.project.getBuild().setDirectory( this.cloverOutputDirectory );

        // TODO: Ulgy hack below. Changing the directory should be enough for changing the values of all other
        // properties depending on it!
        this.project.getBuild().setOutputDirectory( new File( this.cloverOutputDirectory, "classes" ).getPath() );
        this.project.getBuild().setTestOutputDirectory(
            new File( this.cloverOutputDirectory, "test-classes" ).getPath() );
    }

    private void redirectSourceDirectories()
    {
        String oldSourceDirectory = this.project.getBuild().getSourceDirectory();

        this.project.getBuild().setSourceDirectory( this.cloverOutputSourceDirectory );
        
        // Maven2 limitation: changing the source directory doesn't change the compile source roots
        // See http://jira.codehaus.org/browse/MNG-1945
        List sourceRoots = this.project.getCompileSourceRoots();
        for (int i = 0; i < sourceRoots.size(); i++)
        {
            String sourceRoot = (String) this.project.getCompileSourceRoots().get( i );
            if (sourceRoot.equals(oldSourceDirectory))
            {
                this.project.getCompileSourceRoots().remove( i );

                // Note: Ideally we should add the new compile source root at the same place as the
                // one we're removing but there's no API for this...
                this.project.addCompileSourceRoot( this.project.getBuild().getSourceDirectory() );
            }
        }
    }
    
    private void addCloverDependencyToCompileClasspath()
        throws MojoExecutionException
    {
        Artifact cloverArtifact = null;
        Iterator artifacts = this.pluginArtifacts.iterator();
        while ( artifacts.hasNext() && cloverArtifact == null )
        {
            Artifact artifact = (Artifact) artifacts.next();
            if ( "clover".equalsIgnoreCase( artifact.getArtifactId() ) )
            {
                cloverArtifact = artifact;
            }
        }

        if ( cloverArtifact == null )
        {
            throw new MojoExecutionException( "Couldn't find 'clover' artifact in plugin dependencies" );
        }

        cloverArtifact = factory.createArtifact( cloverArtifact.getGroupId(), cloverArtifact.getArtifactId(),
                                                 cloverArtifact.getVersion(), Artifact.SCOPE_COMPILE,
                                                 cloverArtifact.getType() );

        // TODO: use addArtifacts
        Set set = new HashSet( this.project.getDependencyArtifacts() );
        set.add( cloverArtifact );
        this.project.setDependencyArtifacts( set );
    }

    /**
     * @return the CLI args to be passed to CloverInstr
     * @todo handle multiple source roots. At the moment only the first source root is instrumented
     */
    private String[] createCliArgs() throws MojoExecutionException
    {
        List parameters = new ArrayList();
     
        parameters.add( "-p" );
        parameters.add( this.flushPolicy );
        parameters.add( "-f" );
        parameters.add( "" + this.flushInterval );

        parameters.add( "-i" );
        parameters.add( this.cloverDatabase );
        parameters.add( "-s" );

        // TODO: Allow support for several source roots in the future.
        parameters.add( (String) this.project.getCompileSourceRoots().get( 0 ) );

        parameters.add( "-d" );
        parameters.add( this.cloverOutputSourceDirectory );

        if ( this.jdk != null )
        {
            if ( this.jdk.equals( "1.4" ) )
            {
                parameters.add( "-jdk14" );
            }
            else if ( this.jdk.equals( "1.5" ) )
            {
                parameters.add( "-jdk15" );
            }
            else
            {
                throw new MojoExecutionException("Unsupported jdk version [" + this.jdk 
                    + "]. Valid values are [1.4] and [1.5]");
            }
        }
        
        getLog().debug( "Instrumenting using parameters [" + parameters.toString() + "]");
        
        return (String[]) parameters.toArray(new String[0]);
    }
}
