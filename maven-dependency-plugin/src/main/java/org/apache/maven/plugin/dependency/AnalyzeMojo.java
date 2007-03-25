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

import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzerException;

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
     * @parameter expression="${mdep.analyze.failBuild}"
     */
    private boolean failBuild = false;

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
     * @parameter expression="${mdep.analyze.ignore.direct}" default-value="true"
     */
    private boolean ignoreDirect = true;

    /**
     * Ignore Runtime,Provide,Test,System scopes for unused dependency analysis
     * 
     * @parameter expression="${mdep.analyze.ignore.noncompile}" default-value="true"
     */
    private boolean ignoreNonCompile = true;

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

            getLog().info( "Used declared dependencies:" );

            logArtifacts( analysis.getUsedDeclaredArtifacts(), false );

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
            if ( warn )
            {
                getLog().warn( "   None" );
            }
            else
            {
                getLog().info( "   None" );
            }
        }
        else
        {
            for ( Iterator iterator = artifacts.iterator(); iterator.hasNext(); )
            {
                Artifact artifact = (Artifact) iterator.next();
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
}
