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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.javadoc.resolver.SourceResolverConfig;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates documentation for the <code>Java Test code</code> in an <b>NON aggregator</b> project using the standard
 * <a href="http://java.sun.com/j2se/javadoc/">Javadoc Tool</a>.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.3
 * @goal test-javadoc
 * @execute phase=generate-test-sources
 * @requiresDependencyResolution test
 * @see <a href="http://java.sun.com/j2se/javadoc/">Javadoc Tool</a>
 * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#options">Javadoc Options </a>
 */
public class TestJavadocReport
    extends JavadocReport
{
    // ----------------------------------------------------------------------
    // Javadoc Options (should be inline with options defined in TestJavadocJar)
    // ----------------------------------------------------------------------

    /**
     * Specifies the Test title to be placed near the top of the overview summary file.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#doctitle">doctitle</a>.
     * <br/>
     *
     * @parameter expression="${testDoctitle}" alias="doctitle"
     * default-value="${project.name} ${project.version} Test API"
     * @since 2.5
     */
    private String testDoctitle;

    /**
     * Specifies that Javadoc should retrieve the text for the Test overview documentation from the "source" file
     * specified by path/filename and place it on the Overview page (overview-summary.html).
     * <br/>
     * <b>Note</b>: could be in conflict with &lt;nooverview/&gt;.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#overview">overview</a>.
     * <br/>
     *
     * @parameter expression="${testOverview}" alias="overview"
     * default-value="${basedir}/src/test/javadoc/overview.html"
     * @since 2.5
     */
    private File testOverview;

    /**
     * Specifies the Test title to be placed in the HTML title tag.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#windowtitle">windowtitle</a>.
     * <br/>
     *
     * @parameter expression="${testWindowtitle}" alias="windowtitle"
     * default-value="${project.name} ${project.version} Test API"
     * @since 2.5
     */
    private String testWindowtitle;

    // ----------------------------------------------------------------------
    // Mojo Parameters (should be inline with options defined in TestJavadocJar)
    // ----------------------------------------------------------------------

    /**
     * Specifies the destination directory where test Javadoc saves the generated HTML files.
     *
     * @parameter expression="${reportTestOutputDirectory}" default-value="${project.reporting.outputDirectory}/testapidocs"
     * @required
     */
    private File reportOutputDirectory;

    /**
     * The name of the destination directory.
     * <br/>
     *
     * @parameter expression="${destDir}" default-value="testapidocs"
     */
    private String destDir;

    /**
     * Specifies the Test Javadoc resources directory to be included in the Javadoc (i.e. package.html, images...).
     * <br/>
     * Could be used in addition of <code>docfilessubdirs</code> parameter.
     * <br/>
     * See <a href="#docfilessubdirs">docfilessubdirs</a>.
     *
     * @parameter expression="${basedir}/src/test/javadoc" alias="javadocDirectory"
     * @since 2.5
     */
    private File testJavadocDirectory;

    // ----------------------------------------------------------------------
    // Report Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The name of the Test Javadoc report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     *
     * @parameter expression="${testName}" alias="name"
     * @since 2.5
     */
    private String testName;

    /**
     * The description of the Test Javadoc report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     *
     * @parameter expression="${testDescription}" alias="description"
     * @since 2.5
     */
    private String testDescription;

    // ----------------------------------------------------------------------
    // Report public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    protected void executeReport( Locale unusedLocale )
        throws MavenReportException
    {
        addMainJavadocLink();

        super.executeReport( unusedLocale );
    }

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        if ( StringUtils.isEmpty( testName ) )
        {
            return getBundle( locale ).getString( "report.test-javadoc.name" );
        }

        return testName;
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        if ( StringUtils.isEmpty( testDescription ) )
        {
            return getBundle( locale ).getString( "report.test-javadoc.description" );
        }

        return testDescription;
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return destDir + "/index";
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

    public void setDestDir( String destDir )
    {
        this.destDir = destDir;
        updateReportOutputDirectory( reportOutputDirectory, destDir );
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

    // ----------------------------------------------------------------------
    // Protected methods
    // Important Note: should be inline with methods defined in TestJavadocJar
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    protected List<String> getProjectBuildOutputDirs( MavenProject p )
    {
        List<String> dirs = new ArrayList<String>();
        if ( StringUtils.isNotEmpty( p.getBuild().getOutputDirectory() ) )
        {
            dirs.add( p.getBuild().getOutputDirectory() );
        }
        if ( StringUtils.isNotEmpty( p.getBuild().getTestOutputDirectory() ) )
        {
            dirs.add( p.getBuild().getTestOutputDirectory() );
        }

        return dirs;
    }

    /** {@inheritDoc} */
    protected List<String> getProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getPackaging().toLowerCase() ) )
        {
            return Collections.emptyList();
        }

        return ( p.getTestCompileSourceRoots() == null ? Collections.EMPTY_LIST
                        : new LinkedList<String>( p.getTestCompileSourceRoots() ) );
    }

    /** {@inheritDoc} */
    protected List<String> getExecutionProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getExecutionProject().getPackaging().toLowerCase() ) )
        {
            return Collections.emptyList();
        }

        return ( p.getExecutionProject().getTestCompileSourceRoots() == null ? Collections.EMPTY_LIST
                        : new LinkedList<String>( p.getExecutionProject().getTestCompileSourceRoots() ) );
    }

    /** {@inheritDoc} */
    protected List<Artifact> getProjectArtifacts( MavenProject p )
    {
        return ( p.getTestArtifacts() == null ? Collections.EMPTY_LIST
                        : new LinkedList<Artifact>( p.getTestArtifacts() ) );
    }

    /** {@inheritDoc} */
    protected File getJavadocDirectory()
    {
        return testJavadocDirectory;
    }

    /** {@inheritDoc} */
    protected String getDoctitle()
    {
        return testDoctitle;
    }

    /** {@inheritDoc} */
    protected File getOverview()
    {
        return testOverview;
    }

    /** {@inheritDoc} */
    protected String getWindowtitle()
    {
        return testWindowtitle;
    }

    /** {@inheritDoc} */
    protected List<Artifact> getCompileArtifacts( ArtifactResolutionResult result )
    {
        return JavadocUtil.getCompileArtifacts( result.getArtifacts(), true );
    }

    /**
     * Gets the resource bundle for the specified locale.
     *
     * @param locale The locale of the currently generated report.
     * @return The resource bundle for the requested locale.
     */
    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "test-javadoc-report", locale, getClass().getClassLoader() );
    }

    /**
     * Add the <code>../apidocs</code> to the links parameter so Test report could be linked to the Main report.
     */
    private void addMainJavadocLink()
    {
        if ( links == null )
        {
            links = new ArrayList<String>();
        }

        // TODO the prerequisite is that the main report is in apidocs
        File apidocs = new File( getReportOutputDirectory().getParentFile(), "apidocs" );
        if ( apidocs.isDirectory() && !links.contains( "../apidocs" ) )
        {
            links.add( "../apidocs" );
        }
    }
    
    /**
     * Overriden to enable the resolution of -test-sources jar files.
     * 
     * {@inheritDoc}
     */
    protected SourceResolverConfig configureDependencySourceResolution( final SourceResolverConfig config )
    {
        return super.configureDependencySourceResolution( config ).withoutCompileSources().withTestSources();
    }
}
