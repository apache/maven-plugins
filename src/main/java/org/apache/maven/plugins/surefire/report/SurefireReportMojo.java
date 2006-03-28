package org.apache.maven.plugins.surefire.report;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.model.ReportPlugin;

import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.FileUtils;


/**
 * @description  Creates a nicely formatted Surefire Test Report in html format
 * @goal         report
 * @execute      phase="test" lifecycle="surefire"
 * @author       <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version      $Id$
 */
public class SurefireReportMojo
    extends AbstractMavenReport
{
    /**
     * Location where generated html will be created.
     *
     * @parameter expression="${project.build.directory}/site "
     *
     */
    private String outputDirectory;

    /**
     * Doxia Site Renderer
     *
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * Maven Project
     *
     * @parameter expression="${project}"
     * @required @readonly
     */
    private MavenProject project;

    /**
     * If set to <code>false</code>, only failures are shown.
     *
     * @parameter expression="${showSuccess}" default-value="true"
     * @required
     */
    private boolean showSuccess;

    /**
     * This directory contains the XML Report files that must be parsed and rendered to HTML format.
     *
     * @parameter expression="${project.build.directory}/surefire-reports"
     * @required
     */
    private File reportsDirectory;

    /**
     * The default filename to use for the report.
     *
     * @parameter expression="${outputName}" default-value="surefire-report"
     * @required
     */
    private String outputName;

    /**
     * Location of the Xrefs to link
     *
     * @parameter expression="${project.build.directory}/site/xref-test"
     */
    private String xrefLocation;

    /**
     * @parameter expression="${linkXref}" default-value="true"
     */
    private boolean linkXref;

    public void executeReport( Locale locale )
                       throws MavenReportException
    {
        SurefireReportGenerator report = new SurefireReportGenerator( reportsDirectory,
                                                                      locale, showSuccess,
                                                                      doXref() ? xrefLocation : null);

        try
        {
            report.doGenerateReport( getBundle( locale ),
                                     getSink(  ) );
        }
        catch ( Exception e )
        {
            throw new MavenReportException( "Failed to generate report", e );
        }
    }

    private boolean doXref()
    {
        List reportPlugins = getProject().getReportPlugins();

        boolean retValue = false;

        for( Iterator iter = reportPlugins.iterator(); iter.hasNext(); )
        {
            ReportPlugin plugin = ( ReportPlugin ) iter.next();

            if( plugin.getArtifactId().equals( "maven-jxr-plugin" ) ||
                plugin.getArtifactId().equals( "jxr-maven-plugin" ) )
            {
                retValue = true;
            }
        }
        return retValue;
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.surefire.name" );
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.surefire.description" );
    }

    protected SiteRenderer getSiteRenderer(  )
    {
        return siteRenderer;
    }

    protected MavenProject getProject(  )
    {
        return project;
    }

    public String getOutputName(  )
    {
        return outputName;
    }

    protected String getOutputDirectory(  )
    {
        return outputDirectory;
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "surefire-report",
                                         locale,
                                         this.getClass(  ).getClassLoader(  ) );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        // Only execute reports for java projects
        ArtifactHandler artifactHandler = this.project.getArtifact().getArtifactHandler();
        return "java".equals( artifactHandler.getLanguage() );
    }
}
