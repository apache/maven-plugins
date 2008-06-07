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
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates documentation for the <code>Java Test code</code> in the project using the standard
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
    // Javadoc Options
    // ----------------------------------------------------------------------

    /**
     * Specifies the title to be placed near the top of the overview summary file.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#doctitle">doctitle</a>.
     *
     * @parameter expression="${doctitle}" default-value="${project.name} ${project.version} Test API"
     */
    private String doctitle;

    /**
     * Specifies that javadoc should retrieve the text for the overview documentation from the "source" file
     * specified by path/filename and place it on the Overview page (overview-summary.html).
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#overview">overview</a>.
     *
     * @parameter expression="${overview}" default-value="${basedir}/src/test/javadoc/overview.html"
     */
    private File overview;

    /**
     * Specifies the title to be placed in the HTML title tag.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#windowtitle">windowtitle</a>.
     *
     * @parameter expression="${windowtitle}" default-value="${project.name} ${project.version} Test API"
     */
    private String windowtitle;

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Specifies the destination directory where test javadoc saves the generated HTML files.
     *
     * @parameter expression="${project.reporting.outputDirectory}/testapidocs"
     * @required
     */
    private File reportOutputDirectory;

    /**
     * The name of the destination directory.
     *
     * @parameter expression="${destDir}" default-value="testapidocs"
     */
    private String destDir;

    /**
     * Specifies the test Javadoc ressources directory to be included in the Javadoc (i.e. package.html, images...).
     *
     * @parameter expression="${basedir}/src/test/javadoc"
     */
    private File javadocDirectory;

    // ----------------------------------------------------------------------
    // Report Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The name of the test Javadoc report.
     *
     * @parameter expression="${name}"
     */
    private String name;

    /**
     * The description of the test Javadoc report.
     *
     * @parameter expression="${description}"
     */
    private String description;

    // ----------------------------------------------------------------------
    // Report public methods
    // ----------------------------------------------------------------------

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        if ( StringUtils.isEmpty( name ) )
        {
            return getBundle( locale ).getString( "report.test-javadoc.name" );
        }

        return name;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        if ( StringUtils.isEmpty( description ) )
        {
            return getBundle( locale ).getString( "report.test-javadoc.description" );
        }

        return description;
    }

    /**
     * @see org.apache.maven.plugin.javadoc.JavadocReport#getOutputName()
     */
    public String getOutputName()
    {
        return destDir + "/index";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getReportOutputDirectory()
     */
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
        if ( ( reportOutputDirectory != null ) && ( !reportOutputDirectory.getAbsolutePath().endsWith( destDir ) ) )
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
    // ----------------------------------------------------------------------

    /**
     * @see org.apache.maven.plugin.javadoc.AbstractJavadocMojo#getProjectBuildOutputDirs(org.apache.maven.project.MavenProject)
     */
    protected List getProjectBuildOutputDirs( MavenProject p )
    {
        List dirs = new ArrayList();
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

    /**
     * @see org.apache.maven.plugin.javadoc.AbstractJavadocMojo#getProjectSourceRoots(org.apache.maven.project.MavenProject)
     */
    protected List getProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getPackaging().toLowerCase() ) )
        {
            return Collections.EMPTY_LIST;
        }

        return p.getTestCompileSourceRoots();
    }

    /**
     * @see org.apache.maven.plugin.javadoc.AbstractJavadocMojo#getExecutionProjectSourceRoots(org.apache.maven.project.MavenProject)
     */
    protected List getExecutionProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getExecutionProject().getPackaging().toLowerCase() ) )
        {
            return Collections.EMPTY_LIST;
        }

        return p.getExecutionProject().getTestCompileSourceRoots();
    }

    /**
     * @see org.apache.maven.plugin.javadoc.AbstractJavadocMojo#getProjectArtifacts(org.apache.maven.project.MavenProject)
     */
    protected List getProjectArtifacts( MavenProject p )
    {
        return p.getTestArtifacts();
    }

    /**
     * @see org.apache.maven.plugin.javadoc.AbstractJavadocMojo#getJavadocDirectory()
     */
    protected File getJavadocDirectory()
    {
        return javadocDirectory;
    }

    /**
     * @see org.apache.maven.plugin.javadoc.AbstractJavadocMojo#getDoctitle()
     */
    protected String getDoctitle()
    {
        return doctitle;
    }

    /**
     * @see org.apache.maven.plugin.javadoc.AbstractJavadocMojo#getOverview()
     */
    protected File getOverview()
    {
        return overview;
    }

    /**
     * @see org.apache.maven.plugin.javadoc.AbstractJavadocMojo#getWindowtitle()
     */
    protected String getWindowtitle()
    {
        return windowtitle;
    }

    /**
     * @see org.apache.maven.plugin.javadoc.AbstractJavadocMojo#getCompileArtifacts(org.apache.maven.artifact.resolver.ArtifactResolutionResult)
     */
    protected List getCompileArtifacts( ArtifactResolutionResult result )
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
}
