package org.apache.maven.plugin.javadoc;

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
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates documentation for the <code>Java code</code> in an <b>NON aggregator</b> project using the standard
 * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/javadoc/">Javadoc Tool</a>.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.0
 * @see <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/javadoc/">Javadoc Tool</a>
 * @see <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#options">Javadoc Options</a>
 */
@Mojo( name = "javadoc", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true )
@Execute( phase = LifecyclePhase.GENERATE_SOURCES )
public class JavadocReport
    extends AbstractJavadocMojo
    implements MavenReport
{
    // ----------------------------------------------------------------------
    // Report Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Specifies the destination directory where javadoc saves the generated HTML files.
     */
    @Parameter( property = "reportOutputDirectory", defaultValue = "${project.reporting.outputDirectory}/apidocs",
                required = true )
    private File reportOutputDirectory;

    /**
     * The name of the destination directory.
     * <br/>
     *
     * @since 2.1
     */
    @Parameter( property = "destDir", defaultValue = "apidocs" )
    private String destDir;

    /**
     * The name of the Javadoc report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     *
     * @since 2.1
     */
    @Parameter( property = "name" )
    private String name;

    /**
     * The description of the Javadoc report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     *
     * @since 2.1
     */
    @Parameter( property = "description" )
    private String description;

    // ----------------------------------------------------------------------
    // Report public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        if ( StringUtils.isEmpty( name ) )
        {
            return getBundle( locale ).getString( "report.javadoc.name" );
        }

        return name;
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        if ( StringUtils.isEmpty( description ) )
        {
            return getBundle( locale ).getString( "report.javadoc.description" );
        }

        return description;
    }

    /** {@inheritDoc} */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        outputDirectory = getReportOutputDirectory();

        try
        {
            executeReport( locale );
        }
        catch ( MavenReportException e )
        {
            if ( failOnError )
            {
                throw e;
            }
            getLog().error( "Error while creating javadoc report: " + e.getMessage(), e );
        }
        catch ( RuntimeException e )
        {
            if ( failOnError )
            {
                throw e;
            }
            getLog().error( "Error while creating javadoc report: " + e.getMessage(), e );
        }
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return destDir + "/index";
    }

    /** {@inheritDoc} */
    public boolean isExternalReport()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <br/>
     * The logic is the following:
     * <table>
     *   <tbody>
     *     <tr>
     *       <th> isAggregator </th>
     *       <th> hasSourceFiles </th>
     *       <th> isRootProject </th>
     *       <th> Generate Report </th>
     *     </tr>
     *     <tr>
     *       <td>True</td>
     *       <td>True</td>
     *       <td>True</td>
     *       <td>True</td>
     *     </tr>
     *     <tr>
     *       <td>True</td>
     *       <td>True</td>
     *       <td>False</td>
     *       <td>False</td>
     *     </tr>
     *     <tr>
     *       <td>True</td>
     *       <td>False</td>
     *       <td>True</td>
     *       <td>False</td>
     *     </tr>
     *     <tr>
     *       <td>True</td>
     *       <td>False</td>
     *       <td>False</td>
     *       <td>False</td>
     *     </tr>
     *     <tr>
     *       <td>False</td>
     *       <td>True</td>
     *       <td>True</td>
     *       <td>True</td>
     *     </tr>
     *     <tr>
     *       <td>False</td>
     *       <td>True</td>
     *       <td>False</td>
     *       <td>True</td>
     *     </tr>
     *     <tr>
     *        <td>False</td>
     *        <td>False</td>
     *        <td>True</td>
     *        <td>False</td>
     *      </tr>
     *      <tr>
     *        <td>False</td>
     *        <td>False</td>
     *        <td>False</td>
     *        <td>False</td>
     *      </tr>
     *    </tbody>
     *  </table>
     */
    public boolean canGenerateReport()
    {
        boolean canGenerate = false;

        if ( !this.isAggregator() || ( this.isAggregator() && this.project.isExecutionRoot() ) )
        {
            List<String> sourcePaths;
            List<String> files;
            try
            {
                sourcePaths = getSourcePaths();
                files = getFiles( sourcePaths );
            }
            catch ( MavenReportException e )
            {
                getLog().error( e.getMessage(), e );
                return false;
            }

            canGenerate = canGenerateReport( files );
        }
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( " canGenerateReport = " + canGenerate + " for project " + this.project );
        }
        return canGenerate;
    }

    /** {@inheritDoc} */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_REPORTS;
    }

    /** {@inheritDoc} */
    public File getReportOutputDirectory()
    {
        if ( reportOutputDirectory == null )
        {
            return outputDirectory;
        }

        return reportOutputDirectory;
    }

    /**
     * Method to set the directory where the generated reports will be put
     *
     * @param reportOutputDirectory the directory file to be set
     */
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        updateReportOutputDirectory( reportOutputDirectory, destDir );
    }

    /**
     * @param theDestDir The destiation directory.
     */
    public void setDestDir( String theDestDir )
    {
        this.destDir = theDestDir;
        updateReportOutputDirectory( reportOutputDirectory, theDestDir );
    }

    private void updateReportOutputDirectory( File reportOutputDirectory, String destDir )
    {
        if ( reportOutputDirectory != null && destDir != null
             && !reportOutputDirectory.getAbsolutePath().endsWith( destDir ) )
        {
            this.reportOutputDirectory = new File( reportOutputDirectory, destDir );
        }
        else
        {
            this.reportOutputDirectory = reportOutputDirectory;
        }
    }

    /** {@inheritDoc} */
    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "Skipping javadoc generation" );
            return;
        }

        try
        {
            RenderingContext context = new RenderingContext( outputDirectory, getOutputName() + ".html" );
            SiteRendererSink sink = new SiteRendererSink( context );
            Locale locale = Locale.getDefault();
            generate( sink, locale );
        }
        catch ( MavenReportException e )
        {
            failOnError( "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation", e );
        }
        catch ( RuntimeException e )
        {
            failOnError( "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation", e );
        }
    }

    /**
     * Gets the resource bundle for the specified locale.
     *
     * @param locale The locale of the currently generated report.
     * @return The resource bundle for the requested locale.
     */
    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "javadoc-report", locale, getClass().getClassLoader() );
    }
}
