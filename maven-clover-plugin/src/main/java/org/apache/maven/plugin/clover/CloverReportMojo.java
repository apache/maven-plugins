package org.apache.maven.plugin.clover;

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

import com.cenqua.clover.reporters.html.HtmlReporter;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Generate a <a href="http://cenqua.com/clover">Clover</a> report.
 * The generated report is an external report generated  by Clover itself.
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 * @goal clover
 * @execute phase="test" lifecycle="clover"
 */
public class CloverReportMojo
    extends AbstractMavenReport
{
    // TODO: Need some way to share config elements and code between report mojos and main build
    // mojos. See http://jira.codehaus.org/browse/MNG-1886
    
    /**
     * The location of the <a href="http://cenqua.com/clover/doc/adv/database.html">Clover database</a>.
     * 
     * @parameter expression="${project.build.directory}/clover/clover.db"
     * @required
     */
    private String cloverDatabase;

    /**
     * The directory where the Clover report will be generated.
     * 
     * @parameter expression="${project.reporting.outputDirectory}/clover"
     * @required
     */
    private File outputDirectory;

    /**
     * When the Clover Flush Policy is set to "interval" or threaded this value is the minimum 
     * period between flush operations (in milliseconds).
     *
     * @parameter default-value="500"
     */
    protected int flushInterval;

    /**
     * If true we'll wait 2*flushInterval to ensure coverage data is flushed to the Clover 
     * database before running any query on it. 
     * 
     * Note: The only use case where you would want to turn this off is if you're running your 
     * tests in a separate JVM. In that case the coverage data will be flushed by default upon
     * the JVM shutdown and there would be no need to wait for the data to be flushed. As we
     * can't control whether users want to fork their tests or not, we're offering this parameter
     * to them.  
     * 
     * @parameter default-value="true"
     */
    protected boolean waitForFlush;
    
    /**
     * @component
     */
    private SiteRenderer siteRenderer;

    /**
     * The Maven project. 
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        // Only execute reports for java projects
        ArtifactHandler artifactHandler = this.project.getArtifact().getArtifactHandler();
        if ( !"java".equals( artifactHandler.getLanguage() ) )
        {
            getLog().debug( "Not generating a Clover report as this is not a Java project." );
            return;
        }

        AbstractCloverMojo.waitForFlush( this.waitForFlush, this.flushInterval );
        
        int result = HtmlReporter.mainImpl( createCliArgs() );
        if ( result != 0 )
        {
            throw new MavenReportException( "Clover has failed to instrument the source files" );
        }
    }

    /**
     * @return the CLI args to be passed to the reporter
     * @todo handle multiple source roots. At the moment only the first source root is instrumented
     */
    private String[] createCliArgs()
    {
        return new String[] {
            "-t", "Maven Clover report", 
            "-p", (String) this.project.getCompileSourceRoots().get( 0 ),
            "-i", this.cloverDatabase, 
            "-o", this.outputDirectory.getPath() };
    }

    public String getOutputName()
    {
        return "clover/index";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.clover.description" );
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "clover-report", locale, CloverReportMojo.class.getClassLoader() );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return this.outputDirectory.getAbsoluteFile().toString();
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return this.siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return this.project;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.clover.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#generate(org.codehaus.doxia.sink.Sink, java.util.Locale)
     */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        executeReport( locale );
    }

    public boolean isExternalReport()
    {
        return true;
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
