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
import java.util.Locale;
import java.util.ResourceBundle;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzerException;
import org.apache.maven.doxia.sink.Sink;

/**
 * This goal analyzes your project's dependencies and lists dependencies that
 * should be declared, but are not, and dependencies that are declared but
 * unused.It directly generate an HTML report.
 * 
 * 
 * @version $Id: AnalyzeReportMojo.java  2007-07-19 16:12:47Z  $
 * @since 2.0-alpha-5
 * @execute phase="test-compile"
 * @goal analyze-report
 * @requiresDependencyResolution test
 */

public class AnalyzeReportMojo
    extends AbstractMavenReport
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
     * 
     * 
     * @parameter expression="${component.org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer}"
     * @required
     * @readonly
     */
    private ProjectDependencyAnalyzer analyzer;

     
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
    public void executeReport(Locale locale)
        throws MavenReportException
    {
       
        // Step 0: Checking pom availability
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
                
        // Step 1: Analyse the project
        ProjectDependencyAnalysis analysis = null;
        try {
            analysis = analyzer.analyze( project );
          
        } catch ( ProjectDependencyAnalyzerException exception ) {
            throw new MavenReportException( "Cannot analyze dependencies", exception );
        }
                
                             
         // Step 2: Create sink and bundle
         Sink sink = getSink();
         ResourceBundle  bundle =  getBundle(locale);
         
         // Step 3: Generate the repport
         AnalyzeReportView analyzethis = new AnalyzeReportView();
         analyzethis.generateReport(analysis, sink, bundle);
    }

   
   
   
     protected MavenProject getProject()
    {
        return project;
    }
     
    protected String getOutputDirectory()
    {
       
        getLog().info(outputDirectory.toString());
  
        return outputDirectory.toString();
    }
    
     /**
     * @parameter expression="${component.org.apache.maven.doxia.siterenderer.Renderer}"
     * @required
     * @readonly
     */
    private Renderer siteRenderer;
    
    
        public String getName( Locale locale )
    {
        return "Analysis Dependencies" ;
    }

    public String getDescription( Locale locale )
    {
        return "Analysis dependencies of the project (used declared, used undeclared, unused declared)" ;
    }

    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }
    
        public String getOutputName()
    {
        return "Analysis Dependencies";
    }
  
        
   /**
   *
   * @see org.apache.maven.reporting.MavenReport#getOutputName()
   * @param locale the current locale
   */
    protected ResourceBundle getBundle( Locale locale ) {
    return ResourceBundle.getBundle( "analyze-report", locale, this.getClass().getClassLoader() );
    }

}
