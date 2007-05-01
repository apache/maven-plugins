package org.apache.maven.plugin.dependency;

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

import java.io.File;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzerException;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;


/**
 * This goal analyzes your project's dependencies and lists dependencies that
 * should be declared, but are not, and dependencies that are declared but
 * unused. It also executes the analyze-dep-mgt goal.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @goal analyze
 * @requiresDependencyResolution test
 * @execute phase="test-compile"
 * @since 2.0-alpha-3
 */
public class AnalyzeMojo
    extends AbstractMojo
{
    // fields -----------------------------------------------------------------

    /**
     * 
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Fail Build on problem
     * 
     * @parameter expression="${mdep.analyze.failBuild}" default-value="false"
     */
    private boolean failBuild = false;

    /**
     * Output used dependencies
     * 
     * @parameter expression="${mdep.analyze.displayUsed}" default-value="false"
     */
    private boolean displayUsed = false;

    /**
     * 
     * 
     * @parameter expression="${component.org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer}"
     * @required
     * @readonly
     */
    private ProjectDependencyAnalyzer analyzer;

    /**
     * Ignore Direct Dependency Overrides of dependencyManagement section.
     * 
     * @parameter expression="${mdep.analyze.ignore.direct}"
     *            default-value="true"
     */
    private boolean ignoreDirect = true;

    /**
     * Ignore Runtime,Provide,Test,System scopes for unused dependency analysis
     * 
     * @parameter expression="${mdep.analyze.ignore.noncompile}"
     *            default-value="true"
     */
    private boolean ignoreNonCompile = true;

    /**
     * Output the xml for the missing dependencies
     * @since 2.0-alpha-5
     * @parameter expression="${mdep.analyze.outputXML}" default-value="true"
     */
    private boolean outputXML = true;
    
    /**
     * Output scriptable values
     * @since 2.0-alpha-5
     * @parameter expression="${mdep.analyze.scriptable}" default-value="false"
     */
    private boolean scriptableOutput = false;
    
    /**
     * Flag to use for scriptable output
     * @since 2.0-alpha-5
     * @parameter expression="${mdep.analyze.flag}" default-value="$$$%%%"
     */
    private String scriptableFlag;
    
    /**
     * Flag to use for scriptable output
     * 
     * @parameter expression="${basedir}" 
     * @readonly
     * @since 2.0-alpha-5
     */
    private File baseDir;
    
    /**
     * Target folder
     * 
     * @parameter expression="${project.build.directory}"
     * @readonly
     * @since 2.0-alpha-5
     */
    protected File outputDirectory;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( "pom".equals( project.getPackaging() ) )
        {
            getLog().info( "Skipping pom project" );
            return;
        }
        
        if ( outputDirectory == null || !outputDirectory.exists())
        {
            getLog().info( "Skipping project with no Target directory" );
            return;
        }

        boolean result = checkDependencies();

        if ( result && this.failBuild )
        {
            throw new MojoExecutionException( "Found Dependency errors." );
        }

        // now do AnalyzeDepMgt (put this in a lifecycle later)
        AnalyzeDepMgt adm = new AnalyzeDepMgt();
        adm.setLog( getLog() );
        adm.setProject( this.project );
        adm.setFailBuild( this.failBuild );
        adm.setPluginContext( this.getPluginContext() );
        adm.setIgnoreDirect( this.ignoreDirect );
        adm.execute();
    }

    // private methods --------------------------------------------------------

    private boolean checkDependencies()
        throws MojoExecutionException
    {
        boolean result = false;
        try
        {
            ProjectDependencyAnalysis analysis = analyzer.analyze( project );

            if ( this.displayUsed )
            {
                getLog().info( "Used declared dependencies:" );

                logArtifacts( analysis.getUsedDeclaredArtifacts(), false );
            }

            getLog().info( "Used undeclared dependencies:" );

            Set usedUndeclared = analysis.getUsedUndeclaredArtifacts();
            logArtifacts( usedUndeclared, true );

            getLog().info( "Unused declared dependencies:" );

            Set unusedDeclared = analysis.getUnusedDeclaredArtifacts();

            if ( ignoreNonCompile )
            {
                Iterator iter = unusedDeclared.iterator();
                while ( iter.hasNext() )
                {
                    Artifact artifact = (Artifact) iter.next();
                    if ( !artifact.getScope().equals( Artifact.SCOPE_COMPILE ) )
                    {
                        iter.remove();
                    }
                }
            }
            logArtifacts( unusedDeclared, false );

            if ( outputXML )
            {
                writeDependencyXML( usedUndeclared );
            }
            if (scriptableOutput)
            {
                writeScriptableOutput( usedUndeclared );
            }

            if ( ( usedUndeclared != null && !usedUndeclared.isEmpty() ) || unusedDeclared != null
                && !unusedDeclared.isEmpty() )
            {
                getLog().warn( "Potential problems discovered." );
                result = true;
            }
        }
        catch ( ProjectDependencyAnalyzerException exception )
        {
            throw new MojoExecutionException( "Cannot analyze dependencies", exception );
        }

        return result;
    }

    private void logArtifacts( Set artifacts, boolean warn )
    {
        if ( artifacts.isEmpty() )
        {
            getLog().info( "   None" );
        }
        else
        {
            for ( Iterator iterator = artifacts.iterator(); iterator.hasNext(); )
            {
                Artifact artifact = (Artifact) iterator.next();
                
                //called because artifact will set the version to -SNAPSHOT only if I do this. MNG-2961
                artifact.isSnapshot();
                
                if ( warn )
                {
                    getLog().warn( "   " + artifact );
                }
                else
                {
                    getLog().info( "   " + artifact );
                }

            }
        }
    }

    private void writeDependencyXML( Set artifacts )
    {
        if ( !artifacts.isEmpty() )
        {
            getLog().info( "Add the following to your pom to correct the missing dependencies: " );

            StringWriter out = new StringWriter();
            PrettyPrintXMLWriter writer = new PrettyPrintXMLWriter( out );

            Iterator iter = artifacts.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = (Artifact) iter.next();

                //called because artifact will set the version to -SNAPSHOT only if I do this. MNG-2961
                artifact.isSnapshot();
                
                writer.startElement( "dependency" );
                writer.startElement( "groupId" );
                writer.writeText( artifact.getGroupId() );
                writer.endElement();
                writer.startElement( "artifactId" );
                writer.writeText( artifact.getArtifactId() );
                writer.endElement();
                writer.startElement( "version" );
                writer.writeText( artifact.getBaseVersion());
                writer.endElement();

                if ( !Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) )
                {
                    writer.startElement( "scope" );
                    writer.writeText( artifact.getScope() );
                    writer.endElement();
                }
                writer.endElement();
            }

            getLog().info( "\n" + out.getBuffer() );
        }
    }
    
   public void writeScriptableOutput(Set artifacts)
    {
       if ( !artifacts.isEmpty() )
       {
           getLog().info( "Missing dependencies: " );
           String pomFile = baseDir.getAbsolutePath()+File.separatorChar+"pom.xml";
           StringBuffer buf = new StringBuffer();
           Iterator iter = artifacts.iterator();
           while ( iter.hasNext() )
           {
               Artifact artifact = (Artifact) iter.next();
               
               //called because artifact will set the version to -SNAPSHOT only if I do this. MNG-2961
               artifact.isSnapshot();
               
               buf.append( scriptableFlag+":"+pomFile+":"+artifact.getDependencyConflictId()+":"+artifact.getClassifier()+":"+artifact.getBaseVersion()+":"+artifact.getScope()+"\n");
           }
           getLog().info( "\n" +buf);
       }
    }
}
