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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzerException;

/**
 * Analyzes the dependencies of this project and produces a report that summarizes which are: used and declared; used
 * and undeclared; unused and declared.
 * 
 * @version $Id$
 * @since 2.0-alpha-5
 * @goal analyze-report
 * @requiresDependencyResolution test
 * @execute phase="test-compile"
 */
public class AnalyzeReportMojo
    extends AbstractMavenReport
{
    // fields -----------------------------------------------------------------

    /**
     * The Maven project to analyze.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The Maven project dependency analyzer to use.
     * 
     * @component
     * @required
     * @readonly
     */
    private ProjectDependencyAnalyzer analyzer;

    /**
     * @component
     * @required
     * @readonly
     */
    private Renderer siteRenderer;

    /**
     * Target folder
     * 
     * @parameter expression="${project.build.directory}"
     * @readonly
     * @since 2.0-alpha-5
     */
    private File outputDirectory;

    /**
     * Ignore Runtime,Provide,Test,System scopes for unused dependency analysis
     * 
     * @parameter expression="${ignoreNonCompile}" default-value="false"
     */
    private boolean ignoreNonCompile;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        // Step 0: Checking pom availability
        if ( "pom".equals( project.getPackaging() ) )
        {
            getLog().info( "Skipping pom project" );
            return;
        }

        if ( outputDirectory == null || !outputDirectory.exists() )
        {
            getLog().info( "Skipping project with no Target directory" );
            return;
        }

        // Step 1: Analyze the project
        ProjectDependencyAnalysis analysis = null;
        try
        {
            analysis = analyzer.analyze( project );
        }
        catch ( ProjectDependencyAnalyzerException exception )
        {
            throw new MavenReportException( "Cannot analyze dependencies", exception );
        }


        //remove everything that's not in the compile scope
        if ( ignoreNonCompile )
        {
            Set<Artifact> filteredUnusedDeclared = new HashSet<Artifact>( analysis.getUnusedDeclaredArtifacts() );
            Iterator<Artifact> iter = filteredUnusedDeclared.iterator();
            while ( iter.hasNext() )
            {
                Artifact artifact = iter.next();
                if ( !artifact.getScope().equals( Artifact.SCOPE_COMPILE ) )
                {
                    iter.remove();
                }
            }
            
            ProjectDependencyAnalysis analysisTemp =
                new ProjectDependencyAnalysis( analysis.getUsedDeclaredArtifacts(),
                                               analysis.getUsedUndeclaredArtifacts(), filteredUnusedDeclared );
            analysis = analysisTemp;
        }
        
        
        // Step 2: Create sink and bundle
        Sink sink = getSink();
        ResourceBundle bundle = getBundle( locale );

        // Step 3: Generate the report
        AnalyzeReportView analyzethis = new AnalyzeReportView();
        analyzethis.generateReport( analysis, sink, bundle );
    }

    // MavenReport methods ----------------------------------------------------

    /*
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "dependency-analysis";
    }

    /*
     * @see org.apache.maven.reporting.AbstractMavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "analyze.report.name" );
    }

    /*
     * @see org.apache.maven.reporting.AbstractMavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "analyze.report.description" );
    }

    // AbstractMavenReport methods --------------------------------------------

    /*
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /*
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        getLog().info( outputDirectory.toString() );

        return outputDirectory.toString();
    }

    /*
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    // protected methods ------------------------------------------------------

    /**
     * @param locale the current locale
     */
    protected ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "analyze-report", locale, this.getClass().getClassLoader() );
    }
}
